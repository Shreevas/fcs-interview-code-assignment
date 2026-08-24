package com.fulfilment.application.monolith.fulfillment.adapter.database;

import com.fulfilment.application.monolith.fulfillment.model.Fulfillment;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class FulfillmentRepository implements PanacheRepository<Fulfillment> {

  public boolean exists(Long productId, Long storeId, Long warehouseId) {
    return count(
            "productId = ?1 and storeId = ?2 and warehouseId = ?3", productId, storeId, warehouseId)
        > 0;
  }

  public long countDistinctWarehousesForProductAndStore(Long productId, Long storeId) {
    return find("productId = ?1 and storeId = ?2", productId, storeId)
        .stream()
        .map(f -> f.warehouseId)
        .distinct()
        .count();
  }

  public long countDistinctWarehousesForStore(Long storeId) {
    return find("storeId = ?1", storeId).stream().map(f -> f.warehouseId).distinct().count();
  }

  public long countDistinctProductsForWarehouse(Long warehouseId) {
    return find("warehouseId = ?1", warehouseId).stream().map(f -> f.productId).distinct().count();
  }

  public List<Fulfillment> listByStore(Long storeId) {
    return find("storeId", storeId).list();
  }
}