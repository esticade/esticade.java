package io.esticade.driver;

import com.rabbitmq.client.*;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.ConnectionFactory;
import io.esticade.Event;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * Created by Jaan on 06.04.2016.
 */
class RabbitMQ implements Connector {
    public static final String EXCHANGE = "events";
    private Connection connection;
    private Channel channel;
    private BasicProperties props;

    RabbitMQ(String connectionUri) throws NoSuchAlgorithmException, KeyManagementException, URISyntaxException, IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(connectionUri);
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE, "topic", true);

        props = new BasicProperties.Builder()
                .contentType("application/json")
                .build();
    }

    @Override
    public void emit(Event event) {
        try {
            channel.basicPublish(EXCHANGE, event.correlationBlock + "." + event.name, props, event.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void registerListener(String routingKey, String queueName, Consumer<JsonObject> callback) {
        try {
            AMQP.Queue.DeclareOk queueOk = channel.queueDeclare(queueName, false, false, true, null);
            channel.queueBind(queueOk.getQueue(), EXCHANGE, routingKey);
            channel.basicConsume(queueOk.getQueue(), false, createConsumer(callback));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private DefaultConsumer createConsumer(final Consumer<JsonObject> callback) {
        return new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       BasicProperties properties,
                                       byte[] body)
                    throws IOException
            {
                long deliveryTag = envelope.getDeliveryTag();

                JsonObject obj = parseJsonObject(body);

                callback.accept(obj);

                channel.basicAck(deliveryTag, false);
            }
        };
    }

    private JsonObject parseJsonObject(byte[] body) {
        JsonReader reader = Json.createReader(new ByteArrayInputStream(body));
        JsonObject obj = reader.readObject();
        reader.close();
        return obj;
    }

}