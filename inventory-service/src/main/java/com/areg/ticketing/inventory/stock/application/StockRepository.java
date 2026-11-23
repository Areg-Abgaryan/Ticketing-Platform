/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.stock.application;

import com.areg.ticketing.inventory.stock.domain.TicketStock;
import java.util.Optional;
import java.util.UUID;

/** Port: how the application layer loads and stores ticket stocks. */
public interface StockRepository {

    Optional<TicketStock> findByEventId(UUID eventId);

    /** Inserts a stock known to be new, without first looking it up. */
    void add(TicketStock stock);

    /** Stores the new {@code available} of an existing stock (inserts it if it does not exist yet). */
    void save(TicketStock stock);
}
