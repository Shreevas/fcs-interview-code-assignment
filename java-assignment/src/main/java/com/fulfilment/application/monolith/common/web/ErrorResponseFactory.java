package com.fulfilment.application.monolith.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.core.Response;

/** Builds the JSON error payload shared by every REST resource's exception mapper. */
public final class ErrorResponseFactory {

  private ErrorResponseFactory() {}

  public static Response build(int status, Throwable exception, ObjectMapper objectMapper) {
    ObjectNode exceptionJson = objectMapper.createObjectNode();
    exceptionJson.put("exceptionType", exception.getClass().getName());
    exceptionJson.put("code", status);

    if (exception.getMessage() != null) {
      exceptionJson.put("error", exception.getMessage());
    }

    return Response.status(status).entity(exceptionJson).build();
  }
}