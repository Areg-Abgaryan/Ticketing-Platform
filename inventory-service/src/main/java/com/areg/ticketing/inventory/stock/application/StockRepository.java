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

    void save(TicketStock stock);
}
