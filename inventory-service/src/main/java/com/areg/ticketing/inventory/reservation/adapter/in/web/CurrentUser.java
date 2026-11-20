/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.reservation.adapter.in.web;

import java.util.UUID;

/**
 * Port: who is calling. The user id never comes from the request body or parameters, only from here.
 * Today a fixed dev user; later the {@code sub} claim of the JWT.
 */
public interface CurrentUser {

    UUID id();
}
