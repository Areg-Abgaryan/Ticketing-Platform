/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.reservation.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

class ConflictRetryTest {

    private final AtomicInteger calls = new AtomicInteger();

    private ObjectOptimisticLockingFailureException versionConflict() {
        return new ObjectOptimisticLockingFailureException("TicketStockEntity", "id");
    }

    @Test
    void returnsTheResultWithoutRetryWhenNothingFails() {
        final String result = ConflictRetry.run(3, () -> {
            calls.incrementAndGet();
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(calls).hasValue(1);
    }

    @Test
    void retriesAfterVersionConflictsUntilItSucceeds() {
        final String result = ConflictRetry.run(3, () -> {
            if (calls.incrementAndGet() < 3) {
                throw versionConflict();
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
        assertThat(calls).hasValue(3);
    }

    @Test
    void retriesAfterUniqueConstraintViolation() {
        final String result = ConflictRetry.run(2, () -> {
            if (calls.incrementAndGet() == 1) {
                throw new DataIntegrityViolationException("duplicate key");
            }
            return "ok";
        });

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void givesUpAfterTheMaximumNumberOfAttempts() {
        assertThatThrownBy(() -> ConflictRetry.run(3, () -> {
            calls.incrementAndGet();
            throw versionConflict();
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        assertThat(calls).hasValue(3);
    }

    @Test
    void doesNotRetryOtherFailures() {
        assertThatThrownBy(() -> ConflictRetry.run(3, () -> {
            calls.incrementAndGet();
            throw new IllegalStateException("not a conflict");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(calls).hasValue(1);
    }
}
