package io.esticade;

import io.esticade.driver.ConnectionFactory;
import io.esticade.driver.Connector;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.io.IOException;
import java.util.UUID;

public class Event {
    public String correlationId;
    public String correlationBlock;
    public String eventId;
    public String parentId;
    public String service;

    public String name;
    public JsonValue body;

    private String currentService;

    Event(String serviceName, String name, JsonValue payload){
        initProperties(serviceName, name, payload);
    }

    private Event(String serviceName, String name, JsonValue payload, Event parentEvent){
        initProperties(serviceName, name, payload);
        this.correlationId = parentEvent.correlationId;
        this.correlationBlock = parentEvent.correlationBlock;
        this.parentId = parentEvent.eventId;
    }

    Event(String serviceName, JsonObject obj) {
        currentService = serviceName;

        correlationId = obj.getString("correlationId");
        correlationBlock = obj.getString("correlationBlock");
        eventId = obj.getString("eventId");
        parentId = obj.getString("parentId", null);
        service = obj.getString("service");
        name = obj.getString("name");
        body = obj.get("body");
    }

    private void initProperties(String serviceName, String name, JsonValue payload) {
        this.name = name;
        this.body = payload;
        eventId = UUID.randomUUID().toString();
        correlationId = UUID.randomUUID().toString();
        correlationBlock = "1";
        service = serviceName;
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
                .add("name", name)
                .add("body", body);

        if (parentId != null) {
            builder.add("parentId", parentId);
        }

        return builder.build();

    }

    public void emit(String eventName, JsonValue payload) {
        Connector connector = null;
        try {
            connector = ConnectionFactory.getConnection();
            connector.emit(new Event(currentService, eventName, payload, this));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
