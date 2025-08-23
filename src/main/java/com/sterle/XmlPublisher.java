package com.sterle;

import jakarta.jms.*;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.activemq.ActiveMQConnectionFactory;

public class XmlPublisher {
    private static final Random random = new Random();

    public static double randomLatitude() {
        return -90.0 + (90.0 - (-90.0)) * random.nextDouble(); // -90 to 90
    }

    public static double randomLongitude() {
        return -180.0 + (180.0 - (-180.0)) * random.nextDouble(); // -180 to 180
    }

    public static final int SERVICE_COUNT = 5;
    public static final int SYSTEM_COUNT = 1;

    public static List<UUID> serviceIds = Stream.generate(() -> UUID.randomUUID()).limit(SERVICE_COUNT).toList();
    public static List<String> serviceNames = IntStream.rangeClosed(1, SERVICE_COUNT).boxed().map(i -> "Service_" + i)
            .toList();

    public static List<UUID> systemIds = Stream.generate(() -> UUID.randomUUID()).limit(SYSTEM_COUNT).toList();
    public static List<String> systemNames = IntStream.rangeClosed(1, SYSTEM_COUNT).boxed().map(i -> "SystemName" + i)
            .toList();

    public static enum ObjectState {
        NEW,
        UPDATED, 
        DELETED;
    
        public static ObjectState random() {
            ObjectState[] values = values();
            return values[new Random().nextInt(values.length)];
        }
    }


    public static String genXmlMsg(int serviceId, int systemId) {
        return String.format("""
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
                <ObjectState>%s<ObjectState>
                <MessageData>
                    <Entity>
                        <UUID>%s</UUID>
                        <Lat>%.6f</Lat>
                        <Lon>%.6f</Lon>
                    </Entity>
                </MessageData>
                """, serviceIds.get(serviceId), serviceNames.get(serviceId), systemIds.get(systemId),
                systemNames.get(systemId), ObjectState.random(),UUID.randomUUID(), randomLatitude(), randomLongitude());
    }

    public static void main(String[] args) throws Exception {
        var executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < SERVICE_COUNT; i++) {
            for (int j = 0; j < SYSTEM_COUNT; j++) {
                final int serviceIdx = i; 
                final int systemIdx = j;  
                executor.submit(() ->{
                    try {
                        connPub(serviceIdx, systemIdx);
                    } catch (JMSException | InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    public static void connPub(int serviceIdx, int systemIdx) throws JMSException, InterruptedException {
        var name = serviceNames.get(serviceIdx);
        var sysName = systemNames.get(systemIdx);

        ConnectionFactory factory = new ActiveMQConnectionFactory(  "tcp://localhost:61616");
        Connection connection = factory.createConnection();
        connection.setClientID(name +":" + sysName);
        connection.start();

        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Topic topic = session.createTopic(name);

        MessageProducer producer = session.createProducer(topic);
        producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        while(true) {
            TextMessage message = session.createTextMessage(genXmlMsg(serviceIdx, systemIdx));
            producer.send(message);
            System.out.println("Published: " + message.getText());
            Thread.sleep(1000);
        }
    }
}
