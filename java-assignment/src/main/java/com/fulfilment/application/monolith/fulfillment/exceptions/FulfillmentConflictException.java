package com.fulfilment.application.monolith.fulfillment.exceptions;

/** Raised when the exact same product/store/warehouse association already exists. */
public class FulfillmentConflictException extends FulfillmentDomainException {

  public FulfillmentConflictException(String message) {
    super(message);
  }
}