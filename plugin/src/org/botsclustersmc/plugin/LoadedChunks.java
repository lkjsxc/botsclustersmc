package org.botsclustersmc.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Keep async chunk completion and its first owned use in the same callback when possible. */
public final class LoadedChunks {
    private LoadedChunks() {}

    public static void use(JavaPlugin plugin, Location location, Consumer<Chunk> action,
                           Consumer<Throwable> failure) {
        Location at = location.clone();
        Objects.requireNonNull(at.getWorld(), "world");
        at.checkFinite();
        new Request(plugin, at, action, failure).load();
    }

    private static final class Request {
        private final JavaPlugin plugin;
        private final Location at;
        private final Consumer<Chunk> action;
        private final Consumer<Throwable> failure;
        private final AtomicBoolean done = new AtomicBoolean();
        private int attempts;

        Request(JavaPlugin plugin, Location at, Consumer<Chunk> action, Consumer<Throwable> failure) {
            this.plugin = plugin;
            this.at = at;
            this.action = action;
            this.failure = failure;
        }

        void load() {
            if (done.get()) return;
            if (!plugin.isEnabled()) { fail(new IllegalStateException("Plugin disabled during chunk load")); return; }
            if (++attempts > 8) { fail(new IllegalStateException("Chunk did not remain loaded on its owner: " + at)); return; }
            try {
                at.getWorld().getChunkAtAsync(at.getBlockX() >> 4, at.getBlockZ() >> 4, true)
                    .whenComplete((chunk, error) -> {
                        if (error != null) { fail(error); return; }
                        try {
                            if (Bukkit.isOwnedByCurrentRegion(at)) useLoaded(chunk);
                            else Bukkit.getRegionScheduler().run(plugin, at, task -> useLoaded(chunk));
                        } catch (Throwable e) { fail(e); }
                    });
            } catch (Throwable e) { fail(e); }
        }

        void useLoaded(Chunk chunk) {
            if (done.get()) return;
            try {
                if (!plugin.isEnabled()) throw new IllegalStateException("Plugin disabled during chunk handoff");
                if (!Bukkit.isOwnedByCurrentRegion(at)) throw new IllegalStateException("Chunk callback is not on its owner");
                if (!at.getWorld().isChunkLoaded(at.getBlockX() >> 4, at.getBlockZ() >> 4)) {
                    // The async load's temporary ticket can expire before a scheduled handoff.
                    // Request another async load; never force a synchronous load or spin on a tick.
                    Bukkit.getRegionScheduler().run(plugin, at, task -> load());
                    return;
                }
                if (done.compareAndSet(false, true)) {
                    try { action.accept(Objects.requireNonNull(chunk, "loaded chunk")); }
                    catch (Throwable e) { failure.accept(e); }
                }
            } catch (Throwable e) { fail(e); }
        }

        void fail(Throwable error) {
            if (done.compareAndSet(false, true)) failure.accept(error);
        }
    }
}
