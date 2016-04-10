package io.esticade.driver;

import java.io.IOException;

public class ConnectionFactory {
    private static Connector connector;
    public static Connector getConnection() throws IOException {
        if(connector == null){
            try {
                connector = new RabbitMQ("amqp://localhost");
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
        return connector;
    }
}
