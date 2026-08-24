package com.fulfilment.application.monolith.fulfillment;

import com.fulfilment.application.monolith.fulfillment.exceptions.FulfillmentConflictException;
import com.fulfilment.application.monolith.fulfillment.exceptions.FulfillmentConstraintViolationException;
import com.fulfilment.application.monolith.fulfillment.exceptions.FulfillmentNotFoundException;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.StoreRepository;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import org.jboss.logging.Logger;

@ApplicationScoped
public class FulfillmentService {

  static final int MAX_WAREHOUSES_PER_PRODUCT_PER_STORE = 2;
  static final int MAX_WAREHOUSES_PER_STORE = 3;
  static final int MAX_PRODUCTS_PER_WAREHOUSE = 5;

  private static final Logger LOGGER = Logger.getLogger(FulfillmentService.class);

  private final FulfillmentRepository fulfillmentRepository;
  private final ProductRepository productRepository;
  private final StoreRepository storeRepository;
  private final WarehouseRepository warehouseRepository;

  public FulfillmentService(
      FulfillmentRepository fulfillmentRepository,
      ProductRepository productRepository,
      StoreRepository storeRepository,
      WarehouseRepository warehouseRepository) {
    this.fulfillmentRepository = fulfillmentRepository;
    this.productRepository = productRepository;
    this.storeRepository = storeRepository;
    this.warehouseRepository = warehouseRepository;
  }

  @Transactional
  public Fulfillment createAssociation(Long productId, Long storeId, Long warehouseId) {
    if (productRepository.findByIdOptional(productId).isEmpty()) {
      throw new FulfillmentNotFoundException("Product with id of " + productId + " does not exist.");
    }
    if (storeRepository.findByIdOptional(storeId).isEmpty()) {
      throw new FulfillmentNotFoundException("Store with id of " + storeId + " does not exist.");
    }
    DbWarehouse warehouse = warehouseRepository.findById(warehouseId);
    if (warehouse == null) {
      throw new FulfillmentNotFoundException("Warehouse with id of " + warehouseId + " does not exist.");
    }

    if (fulfillmentRepository.exists(productId, storeId, warehouseId)) {
      throw new FulfillmentConflictException(
          "Product " + productId + " is already fulfilled by warehouse " + warehouseId + " at store " + storeId + ".");
    }

    if (fulfillmentRepository.countDistinctWarehousesForProductAndStore(productId, storeId)
        >= MAX_WAREHOUSES_PER_PRODUCT_PER_STORE) {
      throw new FulfillmentConstraintViolationException(
          "Product " + productId + " already has the maximum of "
              + MAX_WAREHOUSES_PER_PRODUCT_PER_STORE
              + " fulfillment warehouses for store " + storeId + ".");
    }

    if (fulfillmentRepository.countDistinctWarehousesForStore(storeId) >= MAX_WAREHOUSES_PER_STORE) {
      throw new FulfillmentConstraintViolationException(
          "Store " + storeId + " already has the maximum of " + MAX_WAREHOUSES_PER_STORE + " fulfillment warehouses.");
    }

    if (fulfillmentRepository.countDistinctProductsForWarehouse(warehouseId) >= MAX_PRODUCTS_PER_WAREHOUSE) {
      throw new FulfillmentConstraintViolationException(
          "Warehouse " + warehouseId + " already stores the maximum of " + MAX_PRODUCTS_PER_WAREHOUSE + " product types.");
    }

    Fulfillment fulfillment = new Fulfillment(productId, storeId, warehouseId);
    fulfillmentRepository.persist(fulfillment);
    LOGGER.infof(
        "Associated product %d with warehouse %d for store %d", productId, warehouseId, storeId);
    return fulfillment;
  }

  public List<Fulfillment> listByStore(Long storeId) {
    return fulfillmentRepository.listByStore(storeId);
  }

  public List<Fulfillment> listAll() {
    return fulfillmentRepository.listAll();
  }

  @Transactional
  public void remove(Long id) {
    Fulfillment fulfillment = fulfillmentRepository.findById(id);
    if (fulfillment == null) {
      throw new FulfillmentNotFoundException("Fulfillment association with id of " + id + " does not exist.");
    }
    fulfillmentRepository.delete(fulfillment);
  }
}