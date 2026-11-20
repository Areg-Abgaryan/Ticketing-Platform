/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.reservation.adapter.in.web;

import jakarta.validation.constraints.Positive;

/** Body of the reserve call. There is deliberately no user id here. */
record ReserveRequest(@Positive int quantity) {
}
