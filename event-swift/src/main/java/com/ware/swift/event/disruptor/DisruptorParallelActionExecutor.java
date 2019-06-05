package com.ware.swift.event.disruptor;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.ware.swift.event.parallel.action.AbstractParallelActionExecutor;
import com.ware.swift.event.parallel.action.Action;
import com.ware.swift.event.parallel.action.ActionBridge;
import com.ware.swift.event.parallel.action.IActionQueue;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author pengbingting
 */
public class DisruptorParallelActionExecutor extends AbstractParallelActionExecutor {

    private ActionEventTranslator translator = new ActionEventTranslator();
    @SuppressWarnings("rawtypes")
    private Disruptor[] disruptorArray;
    private ThreadPoolExecutorIndexAllocator ringBufferExecutorIndexChooser;
    private ReentrantLock lock = new ReentrantLock(true);
    private ConcurrentMap<String, IActionQueue<Action>> parallelQueue = new ConcurrentHashMap<>();
    private AtomicBoolean isOperating = new AtomicBoolean(false);
    private Sequence actionQueueSize = new Sequence(0);

    public DisruptorParallelActionExecutor(int queueCount, int ringBufferSize) {
        if (queueCount <= 0) {
            queueCount = DEFAULT_QUEUE_SIZE;
        }

        disruptorArray = new Disruptor[queueCount];
        for (int i = 0; i < queueCount; i++) {
            disruptorArray[i] = builderDisruptor(ringBufferSize);
        }

        if (isPowerOfTwo(queueCount)) {
            ringBufferExecutorIndexChooser = new PowerOfTwoExecutorIndexChooser(
                    queueCount);
        } else {
            ringBufferExecutorIndexChooser = new GenericExecutorIndexChooser(queueCount);
        }

    }

    @Override
    public void execute(Runnable command) {
        execute(command.getClass().getName(), command);
    }

    /**
     * 濡傛灉杩欓噷涓嶅寘瑁呮垚涓�涓狝ction锛屽垯璺烻uperFastParallelQueueExecutor 娌′粈涔堝澶х殑鍖哄埆
     */
    @Override
    public void execute(String topic, Runnable command) {
        if (!(command instanceof Action)) {
            ActionBridge actionBridge = new ActionBridge(command);
            enParallelAction(topic, actionBridge);
        } else {
            Action action = (Action) command;
            enParallelAction(topic, action);
        }
    }

    @SuppressWarnings("unchecked")
    private Disruptor<ActionEvent> builderDisruptor(int ringBufferSize) {
        Disruptor<ActionEvent> disruptor = new Disruptor<>(
                new ActionEventFactory(), ringBufferSize, DaemonThreadFactory.INSTANCE,
                ProducerType.SINGLE, new LiteBlockingWaitStrategy());
        disruptor.handleEventsWith(new ActionEventHandler());
        disruptor.start();
        return disruptor;
    }

    public static class ActionEventTranslator
            implements EventTranslatorOneArg<ActionEvent, Action> {

        public void translateTo(ActionEvent event, long sequence, Action value) {
            event.setValue(value);
        }
    }

    public static class ActionEventFactory implements EventFactory<ActionEvent> {

        public ActionEvent newInstance() {

            return new ActionEvent();
        }
    }

    public static class ActionEvent implements Serializable {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

        private Action value;

        public ActionEvent() {
        }

        public Action getValue() {
            return value;
        }

        public void setValue(Action value) {
            this.value = value;
        }
    }

    public static class ActionEventHandler implements EventHandler<ActionEvent> {

        public void onEvent(ActionEvent arg0, long arg1, boolean arg2) throws Exception {
            Action action = arg0.getValue();
            action.run();
            arg0.setValue(action);
            action = null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void enParallelAction(String queueTopic, Action action) {
        // 1、
        IActionQueue<Action> actionQueue = parallelQueue.get(queueTopic);
        // 2、
        if (actionQueue == null) {
            lock.lock();
            try {
                isOperating.set(true);
                actionQueue = parallelQueue.get(queueTopic);
                if (actionQueue == null) {
                    int index = ringBufferExecutorIndexChooser.allocator();
                    actionQueue = new DisruptorParallelActionQueue(disruptorArray[index],
                            this.translator);
                    parallelQueue.put(queueTopic, actionQueue);
                    actionQueueSize.addAndGet(1);
                }
            } finally {
                isOperating.set(false);
                lock.unlock();
            }
        }

        // 3、
        action.setTopicName(queueTopic);

        // 4、
        trrigerWithRejectActionPolicy(actionQueue, action);
    }

    @Override
    public void removeParallelAction(String queueTopic) {
        removeTopic(queueTopic);
    }

    @Override
    public void adjustPoolSize(int newCorePoolSize, int newMaxiPoolSize) {

        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void stop() {
        if (disruptorArray == null) {
            return;
        }

        for (Disruptor<DisruptorParallelQueueExecutor.RunnableEvent> disruptor : disruptorArray) {
            disruptor.shutdown();
        }

        disruptorArray = null;
        super.stop();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void registerTopics(String... topics) {
        if (topics == null || topics.length == 0) {
            return;
        }

        try {
            lock.lock();
            for (String queueTopic : topics) {
                IActionQueue<Action> actionQueue = parallelQueue.get(queueTopic);
                if (actionQueue != null) {
                    continue;
                }

                int index = ringBufferExecutorIndexChooser.allocator();
                actionQueue = new DisruptorParallelActionQueue(disruptorArray[index],
                        this.translator);
                parallelQueue.put(queueTopic, actionQueue);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void removeTopic(String topic) {

        if (parallelQueue.remove(topic) != null) {
            actionQueueSize.addAndGet(-1);
        }
    }

    @Override
    public void executeOneTime(Runnable command) {

        if (command instanceof Action) {
            Action action = (Action) command;
            executeOneTimeAction(action);
        } else {
            ActionBridge actionBridge = new ActionBridge(command);
            executeOneTimeAction(actionBridge);
        }
    }

    @Override
    public void executeOneTimeAction(Action action) {
        @SuppressWarnings("unchecked")
        IActionQueue<Action> actionQueue = new DisruptorParallelActionQueue(
                disruptorArray[ringBufferExecutorIndexChooser.allocator()], this.translator);
        actionQueue.enqueue(action);
    }

}