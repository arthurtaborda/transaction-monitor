package com.arthurtaborda.transactionmonitor;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static java.lang.System.currentTimeMillis;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@RunWith(VertxUnitRunner.class)
public class TransactionsApiTest {

    private static final int PORT = 9090;
    private static FakeTransactionRepository transactionRepository;

    @BeforeClass
    public static void setUp(TestContext context) throws IOException, InterruptedException {
        Vertx vertx = Vertx.vertx();

        transactionRepository = new FakeTransactionRepository();
        RestApi verticle = new RestApi(transactionRepository);

        // deploy the verticle
        DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("port", PORT));
        vertx.deployVerticle(verticle, options, context.asyncAssertSuccess());

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = PORT;
    }

    @After
    public void tearDown() throws Exception {
        transactionRepository.clear();
    }

    @Test
    public void whenTransactionIsSuccessful_return201() {
        given().body(new Transaction(300, currentTimeMillis()))
               .contentType(ContentType.JSON)
               .when()
               .post("/transactions")
               .then()
               .statusCode(201);
    }

    @Test
    public void whenTransactionIsOlderThan60Sec_return204() {
        given().body(new Transaction(300, currentTimeMillis() - 61000))
               .contentType(ContentType.JSON)
               .when()
               .post("/transactions")
               .then()
               .statusCode(204);
    }

    @Test
    public void whenNoTransactionWasAdded_useDefaultValues() {
        when()
                .get("/statistics")
                .then()
                .statusCode(200)
                .body(not(empty()))
                .body("sum", equalTo(0.0f))
                .body("avg", equalTo(0.0f))
                .body("max", equalTo(0.0f))
                .body("max", equalTo(0.0f))
                .body("count", equalTo(0));
    }

    @Test
    public void whenTransactionIsSuccessful_updateStatistics() {
        given().body(new Transaction(300, currentTimeMillis()))
               .contentType(ContentType.JSON)
               .when()
               .post("/transactions");

        when()
                .get("/statistics")
                .then()
                .statusCode(200)
                .body(not(empty()))
                .body("sum", equalTo(300.0f))
                .body("avg", equalTo(300.0f))
                .body("max", equalTo(300.0f))
                .body("max", equalTo(300.0f))
                .body("count", equalTo(1));
    }
}
