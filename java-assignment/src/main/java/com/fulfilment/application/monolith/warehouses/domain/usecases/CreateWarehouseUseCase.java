package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CreateWarehouseUseCase implements CreateWarehouseOperation {

  private static final Logger LOGGER = Logger.getLogger(CreateWarehouseUseCase.class);

  private final WarehouseStore warehouseStore;
  private final WarehouseFeasibilityValidator validator;

  public CreateWarehouseUseCase(WarehouseStore warehouseStore, WarehouseFeasibilityValidator validator) {
    this.warehouseStore = warehouseStore;
    this.validator = validator;
  }

  @Override
  public Warehouse create(Warehouse warehouse) {
    if (warehouseStore.findByBusinessUnitCode(warehouse.businessUnitCode) != null) {
      throw new WarehouseValidationException(
          "A warehouse with business unit code '" + warehouse.businessUnitCode + "' already exists.");
    }

    Location location = validator.requireExistingLocation(warehouse.location);
    List<Warehouse> activeAtLocation = validator.activeWarehousesAtLocation(warehouse.location, null);

    validator.requireWarehouseCountFeasible(location, activeAtLocation);
    validator.requireCapacityFeasible(location, activeAtLocation, warehouse.capacity);
    validator.requireStockWithinCapacity(warehouse.capacity, warehouse.stock);

    warehouse.createdAt = LocalDateTime.now();
    warehouse.archivedAt = null;

    Warehouse created = warehouseStore.create(warehouse);
    LOGGER.infof("Created warehouse [ businessUnitCode=%s, location=%s ]", created.businessUnitCode, created.location);
    return created;
  }
}