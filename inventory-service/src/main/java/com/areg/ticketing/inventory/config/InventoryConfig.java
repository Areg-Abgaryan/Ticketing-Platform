/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.config;

import com.areg.ticketing.inventory.reservation.application.ReservationRepository;
import com.areg.ticketing.inventory.reservation.application.ReservationService;
import com.areg.ticketing.inventory.stock.application.StockRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Creates the application beans that are plain Java classes without Spring stereotype annotations. */
@Configuration
class InventoryConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ReservationService reservationService(StockRepository stocks, ReservationRepository reservations, Clock clock,
                                          InventoryProperties properties) {
        return new ReservationService(stocks, reservations, clock, properties.reservationTtl());
    }
}
