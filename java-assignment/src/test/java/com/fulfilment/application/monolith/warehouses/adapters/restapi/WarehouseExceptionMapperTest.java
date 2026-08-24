package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseNotFoundException;
import com.fulfilment.application.monolith.warehouses.domain.exceptions.WarehouseValidationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WarehouseExceptionMapperTest {

  private WarehouseExceptionMapper mapper;

  @BeforeEach
  public void setUp() {
    mapper = new WarehouseExceptionMapper();
    mapper.objectMapper = new ObjectMapper();
  }

  @Test
  public void testNotFoundMapsTo404() {
    Response response = mapper.toResponse(new WarehouseNotFoundException("nope"));

    assertEquals(404, response.getStatus());
  }

  @Test
  public void testValidationMapsTo400() {
    Response response = mapper.toResponse(new WarehouseValidationException("nope"));

    assertEquals(400, response.getStatus());
  }
}