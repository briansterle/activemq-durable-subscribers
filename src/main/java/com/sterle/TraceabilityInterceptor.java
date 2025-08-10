package com.sterle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.activemq.broker.Broker;
import org.apache.activemq.broker.BrokerPluginSupport;
import org.apache.activemq.broker.ConnectionContext;
import org.apache.activemq.broker.ProducerBrokerExchange;
import org.apache.activemq.broker.region.Subscription;
import org.apache.activemq.command.ConsumerInfo;
import org.apache.activemq.command.Message;
import org.apache.activemq.command.MessageDispatch;
import org.apache.activemq.command.ProducerInfo;

import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.XAddParams;

// what do i want to know?
// who has consumed what particular messageId
// who has published that particular messageId
public class TraceabilityInterceptor extends BrokerPluginSupport {

    JedisPooled jedis;

    @Override
    public Broker installPlugin(final Broker next) {
        setNext(next);
        jedis = new JedisPooled("redis", 6379);
        return this;
    }

    @Override
    public void addProducer(ConnectionContext context, ProducerInfo info) throws Exception {
        String producerId = info.getProducerId().toString();
        String clientId = context.getClientId();
        if (clientId != null) {
            producerIdToClientId.put(producerId, clientId);
        }
        super.addProducer(context, info);
    }

    private final ConcurrentMap<String, String> consumerIdToClientId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> producerIdToClientId = new ConcurrentHashMap<>();


    @Override
    public Subscription addConsumer(ConnectionContext context, ConsumerInfo info) throws Exception {
        String consumerId = info.getConsumerId().toString();
        String clientId = context.getClientId();

        if (clientId != null) {
            consumerIdToClientId.put(consumerId, clientId);
        }

        return super.addConsumer(context, info);
    }

    @Override
    public void postProcessDispatch(MessageDispatch messageDispatch) {
        String consumerId = messageDispatch.getConsumerId().toString();
        // var msgData = new String(messageDispatch.getMessage().getContent().data);

        String messageId = messageDispatch.getMessage().getMessageId().toString();
        String destination = messageDispatch.getDestination().getPhysicalName();

        String clientId = consumerIdToClientId.getOrDefault(consumerId, "unknown");
        var key = "read:" + messageId;
        jedis.xadd("read", Map.of("reader", clientId, "msg_id", messageId), XAddParams.xAddParams());
        System.out.println(clientId + " <<< " + key);

        super.postProcessDispatch(messageDispatch);
    }

    @Override
    public void send(ProducerBrokerExchange exchange, Message msg) throws Exception {
        try {
            if (msg != null && msg.getDestination() != null) {
                String producedBy = producerIdToClientId.getOrDefault(msg.getProducerId().toString(), msg.getProducerId().toString());
                var key = "wrote:" + msg.getMessageId().toString();
                jedis.xadd("wrote", Map.of("writer", producedBy, "msg_id", msg.getMessageId().toString()), XAddParams.xAddParams());

                System.out.println(producedBy + " >>> " + key);
            } else {
                System.out.println(">>> Intercepted send with null destination");
            }
        } catch (Exception e) {
            System.err.println("Plugin error: " + e.getMessage());
        }
        super.send(exchange, msg);
    }

}
