package com.arthurtaborda.transactionmonitor;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.DoubleSummaryStatistics;

import static java.math.RoundingMode.FLOOR;

public class RestApi extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestApi.class.getName());

    private static final int PORT = 9090;

    private final TransactionRepository transactionRepository;

    public RestApi(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public void start() throws Exception {
        HttpServerOptions serverOptions = new HttpServerOptions().setPort(PORT);

        Router router = Router.router(vertx);
        router.route()
              .handler(BodyHandler.create());

        transactionEndpoint(router);
        statisticsEndpoint(router);

        HttpServer server = vertx.createHttpServer(serverOptions);
        server.requestHandler(router::accept)
              .listen(result -> {
                  if (result.succeeded()) {
                      LOGGER.info("Http server listening on port " + PORT);
                  }
              });

    }

    private void transactionEndpoint(Router router) {
        router.post("/transactions")
              .consumes("application/json")
              .handler(ctx -> {
                  JsonObject request = ctx.getBodyAsJson();
                  Transaction transaction = new Transaction(request.getDouble("amount"),
                                                            request.getLong("timestamp"));

                  boolean added = transactionRepository.addTransaction(transaction);

                  HttpServerResponse response = ctx.response();
                  if (added) {
                      response.setStatusCode(201);
                  } else {
                      response.setStatusCode(204);
                  }

                  response.end();
              });
    }

    private void statisticsEndpoint(Router router) {
        router.get("/statistics")
              .produces("application/json")
              .handler(ctx -> {
                  DoubleSummaryStatistics statistics = transactionRepository.getStatistics();

                  JsonObject json = new JsonObject();
                  json.put("sum", precisely(statistics.getSum()));
                  json.put("avg", precisely(statistics.getAverage()));
                  json.put("max", precisely(statistics.getMax()));
                  json.put("min", precisely(statistics.getMin()));
                  json.put("count", statistics.getCount());

                  ctx.response()
                     .setStatusCode(200)
                     .putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
                     .end(json.toString());
              });
    }

    private Double precisely(double val) {
        if (val == Double.POSITIVE_INFINITY || val == Double.NEGATIVE_INFINITY) {
            return Double.valueOf(0);
        }
        return new BigDecimal(val).setScale(2, FLOOR).doubleValue();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }
}