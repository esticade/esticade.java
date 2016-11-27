package io.esticade;

import java.io.IOException;

/**
 * Created by Jaan on 27.11.2016.
 */
public class TestRunner {
    public static void main(String[] args) throws IOException, InterruptedException {
        Service service = new Service("Testicade");

        service.on("Test", event -> System.out.println("Captured event"));

        while(true){
            service.emit("Test");
            System.out.println("Emitted...");
            Thread.sleep(1000);
        }
    }
}
