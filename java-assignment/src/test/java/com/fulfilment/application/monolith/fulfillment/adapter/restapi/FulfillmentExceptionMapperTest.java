package com.fulfilment.application.monolith.fulfillment.adapter.restapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fulfilment.application.monolith.fulfillment.exceptions.FulfillmentConflictException;
import com.fulfilment.application.monolith.fulfillment.exceptions.FulfillmentConstraintViolationException;
import com.fulfilment.application.monolith.fulfillment.exceptions.FulfillmentNotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FulfillmentExceptionMapperTest {

  private FulfillmentExceptionMapper mapper;

  @BeforeEach
  public void setUp() {
    mapper = new FulfillmentExceptionMapper();
    mapper.objectMapper = new ObjectMapper();
  }

  @Test
  public void testNotFoundMapsTo404() {
    assertEquals(404, mapper.toResponse(new FulfillmentNotFoundException("nope")).getStatus());
  }

  @Test
  public void testConflictMapsTo409() {
    assertEquals(409, mapper.toResponse(new FulfillmentConflictException("nope")).getStatus());
  }

  @Test
  public void testConstraintViolationMapsTo422() {
    assertEquals(
        422, mapper.toResponse(new FulfillmentConstraintViolationException("nope")).getStatus());
  }
}