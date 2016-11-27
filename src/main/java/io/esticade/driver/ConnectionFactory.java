package io.esticade.driver;
import java.io.IOException;

public class ConnectionFactory {
    private static Connector connector;
    public static Connector getConnection() throws IOException {
        Configuration config = Configuration.getConfig();
        
        if(connector == null){
            try {
                connector = new RabbitMQ(config.getAmqpUrl(), config.getExchange(), config.isEngraved());
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
        return connector;
    }

    public static void shutdown() {
        new AsyncShutdown(connector).start();
        connector = null;
    }
}
