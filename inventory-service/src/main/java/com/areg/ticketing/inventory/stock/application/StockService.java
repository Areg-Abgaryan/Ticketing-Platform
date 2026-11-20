/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.stock.application;

import com.areg.ticketing.inventory.stock.domain.TicketStock;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Application service: creating the stock of an event and reading how many tickets are left. */
@Transactional
public class StockService {

    private final StockRepository stocks;

    public StockService(StockRepository stocks) {
        this.stocks = stocks;
    }

    /** Creates the stock of an event with all tickets available. Fails if the event already has a stock. */
    public TicketStock create(UUID eventId, int total) {
        if (stocks.findByEventId(eventId).isPresent()) {
            throw new StockAlreadyExistsException(eventId);
        }
        final TicketStock stock = TicketStock.create(eventId, total);
        stocks.save(stock);
        return stock;
    }

    /** Returns the current stock of an event. Fails if the event has none. */
    @Transactional(readOnly = true)
    public TicketStock get(UUID eventId) {
        return stocks.findByEventId(eventId).orElseThrow(() -> new StockNotFoundException(eventId));
    }
}
