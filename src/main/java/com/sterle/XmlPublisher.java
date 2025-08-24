package com.sterle;

import jakarta.jms.*;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.activemq.ActiveMQConnectionFactory;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.sterle.EntityMsg.EntityMessage;

public class XmlPublisher {
    private static final Random random = new Random();
    private static final XmlMapper xmlMapper = new XmlMapper();

    static double randomLatitude() {
        return -90.0 + (90.0 - (-90.0)) * random.nextDouble(); // -90 to 90
    }

    static double randomLongitude() {
        return -180.0 + (180.0 - (-180.0)) * random.nextDouble(); // -180 to 180
    }

    static final int SERVICE_COUNT = 10;
    static final int SYSTEM_COUNT = 1;

    static List<UUID> serviceIds = Stream.generate(() -> UUID.randomUUID()).limit(SERVICE_COUNT).toList();
    static List<String> serviceNames = IntStream.rangeClosed(1, SERVICE_COUNT).boxed().map(i -> "Service_" + i)
            .toList();
    static List<String> topicNames = IntStream.rangeClosed(1, SERVICE_COUNT).boxed().map(i -> "Entity_" + i)
            .toList();

    static List<UUID> systemIds = Stream.generate(() -> UUID.randomUUID()).limit(SYSTEM_COUNT).toList();
    static List<String> systemNames = IntStream.rangeClosed(1, SYSTEM_COUNT).boxed().map(i -> "SystemName" + i)
            .toList();

    static enum ObjectState {
        NEW,
        UPDATED,
        DELETED;

        static ObjectState random() {
            ObjectState[] values = values();
            return values[new Random().nextInt(values.length)];
        }
    }

    static String genXmlMsg(int serviceId, int systemId) {
        return String.format("""
            <EntityMessage>
                <MessageHeader>
                    <ServiceId>
                        <UUID>%s</UUID>
                        <Name>%s</Name>
                    </ServiceId>
                    <SystemId>
                        <UUID>%s</UUID>
                        <Name>%s</Name>
                    </SystemId>
                </MessageHeader>
                <ObjectState>%s</ObjectState>
                <MessageData>
                    <Entity>
                        <UUID>%s</UUID>
                        <Lat>%.6f</Lat>
                        <Lon>%.6f</Lon>
                    </Entity>
                </MessageData>
            </EntityMessage>
                """, serviceIds.get(serviceId), serviceNames.get(serviceId), systemIds.get(systemId),
                systemNames.get(systemId), ObjectState.random(), UUID.randomUUID(), randomLatitude(),
                randomLongitude());
    }

    public static void main(String[] args) throws Exception {

        var executor = Executors.newFixedThreadPool(SERVICE_COUNT * 2);
        for (int i = 0; i < SERVICE_COUNT; i++) {
            for (int j = 0; j < SYSTEM_COUNT; j++) {
                final int serviceIdx = i;
                final int systemIdx = j;
                String topicName = "Entity_" + i;
                executor.submit(() -> {
                    try {
                        connPub(serviceIdx, systemIdx, topicName);
                    } catch (JMSException | InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                executor.submit(() -> {
                    try {
                        connSub(serviceIdx, topicName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    static void connSub(int consumerId, String topicName) throws Exception {
        ConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        Connection connection = factory.createConnection();
        var clientId = "ReaderService_" + consumerId;
        connection.setClientID(clientId);
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        // Topic topic = session.createTopic(topicName);
        List<MessageConsumer> consumers = topicNames.stream().map(topicNm -> {
            try {
                var topic =  session.createTopic(topicNm);
                return session.createConsumer(topic);
            } catch (JMSException e) {
                return null;
            }
        }).toList();
        
        // MessageConsumer consumer = session.createConsumer(topic);
        int x = 0;
        System.out.println(clientId + "listening for messages...");
        while (true) {
            int randConsumerIdx = new Random().nextInt(topicNames.size());
            x++;
            Message message =  consumers.get(randConsumerIdx).receive();
            if (message instanceof TextMessage textMessage) {
                var xml = textMessage.getText();
                EntityMessage msg = xmlMapper.readValue(xml, EntityMessage.class);
                System.out.printf("%s read msg #%d from %s with entity id %s\n", clientId, x, msg.messageHeader().serviceId(),
                        msg.messageData().entity().uuid());
            } else if (message != null) {
                System.out.println("Received non-text message");
            }
        }
    }

    static void connPub(int serviceIdx, int systemIdx, String topicName) throws JMSException, InterruptedException {
        var name = serviceNames.get(serviceIdx);
        var sysName = systemNames.get(systemIdx);

        ConnectionFactory factory = new ActiveMQConnectionFactory("tcp://localhost:61616");
        Connection connection = factory.createConnection();
        connection.setClientID(name + ":" + sysName);
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = session.createTopic(topicName);

        MessageProducer producer = session.createProducer(topic);
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        while (true) {
            TextMessage message = session.createTextMessage(genXmlMsg(serviceIdx, systemIdx));
            producer.send(message);
            Thread.sleep(200);
        }
    }
}
