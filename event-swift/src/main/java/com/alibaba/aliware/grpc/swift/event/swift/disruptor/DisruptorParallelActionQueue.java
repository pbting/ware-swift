package com.alibaba.aliware.grpc.swift.event.swift.disruptor;

import java.util.LinkedList;
import java.util.Queue;

import com.alibaba.aliware.grpc.swift.event.swift.disruptor.DisruptorParallelActionExecutor.ActionEvent;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.action.AbstractActionQueue;
import com.alibaba.aliware.grpc.swift.event.swift.parallel.action.Action;

import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.dsl.Disruptor;

class DisruptorParallelActionQueue extends AbstractActionQueue{
	private Disruptor<DisruptorParallelActionExecutor.ActionEvent> disruptor ;
	private EventTranslatorOneArg<DisruptorParallelActionExecutor.ActionEvent,Action> translator;
	
	public DisruptorParallelActionQueue(Queue<Action> queue, Disruptor<DisruptorParallelActionExecutor.ActionEvent> disruptor) {
		super(queue);
		this.disruptor = disruptor;
	}

	public DisruptorParallelActionQueue(Disruptor<DisruptorParallelActionExecutor.ActionEvent> ringBuffer, EventTranslatorOneArg<DisruptorParallelActionExecutor.ActionEvent,Action> translator) {
		super(new LinkedList<Action>());
		this.disruptor = ringBuffer;
		this.translator = translator;
	}
	
	public DisruptorParallelActionQueue(Queue<Action> queue) {
		super(queue);
	}

	@Override
	public void doExecute(Runnable runnable) {
		Action action = (Action) runnable;
		disruptor.publishEvent(translator, action);
	}
}
