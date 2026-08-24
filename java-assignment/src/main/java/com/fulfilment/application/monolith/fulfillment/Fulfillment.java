package com.fulfilment.application.monolith.fulfillment;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * Associates a {@code Warehouse} (by its database id, not its reusable business unit code, so
 * history survives a warehouse replacement) as a fulfillment unit for a given {@code Product} at
 * a given {@code Store}.
 */
@Entity
@Table(
    name = "fulfillment",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"productId", "storeId", "warehouseId"}))
public class Fulfillment extends PanacheEntity {

  @Column(nullable = false)
  public Long productId;

  @Column(nullable = false)
  public Long storeId;

  @Column(nullable = false)
  public Long warehouseId;

  @Column(nullable = false)
  public LocalDateTime createdAt;

  public Fulfillment() {}

  public Fulfillment(Long productId, Long storeId, Long warehouseId) {
    this.productId = productId;
    this.storeId = storeId;
    this.warehouseId = warehouseId;
    this.createdAt = LocalDateTime.now();
  }
}