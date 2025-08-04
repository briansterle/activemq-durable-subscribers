package com.sterle;

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

public class TraceabilityInterceptor extends BrokerPluginSupport {

    @Override
    public Broker installPlugin(final Broker next) {
        setNext(next);
        return this;
    }

    private final ConcurrentMap<String, String> consumerIdToClientId = new ConcurrentHashMap<>();

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

        System.out.println("Dispatched message " + messageId +
                " to consumerId=" + consumerId + " (clientID=" + clientId + ") on destination: " + destination);

        super.postProcessDispatch(messageDispatch);
    }

    @Override
    public void send(ProducerBrokerExchange exchange, Message msg) throws Exception {
        try {
            if (msg != null && msg.getDestination() != null) {
                System.out.println(">>> Intercepted send to: " + msg.getDestination().getPhysicalName());
            } else {
                System.out.println(">>> Intercepted send with null destination");
            }
        } catch (Exception e) {
            System.err.println("Plugin error: " + e.getMessage());
        }
        super.send(exchange, msg);
    }

}
