package org.botsclustersmc.core;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/** A single request's reply slot. Poll never waits; discarded tickets are never reused. */
public final class InferenceTicket {
    private final long request;
    private final CompletableFuture<InferencePool.Result> completion=new CompletableFuture<>();

    public InferenceTicket(long request) { this.request=request; }
    public void deliver(InferencePool.Result result) {
        Objects.requireNonNull(result);
        if(result.request()!=request) {
            fail(new IllegalArgumentException("Inference reply identity differs from its ticket"));
            return;
        }
        completion.complete(result);
    }
    public void fail(Throwable cause) { completion.completeExceptionally(Objects.requireNonNull(cause)); }
    public InferencePool.Result poll() { return completion.getNow(null); }
    public void cancel() { completion.cancel(false); }
}
