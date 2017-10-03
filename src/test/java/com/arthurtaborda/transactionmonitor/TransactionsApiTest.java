package com.arthurtaborda.transactionmonitor;

import com.arthurtaborda.transactionmonitor.repository.FakeTransactionRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.AfterClass;
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
    private static Vertx vertx;

    @BeforeClass
    public static void setUp(TestContext context) throws IOException, InterruptedException {
        vertx = Vertx.vertx();

        transactionRepository = new FakeTransactionRepository();
        RestApi verticle = new RestApi(PORT, transactionRepository);

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

    @AfterClass
    public static void tearDownClass() throws Exception {
        vertx.close();
    }

    @Test
    public void whenTransactionRequestHasMissingAmount_return400() {
        given().body(new TransactionRequest(null, currentTimeMillis()).toJson())
               .contentType(ContentType.JSON)
               .when()
               .post("/transactions")
               .then()
               .statusCode(400)
               .body(equalTo("Amount is required"));
    }

    @Test
    public void whenTransactionRequestHasInvalidAmount_return400() {
        given().body(new TransactionRequest("invalid value", currentTimeMillis()).toJson())
               .contentType(ContentType.JSON)
               .when()
               .post("/transactions")
               .then()
               .statusCode(400)
               .body(equalTo("Amount is invalid"));
    }

    @Test
    public void whenTransactionRequestHasMissingTimestamp_return400() {
        given().body(new TransactionRequest(300, null).toJson())
               .contentType(ContentType.JSON)
               .when()
               .post("/transactions")
               .then()
               .statusCode(400)
               .body(equalTo("Timestamp is required"));
    }

    @Test
    public void whenTransactionRequestHasInvalidTimestamp_return400() {
        given().body(new TransactionRequest(300, "invalid value").toJson())
               .contentType(ContentType.JSON)
               .when()
               .post("/transactions")
               .then()
               .statusCode(400)
               .body(equalTo("Timestamp is invalid"));
    }

    @Test
    public void whenTransactionIsSuccessful_return201() {
        given().body(new TransactionRequest(300, currentTimeMillis()).toJson())
               .contentType(ContentType.JSON)
               .when()
               .post("/transactions")
               .then()
               .statusCode(201);
    }

    @Test
    public void whenTransactionIsOlderThan60Sec_return204() {
        given().body(new TransactionRequest(300, currentTimeMillis() - 61000).toJson())
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
        given().body(new TransactionRequest(300, currentTimeMillis()).toJson())
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
