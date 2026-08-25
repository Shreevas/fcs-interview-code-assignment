package com.fulfilment.application.monolith.fulfillment.validator;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.fulfillment.adapter.database.FulfillmentRepository;
import com.fulfilment.application.monolith.fulfillment.exceptions.FulfillmentConflictException;
import com.fulfilment.application.monolith.fulfillment.exceptions.FulfillmentConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FulfillmentValidatorTest {

  private static final Long PRODUCT_ID = 1L;
  private static final Long STORE_ID = 2L;
  private static final Long WAREHOUSE_ID = 3L;

  private FulfillmentRepository fulfillmentRepository;
  private FulfillmentValidator validator;

  @BeforeEach
  public void setUp() {
    fulfillmentRepository = mock(FulfillmentRepository.class);
    validator = new FulfillmentValidator(fulfillmentRepository);
  }

  @Test
  public void testRequireNotDuplicateThrowsWhenAlreadyAssociated() {
    when(fulfillmentRepository.exists(PRODUCT_ID, STORE_ID, WAREHOUSE_ID)).thenReturn(true);

    assertThrows(
        FulfillmentConflictException.class,
        () -> validator.requireNotDuplicate(PRODUCT_ID, STORE_ID, WAREHOUSE_ID));
  }

  @Test
  public void testRequireNotDuplicatePassesWhenNotAssociated() {
    when(fulfillmentRepository.exists(PRODUCT_ID, STORE_ID, WAREHOUSE_ID)).thenReturn(false);

    validator.requireNotDuplicate(PRODUCT_ID, STORE_ID, WAREHOUSE_ID);
  }

  @Test
  public void testRequireWarehouseCountFeasibleForProductAndStoreThrowsAtMax() {
    when(fulfillmentRepository.countDistinctWarehousesForProductAndStore(PRODUCT_ID, STORE_ID))
        .thenReturn((long) FulfillmentValidator.MAX_WAREHOUSES_PER_PRODUCT_PER_STORE);

    assertThrows(
        FulfillmentConstraintViolationException.class,
        () -> validator.requireWarehouseCountFeasibleForProductAndStore(PRODUCT_ID, STORE_ID));
  }

  @Test
  public void testRequireWarehouseCountFeasibleForProductAndStorePassesOneBelowMax() {
    when(fulfillmentRepository.countDistinctWarehousesForProductAndStore(PRODUCT_ID, STORE_ID))
        .thenReturn((long) FulfillmentValidator.MAX_WAREHOUSES_PER_PRODUCT_PER_STORE - 1);

    validator.requireWarehouseCountFeasibleForProductAndStore(PRODUCT_ID, STORE_ID);
  }

  @Test
  public void testRequireWarehouseCountFeasibleForStoreThrowsAtMax() {
    when(fulfillmentRepository.countDistinctWarehousesForStore(STORE_ID))
        .thenReturn((long) FulfillmentValidator.MAX_WAREHOUSES_PER_STORE);

    assertThrows(
        FulfillmentConstraintViolationException.class,
        () -> validator.requireWarehouseCountFeasibleForStore(STORE_ID));
  }

  @Test
  public void testRequireWarehouseCountFeasibleForStorePassesOneBelowMax() {
    when(fulfillmentRepository.countDistinctWarehousesForStore(STORE_ID))
        .thenReturn((long) FulfillmentValidator.MAX_WAREHOUSES_PER_STORE - 1);

    validator.requireWarehouseCountFeasibleForStore(STORE_ID);
  }

  @Test
  public void testRequireProductCountFeasibleForWarehouseThrowsAtMax() {
    when(fulfillmentRepository.countDistinctProductsForWarehouse(WAREHOUSE_ID))
        .thenReturn((long) FulfillmentValidator.MAX_PRODUCTS_PER_WAREHOUSE);

    assertThrows(
        FulfillmentConstraintViolationException.class,
        () -> validator.requireProductCountFeasibleForWarehouse(WAREHOUSE_ID));
  }

  @Test
  public void testRequireProductCountFeasibleForWarehousePassesOneBelowMax() {
    when(fulfillmentRepository.countDistinctProductsForWarehouse(WAREHOUSE_ID))
        .thenReturn((long) FulfillmentValidator.MAX_PRODUCTS_PER_WAREHOUSE - 1);

    validator.requireProductCountFeasibleForWarehouse(WAREHOUSE_ID);
  }
}