package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfilment.application.monolith.common.web.ErrorResponseFactory;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseDomainException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class WarehouseExceptionMapper implements ExceptionMapper<WarehouseDomainException> {

  private static final Logger LOGGER = Logger.getLogger(WarehouseExceptionMapper.class);

  @Inject ObjectMapper objectMapper;

  @Override
  public Response toResponse(WarehouseDomainException exception) {
    int status = (exception instanceof WarehouseNotFoundException) ? 404 : 400;
    LOGGER.warnf("Rejected warehouse request: %s", exception.getMessage());
    return ErrorResponseFactory.build(status, exception, objectMapper);
  }
}