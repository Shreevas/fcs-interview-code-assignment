package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fulfilment.application.monolith.warehouses.adapters.database.DbWarehouse;
import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.ports.ArchiveWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.CreateWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.warehouse.api.WarehouseResource;
import com.warehouse.api.beans.Warehouse;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@RequestScoped
public class WarehouseResourceImpl implements WarehouseResource {

  @Inject private WarehouseRepository warehouseRepository;
  @Inject private CreateWarehouseOperation createWarehouseOperation;
  @Inject private ReplaceWarehouseOperation replaceWarehouseOperation;
  @Inject private ArchiveWarehouseOperation archiveWarehouseOperation;

  @Override
  public List<Warehouse> listAllWarehousesUnits() {
    return warehouseRepository.getAll().stream().map(this::toWarehouseResponse).toList();
  }

  @Override
  @Transactional
  public Warehouse createANewWarehouseUnit(@NotNull Warehouse data) {
    var created = createWarehouseOperation.create(toDomainWarehouse(data));
    return toWarehouseResponse(created);
  }

  @Override
  public Warehouse getAWarehouseUnitByID(String id) {
    DbWarehouse entity = findExistingById(id);
    return toWarehouseResponse(entity.toWarehouse());
  }

  @Override
  @Transactional
  public void archiveAWarehouseUnitByID(String id) {
    DbWarehouse entity = findExistingById(id);
    archiveWarehouseOperation.archive(entity.toWarehouse());
  }

  @Override
  @Transactional
  public Warehouse replaceTheCurrentActiveWarehouse(String businessUnitCode, @NotNull Warehouse data) {
    var newWarehouse = toDomainWarehouse(data);
    newWarehouse.businessUnitCode = businessUnitCode;

    var replaced = replaceWarehouseOperation.replace(newWarehouse);
    return toWarehouseResponse(replaced);
  }

  private DbWarehouse findExistingById(String id) {
    DbWarehouse entity;
    try {
      entity = warehouseRepository.findById(Long.valueOf(id));
    } catch (NumberFormatException e) {
      throw new WarehouseNotFoundException("Warehouse with id of " + id + " does not exist.");
    }

    if (entity == null || entity.archivedAt != null) {
      throw new WarehouseNotFoundException("Warehouse with id of " + id + " does not exist.");
    }
    return entity;
  }

  private Warehouse toWarehouseResponse(
      com.fulfilment.application.monolith.warehouses.domain.models.Warehouse warehouse) {
    var response = new Warehouse();
    if (warehouse.id != null) {
      response.setId(String.valueOf(warehouse.id));
    }
    response.setBusinessUnitCode(warehouse.businessUnitCode);
    response.setLocation(warehouse.location);
    response.setCapacity(warehouse.capacity);
    response.setStock(warehouse.stock);

    return response;
  }

  private com.fulfilment.application.monolith.warehouses.domain.models.Warehouse toDomainWarehouse(
      Warehouse data) {
    var warehouse = new com.fulfilment.application.monolith.warehouses.domain.models.Warehouse();
    warehouse.businessUnitCode = data.getBusinessUnitCode();
    warehouse.location = data.getLocation();
    warehouse.capacity = data.getCapacity();
    warehouse.stock = data.getStock();
    return warehouse;
  }
}