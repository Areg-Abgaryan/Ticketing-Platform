/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.stock.adapter.in.web;

import jakarta.validation.constraints.Positive;

/** Body of the create-stock call: how many tickets the event has in total. */
record CreateStockRequest(@Positive int total) {
}
