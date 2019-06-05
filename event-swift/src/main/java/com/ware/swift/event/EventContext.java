package com.ware.swift.event;

import java.io.Serializable;
import java.util.EventObject;
import java.util.Hashtable;

public class EventContext extends EventObject implements Serializable {
    public final static String EVENT_TOPIC = "event.topic";
    public final static String EVENT_CALLBACK = "event.call.back";

    protected boolean isInterrupt;

    protected Hashtable<String, Object> eventContext = null;

    public EventContext(Object source) {
        super(source);
    }

    public boolean isInterrupt() {
        return isInterrupt;
    }

    public void setInterrupt(boolean isBroken) {
        this.isInterrupt = isBroken;
    }

    public void setParameter(String key, Object value) {
        Hashtable<String, Object> tmpContext = getEventContext();

        tmpContext.put(key, value);
    }

    protected Hashtable<String, Object> getEventContext() {
        Hashtable<String, Object> tmpContext = eventContext;
        if (tmpContext == null) {
            synchronized (this) {
                tmpContext = eventContext;
                if (tmpContext == null) {
                    eventContext = new Hashtable<>();
                    tmpContext = eventContext;
                }
            }
        }
        return tmpContext;
    }

    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key) {

        return eventContext == null ? null : (T) eventContext.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, T defaultValue) {

        return eventContext == null ? null
                : (T) eventContext.getOrDefault(key, defaultValue);
    }

    public void removeParameter(String key) {
        if (eventContext == null) {
            return;
        }
        eventContext.remove(key);
    }
}