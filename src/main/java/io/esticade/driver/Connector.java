package io.esticade.driver;
import com.fasterxml.jackson.databind.JsonNode;
import io.esticade.Event;

import java.util.ArrayList;
import java.util.Timer;
import java.util.function.Consumer;

public abstract class Connector {
    private ArrayList<Timer> timers = new ArrayList<>();

    public abstract void emit(Event event);
    public abstract String registerListener(String routingKey, String queueName, Consumer<JsonNode> callback);

    protected abstract void terminate();

    public abstract void deleteListener(String tag);

    public abstract boolean isPending();

    public void shutdown(){
        timers.forEach(Timer::cancel);
        terminate();
    }

    public Timer getTimer() {
        Timer timer = new Timer();
        timers.add(timer);
        return timer;
    }

    public void clearTimer(Timer timer){
        timers.remove(timer);
    }
}
