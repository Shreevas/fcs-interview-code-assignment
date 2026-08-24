package com.fulfilment.application.monolith.fulfillment.service;

import com.fulfilment.application.monolith.fulfillment.adapter.database.FulfillmentRepository;
import com.fulfilment.application.monolith.fulfillment.exceptions.FulfillmentNotFoundException;
import com.fulfilment.application.monolith.fulfillment.model.Fulfillment;
import com.fulfilment.application.monolith.fulfillment.validator.FulfillmentValidator;
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

  private static final Logger LOGGER = Logger.getLogger(FulfillmentService.class);

  private final FulfillmentRepository fulfillmentRepository;
  private final ProductRepository productRepository;
  private final StoreRepository storeRepository;
  private final WarehouseRepository warehouseRepository;
  private final FulfillmentValidator fulfillmentValidator;

  public FulfillmentService(
      FulfillmentRepository fulfillmentRepository,
      ProductRepository productRepository,
      StoreRepository storeRepository,
      WarehouseRepository warehouseRepository,
      FulfillmentValidator fulfillmentValidator) {
    this.fulfillmentRepository = fulfillmentRepository;
    this.productRepository = productRepository;
    this.storeRepository = storeRepository;
    this.warehouseRepository = warehouseRepository;
    this.fulfillmentValidator = fulfillmentValidator;
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

    fulfillmentValidator.requireNotDuplicate(productId, storeId, warehouseId);
    fulfillmentValidator.requireWarehouseCountFeasibleForProductAndStore(productId, storeId);
    fulfillmentValidator.requireWarehouseCountFeasibleForStore(storeId);
    fulfillmentValidator.requireProductCountFeasibleForWarehouse(warehouseId);

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