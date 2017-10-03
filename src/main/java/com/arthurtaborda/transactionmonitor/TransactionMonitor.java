package com.arthurtaborda.transactionmonitor;

import com.arthurtaborda.transactionmonitor.repository.InMemTransactionRepository;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class TransactionMonitor {

    private static final int PORT = 9090;

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        RestApi verticle = new RestApi(PORT, new InMemTransactionRepository(vertx));
        DeploymentOptions options = new DeploymentOptions().setConfig(new JsonObject().put("port", 9090));
        vertx.deployVerticle(verticle, options);
    }
}
