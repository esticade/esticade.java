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
import java.util.HashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

class RabbitMQ extends Connector implements ShutdownListener {
    private final boolean engraved;
    private final String exchange;
    private final String connectionUri;
    private final ConnectionFactory factory;

    private Connection connection;
    private BasicProperties props;
    private ObjectMapper mapper;
    private Channel amqpChannel;

    private HashMap<String, Listener> listeners = new HashMap<>();

    private int pending = 0;
    private boolean channelRetryEnabled = true;

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

        connection.addShutdownListener(this);

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
                    reRegisterListeners();
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

    private void reRegisterListeners() {
        if(listeners.size() > 0){
            listeners.forEach((s, listener) -> registerListenerRaw(listener));
        }
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
        Listener listener = new Listener()
            .setRoutingKey(routingKey)
            .setQueueName(queueName)
            .setCallback(callback);

        registerListenerRaw(listener);
        listeners.put(listener.getId(), listener);

        return listener.getId();
    }

    private void registerListenerRaw(Listener listener) {
        Channel channel = getChannel();
        try {
            String queue = getQueue(listener.getQueueName());
            channel.queueBind(queue, exchange, listener.getRoutingKey());
            listener.setCTag(channel.basicConsume(queue, false, createConsumer(listener.getCallback())));
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        channelRetryEnabled = false;
        try {
            connection.close();
            connection = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteListener(String tag) {
        Listener listener = listeners.remove(tag);
        deleteListenerRaw(listener);
    }

    private void deleteListenerRaw(Listener listener) {
        Channel channel = getChannel();
        try {
            channel.basicCancel(listener.getCtag());
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

    @Override
    public void shutdownCompleted(ShutdownSignalException e) {
        if(channelRetryEnabled){
            System.out.println("Shutdown called...");
            getChannel();
        }
    }
}
