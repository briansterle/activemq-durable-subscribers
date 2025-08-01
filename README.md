# ActiveMQ Demo

A demo of activemq showing durable vs. nondurable subscriber behavior.

```bash

# startup activemq
make start_activemq

# start up the publisher, writes an incrementing message every second
make publisher

# start up a non durable subscriber, drops any messages that were sent while this subscriber was down
make non_durable_subscriber

# start up a durable subscriber, will read ALL messages sent by the publisher, even while this subscriber was down
make durable_subscriber

```

Key line that makes this possible is:
```java

connection.setClientID("durable-client-id");
```

This identifies the durable subscriber to the broker so it can retain messages while the subscriber is offline. Setting the `clientID` is required for **durable topic subscriptions** in JMS. It tells the broker: "Track this subscriber by name. If they go offline, keep their messages until they reconnect."


For further reliability in the face of broker restarts, this can be configured for producers (Publisher.java in our example):

```java
MessageProducer producer = session.createProducer(topic);
producer.setDeliveryMode(DeliveryMode.PERSISTENT);
```

This persists messages to disk so they survive broker restarts.