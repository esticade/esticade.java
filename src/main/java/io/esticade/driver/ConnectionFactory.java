package io.esticade.driver;
import java.io.IOException;

public class ConnectionFactory {
    private static Connector connector;
    public static Connector getConnection() throws IOException {
        Configuration config = Configuration.getConfig();

        if(connector == null){
            try {
                connector = new RabbitMQ(config.amqpUrl, config.exchange, config.engraved);
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
        return connector;
    }

    public static void shutdown() {
        connector.shutdown();
        connector = null;
    }
}
