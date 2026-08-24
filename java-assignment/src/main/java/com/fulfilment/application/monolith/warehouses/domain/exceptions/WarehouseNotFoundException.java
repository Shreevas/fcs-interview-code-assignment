package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/** Raised when a warehouse operation references a business unit code with no active warehouse. */
public class WarehouseNotFoundException extends WarehouseDomainException {

  public WarehouseNotFoundException(String message) {
    super(message);
  }
}