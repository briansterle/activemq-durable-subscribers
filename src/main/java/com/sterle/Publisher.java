package com.sterle;


import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

public class Publisher {
    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ActiveMQConnectionFactory(  "tcp://localhost:61616"
);
        Connection connection = factory.createConnection();
        connection.setClientID("Publisher");
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = session.createTopic("demo.topic");

        MessageProducer producer = session.createProducer(topic);
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);

        for (int i = 1; i <= 100; i++) {
            TextMessage message = session.createTextMessage("Message " + i);
            producer.send(message);
            System.out.println("Published: " + message.getText());
            Thread.sleep(1000);
        }

        session.close();
        connection.close();
    }
}
