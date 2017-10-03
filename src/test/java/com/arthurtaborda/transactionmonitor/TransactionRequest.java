package com.arthurtaborda.transactionmonitor;

import io.vertx.core.json.JsonObject;

public class TransactionRequest {

    public Object amount;
    public Object timestamp;

    public TransactionRequest(Object amount, Object timestamp) {
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public String toJson() {
        JsonObject json = new JsonObject();
        if (amount != null) {
            json.put("amount", amount);
        }
        if (timestamp != null) {
            json.put("timestamp", timestamp);
        }

        return json.toString();
    }
}
