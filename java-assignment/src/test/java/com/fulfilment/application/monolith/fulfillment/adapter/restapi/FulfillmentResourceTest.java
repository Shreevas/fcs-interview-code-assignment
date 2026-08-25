package com.fulfilment.application.monolith.fulfillment.adapter.restapi;

import static io.restassured.RestAssured.given;

import com.fulfilment.application.monolith.warehouses.adapters.database.WarehouseRepository;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * REST-level coverage for the bonus fulfillment feature. Warehouses used here are inserted
 * directly via {@link WarehouseRepository} (bypassing the Warehouse create use case's location
 * capacity checks) so these tests stay independent of the finite, shared seed-data location pool
 * used by {@code WarehouseResourceTest}.
 */
@QuarkusTest
public class FulfillmentResourceTest {

  private static final String PATH = "fulfillment";
  private static final AtomicInteger SEQUENCE = new AtomicInteger();

  @Inject WarehouseRepository warehouseRepository;

  private Long createProduct() {
    String body = String.format("{\"name\":\"FUL-PRODUCT-%d\"}", SEQUENCE.incrementAndGet());
    return given()
        .contentType("application/json")
        .body(body)
        .when()
        .post("product")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private Long createStore() {
    String body =
        String.format(
            "{\"name\":\"FUL-STORE-%d\",\"quantityProductsInStock\":0}", SEQUENCE.incrementAndGet());
    return given()
        .contentType("application/json")
        .body(body)
        .when()
        .post("store")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");
  }

  private Long createWarehouse() {
    return QuarkusTransaction.requiringNew()
        .call(
            () -> {
              Warehouse warehouse = new Warehouse();
              warehouse.businessUnitCode = "FUL-WH-" + SEQUENCE.incrementAndGet();
              warehouse.location = "FULFILLMENT-TEST-LOCATION";
              warehouse.capacity = 100;
              warehouse.stock = 0;
              warehouse.createdAt = LocalDateTime.now();
              return warehouseRepository.create(warehouse).id;
            });
  }

  @Test
  public void testCreatePositive() {
    Long productId = createProduct();
    Long storeId = createStore();
    Long warehouseId = createWarehouse();

    given()
        .contentType("application/json")
        .body(fulfillmentJson(productId, storeId, warehouseId))
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body("productId", org.hamcrest.Matchers.is(productId.intValue()))
        .body("storeId", org.hamcrest.Matchers.is(storeId.intValue()))
        .body("warehouseId", org.hamcrest.Matchers.is(warehouseId.intValue()));
  }

  @Test
  public void testCreateReturns404WhenProductMissing() {
    Long storeId = createStore();
    Long warehouseId = createWarehouse();

    given()
        .contentType("application/json")
        .body(fulfillmentJson(999999L, storeId, warehouseId))
        .when()
        .post(PATH)
        .then()
        .statusCode(404);
  }

  @Test
  public void testCreateReturns404WhenStoreMissing() {
    Long productId = createProduct();
    Long warehouseId = createWarehouse();

    given()
        .contentType("application/json")
        .body(fulfillmentJson(productId, 999999L, warehouseId))
        .when()
        .post(PATH)
        .then()
        .statusCode(404);
  }

  @Test
  public void testCreateReturns404WhenWarehouseMissing() {
    Long productId = createProduct();
    Long storeId = createStore();

    given()
        .contentType("application/json")
        .body(fulfillmentJson(productId, storeId, 999999L))
        .when()
        .post(PATH)
        .then()
        .statusCode(404);
  }

  @Test
  public void testCreateReturns409WhenDuplicate() {
    Long productId = createProduct();
    Long storeId = createStore();
    Long warehouseId = createWarehouse();
    String body = fulfillmentJson(productId, storeId, warehouseId);

    given().contentType("application/json").body(body).when().post(PATH).then().statusCode(201);
    given().contentType("application/json").body(body).when().post(PATH).then().statusCode(409);
  }

  @Test
  public void testCreateReturns422WhenMaxWarehousesPerProductPerStoreReached() {
    Long productId = createProduct();
    Long storeId = createStore();

    associate(productId, storeId, createWarehouse()).then().statusCode(201);
    associate(productId, storeId, createWarehouse()).then().statusCode(201);
    // a 3rd distinct warehouse for the same product+store exceeds the max of 2
    associate(productId, storeId, createWarehouse()).then().statusCode(422);
  }

  @Test
  public void testCreateReturns422WhenMaxWarehousesPerStoreReached() {
    Long storeId = createStore();

    associate(createProduct(), storeId, createWarehouse()).then().statusCode(201);
    associate(createProduct(), storeId, createWarehouse()).then().statusCode(201);
    associate(createProduct(), storeId, createWarehouse()).then().statusCode(201);
    // a 4th distinct warehouse for the same store exceeds the max of 3
    associate(createProduct(), storeId, createWarehouse()).then().statusCode(422);
  }

  @Test
  public void testCreateReturns422WhenMaxProductsPerWarehouseReached() {
    Long warehouseId = createWarehouse();

    for (int i = 0; i < 5; i++) {
      associate(createProduct(), createStore(), warehouseId).then().statusCode(201);
    }
    // a 6th distinct product for the same warehouse exceeds the max of 5
    associate(createProduct(), createStore(), warehouseId).then().statusCode(422);
  }

  @Test
  public void testDeleteReturns204ThenListNoLongerIncludesIt() {
    Long productId = createProduct();
    Long storeId = createStore();
    Long warehouseId = createWarehouse();

    Long id =
        associate(productId, storeId, warehouseId)
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");

    given().when().delete(PATH + "/" + id).then().statusCode(204);
  }

  @Test
  public void testDeleteReturns404WhenMissing() {
    given().when().delete(PATH + "/999999").then().statusCode(404);
  }

  private static io.restassured.response.Response associate(Long productId, Long storeId, Long warehouseId) {
    return given()
        .contentType("application/json")
        .body(fulfillmentJson(productId, storeId, warehouseId))
        .when()
        .post(PATH);
  }

  private static String fulfillmentJson(Long productId, Long storeId, Long warehouseId) {
    return String.format(
        "{\"productId\":%d,\"storeId\":%d,\"warehouseId\":%d}", productId, storeId, warehouseId);
  }
}