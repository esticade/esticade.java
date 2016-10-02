package io.esticade.driver;

public class AsyncShutdown extends Thread {
    private Connector connector;

    public AsyncShutdown(Connector connector) {
        this.connector = connector;
    }

    @Override
    public void run() {
        while(connector.isPending()){
            try {
                sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
        connector.shutdown();
    }
}
