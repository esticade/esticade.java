import io.esticade.Service;

import java.io.IOException;

/**
 * Created by Jaan on 12.04.2016.
 */
public class Runner {
    public static void main(String[] args) {
        Service service = null;
        try {
            service = new Service("Test Service");
        } catch (IOException e) {
            e.printStackTrace();
        }
        service.on("ping", event -> event.emit("AllGood"));
        service.on("Cats", event -> event.emit("WeHaveSome", "Cats!"));
    }
}

