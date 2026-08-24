package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

public class ReplaceWarehouseUseCaseTest {

  private WarehouseStore warehouseStore;
  private LocationResolver locationResolver;
  private ReplaceWarehouseUseCase useCase;
  private Warehouse oldWarehouse;

  @BeforeEach
  public void setUp() {
    warehouseStore = mock(WarehouseStore.class);
    locationResolver = mock(LocationResolver.class);
    useCase =
        new ReplaceWarehouseUseCase(
            warehouseStore, new WarehouseFeasibilityValidator(locationResolver, warehouseStore));

    oldWarehouse = warehouse("MWH.001", "ZWOLLE-001", 40, 10);
    oldWarehouse.id = 1L;

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(oldWarehouse);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(new Location("ZWOLLE-001", 2, 100));
    when(warehouseStore.getAll()).thenReturn(List.of(oldWarehouse));
    when(warehouseStore.create(any(Warehouse.class)))
        .thenAnswer(
            invocation -> {
              Warehouse w = invocation.getArgument(0);
              w.id = 2L;
              return w;
            });
  }

  private static Warehouse warehouse(String code, String location, int capacity, int stock) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = code;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }

  @Test
  public void testReplaceArchivesOldAndCreatesNewInOrder() {
    Warehouse newWarehouse = warehouse("MWH.001", "ZWOLLE-001", 40, 10);

    Warehouse result = useCase.replace(newWarehouse);

    assertEquals(2L, result.id);
    InOrder order = inOrder(warehouseStore);
    order.verify(warehouseStore).remove(oldWarehouse);
    order.verify(warehouseStore).create(newWarehouse);
  }

  @Test
  public void testReplaceThrowsWhenNoActiveWarehouseForCode() {
    when(warehouseStore.findByBusinessUnitCode("MWH.999")).thenReturn(null);
    Warehouse newWarehouse = warehouse("MWH.999", "ZWOLLE-001", 40, 10);

    assertThrows(WarehouseNotFoundException.class, () -> useCase.replace(newWarehouse));
    org.mockito.Mockito.verify(warehouseStore, never()).remove(any());
  }

  @Test
  public void testReplaceThrowsWhenLocationChanged() {
    Warehouse newWarehouse = warehouse("MWH.001", "TILBURG-001", 40, 10);

    assertThrows(WarehouseValidationException.class, () -> useCase.replace(newWarehouse));
    org.mockito.Mockito.verify(warehouseStore, never()).remove(any());
  }

  @Test
  public void testReplaceThrowsWhenNewCapacityLessThanOldStock() {
    // old warehouse stock is 10; new capacity of 5 cannot accommodate it
    Warehouse newWarehouse = warehouse("MWH.001", "ZWOLLE-001", 5, 5);

    assertThrows(WarehouseValidationException.class, () -> useCase.replace(newWarehouse));
  }

  @Test
  public void testReplaceThrowsWhenNewStockDoesNotMatchOldStock() {
    Warehouse newWarehouse = warehouse("MWH.001", "ZWOLLE-001", 40, 11);

    assertThrows(WarehouseValidationException.class, () -> useCase.replace(newWarehouse));
  }

  @Test
  public void testReplaceExcludesOldWarehouseFromCapacityTally() {
    // location max capacity is exactly the old warehouse's capacity; replacing at the same
    // capacity must succeed because the old warehouse is excluded from the location's tally.
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(new Location("ZWOLLE-001", 2, 40));
    Warehouse newWarehouse = warehouse("MWH.001", "ZWOLLE-001", 40, 10);

    Warehouse result = useCase.replace(newWarehouse);

    assertEquals(2L, result.id);
  }
}