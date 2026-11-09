/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.stock.application;

import java.util.UUID;

public class StockNotFoundException extends RuntimeException {

    public StockNotFoundException(UUID eventId) {
        super("No ticket stock for event " + eventId);
    }
}
