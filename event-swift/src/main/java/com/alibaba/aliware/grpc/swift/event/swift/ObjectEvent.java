package com.alibaba.aliware.grpc.swift.event.swift;

public class ObjectEvent<V> extends EventContext {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private V value;
	private int eventType;

	/**
	 *
	 * @param value
	 * @param eventType
	 */
	public ObjectEvent(Object source, V value, int eventType) {
		super(source);
		this.value = value;
		this.eventType = eventType;
	}

	public int getEventType() {
		return eventType;
	}

	public V getValue() {
		return value;
	}

	public void setValue(V value) {
		this.value = value;
	}

	public void setEventType(int eventType) {
		this.eventType = eventType;
	}

	public void setEventTopic(String eventTopic) {

		getEventContext().put(EVENT_TOPIC, eventTopic);
	}

	public String getEventTopic() {

		if (eventContext == null) {
			return String.valueOf(eventType);
		}

		String eventTopic = (String) eventContext.get(EVENT_TOPIC);
		if (eventTopic != null) {
			return eventTopic;
		}

		return String.valueOf(eventType);
	}

}
