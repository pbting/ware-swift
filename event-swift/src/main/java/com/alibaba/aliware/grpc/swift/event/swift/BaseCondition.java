package com.alibaba.aliware.grpc.swift.event.swift;

/**
 * <pre>
 * [说明:]
 * O: 就是被观察这对象，因为它承载了数据源，提供条件的判断。
 * V: 满足某个条件的固定值
 * </pre>
 * 详细使用
 */
public abstract class BaseCondition<O, V> {
    protected O observiable;
    protected V value;

    public BaseCondition(O observiable, V v) {
        this.observiable = observiable;
        this.value = v;
    }

    /**
     * 业务触发的前提条件。需要业务人员根据自己的业务需求来实现
     *
     * @return
     */
    public abstract boolean isFinished();

    /**
     * 当 isFinished return true 时，handler 中的业务逻辑才会触发
     */
    public abstract void handler();

    public O getObserviable() {
        final O o = observiable;
        return o;
    }

    public V getValue() {
        final V v = value;
        return v;
    }
}
