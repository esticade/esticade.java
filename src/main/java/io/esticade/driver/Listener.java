package io.esticade.driver;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.function.Consumer;

class Listener {
    private String id;
    private String ctag;
    private String routingKey;
    private String queueName;

    private static int idCounter;
    private Consumer<JsonNode> callback;

    public Listener() {
        id = String.valueOf(idCounter++);
    }

    public String getId() {
        return id;
    }

    public String getCtag() {
        return ctag;
    }

    public Listener setCTag(String ctag) {
        this.ctag = ctag;
        return this;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public Listener setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
        return this;
    }

    public String getQueueName() {
        return queueName;
    }

    public Listener setQueueName(String queueName) {
        this.queueName = queueName;
        return this;
    }

    public Listener setCallback(Consumer<JsonNode> callback) {
        this.callback = callback;
        return this;
    }

    public Consumer<JsonNode> getCallback() {
        return callback;
    }
}
