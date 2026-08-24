package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Objects;

/**
 * Shared feasibility checks for creating or replacing a warehouse. Kept separate from the use
 * cases so the location/capacity/count rules aren't duplicated between {@link
 * CreateWarehouseUseCase} and {@link ReplaceWarehouseUseCase}.
 */
@ApplicationScoped
public class WarehouseFeasibilityValidator {

  private final LocationResolver locationResolver;
  private final WarehouseStore warehouseStore;

  public WarehouseFeasibilityValidator(LocationResolver locationResolver, WarehouseStore warehouseStore) {
    this.locationResolver = locationResolver;
    this.warehouseStore = warehouseStore;
  }

  public Location requireExistingLocation(String identifier) {
    Location location = locationResolver.resolveByIdentifier(identifier);
    if (location == null) {
      throw new WarehouseValidationException("Location '" + identifier + "' does not exist.");
    }
    return location;
  }

  /**
   * Active (non-archived) warehouses currently at the given location, optionally excluding one
   * business unit code (used by replace to exclude the warehouse being replaced from its own
   * location's tally, since it is archived as part of the same operation).
   */
  public List<Warehouse> activeWarehousesAtLocation(String locationIdentifier, String excludeBusinessUnitCode) {
    return warehouseStore.getAll().stream()
        .filter(w -> w.archivedAt == null)
        .filter(w -> Objects.equals(w.location, locationIdentifier))
        .filter(w -> excludeBusinessUnitCode == null || !excludeBusinessUnitCode.equals(w.businessUnitCode))
        .toList();
  }

  public void requireWarehouseCountFeasible(Location location, List<Warehouse> activeAtLocation) {
    if (activeAtLocation.size() >= location.maxNumberOfWarehouses) {
      throw new WarehouseValidationException(
          "Maximum number of warehouses reached for location '" + location.identification + "'.");
    }
  }

  public void requireCapacityFeasible(Location location, List<Warehouse> activeAtLocation, int newCapacity) {
    int currentCapacity = activeAtLocation.stream().mapToInt(w -> w.capacity).sum();
    if (currentCapacity + newCapacity > location.maxCapacity) {
      throw new WarehouseValidationException(
          "Warehouse capacity exceeds the maximum capacity available for location '"
              + location.identification
              + "'.");
    }
  }

  public void requireStockWithinCapacity(int capacity, int stock) {
    if (stock > capacity) {
      throw new WarehouseValidationException("Warehouse stock cannot exceed its capacity.");
    }
  }
}