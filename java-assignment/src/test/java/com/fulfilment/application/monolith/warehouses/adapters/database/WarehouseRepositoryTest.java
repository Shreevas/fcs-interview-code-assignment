package com.fulfilment.application.monolith.warehouses.adapters.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** Each test runs inside its own transaction that is rolled back afterwards, keeping the
 * {@code import.sql} seed data stable across tests. */
@QuarkusTest
public class WarehouseRepositoryTest {

  @Inject WarehouseRepository warehouseRepository;

  private static Warehouse newWarehouse(String code, String location, int capacity, int stock) {
    Warehouse warehouse = new Warehouse();
    warehouse.businessUnitCode = code;
    warehouse.location = location;
    warehouse.capacity = capacity;
    warehouse.stock = stock;
    warehouse.createdAt = LocalDateTime.now();
    return warehouse;
  }

  @Test
  @TestTransaction
  public void testCreatePersistsNewRowWithGeneratedId() {
    Warehouse created = warehouseRepository.create(newWarehouse("MWH.900", "HELMOND-001", 20, 5));

    assertNotNull(created.id);
    assertEquals("MWH.900", created.businessUnitCode);
  }

  @Test
  @TestTransaction
  public void testFindByBusinessUnitCodeReturnsActiveRow() {
    warehouseRepository.create(newWarehouse("MWH.901", "HELMOND-001", 20, 5));

    Warehouse found = warehouseRepository.findByBusinessUnitCode("MWH.901");

    assertNotNull(found);
    assertEquals("HELMOND-001", found.location);
  }

  @Test
  @TestTransaction
  public void testFindByBusinessUnitCodeReturnsNullWhenNoActiveRow() {
    assertNull(warehouseRepository.findByBusinessUnitCode("DOES-NOT-EXIST"));
  }

  @Test
  @TestTransaction
  public void testFindByBusinessUnitCodeIgnoresArchivedRows() {
    Warehouse created = warehouseRepository.create(newWarehouse("MWH.902", "HELMOND-001", 20, 5));
    warehouseRepository.remove(created);

    assertNull(warehouseRepository.findByBusinessUnitCode("MWH.902"));
  }

  @Test
  @TestTransaction
  public void testUpdateMutatesActiveRowFields() {
    warehouseRepository.create(newWarehouse("MWH.903", "HELMOND-001", 20, 5));

    Warehouse update = newWarehouse("MWH.903", "HELMOND-001", 40, 15);
    Warehouse updated = warehouseRepository.update(update);

    assertNotNull(updated);
    assertEquals(40, updated.capacity);
    assertEquals(15, updated.stock);
  }

  @Test
  @TestTransaction
  public void testUpdateReturnsNullWhenNoActiveRow() {
    assertNull(warehouseRepository.update(newWarehouse("DOES-NOT-EXIST", "HELMOND-001", 1, 1)));
  }

  @Test
  @TestTransaction
  public void testRemoveSetsArchivedAtOnActiveRow() {
    Warehouse created = warehouseRepository.create(newWarehouse("MWH.904", "HELMOND-001", 20, 5));

    warehouseRepository.remove(created);

    assertNull(warehouseRepository.findByBusinessUnitCode("MWH.904"));
  }

  @Test
  @TestTransaction
  public void testGetAllIncludesBothActiveAndArchivedRows() {
    Warehouse created = warehouseRepository.create(newWarehouse("MWH.905", "HELMOND-001", 20, 5));
    warehouseRepository.remove(created);

    boolean present = warehouseRepository.getAll().stream()
        .anyMatch(w -> "MWH.905".equals(w.businessUnitCode));

    assertEquals(true, present);
  }
}