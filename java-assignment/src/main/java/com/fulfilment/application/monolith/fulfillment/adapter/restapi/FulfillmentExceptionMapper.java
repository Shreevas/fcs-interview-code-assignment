package com.fulfilment.application.monolith.fulfillment.adapter.restapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfilment.application.monolith.common.web.ErrorResponseFactory;
import com.fulfilment.application.monolith.fulfillment.exceptions.FulfillmentConflictException;
import com.fulfilment.application.monolith.fulfillment.exceptions.FulfillmentConstraintViolationException;
import com.fulfilment.application.monolith.fulfillment.exceptions.FulfillmentDomainException;
import com.fulfilment.application.monolith.fulfillment.exceptions.FulfillmentNotFoundException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class FulfillmentExceptionMapper implements ExceptionMapper<FulfillmentDomainException> {

  private static final Logger LOGGER = Logger.getLogger(FulfillmentExceptionMapper.class);

  @Inject ObjectMapper objectMapper;

  @Override
  public Response toResponse(FulfillmentDomainException exception) {
    int status;
    if (exception instanceof FulfillmentNotFoundException) {
      status = 404;
    } else if (exception instanceof FulfillmentConflictException) {
      status = 409;
    } else if (exception instanceof FulfillmentConstraintViolationException) {
      status = 422;
    } else {
      status = 400;
    }

    LOGGER.warnf("Rejected fulfillment request: %s", exception.getMessage());
    return ErrorResponseFactory.build(status, exception, objectMapper);
  }
}