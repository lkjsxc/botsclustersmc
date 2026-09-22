# Troubleshooting the clone-to-train path

Run `./scripts/doctor.sh`. Preserve the working directory and existing state.
Do not fix a failure by deleting weights/worlds, lowering the requested bot count
silently, disabling ownership/integrity checks, or using an old unrelated binary.

## First build

Use Linux x86_64, Java 21 **JDK** (including javac and jar), Bash, git, curl,
ca-certificates, a C toolchain, pkg-config, cmake, unzip and util-linux. See the
installation command in README.ja.md. Initial compilation needs 20 GiB free.
The project pins nightly-2026-02-04 and Azalea's exact revision; do not replace
them with whatever Rust/Minecraft happens to be newest. Internet is required
for first use. Retry the same start command after correcting DNS, proxy, TLS or
storage problems. Completed dependency downloads and compiler output are reused.

Missing `runtime/host-lib/gson.jar` no longer requires a ZIP: startup fetches the
pinned Maven artifact automatically. A cached but incorrect jar is rejected;
preserve it for diagnosis instead of bypassing the checksum. A missing
`pins/folia.lock` means the checkout is incomplete. Restore that tracked file.

`JAVA_BIN` must identify Java 21; its sibling javac/jar must exist. System glibc
is used when compiling locally; no imported glibc-2.39 binary is required.
Do not replace your system libc manually. Windows/macOS hosts should run this
on a Linux server/VM rather than executing the Linux scripts natively.

## Runtime

TCP25565 must be free. Stop the other Minecraft server yourself; startup never
kills an unrelated process. A cgroup smaller than Java heap plus native headroom
is rejected. The recommended allocation is 16 CPU / 12 GiB RAM / 120 GB.
If deliberately testing a 4 GiB environment, use `JAVA_HEAP_GB=2` and
`FOLIA_THREADS=2`, but this is not a guarantee of long-term capacity.

`BOT_PREFIX` must have 1..13 ASCII letters/digits/underscores. Use `bcmc`, not the
14-character repository name. Prefix defaults agree in Java, Rust and `.env`.
If a bridge/reset error occurs, inspect `academy/.runtime/lab/fatal.txt` and
`academy/logs/folia.err.log`, `folia.out.log`, and `bots.err.log`.
Bot joining starts only after real server protocol and every cell are verified.

## Persistence and progress

An existing unowned `academy/`, changed population, symlink component, or mismatched
checkpoint pair is refused. Use a fresh clone for a new independent run; keep old
installations. To retain training, restore an entire stopped compatible Academy.
Never assume `foundations_passed` certifies continued mastery or cooperation.

Movement is not evidence of an optimizer update. Check `policy_version`,
`trained_samples`, `optimizer_steps`, and `logs/learning.csv`. Early failed
attempts are expected with random initialization. Promotion requires every bot
to pass frozen current-task and per-skill retention exams. There is no time-based
shortcut. The 18 RL-next research contracts are not selectable playable stages.

`./stop.sh` requests shutdown; wait for the original start terminal to report
`Clean shutdown: model, curriculum and world saved.` before restarting.
`./scripts/diagnostics.sh` excludes .env/worlds/weights, but review names and IPs
in logs before publishing the resulting diagnostic archive.
