package com.fulfilment.application.monolith.fulfillment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.fulfillment.adapter.database.FulfillmentRepository;
import com.fulfilment.application.monolith.fulfillment.exceptions.FulfillmentConflictException;
import com.fulfilment.application.monolith.fulfillment.exceptions.FulfillmentConstraintViolationException;
import com.fulfilment.application.monolith.fulfillment.exceptions.FulfillmentNotFoundException;
import com.fulfilment.application.monolith.fulfillment.model.Fulfillment;
import com.fulfilment.application.monolith.fulfillment.validator.FulfillmentValidator;
import com.fulfilment.application.monolith.products.Product;
import com.fulfilment.application.monolith.products.ProductRepository;
import com.fulfilment.application.monolith.stores.Store;
import com.fulfilment.application.monolith.stores.StoreRepository;
import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Service-level tests exercise the full flow (existence checks + delegating to {@link
 * FulfillmentValidator}) through the public API. Rule-by-rule edge cases for the validator itself
 * live in {@code FulfillmentValidatorTest}.
 */
public class FulfillmentServiceTest {

  private static final Long PRODUCT_ID = 1L;
  private static final Long STORE_ID = 2L;
  private static final Long WAREHOUSE_ID = 3L;

  private FulfillmentRepository fulfillmentRepository;
  private ProductRepository productRepository;
  private StoreRepository storeRepository;
  private WarehouseRepository warehouseRepository;
  private FulfillmentService service;

  @BeforeEach
  public void setUp() {
    fulfillmentRepository = mock(FulfillmentRepository.class);
    productRepository = mock(ProductRepository.class);
    storeRepository = mock(StoreRepository.class);
    warehouseRepository = mock(WarehouseRepository.class);
    FulfillmentValidator fulfillmentValidator = new FulfillmentValidator(fulfillmentRepository);
    service =
        new FulfillmentService(
            fulfillmentRepository,
            productRepository,
            storeRepository,
            warehouseRepository,
            fulfillmentValidator);

    when(productRepository.findByIdOptional(PRODUCT_ID)).thenReturn(Optional.of(new Product("TONSTAD")));
    when(storeRepository.findByIdOptional(STORE_ID)).thenReturn(Optional.of(new Store("KALLAX")));
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(new DbWarehouse());
  }

  @Test
  public void testCreateAssociationPersistsWhenAllChecksPass() {
    Fulfillment created = service.createAssociation(PRODUCT_ID, STORE_ID, WAREHOUSE_ID);

    assertEquals(PRODUCT_ID, created.productId);
    assertEquals(STORE_ID, created.storeId);
    assertEquals(WAREHOUSE_ID, created.warehouseId);
    verify(fulfillmentRepository, times(1)).persist(anyFulfillment());
  }

  @Test
  public void testCreateAssociationThrowsWhenProductMissing() {
    when(productRepository.findByIdOptional(PRODUCT_ID)).thenReturn(Optional.empty());

    assertThrows(
        FulfillmentNotFoundException.class,
        () -> service.createAssociation(PRODUCT_ID, STORE_ID, WAREHOUSE_ID));
    verify(fulfillmentRepository, never()).persist(anyFulfillment());
  }

  @Test
  public void testCreateAssociationThrowsWhenStoreMissing() {
    when(storeRepository.findByIdOptional(STORE_ID)).thenReturn(Optional.empty());

    assertThrows(
        FulfillmentNotFoundException.class,
        () -> service.createAssociation(PRODUCT_ID, STORE_ID, WAREHOUSE_ID));
    verify(fulfillmentRepository, never()).persist(anyFulfillment());
  }

  @Test
  public void testCreateAssociationThrowsWhenWarehouseMissing() {
    when(warehouseRepository.findById(WAREHOUSE_ID)).thenReturn(null);

    assertThrows(
        FulfillmentNotFoundException.class,
        () -> service.createAssociation(PRODUCT_ID, STORE_ID, WAREHOUSE_ID));
    verify(fulfillmentRepository, never()).persist(anyFulfillment());
  }

  @Test
  public void testCreateAssociationThrowsWhenValidatorRejectsDuplicate() {
    // integration-style: proves the service actually consults FulfillmentValidator's rules,
    // rather than duplicating the rule-by-rule checks already covered in FulfillmentValidatorTest.
    when(fulfillmentRepository.exists(PRODUCT_ID, STORE_ID, WAREHOUSE_ID)).thenReturn(true);

    assertThrows(
        FulfillmentConflictException.class,
        () -> service.createAssociation(PRODUCT_ID, STORE_ID, WAREHOUSE_ID));
    verify(fulfillmentRepository, never()).persist(anyFulfillment());
  }

  @Test
  public void testCreateAssociationThrowsWhenValidatorRejectsConstraintViolation() {
    // integration-style: same idea, but for a cardinality-constraint rejection instead of a
    // duplicate rejection.
    when(fulfillmentRepository.countDistinctWarehousesForStore(STORE_ID))
        .thenReturn((long) FulfillmentValidator.MAX_WAREHOUSES_PER_STORE);

    assertThrows(
        FulfillmentConstraintViolationException.class,
        () -> service.createAssociation(PRODUCT_ID, STORE_ID, WAREHOUSE_ID));
    verify(fulfillmentRepository, never()).persist(anyFulfillment());
  }

  @Test
  public void testRemoveDeletesExistingAssociation() {
    Fulfillment fulfillment = new Fulfillment(PRODUCT_ID, STORE_ID, WAREHOUSE_ID);
    when(fulfillmentRepository.findById(10L)).thenReturn(fulfillment);

    service.remove(10L);

    verify(fulfillmentRepository, times(1)).delete(fulfillment);
  }

  @Test
  public void testRemoveThrowsWhenMissing() {
    when(fulfillmentRepository.findById(10L)).thenReturn(null);

    assertThrows(FulfillmentNotFoundException.class, () -> service.remove(10L));
  }

  // any(Fulfillment.class)/any() as a direct argument to the overloaded Panache persist(...) is
  // ambiguous to javac (it matches both the Iterable and Stream overloads); binding it to a
  // concretely-typed local first sidesteps the ambiguity.
  private static Fulfillment anyFulfillment() {
    Fulfillment matcher = any();
    return matcher;
  }
}
