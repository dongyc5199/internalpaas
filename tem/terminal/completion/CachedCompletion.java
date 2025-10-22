package com.waveterm.demo.terminal.completion;

class CachedCompletion {

    private final CompletionResponse response;
    private final long storedAt;

    CachedCompletion(CompletionResponse response, long storedAt) {
        this.response = response;
        this.storedAt = storedAt;
    }

    CompletionResponse response() {
        return response;
    }

    boolean isExpired(long ttlMillis) {
        return ttlMillis > 0 && System.currentTimeMillis() - storedAt >= ttlMillis;
    }
}
