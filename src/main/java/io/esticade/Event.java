package io.esticade;

import io.esticade.driver.ConnectionFactory;
import io.esticade.driver.Connector;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.io.IOException;
import java.util.UUID;

public final class Event {
    public final String correlationId;
    public final String correlationBlock;
    public final String eventId;
    public final String parentId;
    public final String service;

    public final String name;
    public final JsonValue body;

    private ServiceParams serviceParams;

    Event(ServiceParams serviceParams, String name, JsonValue payload){
        this.name = name;
        this.body = payload;
        this.serviceParams = serviceParams;
        this.eventId = UUID.randomUUID().toString();
        this.correlationId = UUID.randomUUID().toString();
        this.correlationBlock = serviceParams.serviceName;
        this.parentId = null;
        this.service = serviceParams.serviceName;
    }

    private Event(ServiceParams serviceParams, String name, JsonValue payload, Event parentEvent){
        this.name = name;
        this.body = payload;
        this.serviceParams = serviceParams;
        this.eventId = UUID.randomUUID().toString();
        this.correlationId = parentEvent.correlationId;
        this.correlationBlock = parentEvent.correlationBlock;
        this.parentId = parentEvent.eventId;
        this.service = serviceParams.serviceName;
    }

    Event(ServiceParams serviceParams, JsonObject obj) {
        this.name = obj.getString("name");
        this.body = obj.get("body");
        this.serviceParams = serviceParams;
        this.correlationId = obj.getString("correlationId");
        this.correlationBlock = obj.getString("correlationBlock");
        this.eventId = obj.getString("eventId");
        this.parentId = obj.getString("parentId", null);
        this.service = obj.getString("service");
    }

    @Override
    public String toString() {
        return toJsonObject().toString();
    }

    private JsonObject toJsonObject() {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("correlationId", correlationId)
                .add("correlationBlock", correlationBlock)
                .add("eventId", eventId)
                .add("service", service)
                .add("name", name);

        if (body != null) {
            builder.add("body", body);
        } else {
            builder.addNull("body");
        }

        if (parentId != null) {
            builder.add("parentId", parentId);
        }

        return builder.build();
    }

    public void emit(String eventName, JsonValue payload) {
        try {
            ConnectionFactory.getConnection()
                .emit(new Event(serviceParams, eventName, payload, this));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void emit(String eventName, String payload) {
        emit(eventName, JsonConvert.toJsonValue(payload));
    }

    public void emit(String eventName, int payload) {
        emit(eventName, JsonConvert.toJsonValue(payload));
    }

    public void emit(String eventName, double payload) {
        emit(eventName, JsonConvert.toJsonValue(payload));
    }

    public void emit(String eventName, boolean payload) {
        emit(eventName, JsonConvert.toJsonValue(payload));
    }

    public void emit(String eventName) {
        emit(eventName, JsonValue.NULL);
    }
}
