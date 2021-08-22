package org.apache.eventmesh.connector.standalone.consumer;

import io.openmessaging.api.AsyncGenericMessageListener;
import io.openmessaging.api.AsyncMessageListener;
import io.openmessaging.api.GenericMessageListener;
import io.openmessaging.api.Message;
import io.openmessaging.api.MessageListener;
import io.openmessaging.api.MessageSelector;
import org.apache.eventmesh.api.AbstractContext;
import org.apache.eventmesh.api.consumer.MeshMQPushConsumer;
import org.apache.eventmesh.connector.standalone.MessagingAccessPointImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;

public class StandaloneMeshMQPushConsumerAdaptor implements MeshMQPushConsumer {

    private final Logger logger = LoggerFactory.getLogger(StandaloneMeshMQPushConsumerAdaptor.class);

    private StandaloneConsumer consumer;

    public StandaloneMeshMQPushConsumerAdaptor() {
    }

    @Override
    public void init(Properties keyValue) throws Exception {
        String producerGroup = keyValue.getProperty("producerGroup");

        MessagingAccessPointImpl messagingAccessPoint = new MessagingAccessPointImpl(keyValue);
        consumer = (StandaloneConsumer) messagingAccessPoint.createConsumer(keyValue);

    }

    @Override
    public void updateOffset(List<Message> msgs, AbstractContext context) {
        for(Message message : msgs) {
            consumer.updateOffset(message);
        }
    }

    @Override
    public void subscribe(String topic, AsyncMessageListener listener) throws Exception {
        // todo: support subExpression
        consumer.subscribe(topic, "*", listener);
    }

    @Override
    public void unsubscribe(String topic) {
        consumer.unsubscribe(topic);
    }

    @Override
    public void subscribe(String topic, String subExpression, MessageListener listener) {
        throw new UnsupportedOperationException("not supported yet");
    }

    @Override
    public void subscribe(String topic, MessageSelector selector, MessageListener listener) {
        throw new UnsupportedOperationException("not supported yet");
    }

    @Override
    public <T> void subscribe(String topic, String subExpression, GenericMessageListener<T> listener) {
        throw new UnsupportedOperationException("not supported yet");
    }

    @Override
    public <T> void subscribe(String topic, MessageSelector selector, GenericMessageListener<T> listener) {
        throw new UnsupportedOperationException("not supported yet");
    }

    @Override
    public void subscribe(String topic, String subExpression, AsyncMessageListener listener) {
        throw new UnsupportedOperationException("not supported yet");
    }

    @Override
    public void subscribe(String topic, MessageSelector selector, AsyncMessageListener listener) {
        throw new UnsupportedOperationException("not supported yet");
    }

    @Override
    public <T> void subscribe(String topic, String subExpression, AsyncGenericMessageListener<T> listener) {
        throw new UnsupportedOperationException("not supported yet");
    }

    @Override
    public <T> void subscribe(String topic, MessageSelector selector, AsyncGenericMessageListener<T> listener) {
        throw new UnsupportedOperationException("not supported yet");
    }

    @Override
    public void updateCredential(Properties credentialProperties) {

    }

    @Override
    public boolean isStarted() {
        return consumer.isStarted();
    }

    @Override
    public boolean isClosed() {
        return consumer.isClosed();
    }

    @Override
    public void start() {
        consumer.start();
    }

    @Override
    public void shutdown() {
        consumer.shutdown();
    }
}