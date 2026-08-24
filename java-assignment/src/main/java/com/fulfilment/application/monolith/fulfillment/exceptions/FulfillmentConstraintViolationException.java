package com.fulfilment.application.monolith.fulfillment.exceptions;

/**
 * Raised when creating an association would violate one of the fulfillment cardinality
 * constraints (max warehouses per product/store, max warehouses per store, max products per
 * warehouse).
 */
public class FulfillmentConstraintViolationException extends FulfillmentDomainException {

  public FulfillmentConstraintViolationException(String message) {
    super(message);
  }
}