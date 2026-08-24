package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Full REST-surface coverage for the Warehouse endpoints. Runs as an in-process {@code
 * @QuarkusTest} (rather than {@code @QuarkusIntegrationTest}) so it actually executes under plain
 * {@code ./mvnw test} and is captured by the JaCoCo coverage agent.
 */
@QuarkusTest
public class WarehouseResourceTest {

  private static final String PATH = "warehouse";

  @Test
  public void testListAllWarehousesUnitsReturnsSeedData() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("MWH.012"), containsString("MWH.023"));
  }

  @Test
  public void testCreateANewWarehouseUnitPersistsAndReturnsIt() {
    String id = createWarehouse("MWH.100", "HELMOND-001", 30, 10);

    given()
        .when()
        .get(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body(containsString("MWH.100"), containsString("HELMOND-001"));
  }

  @Test
  public void testCreateANewWarehouseUnitReturns400WhenBusinessUnitCodeAlreadyExists() {
    createRequest("MWH.001", "VETSBY-001", 10, 5).when().post(PATH).then().statusCode(400);
  }

  @Test
  public void testCreateANewWarehouseUnitReturns400WhenLocationInvalid() {
    createRequest("MWH.101", "NOWHERE-001", 10, 5).when().post(PATH).then().statusCode(400);
  }

  @Test
  public void testCreateANewWarehouseUnitReturns400WhenMaxWarehousesReachedForLocation() {
    // ZWOLLE-001 already has its one allowed active warehouse from the seed data (MWH.001).
    createRequest("MWH.102", "ZWOLLE-001", 10, 5).when().post(PATH).then().statusCode(400);
  }

  @Test
  public void testCreateANewWarehouseUnitReturns400WhenStockExceedsCapacity() {
    createRequest("MWH.103", "EINDHOVEN-001", 10, 20).when().post(PATH).then().statusCode(400);
  }

  @Test
  public void testGetAWarehouseUnitByIDReturns404WhenNotFound() {
    given().when().get(PATH + "/999999").then().statusCode(404);
  }

  @Test
  public void testArchiveAWarehouseUnitByIDPositive() {
    String id = createWarehouse("MWH.200", "ZWOLLE-002", 20, 5);

    given().when().delete(PATH + "/" + id).then().statusCode(204);

    given().when().get(PATH + "/" + id).then().statusCode(404);
  }

  @Test
  public void testArchiveAWarehouseUnitByIDReturns404WhenMissing() {
    given().when().delete(PATH + "/999999").then().statusCode(404);
  }

  @Test
  public void testArchiveAWarehouseUnitByIDReturns404WhenAlreadyArchived() {
    String id = createWarehouse("MWH.201", "AMSTERDAM-002", 20, 5);

    given().when().delete(PATH + "/" + id).then().statusCode(204);
    given().when().delete(PATH + "/" + id).then().statusCode(404);
  }

  @Test
  public void testReplaceTheCurrentActiveWarehouseArchivesOldAndCreatesNew() {
    String oldId = createWarehouse("MWH.300", "AMSTERDAM-002", 40, 10);

    String body = warehouseJson("MWH.300", "AMSTERDAM-002", 45, 10);
    given()
        .contentType("application/json")
        .body(body)
        .when()
        .post(PATH + "/MWH.300/replacement")
        .then()
        .statusCode(200)
        .body(containsString("MWH.300"));

    given().when().get(PATH + "/" + oldId).then().statusCode(404);
  }

  @Test
  public void testReplaceTheCurrentActiveWarehouseReturns404WhenBusinessUnitCodeNotActive() {
    String body = warehouseJson("MWH.999", "ZWOLLE-002", 10, 5);

    given()
        .contentType("application/json")
        .body(body)
        .when()
        .post(PATH + "/MWH.999/replacement")
        .then()
        .statusCode(404);
  }

  @Test
  public void testReplaceTheCurrentActiveWarehouseReturns400WhenStockMismatch() {
    createWarehouse("MWH.400", "VETSBY-001", 50, 20);

    String body = warehouseJson("MWH.400", "VETSBY-001", 50, 21);
    given()
        .contentType("application/json")
        .body(body)
        .when()
        .post(PATH + "/MWH.400/replacement")
        .then()
        .statusCode(400);
  }

  @Test
  public void testReplaceTheCurrentActiveWarehouseReturns400WhenCapacityCannotAccommodateOldStock() {
    createWarehouse("MWH.402", "EINDHOVEN-001", 50, 30);

    // new capacity (10) is below the old warehouse's stock (30)
    String body = warehouseJson("MWH.402", "EINDHOVEN-001", 10, 10);
    given()
        .contentType("application/json")
        .body(body)
        .when()
        .post(PATH + "/MWH.402/replacement")
        .then()
        .statusCode(400);
  }

  private static String warehouseJson(String code, String location, int capacity, int stock) {
    return String.format(
        "{\"businessUnitCode\":\"%s\",\"location\":\"%s\",\"capacity\":%d,\"stock\":%d}",
        code, location, capacity, stock);
  }

  private static io.restassured.specification.RequestSpecification createRequest(
      String code, String location, int capacity, int stock) {
    return given().contentType("application/json").body(warehouseJson(code, location, capacity, stock));
  }

  private static String createWarehouse(String code, String location, int capacity, int stock) {
    return createRequest(code, location, capacity, stock)
        .when()
        .post(PATH)
        .then()
        .statusCode(200)
        .extract()
        .jsonPath()
        .getString("id");
  }
}