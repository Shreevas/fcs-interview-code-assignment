package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/** Raised when a warehouse create/replace request violates one of the business rules. */
public class WarehouseValidationException extends WarehouseDomainException {

  public WarehouseValidationException(String message) {
    super(message);
  }
}