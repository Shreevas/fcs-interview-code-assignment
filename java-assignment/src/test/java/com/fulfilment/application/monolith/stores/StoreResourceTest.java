package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class StoreResourceTest {

  @InjectMock LegacyStoreManagerGateway legacyStoreManagerGateway;

  @Test
  public void testCreateReturns201AndInvokesLegacyGateway() {
    String body = "{\"name\":\"HEMNES\",\"quantityProductsInStock\":7}";

    given()
        .contentType("application/json")
        .body(body)
        .when()
        .post("store")
        .then()
        .statusCode(201)
        .body(containsString("HEMNES"));

    verify(legacyStoreManagerGateway, times(1)).createStoreOnLegacySystem(any(Store.class));
  }

  @Test
  public void testCreateReturns422WhenIdPreset() {
    String body = "{\"id\":999,\"name\":\"MALM\",\"quantityProductsInStock\":1}";

    given()
        .contentType("application/json")
        .body(body)
        .when()
        .post("store")
        .then()
        .statusCode(422);
  }

  @Test
  public void testUpdateReturns200AndInvokesLegacyGateway() {
    Long id = createStore("BILLY", 3);

    String body = "{\"name\":\"BILLY-UPDATED\",\"quantityProductsInStock\":9}";

    given()
        .contentType("application/json")
        .body(body)
        .when()
        .put("store/" + id)
        .then()
        .statusCode(200)
        .body(containsString("BILLY-UPDATED"));

    verify(legacyStoreManagerGateway, times(1)).updateStoreOnLegacySystem(any(Store.class));
  }

  @Test
  public void testUpdateReturns404WhenMissing() {
    String body = "{\"name\":\"GHOST\",\"quantityProductsInStock\":1}";

    given()
        .contentType("application/json")
        .body(body)
        .when()
        .put("store/999999")
        .then()
        .statusCode(404);
  }

  @Test
  public void testUpdateReturns422WhenNameMissing() {
    String body = "{\"quantityProductsInStock\":1}";

    given()
        .contentType("application/json")
        .body(body)
        .when()
        .put("store/1")
        .then()
        .statusCode(422);
  }

  @Test
  public void testPatchAppliesOnlyProvidedFields() {
    Long id = createStore("LACK", 5);

    // regression test: omitting quantityProductsInStock must NOT reset the existing stock to 0
    String body = "{\"name\":\"LACK-RENAMED\"}";

    given()
        .contentType("application/json")
        .body(body)
        .when()
        .patch("store/" + id)
        .then()
        .statusCode(200)
        .body(containsString("LACK-RENAMED"));

    given()
        .when()
        .get("store/" + id)
        .then()
        .statusCode(200)
        .body("quantityProductsInStock", org.hamcrest.Matchers.is(5));
  }

  @Test
  public void testPatchReturns404WhenMissing() {
    String body = "{\"name\":\"GHOST\"}";

    given()
        .contentType("application/json")
        .body(body)
        .when()
        .patch("store/999999")
        .then()
        .statusCode(404);
  }

  @Test
  public void testDeleteReturns204() {
    Long id = createStore("VITTSJO", 2);

    given().when().delete("store/" + id).then().statusCode(204);

    given().when().get("store/" + id).then().statusCode(404);
  }

  @Test
  public void testDeleteReturns404WhenMissing() {
    given().when().delete("store/999999").then().statusCode(404);
  }

  private Long createStore(String name, int quantity) {
    String body = String.format("{\"name\":\"%s\",\"quantityProductsInStock\":%d}", name, quantity);
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
}