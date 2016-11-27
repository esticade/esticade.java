# Esticade
 
A simple library for creating interconnecting services using RabbitMQ on 
the background. Will make the pubsub pattern seamlessly available between 
different processes. Minimalist API is designed to be easy to learn and 
flexible enough for most tasks.

# Install

Add maven dependency to your project:

```xml
<dependency>
   <groupId>io.esticade</groupId>
   <artifactId>esticade</artifactId>
   <version>1.0.2</version>
</dependency>
```

Or using Gradle:

```groovy
compile 'io.esticade:esticade:1.0.2'
```

# API

To start using Esticade, please create an instance of esticade Service
object. 

## Service object

- `Esticade(serviceName)` - Will construct a new service and connect to the exchange.                            
- `on(eventName, callback)` - Will register event listener. Callback will be called with an `Event` object as the only argument. If there are two or more instances of the same service running, the events will be equally divided between all the instances. If this is not a desired behaviour use `alwaysOn`. Will return promise that is fulfilled once the handler is registered.
- `alwaysOn(eventName, callback)` - Same as `on`, except different instances of the same services will all return the event.   
- `emit(eventName[, payload])` - Will emit event to the event network. Returns promise that is fulfilled once the event is emitted.
- `emitChain(eventName[, payload])` - Will create an emit chain, allowing events caused by this event to be listened to. Will return `EventChain` object. Note that the event is not triggered before `execute` is called on the event chain.
- `shutdown()` - Will shut the entire service down, if there is nothing else keeping process alive, the process will terminate.

## Event object

- `name` - Name of the event.
- `body` - The content of the message.
- `correlationId` - Will be same on all the events in the event chain.
- `eventId` - Unique identifier for the event
- `parentId` - Id of the event causing this event in the current chain.
- `emit(eventName[, payload])` - Will emit event to the event network. Returns promise which is fulfilled once the event is emitted.
- `bodyAs(class)` - Will map the object to a java bean
 
## EmitChain object

- `on(eventName, callback)` - Will register event listener in the chain. Will return current `EventChain` object. Callback will be called with an `Event` object as the only argument.
- `execute()` - Trigger the event chain. Returns promise which will be fulfilled once the initiating event is triggered.
- `timeout(timeoutInMsec)` - Set the timeout when the event chain is terminated. Will return current `EventChain` object.
- `timeout(callback)` - Set the callback which is called once the event chain is terminated. Will return current `EventChain` object.
- `timeout(timeoutInMsec, callback)` - Do both of the above. Will return current `EventChain` object.

# Quick start

Install RabbitMQ on the machine and create following services:

Service 1 (Multiplication Service):
```java
import io.esticade.Service;
import java.io.IOException;

class MultiplicationService {
    public static void main(String[] args) throws IOException {
        Service service = new Service("Multiplication Service");

        service.on("MultiplyNumbers", (ev) -> {
            MultiplicationRequest req = ev.bodyAs(MultiplicationRequest.class);
            MultiplicationResult res = new MultiplicationResult();

            res.setA(req.getA());
            res.setB(req.getB());
            res.setResult(req.getA() * req.getB());

            ev.emit("MultipliedNumbers", res);
        });
    }
}
```

Service 2 (Number generator):
```java
import io.esticade.Service;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class NumberGeneratorService {
    public static void main(String[] args) throws IOException, InterruptedException {
        Service service = new Service("Number Generator Service");
        Random rng = new Random();

        while(true) {
            MultiplicationRequest req = new MultiplicationRequest();

            req.setA(rng.nextDouble());
            req.setB(rng.nextDouble());

            service.emit("MultiplyNumbers", req);

            TimeUnit.SECONDS.sleep(1);
        }
    }
}
```

Service 3 (Result displayer):
```java
import io.esticade.Service;
import java.io.IOException;

public class ResultDisplayerService {
    public static void main(String[] args) throws IOException {
        Service service = new Service("Result Displayer Service");

        service.on("MultipliedNumbers", (ev) -> {
            MultiplicationResult res = ev.bodyAs(MultiplicationResult.class);
            System.out.format("%f * %f = %f\n", res.getA(), res.getB(), res.getResult());
        });
    }
}
```

One time script (Request based method):
```java
import io.esticade.Service;
import java.io.IOException;

public class RequestService {
    public static void main(String[] args) throws IOException {
        Service service = new Service("Request Service");
        MultiplicationRequest req = new MultiplicationRequest();

        req.setA(10);
        req.setB(15);

        service.emitChain("MultiplyNumbers", req)
            .on("MultipliedNumbers", (ev) -> {
                MultiplicationResult res = ev.bodyAs(MultiplicationResult.class);
                System.out.format("%f * %f = %f\n", res.getA(), res.getB(), res.getResult());
                service.shutdown();
            })
            .execute();
    }
}
```

Additionally following beans are required for transport in each of the services:
```java
class MultiplicationRequest {
    private double a;
    private double b;

    public double getA() {
        return a;
    }

    public void setA(double a) {
        this.a = a;
    }

    public double getB() {
        return b;
    }

    public void setB(double b) {
        this.b = b;
    }
}

public class MultiplicationResult {
    private double a;
    private double b;
    private double result;

    public double getA() {
        return a;
    }

    public void setA(double a) {
        this.a = a;
    }

    public double getB() {
        return b;
    }

    public void setB(double b) {
        this.b = b;
    }

    public double getResult() {
        return result;
    }

    public void setResult(double result) {
        this.result = result;
    }
}
```

# Configuration

By default the library will try to connect to localhost with user and pass guest/guest. This is the default configuration
for RabbitMQ. If you want to override that, you can override it with a configuration file in any of following locations.

- Environment variables for each of the configuration variables
- A file pointed to by ESTICADERC environment variable
- esticade.json in current working folder or any parent folder.
- .esticaderc in current user home directory
- /etc/esticade/esticaderc

If any of those files is located in that order, it's contents are read and used for configuration. It should contain
JSON object with any of the following properties: 

- `connectionURL` - Default: `amqp://guest:guest@localhost/`
- `exchange` - Default `events`
- `engraved` - Default `false`. Will make named queues (those registered with service.on()) durable. We suggest you leave this
option to `false` during development as otherwise you will get a lot of permanent queues in the rabbitmq server. You should
turn this on in production though, as it will make sure no messages get lost when service restarts. Turning it off when it
has been turned on might cause errors as the durable queues are not redefined as non-durable automatically. You have
to manually delete the queues from RabbitMQ.

Example:

```json
{ 
    "connectionURL": "amqp://user:pass@example.com/vhost",
    "exchange": "EventNetwork"
}
```

## Environment variables

- `ESTICADE_CONNECTION_URL` - AMQP url to connect to
- `ESTICADE_EXCHANGE` - Exchange name
- `ESTICADE_ENGRAVED` - Whether or not to engrave the queues 
