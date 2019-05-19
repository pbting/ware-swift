package com.ware.swift.event.disruptor;

import com.ware.swift.event.common.AtomicLongMap;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.LiteBlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.ware.swift.event.parallel.AbstractParallelQueueExecutor;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class DisruptorParallelQueueExecutor extends AbstractParallelQueueExecutor {

	private RunnableEventTranslator translator = new RunnableEventTranslator();

	private Disruptor<RunnableEvent>[] disruptors;
	private ThreadPoolExecutorIndexAllocator ringBufferExecutorIndexChooser;
	private ConcurrentMap<String, Integer> queueExecutorMapping = new ConcurrentHashMap<String, Integer>();
	private ReentrantLock lock = new ReentrantLock(true);
	private AtomicLongMap<String> topicLastExecuteTime = AtomicLongMap.create();
	private AtomicBoolean isOperating = new AtomicBoolean(false);
	private AtomicBoolean isCronTrriger = new AtomicBoolean(false);

	@SuppressWarnings("unchecked")
	public DisruptorParallelQueueExecutor(int queueCount, int ringBufferSize) {
		if (queueCount <= 0) {
			queueCount = DEFAULT_QUEUE_SIZE;
		}

		disruptors = new Disruptor[queueCount];
		for (int i = 0; i < queueCount; i++) {
			// 半个小时之后还没有任务过来，则销毁线程
			disruptors[i] = builderDisruptor(ringBufferSize);
		}

		if (isPowerOfTwo(queueCount)) {
			ringBufferExecutorIndexChooser = new PowerOfTwoExecutorIndexChooser(
					queueCount);
		}
		else {
			ringBufferExecutorIndexChooser = new GenericExecutorIndexChooser(queueCount);
		}

	}

	@Override
	public void execute(String topic, Runnable command) {
		Integer index = queueExecutorMapping.get(topic);
		if (index == null) {
			try {
				lock.tryLock(3, TimeUnit.SECONDS);
				isOperating.set(true);
				index = queueExecutorMapping.get(topic);
				if (index == null) {
					index = ringBufferExecutorIndexChooser.allocator();
					queueExecutorMapping.put(topic, index);
				}
			}
			catch (Exception e) {
				index = ringBufferExecutorIndexChooser.allocator();
			}
			finally {
				isOperating.set(false);
				lock.unlock();
			}
		}
		Disruptor<RunnableEvent> ringBuffer = disruptors[index];
		runing(ringBuffer, command);
		topicLastExecuteTime.put(topic, System.currentTimeMillis());
	}

	@Override
	public void execute(Runnable command) {
		String topic = command.getClass().getName();
		execute(topic, command);
	}

	private void runing(Disruptor<RunnableEvent> disruptor, Runnable task) {
		disruptor.publishEvent(translator, task);
	}

	@SuppressWarnings("unchecked")
	private Disruptor<RunnableEvent> builderDisruptor(int ringBufferSize) {
		Disruptor<RunnableEvent> disruptor = new Disruptor<RunnableEvent>(
				new StringEventFactory(), ringBufferSize, DaemonThreadFactory.INSTANCE,
				ProducerType.SINGLE, new LiteBlockingWaitStrategy());
		disruptor.handleEventsWith(new RunnableEventHandler());
		disruptor.start();
		return disruptor;
	}

	public static class RunnableEventTranslator
			implements EventTranslatorOneArg<RunnableEvent, Runnable> {

		public void translateTo(RunnableEvent event, long sequence, Runnable arg0) {
			event.setValue(arg0);
		}
	}

	public static class StringEventFactory implements EventFactory<RunnableEvent> {

		public RunnableEvent newInstance() {

			return new RunnableEvent();
		}
	}

	public static class RunnableWapper {

	}

	public static class RunnableEvent implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		private Runnable value;

		public RunnableEvent() {
		}

		public Runnable getValue() {
			return value;
		}

		public void setValue(Runnable value) {
			this.value = value;
		}
	}

	public static class RunnableEventHandler implements EventHandler<RunnableEvent> {

		public void onEvent(RunnableEvent arg0, long arg1, boolean arg2)
				throws Exception {
			Runnable runnable = arg0.getValue();
			runnable.run();
			arg0.setValue(null);
		}
	}

	@Override
	public void stop() {
		if (disruptors == null) {
			return;
		}

		for (Disruptor<RunnableEvent> disruptor : disruptors) {
			disruptor.shutdown();
		}

		disruptors = null;
		super.stop();
	}

	@Override
	public void registerTopics(String... topics) {
		if (topics == null || topics.length == 0) {
			return;
		}

		try {
			lock.tryLock(3, TimeUnit.SECONDS);
			for (String topic : topics) {
				Integer index = queueExecutorMapping.get(topic);
				if (index != null) {
					continue;
				}

				index = ringBufferExecutorIndexChooser.allocator();
				queueExecutorMapping.put(topic, index);
			}
		}
		catch (Exception e) {
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public void removeTopic(String topic) {

		queueExecutorMapping.remove(topic);
	}

	@Override
	public void executeOneTime(Runnable command) {
		Disruptor<RunnableEvent> ringBuffer = disruptors[ringBufferExecutorIndexChooser
				.allocator()];
		runing(ringBuffer, command);
	}
}
