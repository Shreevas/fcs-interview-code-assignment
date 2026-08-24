package com.fulfilment.application.monolith.warehouses.adapters.database;

import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class WarehouseRepository implements WarehouseStore, PanacheRepository<DbWarehouse> {

  private static final String ACTIVE_BY_BUSINESS_UNIT_CODE_QUERY =
      "businessUnitCode = ?1 and archivedAt is null";

  @Override
  public List<Warehouse> getAll() {
    return this.listAll().stream().map(DbWarehouse::toWarehouse).toList();
  }

  @Override
  public Warehouse create(Warehouse warehouse) {
    DbWarehouse entity = new DbWarehouse();
    entity.businessUnitCode = warehouse.businessUnitCode;
    entity.location = warehouse.location;
    entity.capacity = warehouse.capacity;
    entity.stock = warehouse.stock;
    entity.createdAt = warehouse.createdAt != null ? warehouse.createdAt : LocalDateTime.now();
    entity.archivedAt = warehouse.archivedAt;

    persist(entity);

    return entity.toWarehouse();
  }

  @Override
  public Warehouse update(Warehouse warehouse) {
    DbWarehouse entity = findActiveByBusinessUnitCode(warehouse.businessUnitCode);
    if (entity == null) {
      return null;
    }

    entity.location = warehouse.location;
    entity.capacity = warehouse.capacity;
    entity.stock = warehouse.stock;

    return entity.toWarehouse();
  }

  @Override
  public void remove(Warehouse warehouse) {
    DbWarehouse entity = findActiveByBusinessUnitCode(warehouse.businessUnitCode);
    if (entity != null) {
      entity.archivedAt = LocalDateTime.now();
    }
  }

  @Override
  public Warehouse findByBusinessUnitCode(String buCode) {
    DbWarehouse entity = findActiveByBusinessUnitCode(buCode);
    return entity == null ? null : entity.toWarehouse();
  }

  private DbWarehouse findActiveByBusinessUnitCode(String buCode) {
    return find(ACTIVE_BY_BUSINESS_UNIT_CODE_QUERY, buCode).firstResult();
  }
}