/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings of the inventory service, read from {@code ticketing.inventory.*} in application.yml. They are
 * validated at startup, so a missing or non-positive value stops the application instead of failing later.
 *
 * @param reservationTtl       how long tickets stay held while the user pays
 * @param expirySweepInterval  how long to wait between two runs of the cleanup that frees expired holds
 */
@Validated
@ConfigurationProperties("ticketing.inventory")
public record InventoryProperties(
        @NotNull @DurationMin(seconds = 1) Duration reservationTtl,
        @NotNull @DurationMin(seconds = 1) Duration expirySweepInterval) {
}
