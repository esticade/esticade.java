package io.esticade;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.lang.Thread.sleep;
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

    @After
    public void shutdown() {
        service.shutdown();
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
        Event ev = withListener("EmitString", (eventName) -> service.emit(eventName, "Test String"));
        assertEquals("Test String", ((JsonString)ev.body).getString());
    }

    @Test
    public void testIntegerEmit() throws InterruptedException, ExecutionException, TimeoutException {
        Event ev = withListener("EmitNumber", (eventName) -> service.emit(eventName, 123));
        assertEquals(123, ((JsonNumber)ev.body).intValue());
    }


    @Test
    public void testDoubleEmit() throws InterruptedException, ExecutionException, TimeoutException {
        Event ev = withListener("EmitDouble", (eventName) -> service.emit(eventName, 123.456));
        assertEquals(123.456, ((JsonNumber)ev.body).doubleValue(), 0.0001);
    }

    @Test
    public void testBoolEmit() throws InterruptedException, ExecutionException, TimeoutException {
        Event ev = withListener("EmitBool", (eventName) -> service.emit(eventName, true));
        assertEquals(JsonValue.TRUE, ev.body);
    }

    @Test
    public void testNullEmit() throws InterruptedException, ExecutionException, TimeoutException {
        Event ev = withListener("EmitBool", (eventName) -> service.emit(eventName));
        assertEquals(JsonValue.NULL, ev.body);
    }

    @Test
    public void testBalance() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        Service service2 = new Service("TestService");

        CompletableFuture<Event> service1Balance = new CompletableFuture<>();
        CompletableFuture<Event> service2Balance = new CompletableFuture<>();

        service.on("BalanceTest", service1Balance::complete);
        service2.on("BalanceTest", service2Balance::complete);

        service.emit("BalanceTest", 0);
        service1Balance.get(2, TimeUnit.SECONDS);

        assertFalse(service2Balance.isDone());

        service.emit("BalanceTest", 0);
        service2Balance.get(2, TimeUnit.SECONDS);
    }


    @Test
    public void testAlwaysOn() throws IOException, InterruptedException, ExecutionException, TimeoutException {
        Service service2 = new Service("TestService");
        final int[] alwaysOnCounter = {0};

        CompletableFuture<Event> service1Called = new CompletableFuture<>();
        CompletableFuture<Event> service2Called = new CompletableFuture<>();
        CompletableFuture<Event> alwaysOnCalledTwice = new CompletableFuture<>();

        service.alwaysOn("AlwaysOnTest", (Event ev) -> {
            alwaysOnCounter[0]++;
            if(alwaysOnCounter[0] == 2){
                alwaysOnCalledTwice.complete(ev);
            }
        });

        service.on("AlwaysOnTest", service1Called::complete);
        service2.on("AlwaysOnTest", service2Called::complete);

        service.emit("AlwaysOnTest", 0);
        service1Called.get(2, TimeUnit.SECONDS);

        assertFalse(service2Called.isDone());

        service.emit("AlwaysOnTest", 0);
        service2Called.get(2, TimeUnit.SECONDS);

        alwaysOnCalledTwice.get(2, TimeUnit.SECONDS);
    }

    @Test
    public void testEventEmitSupportsDifferentTypes() throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<Event> stringOk = new CompletableFuture<>();
        CompletableFuture<Event> intOk = new CompletableFuture<>();
        CompletableFuture<Event> doubleOk = new CompletableFuture<>();
        CompletableFuture<Event> boolOk = new CompletableFuture<>();

        service.on("EventEmitTestString", stringOk::complete);
        service.on("EventEmitTestInt", intOk::complete);
        service.on("EventEmitTestDouble", doubleOk::complete);
        service.on("EventEmitTestBoolean", boolOk::complete);

        service.on("EventEmitTest", event -> {
            event.emit("EventEmitTestString", "TestString");
            event.emit("EventEmitTestInt", 893);
            event.emit("EventEmitTestDouble", 893.456);
            event.emit("EventEmitTestBoolean", true);
        });

        service.emit("EventEmitTest", 0);

        assertEquals("TestString", ((JsonString)stringOk.get(1, TimeUnit.SECONDS).body).getString());
        assertEquals(893, ((JsonNumber)intOk.get(1, TimeUnit.SECONDS).body).longValue());
        assertEquals(893.456, ((JsonNumber)doubleOk.get(1, TimeUnit.SECONDS).body).doubleValue(), 0.0001);
        assertEquals(JsonValue.TRUE, boolOk.get(1, TimeUnit.SECONDS).body);
    }

    @Test
    public void testDifferentServiceShouldHaveDifferentCorrelationBlocks() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        CompletableFuture<Event> firstService = new CompletableFuture<>();
        CompletableFuture<Event> secondService = new CompletableFuture<>();

        service.on("CorrelationBlockTest1", firstService::complete);
        service.on("CorrelationBlockTest2", secondService::complete);

        service.emit("CorrelationBlockTest1");


        Service service2 = new Service("Second Test Service");
        service2.emit("CorrelationBlockTest2");

        Event service1Event = firstService.get(2, TimeUnit.SECONDS);
        Event service2Event = secondService.get(2, TimeUnit.SECONDS);

        assertNotEquals("Events emitted by different services should have different correlation blocks", service1Event.correlationBlock, service2Event.correlationBlock);
    }

    private Event withListener(String eventName, Consumer<String> emit) throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<Event> future = new CompletableFuture<>();
        service.on(eventName, future::complete);
        emit.accept(eventName);
        return future.get(2, TimeUnit.SECONDS);
    }

}
