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

class RabbitMQ extends Connector {
    private final boolean engraved;
    private final String exchange;
    private final String connectionUri;
    private final ConnectionFactory factory;

    private Connection connection;
    private BasicProperties props;
    private ObjectMapper mapper;
    private Channel amqpChannel;

    private int pending = 0;

    RabbitMQ(String connectionUri, String exchange, boolean engraved) throws NoSuchAlgorithmException, KeyManagementException, URISyntaxException, IOException, TimeoutException {
        this.exchange = exchange;
        this.engraved = engraved;
        this.connectionUri = connectionUri;

        mapper = new ObjectMapper();

        props = new BasicProperties.Builder()
                .contentType("application/json")
                .build();

        factory = new ConnectionFactory();
        factory.setUri(connectionUri);

        // Hack to support default vhost, for some reason the library is parsing it incorrectly from the URL.
        if(factory.getVirtualHost().equals(""))
            factory.setVirtualHost("/");
    }

    private Channel getChannelRaw() throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, TimeoutException {
        connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(this.exchange, "topic", true);

        return channel;
    }

    private Channel getChannel(){
        final int INITIAL_RETRY_SEC = 1;
        final int MAXIMUM_RETRY_SEC = 512;

        int timeoutSec = INITIAL_RETRY_SEC;
        Channel channel = null;

        while(timeoutSec <= MAXIMUM_RETRY_SEC){
            try {
                if(amqpChannel != null && amqpChannel.isOpen()){
                    channel = amqpChannel;
                } else {
                    amqpChannel = channel = getChannelRaw();
                    timeoutSec = INITIAL_RETRY_SEC;
                }
                break;
            } catch (URISyntaxException e) {
                throw new RuntimeException("Invalid AMQP URI given: " + connectionUri, e);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new RuntimeException(e);
            } catch (IOException | TimeoutException e) {
                try {
                    if(timeoutSec < MAXIMUM_RETRY_SEC){
                        System.err.println("ESTICADE: Failed to connect, retrying in " + timeoutSec);
                        Thread.sleep(timeoutSec * 1000);
                    } else {
                        throw new RuntimeException("Failed to establish connection to message queue", e);
                    }
                } catch (InterruptedException e2) {
                    throw new RuntimeException(e2);
                }

                timeoutSec += timeoutSec;
            }
        }

        return channel;
    }

    @Override
    public void emit(Event event) {
        try {
            getChannel().basicPublish(exchange, event.correlationBlock + "." + event.name, props, event.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String registerListener(String routingKey, String queueName, Consumer<JsonNode> callback) {
        Channel channel = getChannel();
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
        Channel channel = getChannel();
        AMQP.Queue.DeclareOk queueOk;
        if(queueName != null){
            boolean durable = engraved;
            boolean autoDelete = !engraved;
            queueOk = channel.queueDeclare(queueName, durable, false, autoDelete, null);
        } else {
            queueOk = channel.queueDeclare();
        }
        return queueOk.getQueue();
    }

    @Override
    protected void terminate() {
        try {
            connection.close();
            connection = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteListener(String tag) {
        Channel channel = getChannel();
        try {
            channel.basicCancel(tag);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isPending() {
        return pending > 0;
    }

    private DefaultConsumer createConsumer(final Consumer<JsonNode> callback) {
        Channel channel = getChannel();
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
                pending++;
                callback.accept(obj);
                pending--;
                channel.basicAck(deliveryTag, false);
            }
        };
    }
}
