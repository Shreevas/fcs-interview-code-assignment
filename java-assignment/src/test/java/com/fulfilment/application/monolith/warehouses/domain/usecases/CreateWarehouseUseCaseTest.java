package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.validators.WarehouseFeasibilityValidator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CreateWarehouseUseCaseTest {

  private WarehouseStore warehouseStore;
  private LocationResolver locationResolver;
  private CreateWarehouseUseCase useCase;

  @BeforeEach
  public void setUp() {
    warehouseStore = mock(WarehouseStore.class);
    locationResolver = mock(LocationResolver.class);
    useCase = new CreateWarehouseUseCase(warehouseStore, new WarehouseFeasibilityValidator(locationResolver, warehouseStore));

    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(new Location("ZWOLLE-001", 2, 100));
    when(warehouseStore.getAll()).thenReturn(List.of());
    when(warehouseStore.create(any(Warehouse.class)))
        .thenAnswer(
            invocation -> {
              Warehouse w = invocation.getArgument(0);
              w.id = 42L;
              return w;
            });
  }

  private Warehouse newWarehouse(String code, String location, int capacity, int stock) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = code;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    return warehouse;
  }

  @Test
  public void testCreatePersistsWarehouseAndSetsTimestamps() {
    Warehouse warehouse = newWarehouse("MWH.100", "ZWOLLE-001", 50, 10);

    Warehouse created = useCase.create(warehouse);

    assertNotNull(created.createdAt);
    assertEquals(42L, created.id);
    assertNull(created.archivedAt);
    verify(warehouseStore, never()).update(any());
  }

  @Test
  public void testCreateThrowsWhenBusinessUnitCodeAlreadyActive() {
    when(warehouseStore.findByBusinessUnitCode("MWH.100"))
        .thenReturn(newWarehouse("MWH.100", "ZWOLLE-001", 50, 10));

    Warehouse warehouse = newWarehouse("MWH.100", "ZWOLLE-001", 50, 10);

    assertThrows(WarehouseValidationException.class, () -> useCase.create(warehouse));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  public void testCreateThrowsWhenLocationInvalid() {
    Warehouse warehouse = newWarehouse("MWH.101", "NOWHERE", 50, 10);

    assertThrows(WarehouseValidationException.class, () -> useCase.create(warehouse));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  public void testCreateThrowsWhenMaxWarehousesForLocationReached() {
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(new Location("ZWOLLE-001", 1, 100));
    when(warehouseStore.getAll())
        .thenReturn(List.of(newWarehouse("MWH.001", "ZWOLLE-001", 20, 5)));

    Warehouse warehouse = newWarehouse("MWH.101", "ZWOLLE-001", 50, 10);

    assertThrows(WarehouseValidationException.class, () -> useCase.create(warehouse));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  public void testCreateThrowsWhenCapacitySumExceedsLocationMaxCapacity() {
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(new Location("ZWOLLE-001", 5, 40));
    when(warehouseStore.getAll())
        .thenReturn(List.of(newWarehouse("MWH.001", "ZWOLLE-001", 30, 5)));

    Warehouse warehouse = newWarehouse("MWH.101", "ZWOLLE-001", 20, 10);

    assertThrows(WarehouseValidationException.class, () -> useCase.create(warehouse));
    verify(warehouseStore, never()).create(any());
  }

  @Test
  public void testCreateThrowsWhenStockExceedsCapacity() {
    Warehouse warehouse = newWarehouse("MWH.101", "ZWOLLE-001", 10, 20);

    assertThrows(WarehouseValidationException.class, () -> useCase.create(warehouse));
    verify(warehouseStore, never()).create(any());
  }
}