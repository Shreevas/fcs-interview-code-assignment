package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ArchiveWarehouseUseCaseTest {

  private WarehouseStore warehouseStore;
  private ArchiveWarehouseUseCase useCase;

  @BeforeEach
  public void setUp() {
    warehouseStore = mock(WarehouseStore.class);
    useCase = new ArchiveWarehouseUseCase(warehouseStore);
  }

  @Test
  public void testArchiveRemovesActiveWarehouse() {
    Warehouse active = new Warehouse();
    active.businessUnitCode = "MWH.001";
    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(active);

    Warehouse request = new Warehouse();
    request.businessUnitCode = "MWH.001";

    useCase.archive(request);

    verify(warehouseStore, times(1)).remove(active);
  }

  @Test
  public void testArchiveThrowsWhenNoActiveWarehouseForCode() {
    when(warehouseStore.findByBusinessUnitCode("MWH.999")).thenReturn(null);

    Warehouse request = new Warehouse();
    request.businessUnitCode = "MWH.999";

    assertThrows(WarehouseNotFoundException.class, () -> useCase.archive(request));
    verify(warehouseStore, never()).remove(org.mockito.ArgumentMatchers.any());
  }
}