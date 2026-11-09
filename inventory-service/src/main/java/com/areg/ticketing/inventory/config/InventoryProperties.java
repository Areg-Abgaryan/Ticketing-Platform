/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of the inventory service, read from {@code ticketing.inventory.*} in application.yml.
 *
 * @param reservationTtl how long tickets stay held while the user pays
 */
@ConfigurationProperties("ticketing.inventory")
public record InventoryProperties(Duration reservationTtl) {
}
