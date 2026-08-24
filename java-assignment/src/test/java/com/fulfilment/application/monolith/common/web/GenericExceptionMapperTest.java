package com.fulfilment.application.monolith.common.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GenericExceptionMapperTest {

  private GenericExceptionMapper mapper;

  @BeforeEach
  public void setUp() {
    mapper = new GenericExceptionMapper();
    mapper.objectMapper = new ObjectMapper();
  }

  @Test
  public void testToResponseUsesStatusFromWebApplicationException() {
    Response response = mapper.toResponse(new WebApplicationException("nope", 404));

    assertEquals(404, response.getStatus());
  }

  @Test
  public void testToResponseDefaultsTo500ForPlainException() {
    Response response = mapper.toResponse(new RuntimeException("boom"));

    assertEquals(500, response.getStatus());
  }
}