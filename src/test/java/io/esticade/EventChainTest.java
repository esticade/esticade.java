package io.esticade;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;

/**
 * Created by jaan.pullerits on 11/04/16.
 */
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

        JsonObject testObject = Json.createObjectBuilder()
                .add("string", "The String")
                .add("bool", false)
                .add("number", 123.456)
                .addNull("null")
                .build();

        service.emitChain("EmitChainTest", testObject)
            .on("EmitChainTest", responseReceived::complete)
            .execute();

        Event msg = responseReceived.get(1, TimeUnit.SECONDS);
        assertEquals(testObject, msg.body);
    }

}
