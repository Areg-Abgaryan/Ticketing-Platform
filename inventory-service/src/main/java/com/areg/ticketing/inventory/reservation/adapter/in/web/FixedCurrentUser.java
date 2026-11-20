/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.reservation.adapter.in.web;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Development stand-in for authentication: every request is made by the same user. Only active in "dev". */
@Component
@Profile("dev")
class FixedCurrentUser implements CurrentUser {

    private static final UUID DEV_USER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Override
    public UUID id() {
        return DEV_USER;
    }
}
