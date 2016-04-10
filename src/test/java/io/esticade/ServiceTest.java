package io.esticade;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import javax.json.*;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class ServiceTest{
    Service service;

    @Before
    public void init() throws IOException {
        service = new Service("TestService");
    }

    @Test
    public void testEmit(){
        JsonObject testObject = Json.createObjectBuilder()
                .add("number", 123)
                .add("string", "test123")
                .add("bool", false)
                .build();

        service.emit("TestEvent", testObject);
    }

    @Test
    public void testOn() throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<Event> future = new CompletableFuture<Event>();

        final JsonObject testObject = Json.createObjectBuilder()
                .add("number", 123)
                .add("string", "test123")
                .add("bool", false)
                .build();

        service.on("TestEvent2", future::complete);

        service.emit("TestEvent2", testObject);

        Event event = future.get(20, TimeUnit.SECONDS);

        assertEquals("TestEvent2", event.name);
        assertEquals("TestService", event.service);
        assertEquals(testObject, event.body);
    }

    @Test
    public void testEventEmit() throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<Event> future = new CompletableFuture<Event>();

        final JsonObject testObject = Json.createObjectBuilder()
                .add("number", 123)
                .add("string", "test123")
                .add("bool", false)
                .build();

        service.on("TestEvent3Response", future::complete);
        service.on("TestEvent3", (Event ev) -> ev.emit("TestEvent3Response", testObject));

        service.emit("TestEvent3", testObject);

        Event event = future.get(1, TimeUnit.SECONDS);

        assertEquals("TestEvent3Response", event.name);
        assertEquals("TestService", event.service);
        assertEquals(testObject, event.body);
    }

    @Test
    public void testStringEmit() throws InterruptedException, ExecutionException, TimeoutException {
        Event ev = withListener("EmitString", (eventName) -> {
            service.emit(eventName, "Test String");
        });

        JsonString result = (JsonString)ev.body;

        assertEquals("Test String", result.getString());
    }

    @Test
    public void testIntegerEmit() throws InterruptedException, ExecutionException, TimeoutException {
        Event ev = withListener("EmitNumber", (eventName) -> {
            service.emit(eventName, 123);
        });

        JsonNumber result = (JsonNumber)ev.body;

        assertEquals(123, result.intValue());
    }


    @Test
    public void testDoubleEmit() throws InterruptedException, ExecutionException, TimeoutException {
        Event ev = withListener("EmitDouble", (eventName) -> {
            service.emit(eventName, 123.456);
        });

        JsonNumber result = (JsonNumber)ev.body;

        assertEquals(123.456, result.doubleValue(), 0.0001);
    }

    private Event withListener(String eventName, Consumer<String> emit) throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<Event> future = new CompletableFuture<Event>();
        service.on(eventName, future::complete);
        emit.accept(eventName);
        return future.get(2, TimeUnit.SECONDS);
    }

}
