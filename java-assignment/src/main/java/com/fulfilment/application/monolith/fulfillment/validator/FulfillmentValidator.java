package com.fulfilment.application.monolith.fulfillment.validator;

import com.fulfilment.application.monolith.fulfillment.adapter.database.FulfillmentRepository;
import com.fulfilment.application.monolith.fulfillment.exceptions.FulfillmentConflictException;
import com.fulfilment.application.monolith.fulfillment.exceptions.FulfillmentConstraintViolationException;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Enforces the fulfillment cardinality rules: a product may be fulfilled by at most {@value
 * #MAX_WAREHOUSES_PER_PRODUCT_PER_STORE} warehouses per store, a store may be fulfilled by at
 * most {@value #MAX_WAREHOUSES_PER_STORE} warehouses in total, and a warehouse may hold at most
 * {@value #MAX_PRODUCTS_PER_WAREHOUSE} distinct product types. Kept separate from {@code
 * FulfillmentService} so the business rules can be read and tested independently of the
 * existence checks and persistence orchestration.
 */
@ApplicationScoped
public class FulfillmentValidator {

  public static final int MAX_WAREHOUSES_PER_PRODUCT_PER_STORE = 2;
  public static final int MAX_WAREHOUSES_PER_STORE = 3;
  public static final int MAX_PRODUCTS_PER_WAREHOUSE = 5;

  private final FulfillmentRepository fulfillmentRepository;

  public FulfillmentValidator(FulfillmentRepository fulfillmentRepository) {
    this.fulfillmentRepository = fulfillmentRepository;
  }

  public void requireNotDuplicate(Long productId, Long storeId, Long warehouseId) {
    if (fulfillmentRepository.exists(productId, storeId, warehouseId)) {
      throw new FulfillmentConflictException(
          "Product "
              + productId
              + " is already fulfilled by warehouse "
              + warehouseId
              + " at store "
              + storeId
              + ".");
    }
  }

  public void requireWarehouseCountFeasibleForProductAndStore(Long productId, Long storeId) {
    if (fulfillmentRepository.countDistinctWarehousesForProductAndStore(productId, storeId)
        >= MAX_WAREHOUSES_PER_PRODUCT_PER_STORE) {
      throw new FulfillmentConstraintViolationException(
          "Product "
              + productId
              + " already has the maximum of "
              + MAX_WAREHOUSES_PER_PRODUCT_PER_STORE
              + " fulfillment warehouses for store "
              + storeId
              + ".");
    }
  }

  public void requireWarehouseCountFeasibleForStore(Long storeId) {
    if (fulfillmentRepository.countDistinctWarehousesForStore(storeId) >= MAX_WAREHOUSES_PER_STORE) {
      throw new FulfillmentConstraintViolationException(
          "Store "
              + storeId
              + " already has the maximum of "
              + MAX_WAREHOUSES_PER_STORE
              + " fulfillment warehouses.");
    }
  }

  public void requireProductCountFeasibleForWarehouse(Long warehouseId) {
    if (fulfillmentRepository.countDistinctProductsForWarehouse(warehouseId)
        >= MAX_PRODUCTS_PER_WAREHOUSE) {
      throw new FulfillmentConstraintViolationException(
          "Warehouse "
              + warehouseId
              + " already stores the maximum of "
              + MAX_PRODUCTS_PER_WAREHOUSE
              + " product types.");
    }
  }
}