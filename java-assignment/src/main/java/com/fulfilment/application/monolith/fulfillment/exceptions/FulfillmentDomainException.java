package com.fulfilment.application.monolith.fulfillment.exceptions;

/** Base type for business-rule violations raised by the fulfillment domain layer. */
public abstract class FulfillmentDomainException extends RuntimeException {

  protected FulfillmentDomainException(String message) {
    super(message);
  }
}