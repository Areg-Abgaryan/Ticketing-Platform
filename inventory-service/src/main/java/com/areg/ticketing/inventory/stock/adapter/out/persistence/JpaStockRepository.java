/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.stock.adapter.out.persistence;

import com.areg.ticketing.inventory.stock.application.StockRepository;
import com.areg.ticketing.inventory.stock.domain.TicketStock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Stores ticket stocks in Postgres through JPA. */
@Repository
class JpaStockRepository implements StockRepository {

    private final TicketStockJpaRepository jpa;

    JpaStockRepository(TicketStockJpaRepository jpa) {
        this.jpa = jpa;
    }

    /**
     * Loads the stock row of an event (the event id is the primary key) and rebuilds the domain object from it.
     * Returns empty if the event has no stock. Inside the service transaction the loaded row stays tracked,
     * which is what lets the version check detect a concurrent update.
     */
    @Override
    public Optional<TicketStock> findByEventId(UUID eventId) {
        return jpa.findById(eventId)
                .map(entity -> TicketStock.rehydrate(entity.eventId(), entity.total(), entity.available()));
    }

    /**
     * Inserts a new stock, or updates {@code available} of an existing one. Inside the service transaction the
     * row loaded here is the same one that was loaded when the stock was read, so the version check at commit
     * fails if another transaction changed the stock in between.
     */
    @Override
    public void save(TicketStock stock) {
        final TicketStockEntity entity = jpa.findById(stock.eventId())
                .orElseGet(() -> new TicketStockEntity(stock.eventId(), stock.total(), stock.available()));
        entity.setAvailable(stock.available());
        jpa.save(entity);
    }
}
