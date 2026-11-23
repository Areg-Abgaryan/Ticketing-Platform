/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.reservation.adapter.in.web;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * Repeats a whole transactional call that lost a race with a concurrent request. The conflict is only detected
 * when the transaction commits, i.e. after the service method has returned, so the retry has to wrap the call
 * from outside. Each attempt starts a fresh transaction and re-reads the current state.
 */
final class ConflictRetry {

    private static final int MAX_BACKOFF_MILLIS = 20;

    private ConflictRetry() {
    }

    /** Runs {@code action}, trying again (up to {@code maxAttempts} times in total) if it hits a write conflict. */
    static <T> T run(int maxAttempts, Supplier<T> action) {
        for (int attempt = 1; ; attempt++) {
            try {
                return action.get();
            } catch (OptimisticLockingFailureException | DataIntegrityViolationException conflict) {
                if (attempt >= maxAttempts) {
                    throw conflict;
                }
                pause();
            }
        }
    }

    /** A short random pause so that competing requests do not collide again in lockstep. */
    private static void pause() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(1, MAX_BACKOFF_MILLIS + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
