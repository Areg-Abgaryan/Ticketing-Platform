/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.stock.adapter.in.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

/** Body of the create-stock call: how many tickets the event has in total (at most 1,000,000). */
record CreateStockRequest(@Positive @Max(1_000_000) int total) {
}
