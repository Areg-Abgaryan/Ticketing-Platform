/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.stock.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.areg.ticketing.inventory.reservation.domain.Reservation;
import com.areg.ticketing.inventory.reservation.domain.ReservationResult;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TicketStockTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(10);

    private final UUID eventId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Test
    void newStockIsFullyAvailable() {
        final TicketStock stock = TicketStock.create(eventId, 100);

        assertThat(stock.total()).isEqualTo(100);
        assertThat(stock.available()).isEqualTo(100);
    }

    @Test
    void reservingTakesTicketsOutOfAvailableStock() {
        final TicketStock stock = TicketStock.create(eventId, 10);

        final ReservationResult result = stock.reserve(userId, 3, NOW, TTL);

        final Reservation reservation = reserved(result);
        assertThat(reservation.quantity()).isEqualTo(3);
        assertThat(reservation.eventId()).isEqualTo(eventId);
        assertThat(reservation.userId()).isEqualTo(userId);
        assertThat(stock.available()).isEqualTo(7);
    }

    @Test
    void canReserveExactlyAllRemainingTickets() {
        final TicketStock stock = TicketStock.create(eventId, 5);

        final ReservationResult result = stock.reserve(userId, 5, NOW, TTL);

        assertThat(result).isInstanceOf(ReservationResult.Reserved.class);
        assertThat(stock.available()).isZero();
    }

    @Test
    void reportsSoldOutAndKeepsStockUnchanged() {
        final TicketStock stock = TicketStock.create(eventId, 2);

        final ReservationResult result = stock.reserve(userId, 3, NOW, TTL);

        assertThat(result).isEqualTo(new ReservationResult.SoldOut(2));
        assertThat(stock.available()).isEqualTo(2);
    }

    @Test
    void lastTicketCanBeTakenOnlyOnce() {
        final TicketStock stock = TicketStock.create(eventId, 1);

        final ReservationResult first = stock.reserve(userId, 1, NOW, TTL);
        final ReservationResult second = stock.reserve(UUID.randomUUID(), 1, NOW, TTL);

        assertThat(first).isInstanceOf(ReservationResult.Reserved.class);
        assertThat(second).isEqualTo(new ReservationResult.SoldOut(0));
    }

    @Test
    void restoreReturnsTicketsToStock() {
        final TicketStock stock = TicketStock.create(eventId, 10);
        stock.reserve(userId, 4, NOW, TTL);

        stock.restore(4);

        assertThat(stock.available()).isEqualTo(10);
    }

    @Test
    void cannotRestoreMoreThanWasTaken() {
        final TicketStock stock = TicketStock.create(eventId, 10);
        stock.reserve(userId, 2, NOW, TTL);

        assertThatThrownBy(() -> stock.restore(3)).isInstanceOf(IllegalStateException.class);
        assertThat(stock.available()).isEqualTo(8);
    }

    @Test
    void rejectsNonPositiveArguments() {
        final TicketStock stock = TicketStock.create(eventId, 10);

        assertThatThrownBy(() -> TicketStock.create(eventId, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stock.reserve(userId, 0, NOW, TTL)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> stock.restore(0)).isInstanceOf(IllegalArgumentException.class);
    }

    private static Reservation reserved(ReservationResult result) {
        return switch (result) {
            case ReservationResult.Reserved reserved -> reserved.reservation();
            case ReservationResult.SoldOut soldOut -> throw new AssertionError("Unexpected " + soldOut);
            case ReservationResult.AlreadyHeld alreadyHeld -> throw new AssertionError("Unexpected " + alreadyHeld);
        };
    }
}
