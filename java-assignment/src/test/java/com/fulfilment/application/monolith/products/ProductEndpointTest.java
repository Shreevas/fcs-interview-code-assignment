package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ProductEndpointTest {

  @Test
  public void testCrudProduct() {
    final String path = "product";

    // List all, should have all 3 products the database has initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));

    // Delete the TONSTAD:
    given().when().delete(path + "/1").then().statusCode(204);

    // List all, TONSTAD should be missing now:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(not(containsString("TONSTAD")), containsString("KALLAX"), containsString("BESTÅ"));
  }

  @Test
  public void testCreateReturns201() {
    String body = "{\"name\":\"MALM\",\"stock\":4}";

    given()
        .contentType("application/json")
        .body(body)
        .when()
        .post("product")
        .then()
        .statusCode(201)
        .body(containsString("MALM"));
  }

  @Test
  public void testCreateReturns422WhenIdPreset() {
    String body = "{\"id\":999,\"name\":\"HEMNES\",\"stock\":1}";

    given()
        .contentType("application/json")
        .body(body)
        .when()
        .post("product")
        .then()
        .statusCode(422);
  }

  @Test
  public void testGetSingleReturns404WhenMissing() {
    given().when().get("product/999999").then().statusCode(404);
  }

  @Test
  public void testUpdateReturns200() {
    String createBody = "{\"name\":\"LACK\",\"stock\":2}";
    Long id =
        given()
            .contentType("application/json")
            .body(createBody)
            .when()
            .post("product")
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");

    String updateBody = "{\"name\":\"LACK-UPDATED\",\"stock\":9}";
    given()
        .contentType("application/json")
        .body(updateBody)
        .when()
        .put("product/" + id)
        .then()
        .statusCode(200)
        .body(containsString("LACK-UPDATED"));
  }

  @Test
  public void testUpdateReturns404WhenMissing() {
    String body = "{\"name\":\"GHOST\",\"stock\":1}";

    given()
        .contentType("application/json")
        .body(body)
        .when()
        .put("product/999999")
        .then()
        .statusCode(404);
  }

  @Test
  public void testUpdateReturns422WhenNameMissing() {
    String body = "{\"stock\":1}";

    given().contentType("application/json").body(body).when().put("product/1").then().statusCode(422);
  }

  @Test
  public void testDeleteReturns404WhenMissing() {
    given().when().delete("product/999999").then().statusCode(404);
  }
}
