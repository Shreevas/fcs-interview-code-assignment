package com.fulfilment.application.monolith.warehouses.domain.exceptions;

/**
 * Base type for business-rule violations raised by the warehouse domain layer. Deliberately
 * free of any JAX-RS dependency so the domain/usecases packages stay framework-agnostic; the
 * translation to HTTP status codes happens in the adapters.restapi layer.
 */
public abstract class WarehouseDomainException extends RuntimeException {

  protected WarehouseDomainException(String message) {
    super(message);
  }
}