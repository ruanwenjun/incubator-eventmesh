/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.eventmesh.connector.standalone.broker;

import io.openmessaging.api.Message;
import org.apache.eventmesh.connector.standalone.broker.model.MessageEntity;
import org.apache.eventmesh.connector.standalone.broker.model.TopicMetadata;
import org.apache.eventmesh.connector.standalone.broker.task.HistoryMessageClearTask;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This broker used to store event, it just support standalone mode, you shouldn't use this module in production environment
 */
public class StandaloneBroker {

    private final ConcurrentHashMap<TopicMetadata, MessageQueue> messageContainer;

    // todo: move the offset manage to consumer
    private final ConcurrentHashMap<TopicMetadata, AtomicLong> offsetMap;

    private StandaloneBroker() {
        this.messageContainer = new ConcurrentHashMap<>();
        this.offsetMap = new ConcurrentHashMap<>();
        startHistoryMessageCleanTask();
    }

    public static StandaloneBroker getInstance() {
        return StandaloneBrokerInstanceHolder.instance;
    }

    /**
     * put message
     *
     * @param topicName topic name
     * @param message   message
     * @throws InterruptedException
     */
    public MessageEntity putMessage(String topicName, Message message) throws InterruptedException {
        TopicMetadata topicMetadata = new TopicMetadata(topicName);
        AtomicLong topicOffset = offsetMap.computeIfAbsent(topicMetadata, k -> new AtomicLong());
        MessageEntity messageEntity = new MessageEntity(topicMetadata, message, topicOffset.getAndIncrement(), System.currentTimeMillis());
        messageContainer.computeIfAbsent(topicMetadata, k -> new MessageQueue()).put(messageEntity);

        return messageEntity;
    }

    /**
     * Get the message, if the queue is empty then await
     *
     * @param topicName
     */
    public Message takeMessage(String topicName) throws InterruptedException {
        TopicMetadata topicMetadata = new TopicMetadata(topicName);
        return messageContainer.computeIfAbsent(topicMetadata, k -> new MessageQueue()).take().getMessage();
    }

    /**
     * Get the message, if the queue is empty return null
     *
     * @param topicName
     */
    public Message getMessage(String topicName) {
        TopicMetadata topicMetadata = new TopicMetadata(topicName);
        MessageEntity head = messageContainer.computeIfAbsent(topicMetadata, k -> new MessageQueue()).getHead();
        if (head == null) {
            return null;
        }
        return head.getMessage();
    }

    /**
     * Get the message by offset
     *
     * @param topicName topic name
     * @param offset    offset
     * @return
     */
    public Message getMessage(String topicName, long offset) {
        TopicMetadata topicMetadata = new TopicMetadata(topicName);
        MessageEntity messageEntity = messageContainer.computeIfAbsent(topicMetadata, k -> new MessageQueue()).getByOffset(offset);
        if (messageEntity == null) {
            return null;
        }
        return messageEntity.getMessage();
    }


    private void startHistoryMessageCleanTask() {
        Thread thread = new Thread(new HistoryMessageClearTask(messageContainer));
        thread.setDaemon(true);
        thread.setName("StandaloneBroker-HistoryMessageCleanTask");
        thread.start();
    }

    public boolean checkTopicExist(String topicName) {
        return messageContainer.containsKey(topicName);
    }

    public void updateOffset(TopicMetadata topicMetadata, long offset) {
        offsetMap.computeIfPresent(topicMetadata, (k, v) -> {
            v.set(offset);
            return v;
        });
    }

    private static class StandaloneBrokerInstanceHolder {
        private static final StandaloneBroker instance = new StandaloneBroker();
    }
}