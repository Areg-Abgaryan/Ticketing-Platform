/*
 * Copyright © 2026 Areg Abgaryan
 */
package com.areg.ticketing.inventory.stock.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface TicketStockJpaRepository extends JpaRepository<TicketStockEntity, UUID> {
}
