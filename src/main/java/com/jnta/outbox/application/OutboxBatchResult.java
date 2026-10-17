package com.jnta.outbox.application;

public record OutboxBatchResult(int processedCount, int failedCount) {
}
