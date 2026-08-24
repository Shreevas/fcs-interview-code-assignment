package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WarehouseFeasibilityValidatorTest {

  private LocationResolver locationResolver;
  private WarehouseStore warehouseStore;
  private WarehouseFeasibilityValidator validator;

  @BeforeEach
  public void setUp() {
    locationResolver = mock(LocationResolver.class);
    warehouseStore = mock(WarehouseStore.class);
    validator = new WarehouseFeasibilityValidator(locationResolver, warehouseStore);
  }

  @Test
  public void testRequireExistingLocationReturnsLocationWhenFound() {
    Location location = new Location("ZWOLLE-001", 1, 40);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001")).thenReturn(location);

    assertEquals(location, validator.requireExistingLocation("ZWOLLE-001"));
  }

  @Test
  public void testRequireExistingLocationThrowsWhenNotFound() {
    when(locationResolver.resolveByIdentifier("UNKNOWN")).thenReturn(null);

    assertThrows(
        WarehouseValidationException.class, () -> validator.requireExistingLocation("UNKNOWN"));
  }

  @Test
  public void testActiveWarehousesAtLocationExcludesArchivedAndOtherLocations() {
    Warehouse active = warehouse("MWH.001", "ZWOLLE-001", null);
    Warehouse archived = warehouse("MWH.002", "ZWOLLE-001", java.time.LocalDateTime.now());
    Warehouse elsewhere = warehouse("MWH.003", "TILBURG-001", null);
    when(warehouseStore.getAll()).thenReturn(List.of(active, archived, elsewhere));

    List<Warehouse> result = validator.activeWarehousesAtLocation("ZWOLLE-001", null);

    assertEquals(1, result.size());
    assertEquals("MWH.001", result.get(0).businessUnitCode);
  }

  @Test
  public void testActiveWarehousesAtLocationExcludesGivenBusinessUnitCode() {
    Warehouse w1 = warehouse("MWH.001", "ZWOLLE-001", null);
    Warehouse w2 = warehouse("MWH.002", "ZWOLLE-001", null);
    when(warehouseStore.getAll()).thenReturn(List.of(w1, w2));

    List<Warehouse> result = validator.activeWarehousesAtLocation("ZWOLLE-001", "MWH.001");

    assertEquals(1, result.size());
    assertEquals("MWH.002", result.get(0).businessUnitCode);
  }

  @Test
  public void testRequireWarehouseCountFeasibleThrowsWhenAtMax() {
    Location location = new Location("ZWOLLE-001", 1, 40);
    List<Warehouse> active = List.of(warehouse("MWH.001", "ZWOLLE-001", null));

    assertThrows(
        WarehouseValidationException.class,
        () -> validator.requireWarehouseCountFeasible(location, active));
  }

  @Test
  public void testRequireWarehouseCountFeasiblePassesWhenBelowMax() {
    Location location = new Location("AMSTERDAM-001", 5, 100);
    List<Warehouse> active = List.of(warehouse("MWH.001", "AMSTERDAM-001", null));

    validator.requireWarehouseCountFeasible(location, active);
  }

  @Test
  public void testRequireCapacityFeasibleThrowsWhenSumExceedsMax() {
    Location location = new Location("ZWOLLE-001", 5, 40);
    Warehouse existing = warehouse("MWH.001", "ZWOLLE-001", null);
    existing.capacity = 30;

    assertThrows(
        WarehouseValidationException.class,
        () -> validator.requireCapacityFeasible(location, List.of(existing), 20));
  }

  @Test
  public void testRequireCapacityFeasiblePassesWhenSumWithinMax() {
    Location location = new Location("ZWOLLE-001", 5, 40);
    Warehouse existing = warehouse("MWH.001", "ZWOLLE-001", null);
    existing.capacity = 10;

    validator.requireCapacityFeasible(location, List.of(existing), 20);
  }

  @Test
  public void testRequireStockWithinCapacityThrowsWhenStockExceedsCapacity() {
    assertThrows(
        WarehouseValidationException.class, () -> validator.requireStockWithinCapacity(10, 20));
  }

  @Test
  public void testRequireStockWithinCapacityPassesWhenStockWithinCapacity() {
    validator.requireStockWithinCapacity(10, 10);
  }

  private static Warehouse warehouse(
      String businessUnitCode, String location, java.time.LocalDateTime archivedAt) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = businessUnitCode;
    warehouse.location = location;
    warehouse.capacity = 10;
    warehouse.stock = 5;
    warehouse.archivedAt = archivedAt;
    return warehouse;
  }
}