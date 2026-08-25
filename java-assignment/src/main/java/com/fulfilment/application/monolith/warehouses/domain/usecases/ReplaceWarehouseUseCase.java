package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import com.fulfilment.application.monolith.warehouses.domain.validators.WarehouseFeasibilityValidator;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private static final Logger LOGGER = Logger.getLogger(ReplaceWarehouseUseCase.class);

  private final WarehouseStore warehouseStore;
  private final WarehouseFeasibilityValidator validator;

  public ReplaceWarehouseUseCase(WarehouseStore warehouseStore, WarehouseFeasibilityValidator validator) {
    this.warehouseStore = warehouseStore;
    this.validator = validator;
  }

  @Override
  public Warehouse replace(Warehouse newWarehouse) {
    Warehouse old = warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (old == null) {
      throw new WarehouseNotFoundException(
          "No active warehouse found for business unit code '" + newWarehouse.businessUnitCode + "'.");
    }

    if (!Objects.equals(old.location, newWarehouse.location)) {
      throw new WarehouseValidationException(
          "A warehouse cannot be replaced with one at a different location.");
    }

    Location location = validator.requireExistingLocation(newWarehouse.location);
    List<Warehouse> activeAtLocation =
        validator.activeWarehousesAtLocation(newWarehouse.location, old.businessUnitCode);

    validator.requireWarehouseCountFeasible(location, activeAtLocation);
    validator.requireCapacityFeasible(location, activeAtLocation, newWarehouse.capacity);
    validator.requireStockWithinCapacity(newWarehouse.capacity, newWarehouse.stock);

    // Capacity Accommodation: the new warehouse must be able to hold the stock being carried over.
    if (newWarehouse.capacity < old.stock) {
      throw new WarehouseValidationException(
          "New warehouse capacity cannot accommodate the stock of the warehouse being replaced.");
    }

    // Stock Matching: the carried-over stock must match exactly.
    if (!Objects.equals(newWarehouse.stock, old.stock)) {
      throw new WarehouseValidationException(
          "New warehouse stock must match the stock of the warehouse being replaced.");
    }

    warehouseStore.remove(old);

    newWarehouse.createdAt = LocalDateTime.now();
    newWarehouse.archivedAt = null;

    Warehouse created = warehouseStore.create(newWarehouse);
    LOGGER.infof(
        "Replaced warehouse [ businessUnitCode=%s, oldId=%d, newId=%d ]",
        created.businessUnitCode, old.id, created.id);
    return created;
  }
}