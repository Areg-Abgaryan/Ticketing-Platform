/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.reservation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.areg.ticketing.inventory.reservation.domain.IllegalReservationStateException;
import com.areg.ticketing.inventory.reservation.domain.Reservation;
import com.areg.ticketing.inventory.reservation.domain.ReservationResult;
import com.areg.ticketing.inventory.reservation.domain.ReservationStatus;
import com.areg.ticketing.inventory.stock.application.InMemoryStockRepository;
import com.areg.ticketing.inventory.stock.application.StockNotFoundException;
import com.areg.ticketing.inventory.stock.domain.TicketStock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ReservationServiceTest {

    private static final Instant START = Instant.parse("2026-01-01T10:00:00Z");
    private static final Duration TTL = Duration.ofMinutes(10);

    private final UUID eventId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final InMemoryStockRepository stocks = new InMemoryStockRepository();
    private final InMemoryReservationRepository reservations = new InMemoryReservationRepository();
    private final MutableClock clock = new MutableClock(START);
    private final ReservationService service = new ReservationService(stocks, reservations, clock, TTL);

    private void givenStock(int total) {
        stocks.save(TicketStock.create(eventId, total));
    }

    private int available() {
        return stocks.findByEventId(eventId).orElseThrow().available();
    }

    private Reservation givenHeldReservation(int quantity) {
        givenStock(10);
        service.reserve(userId, eventId, quantity);
        return reservations.findHeld(userId, eventId).orElseThrow();
    }

    /** Tests of reserve(): the happy path, sold out, the one-hold-per-user rule and unknown events. */
    @Nested
    class Reserve {

        @Test
        void reservesTicketsAndReducesStock() {
            givenStock(10);

            final ReservationResult result = service.reserve(userId, eventId, 3);
            assertThat(result).isInstanceOf(ReservationResult.Reserved.class);
            assertThat(available()).isEqualTo(7);

            final Reservation saved = reservations.findHeld(userId, eventId).orElseThrow();
            assertThat(saved.quantity()).isEqualTo(3);
            assertThat(saved.expiresAt()).isEqualTo(START.plus(TTL));
        }

        @Test
        void soldOutSavesNothing() {
            givenStock(2);

            final ReservationResult result = service.reserve(userId, eventId, 3);

            assertThat(result).isEqualTo(new ReservationResult.SoldOut(2));
            assertThat(reservations.count()).isZero();
            assertThat(available()).isEqualTo(2);
        }

        @Test
        void sameUserCannotHoldTwiceForOneEvent() {
            final Reservation first = givenHeldReservation(2);
            clock.advance(Duration.ofSeconds(60));

            final ReservationResult result = service.reserve(userId, eventId, 2);

            assertThat(result).isEqualTo(new ReservationResult.AlreadyHeld(first.id()));
            assertThat(reservations.count()).isEqualTo(1);
            assertThat(available()).isEqualTo(8);
        }

        @Test
        void anotherUserCanReserveTheSameEvent() {
            givenHeldReservation(2);

            final ReservationResult result = service.reserve(UUID.randomUUID(), eventId, 2);

            assertThat(result).isInstanceOf(ReservationResult.Reserved.class);
            assertThat(available()).isEqualTo(6);
        }

        @Test
        void expiredHoldDoesNotBlockANewReservation() {
            givenHeldReservation(2);
            clock.advance(TTL);

            final ReservationResult result = service.reserve(userId, eventId, 2);

            assertThat(result).isInstanceOf(ReservationResult.Reserved.class);
        }

        @Test
        void failsForUnknownEvent() {
            assertThatThrownBy(() -> service.reserve(userId, eventId, 1))
                    .isInstanceOf(StockNotFoundException.class);
        }
    }

    /** Tests of confirm(): paying a held reservation, and why an expired or unknown one is rejected. */
    @Nested
    class Confirm {

        @Test
        void confirmsAndKeepsTicketsTaken() {
            final Reservation held = givenHeldReservation(2);
            clock.advance(Duration.ofSeconds(30));

            service.confirm(held.id());

            assertThat(reservations.findById(held.id()).orElseThrow().status())
                    .isEqualTo(ReservationStatus.CONFIRMED);
            assertThat(available()).isEqualTo(8);
        }

        @Test
        void cannotConfirmAfterExpiry() {
            final Reservation held = givenHeldReservation(2);
            clock.advance(TTL);

            assertThatThrownBy(() -> service.confirm(held.id()))
                    .isInstanceOf(IllegalReservationStateException.class);
            assertThat(reservations.findById(held.id()).orElseThrow().status())
                    .isEqualTo(ReservationStatus.HELD);
        }

        @Test
        void failsForUnknownReservation() {
            assertThatThrownBy(() -> service.confirm(UUID.randomUUID()))
                    .isInstanceOf(ReservationNotFoundException.class);
        }
    }

    /** Tests of release(): cancelling a hold returns its tickets exactly once. */
    @Nested
    class Release {

        @Test
        void releasesAndReturnsTicketsToStock() {
            final Reservation held = givenHeldReservation(4);

            service.release(held.id());

            assertThat(reservations.findById(held.id()).orElseThrow().status())
                    .isEqualTo(ReservationStatus.RELEASED);
            assertThat(available()).isEqualTo(10);
        }

        @Test
        void releasingTwiceDoesNotReturnTicketsTwice() {
            final Reservation held = givenHeldReservation(4);
            service.release(held.id());

            assertThatThrownBy(() -> service.release(held.id()))
                    .isInstanceOf(IllegalReservationStateException.class);
            assertThat(available()).isEqualTo(10);
        }

        @Test
        void failsForUnknownReservation() {
            assertThatThrownBy(() -> service.release(UUID.randomUUID()))
                    .isInstanceOf(ReservationNotFoundException.class);
        }
    }

    /** Tests of expireStale(): the periodic cleanup frees only unpaid reservations past their TTL. */
    @Nested
    class ExpireStale {

        @Test
        void expiresUnpaidReservationAndReturnsTickets() {
            final Reservation held = givenHeldReservation(3);
            clock.advance(TTL);

            final int expired = service.expireStale();

            assertThat(expired).isEqualTo(1);
            assertThat(reservations.findById(held.id()).orElseThrow().status())
                    .isEqualTo(ReservationStatus.EXPIRED);
            assertThat(available()).isEqualTo(10);
        }

        @Test
        void leavesReservationsThatAreStillValid() {
            givenHeldReservation(3);
            clock.advance(Duration.ofSeconds(60));

            final int expired = service.expireStale();

            assertThat(expired).isZero();
            assertThat(available()).isEqualTo(7);
        }

        @Test
        void leavesConfirmedReservations() {
            final Reservation held = givenHeldReservation(3);
            service.confirm(held.id());
            clock.advance(TTL.plusSeconds(1));

            final int expired = service.expireStale();

            assertThat(expired).isZero();
            assertThat(reservations.findById(held.id()).orElseThrow().status())
                    .isEqualTo(ReservationStatus.CONFIRMED);
            assertThat(available()).isEqualTo(7);
        }

        @Test
        void expiresSeveralReservationsAtOnce() {
            givenHeldReservation(2);
            service.reserve(UUID.randomUUID(), eventId, 3);
            clock.advance(TTL);

            final int expired = service.expireStale();

            assertThat(expired).isEqualTo(2);
            assertThat(available()).isEqualTo(10);
        }
    }
}
