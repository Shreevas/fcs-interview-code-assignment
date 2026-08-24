package com.fulfilment.application.monolith.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/** Catch-all fallback mapper for exceptions not handled by a more specific mapper. */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

  private static final Logger LOGGER = Logger.getLogger(GenericExceptionMapper.class);

  @Inject ObjectMapper objectMapper;

  @Override
  public Response toResponse(Exception exception) {
    LOGGER.error("Failed to handle request", exception);

    int code = 500;
    if (exception instanceof WebApplicationException webApplicationException) {
      code = webApplicationException.getResponse().getStatus();
    }

    return ErrorResponseFactory.build(code, exception, objectMapper);
  }
}