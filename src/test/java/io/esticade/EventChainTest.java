package io.esticade;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EventChainTest {
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
    public void testEventChainCanCatchItsOwnMessages() throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<Event> responseReceived = new CompletableFuture<>();

        JsonNode testObject = JsonNodeFactory.instance.objectNode()
                .put("string", "The String")
                .put("bool", false)
                .put("number", 123.456)
                .putNull("null");

        service.emitChain("EmitChainTest", testObject)
            .on("EmitChainTest", responseReceived::complete)
            .execute();

        Event msg = responseReceived.get(1, TimeUnit.SECONDS);
        assertEquals(testObject, msg.body);
    }

    @Test
    public void testEmitChainCanCatchMessagesFromOtherServices() throws InterruptedException, ExecutionException, TimeoutException, IOException {
        CompletableFuture<Event> responseReceived = new CompletableFuture<>();

        JsonNode testObject = JsonNodeFactory.instance.objectNode()
                .put("string", "The String");

        Service service2 = new Service("Service 2");
        service2.on("EmitChainTest2", (ev) -> ev.emit("EmitChainTest-Response", testObject));

        service.emitChain("EmitChainTest2", testObject)
                .on("EmitChainTest-Response", responseReceived::complete)
                .execute();

        Event msg = responseReceived.get(1, TimeUnit.SECONDS);
        assertEquals(testObject, msg.body);
    }

    @Test
    public void testEmitFromOutsideTheChainWillNotReachHandler() throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<Event> responseReceivedInChainHandler = new CompletableFuture<>();
        CompletableFuture<Event> responseReceivedInNormalHandler = new CompletableFuture<>();
        CompletableFuture<Event> responder = new CompletableFuture<>();

        service.on("OutsideChainTestResponse", responseReceivedInNormalHandler::complete);

        service.on("OutsideChainTest", responder::complete);

        service.emitChain("OutsideChainTest")
                .on("OutsideChainTestResponse", responseReceivedInChainHandler::complete)
                .execute();

        service.emit("OutsideChainTestResponse", 123);

        Event event = responseReceivedInNormalHandler.get(2, TimeUnit.SECONDS);
        assertEquals("The message received by global handler should be 123.", 123, event.body);
        assertFalse("The chain handler should not yet receive event", responseReceivedInChainHandler.isDone());

        responder.get(2, TimeUnit.SECONDS).emit("OutsideChainTestResponse", 456);
        int receivedValue = (int)responseReceivedInChainHandler.get(2, TimeUnit.SECONDS).body;
        assertEquals("Chain handler should receive 456", 456, receivedValue);
    }

    @Test
    public void testEventSentAfterTimeoutWillNotBeReceived() throws InterruptedException {
        CompletableFuture<Event> response = new CompletableFuture<>();
        final boolean[] timeoutCallbackCalled = {false};

        service.on("TimeOutEvent", event -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            event.emit("TimeOutEvent-Response");
        });

        service.emitChain("TimeOutEvent")
                .on("TimeOutEvent-Response", response::complete)
                .timeout(300, event -> timeoutCallbackCalled[0] = true)
                .execute();

        Thread.sleep(600);

        assertFalse("The TimeOutEvent-Response should never be received", response.isDone());
        assertTrue("The timeout callback should have been called", timeoutCallbackCalled[0]);
    }

    @Test
    public void testEventEmitChainSupportsDifferentTypes() throws ExecutionException, InterruptedException, TimeoutException {
        CompletableFuture<Event> stringOk = new CompletableFuture<>();
        CompletableFuture<Event> intOk = new CompletableFuture<>();
        CompletableFuture<Event> doubleOk = new CompletableFuture<>();
        CompletableFuture<Event> boolOk = new CompletableFuture<>();
        CompletableFuture<Event> nullOk = new CompletableFuture<>();

        service.on("EventEmitTestString", stringOk::complete);
        service.on("EventEmitTestInt", intOk::complete);
        service.on("EventEmitTestDouble", doubleOk::complete);
        service.on("EventEmitTestBoolean", boolOk::complete);
        service.on("EventEmitTestNull", nullOk::complete);

        service.emitChain("EventEmitTestString", "TestString").execute();
        service.emitChain("EventEmitTestInt", 893).execute();
        service.emitChain("EventEmitTestDouble", 893.456).execute();
        service.emitChain("EventEmitTestBoolean", true).execute();
        service.emitChain("EventEmitTestNull").execute();

        assertEquals("TestString", stringOk.get(1, TimeUnit.SECONDS).body);
        assertEquals(893, intOk.get(1, TimeUnit.SECONDS).body);
        assertEquals(893.456, (double)doubleOk.get(1, TimeUnit.SECONDS).body, 0.0001);
        assertEquals(true, boolOk.get(1, TimeUnit.SECONDS).body);
        assertEquals(null, nullOk.get(1, TimeUnit.SECONDS).body);
    }
}