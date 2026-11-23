/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.areg.ticketing.inventory.reservation.application.ReservationNotFoundException;
import com.areg.ticketing.inventory.reservation.application.ReservationRepository;
import com.areg.ticketing.inventory.reservation.application.ReservationService;
import com.areg.ticketing.inventory.reservation.domain.Reservation;
import com.areg.ticketing.inventory.reservation.domain.ReservationResult;
import com.areg.ticketing.inventory.reservation.domain.ReservationStatus;
import com.areg.ticketing.inventory.stock.application.StockRepository;
import com.areg.ticketing.inventory.stock.domain.TicketStock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** The reservation flow against a real Postgres: schema, mapping, transactions and concurrent updates. */
// the cleanup job is pushed out of the way so that it cannot sweep the rows the tests create
@SpringBootTest(properties = "ticketing.inventory.expiry-sweep-interval=PT1H")
@ActiveProfiles("dev")
@Testcontainers
class ReservationServiceFunctionalTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private ReservationService service;
    @Autowired
    private StockRepository stocks;
    @Autowired
    private ReservationRepository reservations;

    private final UUID eventId = UUID.randomUUID();

    private int available() {
        return stocks.findByEventId(eventId).orElseThrow().available();
    }

    @Test
    void reservePersistsReservationAndReducesStock() {
        stocks.save(TicketStock.create(eventId, 10));
        final UUID userId = UUID.randomUUID();

        final ReservationResult result = service.reserve(userId, eventId, 3);

        assertThat(result).isInstanceOf(ReservationResult.Reserved.class);
        assertThat(available()).isEqualTo(7);
        final Reservation saved = reservations.findHeld(userId, eventId).orElseThrow();
        assertThat(saved.quantity()).isEqualTo(3);
        assertThat(saved.status()).isEqualTo(ReservationStatus.HELD);
    }

    @Test
    void releaseReturnsTicketsToStockInDatabase() {
        stocks.save(TicketStock.create(eventId, 10));
        final UUID userId = UUID.randomUUID();
        service.reserve(userId, eventId, 4);
        final UUID reservationId = reservations.findHeld(userId, eventId).orElseThrow().id();

        service.release(userId, reservationId);

        assertThat(available()).isEqualTo(10);
        assertThat(reservations.findById(reservationId).orElseThrow().status()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    void confirmKeepsTicketsTaken() {
        stocks.save(TicketStock.create(eventId, 10));
        final UUID userId = UUID.randomUUID();
        service.reserve(userId, eventId, 2);
        final UUID reservationId = reservations.findHeld(userId, eventId).orElseThrow().id();

        service.confirm(userId, reservationId);

        assertThat(available()).isEqualTo(8);
        assertThat(reservations.findById(reservationId).orElseThrow().status()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void expireStaleFreesTicketsOfOverdueReservation() {
        final TicketStock stock = TicketStock.create(eventId, 10);
        final Instant createdAt = Instant.now().minus(20, ChronoUnit.MINUTES);
        stock.reserve(UUID.randomUUID(), 5, createdAt, Duration.ofMinutes(10));
        stocks.save(stock);
        final Reservation overdue = Reservation.rehydrate(UUID.randomUUID(), eventId, UUID.randomUUID(), 5,
                createdAt, createdAt.plus(10, ChronoUnit.MINUTES), ReservationStatus.HELD);
        reservations.save(overdue);
        assertThat(available()).isEqualTo(5);

        final int expired = service.expireStale();

        assertThat(expired).isGreaterThanOrEqualTo(1);
        assertThat(reservations.findById(overdue.id()).orElseThrow().status()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(available()).isEqualTo(10);
    }

    @Test
    void overdueHoldIsExpiredAndReplacedWhenTheSameUserReservesAgain() {
        final UUID userId = UUID.randomUUID();
        final TicketStock stock = TicketStock.create(eventId, 10);
        final Instant createdAt = Instant.now().minus(20, ChronoUnit.MINUTES);
        stock.reserve(userId, 3, createdAt, Duration.ofMinutes(10));
        stocks.save(stock);
        final Reservation overdue = Reservation.rehydrate(UUID.randomUUID(), eventId, userId, 3,
                createdAt, createdAt.plus(10, ChronoUnit.MINUTES), ReservationStatus.HELD);
        reservations.add(overdue);

        final ReservationResult result = service.reserve(userId, eventId, 2);

        assertThat(result).isInstanceOf(ReservationResult.Reserved.class);
        assertThat(reservations.findById(overdue.id()).orElseThrow().status()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(reservations.findHeld(userId, eventId).orElseThrow().quantity()).isEqualTo(2);
        assertThat(available()).isEqualTo(8);
    }

    @Test
    void databaseAllowsOnlyOneHeldReservationPerUserAndEvent() {
        stocks.save(TicketStock.create(eventId, 10));
        final UUID userId = UUID.randomUUID();
        final Instant now = Instant.now();
        reservations.add(Reservation.hold(eventId, userId, 1, now, Duration.ofMinutes(10)));

        assertThatThrownBy(() ->
                reservations.add(Reservation.hold(eventId, userId, 1, now, Duration.ofMinutes(10))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void anotherUserOrAFinishedReservationDoesNotCountAgainstTheUniqueIndex() {
        stocks.save(TicketStock.create(eventId, 10));
        final UUID userId = UUID.randomUUID();
        service.reserve(userId, eventId, 1);
        service.release(userId, reservations.findHeld(userId, eventId).orElseThrow().id());

        assertThat(service.reserve(userId, eventId, 1)).isInstanceOf(ReservationResult.Reserved.class);
        assertThat(service.reserve(UUID.randomUUID(), eventId, 1)).isInstanceOf(ReservationResult.Reserved.class);
    }

    @Test
    void anotherUserCannotConfirmOrReleaseTheReservation() {
        stocks.save(TicketStock.create(eventId, 10));
        final UUID userId = UUID.randomUUID();
        service.reserve(userId, eventId, 2);
        final UUID reservationId = reservations.findHeld(userId, eventId).orElseThrow().id();
        final UUID stranger = UUID.randomUUID();

        assertThatThrownBy(() -> service.confirm(stranger, reservationId))
                .isInstanceOf(ReservationNotFoundException.class);
        assertThatThrownBy(() -> service.release(stranger, reservationId))
                .isInstanceOf(ReservationNotFoundException.class);
        assertThat(available()).isEqualTo(8);
    }

    /**
     * Many users compete for few tickets at the same moment. Some requests may fail on a version conflict
     * (there is no retry yet), but the stock must never go negative and every ticket missing from the stock
     * must belong to exactly one saved reservation.
     */
    @Test
    void neverSellsMoreTicketsThanExist() throws Exception {
        final int tickets = 5;
        final int users = 40;
        stocks.save(TicketStock.create(eventId, tickets));
        final CountDownLatch start = new CountDownLatch(1);
        final List<UUID> userIds = new ArrayList<>();
        final List<Future<ReservationResult>> futures = new ArrayList<>();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < users; i++) {
                final UUID userId = UUID.randomUUID();
                userIds.add(userId);
                futures.add(executor.submit(() -> {
                    start.await();
                    return service.reserve(userId, eventId, 1);
                }));
            }
            start.countDown();
        }

        int reserved = 0;
        for (final Future<ReservationResult> future : futures) {
            try {
                if (future.get() instanceof ReservationResult.Reserved) {
                    reserved++;
                }
            } catch (java.util.concurrent.ExecutionException conflict) {
                // lost a version conflict: acceptable here, retries come later
            }
        }
        final long heldInDatabase = userIds.stream()
                .filter(userId -> reservations.findHeld(userId, eventId).isPresent())
                .count();

        assertThat(reserved).isBetween(1, tickets);
        assertThat(heldInDatabase).isEqualTo(reserved);
        assertThat(available()).isEqualTo(tickets - reserved).isGreaterThanOrEqualTo(0);
    }
}
