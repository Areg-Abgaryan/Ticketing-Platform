/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReservationTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(10);

    private final UUID eventId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private Reservation newReservation() {
        return Reservation.hold(eventId, userId, 2, NOW, TTL);
    }

    @Test
    void newReservationIsHeldAndExpiresAfterTtl() {
        final Reservation reservation = newReservation();

        assertThat(reservation.status()).isEqualTo(ReservationStatus.HELD);
        assertThat(reservation.expiresAt()).isEqualTo(NOW.plus(TTL));
        assertThat(reservation.eventId()).isEqualTo(eventId);
        assertThat(reservation.userId()).isEqualTo(userId);
        assertThat(reservation.quantity()).isEqualTo(2);
    }

    @Test
    void rejectsNonPositiveQuantity() {
        assertThatThrownBy(() -> Reservation.hold(eventId, userId, 0, NOW, TTL))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonPositiveTtl() {
        assertThatThrownBy(() -> Reservation.hold(eventId, userId, 1, NOW, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void confirmsBeforeExpiry() {
        final Reservation reservation = newReservation();

        reservation.confirm(NOW.plusSeconds(60));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void cannotConfirmExpiredReservation() {
        final Reservation reservation = newReservation();

        assertThatThrownBy(() -> reservation.confirm(NOW.plus(TTL)))
                .isInstanceOf(IllegalReservationStateException.class);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.HELD);
    }

    @Test
    void releasesHeldReservation() {
        final Reservation reservation = newReservation();

        reservation.release();

        assertThat(reservation.status()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    void expiresOnlyAfterTtl() {
        final Reservation reservation = newReservation();

        assertThatThrownBy(() -> reservation.expire(NOW.plusSeconds(1)))
                .isInstanceOf(IllegalReservationStateException.class);

        reservation.expire(NOW.plus(TTL));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.EXPIRED);
    }

    @Test
    void cannotLeaveFinalStatus() {
        final Reservation confirmed = newReservation();
        confirmed.confirm(NOW);
        final Reservation released = newReservation();
        released.release();

        assertThatThrownBy(released::release).isInstanceOf(IllegalReservationStateException.class);
        assertThatThrownBy(() -> confirmed.confirm(NOW)).isInstanceOf(IllegalReservationStateException.class);
        assertThatThrownBy(confirmed::release).isInstanceOf(IllegalReservationStateException.class);
        assertThatThrownBy(() -> released.expire(NOW.plus(TTL)))
                .isInstanceOf(IllegalReservationStateException.class);
    }
}
