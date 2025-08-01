package com.sterle;

import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

public class NonDurableSubscriber {

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        Connection connection = factory.createConnection();
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = session.createTopic("demo.topic");

        MessageConsumer consumer = session.createConsumer(topic);

        System.out.println("Non-Durable subscriber listening for messages...");
        while (true) {
            Message message = consumer.receive();
            if (message instanceof TextMessage textMessage) {
                System.out.println("Non-durable received: " + textMessage.getText());
            } else if (message != null) {
                System.out.println("Received non-text message");
            }
        }
    }
}
