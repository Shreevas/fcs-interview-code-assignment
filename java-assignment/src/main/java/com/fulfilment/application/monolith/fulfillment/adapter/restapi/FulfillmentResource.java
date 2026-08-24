package com.fulfilment.application.monolith.fulfillment.adapter.restapi;

import com.fulfilment.application.monolith.fulfillment.model.Fulfillment;
import com.fulfilment.application.monolith.fulfillment.service.FulfillmentService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("fulfillment")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class FulfillmentResource {

  @Inject FulfillmentService fulfillmentService;

  @GET
  public List<Fulfillment> listAll() {
    return fulfillmentService.listAll();
  }

  @GET
  @Path("store/{storeId}")
  public List<Fulfillment> listByStore(@PathParam("storeId") Long storeId) {
    return fulfillmentService.listByStore(storeId);
  }

  @POST
  public Response create(FulfillmentRequest request) {
    if (request.productId == null || request.storeId == null || request.warehouseId == null) {
      throw new WebApplicationException("productId, storeId and warehouseId are all required.", 422);
    }

    Fulfillment created =
        fulfillmentService.createAssociation(request.productId, request.storeId, request.warehouseId);
    return Response.ok(created).status(201).build();
  }

  @DELETE
  @Path("{id}")
  public Response delete(@PathParam("id") Long id) {
    fulfillmentService.remove(id);
    return Response.status(204).build();
  }
}