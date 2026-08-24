package com.fulfilment.application.monolith.fulfillment.adapter.restapi;

/** Request payload for associating a warehouse as a fulfillment unit of a product at a store. */
public class FulfillmentRequest {

  public Long productId;
  public Long storeId;
  public Long warehouseId;
}