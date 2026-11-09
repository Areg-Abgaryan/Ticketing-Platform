/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.stock.application;

import com.areg.ticketing.inventory.stock.domain.TicketStock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryStockRepository implements StockRepository {

    private final Map<UUID, TicketStock> stocks = new HashMap<>();

    @Override
    public Optional<TicketStock> findByEventId(UUID eventId) {
        return Optional.ofNullable(stocks.get(eventId));
    }

    @Override
    public void save(TicketStock stock) {
        stocks.put(stock.eventId(), stock);
    }
}
