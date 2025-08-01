package com.sterle;

import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

public class DurableSubscriber {
    public static void main(String[] args) throws Exception {

        ConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        Connection connection = factory.createConnection();
        connection.setClientID("durable-client-id");
        connection.start();

        Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        Topic topic = session.createTopic("demo.topic");

        TopicSubscriber subscriber = session.createDurableSubscriber(topic, "durable-sub");
        System.out.println("Durable subscriber listening for messages...");
        while (true) {
            Message message = subscriber.receive();
            message.acknowledge();
            if (message instanceof TextMessage textMessage) {
                System.out.println("Durable received: " + textMessage.getText());
            } else if (message != null) {
                System.out.println("Received non-text message");
            }
            Thread.sleep(100);
        }
    }
}
