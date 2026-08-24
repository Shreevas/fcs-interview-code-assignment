package com.fulfilment.application.monolith.fulfillment.exceptions;

/** Raised when a referenced product, store, warehouse, or association does not exist. */
public class FulfillmentNotFoundException extends FulfillmentDomainException {

  public FulfillmentNotFoundException(String message) {
    super(message);
  }
}