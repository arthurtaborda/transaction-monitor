package com.arthurtaborda.transactionmonitor;

import com.arthurtaborda.transactionmonitor.repository.Transaction;
import com.arthurtaborda.transactionmonitor.repository.TransactionRepository;
import com.arthurtaborda.transactionmonitor.repository.TransactionStatistics;
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

public class RestApi extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestApi.class.getName());

    private HttpServer server;
    private int port;
    private final TransactionRepository transactionRepository;

    public RestApi(int port, TransactionRepository transactionRepository) {
        this.port = port;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public void start() throws Exception {
        HttpServerOptions serverOptions = new HttpServerOptions().setPort(port);

        server = vertx.createHttpServer(serverOptions);
        Router router = Router.router(vertx);
        router.route()
              .handler(BodyHandler.create());

        transactionEndpoint(router);
        statisticsEndpoint(router);

        server.requestHandler(router::accept)
              .listen(result -> {
                  if (result.succeeded()) {
                      LOGGER.info("Http server listening on port " + port);
                  }
              });
    }

    @Override
    public void stop() throws Exception {
        server.close();
    }

    private void transactionEndpoint(Router router) {
        router.post("/transactions")
              .consumes("application/json")
              .handler(ctx -> {
                  HttpServerResponse response = ctx.response();
                  JsonObject request = ctx.getBodyAsJson();
                  Double amount = null;
                  try {
                      amount = request.getDouble("amount");
                      if (amount == null) {
                          response.setStatusCode(400).end("Amount is required");
                          return;
                      }
                  } catch (Exception e) {
                      response.setStatusCode(400).end("Amount is invalid");
                      return;
                  }
                  Long timestamp = null;
                  try {
                      timestamp = request.getLong("timestamp");
                      if (timestamp == null) {
                          response.setStatusCode(400).end("Timestamp is required");
                          return;
                      }
                  } catch (Exception e) {
                      response.setStatusCode(400).end("Timestamp is invalid");
                      return;
                  }
                  Transaction transaction = new Transaction(amount, timestamp);

                  boolean added = transactionRepository.addTransaction(transaction);

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
                  TransactionStatistics statistics = transactionRepository.getStatistics();

                  JsonObject json = new JsonObject();
                  json.put("sum", statistics.getSum());
                  json.put("avg", statistics.getAverage());
                  json.put("max", statistics.getMax());
                  json.put("min", statistics.getMin());
                  json.put("count", statistics.getCount());

                  ctx.response()
                     .setStatusCode(200)
                     .putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
                     .end(json.toString());
              });
    }
}
