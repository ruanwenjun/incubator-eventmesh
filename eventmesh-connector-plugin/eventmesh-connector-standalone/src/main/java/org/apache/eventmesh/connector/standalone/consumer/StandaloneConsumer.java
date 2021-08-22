package org.apache.eventmesh.connector.standalone.consumer;

import io.openmessaging.api.AsyncGenericMessageListener;
import io.openmessaging.api.AsyncMessageListener;
import io.openmessaging.api.Consumer;
import io.openmessaging.api.GenericMessageListener;
import io.openmessaging.api.Message;
import io.openmessaging.api.MessageListener;
import io.openmessaging.api.MessageSelector;
import org.apache.eventmesh.common.ThreadPoolFactory;
import org.apache.eventmesh.connector.standalone.broker.StandaloneBroker;
import org.apache.eventmesh.connector.standalone.broker.model.TopicMetadata;
import org.apache.eventmesh.connector.standalone.broker.task.SubScribeTask;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class StandaloneConsumer implements Consumer {

    private StandaloneBroker standaloneBroker;

    private AtomicBoolean isStarted;

    private ConcurrentHashMap<String, AsyncMessageListener> subscribeTable;

    private ConcurrentHashMap<String, SubScribeTask> subscribeTaskTable;

    private ExecutorService consumeExecutorService;

    public StandaloneConsumer(Properties properties) {
        this.standaloneBroker = StandaloneBroker.getInstance();
        this.subscribeTable = new ConcurrentHashMap<>(16);
        this.subscribeTaskTable = new ConcurrentHashMap<>(16);
        this.isStarted = new AtomicBoolean(false);
        consumeExecutorService = ThreadPoolFactory.createThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors() * 2,
                Runtime.getRuntime().availableProcessors() * 2,
                "StandaloneConsumerThread"
        );
    }

    @Override
    public void subscribe(String topic, String subExpression, MessageListener listener) {
    }

    @Override
    public void subscribe(String topic, MessageSelector selector, MessageListener listener) {
    }

    @Override
    public <T> void subscribe(String topic, String subExpression, GenericMessageListener<T> listener) {
    }

    @Override
    public <T> void subscribe(String topic, MessageSelector selector, GenericMessageListener<T> listener) {
    }

    @Override
    public void subscribe(String topic, String subExpression, AsyncMessageListener listener) {
        if (isClosed()) {
            return;
        }
        if (subscribeTable.containsKey(topic)) {
            return;
        }
        subscribeTable.put(topic, listener);
        SubScribeTask subScribeTask = new SubScribeTask(topic, standaloneBroker, listener);
        subscribeTaskTable.put(topic, subScribeTask);
        consumeExecutorService.submit(subScribeTask);
    }

    @Override
    public void subscribe(String topic, MessageSelector selector, AsyncMessageListener listener) {
    }

    @Override
    public <T> void subscribe(String topic, String subExpression, AsyncGenericMessageListener<T> listener) {
    }

    @Override
    public <T> void subscribe(String topic, MessageSelector selector, AsyncGenericMessageListener<T> listener) {
    }

    @Override
    public void unsubscribe(String topic) {
        if (isClosed()) {
            return;
        }
        if (!subscribeTable.containsKey(topic)) {
            return;
        }
        subscribeTable.remove(topic);
        SubScribeTask subScribeTask = subscribeTaskTable.get(topic);
        subScribeTask.shutdown();
        subscribeTaskTable.remove(topic);
    }

    @Override
    public void updateCredential(Properties credentialProperties) {

    }

    @Override
    public boolean isStarted() {
        return isStarted.get();
    }

    @Override
    public boolean isClosed() {
        return !isStarted.get();
    }

    @Override
    public void start() {
        isStarted.compareAndSet(false, true);
    }

    @Override
    public void shutdown() {
        isStarted.compareAndSet(true, false);
        subscribeTable.clear();
        subscribeTaskTable.forEach(((topic, subScribeTask) -> subScribeTask.shutdown()));
        subscribeTaskTable.clear();
    }

    public void updateOffset(Message message) {
        standaloneBroker.updateOffset(new TopicMetadata(message.getTopic()), message.getOffset());
    }
}