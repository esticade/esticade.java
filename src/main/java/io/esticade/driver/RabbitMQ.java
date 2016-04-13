package io.esticade.driver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.ConnectionFactory;
import io.esticade.Event;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

class RabbitMQ implements Connector {
    private final boolean engraved;
    private final String exchange;
    private Connection connection;
    private Channel channel;
    private BasicProperties props;
    private ObjectMapper mapper;

    RabbitMQ(String connectionUri, String exchange, boolean engraved) throws NoSuchAlgorithmException, KeyManagementException, URISyntaxException, IOException, TimeoutException {
        this.exchange = exchange;
        this.engraved = engraved;

        mapper = new ObjectMapper();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUri(connectionUri);

        // Hack to support default
        if(factory.getVirtualHost().equals(""))
            factory.setVirtualHost("/");

        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.exchangeDeclare(this.exchange, "topic", true);

        props = new BasicProperties.Builder()
                .contentType("application/json")
                .build();
    }

    @Override
    public void emit(Event event) {
        try {
            channel.basicPublish(exchange, event.correlationBlock + "." + event.name, props, event.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String registerListener(String routingKey, String queueName, Consumer<JsonNode> callback) {
        try {
            String queue = getQueue(queueName);
            channel.queueBind(queue, exchange, routingKey);
            return channel.basicConsume(queue, false, createConsumer(callback));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getQueue(String queueName) throws IOException {
        AMQP.Queue.DeclareOk queueOk;
        if(queueName != null){
            queueOk = channel.queueDeclare(queueName, false, false, true, null);
        } else {
            queueOk = channel.queueDeclare();
        }
        return queueOk.getQueue();
    }

    @Override
    public void shutdown() {
        try {
            connection.close();
            connection = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteListener(String tag) {
        try {
            channel.basicCancel(tag);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private DefaultConsumer createConsumer(final Consumer<JsonNode> callback) {
        return new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       BasicProperties properties,
                                       byte[] body)
                    throws IOException
            {
                long deliveryTag = envelope.getDeliveryTag();

                JsonNode obj = mapper.readTree(body);
                callback.accept(obj);
                channel.basicAck(deliveryTag, false);
            }
        };
    }
}
