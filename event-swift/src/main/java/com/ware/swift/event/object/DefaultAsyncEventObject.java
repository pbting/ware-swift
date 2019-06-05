package com.ware.swift.event.object;

import com.ware.swift.event.IEventPartitioner;
import com.ware.swift.event.IEventPartitionerRegister;
import com.ware.swift.event.ObjectEvent;
import com.ware.swift.event.parallel.action.ParallelActionExecutor;

public class DefaultAsyncEventObject<V> extends AbstractAsyncEventObject<V> implements IEventPartitionerRegister {

    /**
     * 这里放到子类里面来,目的就是为了区分父类partitioner 的方式。
     * 如果业务方自己继承AbstractAsyncEventObject，那么特定情况下自己重写 分类器 方法即可。
     * 如果业务方使用的是组合的方式，因为不能重写分类器，则需要借助接口适配的方式来实现 分类器 的具体实现。
     */
    protected IEventPartitioner iEventPartitioner;

    public DefaultAsyncEventObject(ParallelActionExecutor executor, boolean isOptimism) {
        super(executor, isOptimism);
    }

    public DefaultAsyncEventObject(String executorName, boolean isOptimism) {
        super(executorName, isOptimism);
    }

    @Override
    public void attachListener() {
        //nothing to do
    }

    @Override
    public String partitioner(ObjectEvent<V> event) {
        if (this.iEventPartitioner != null) {

            return iEventPartitioner.partitioner(event);
        }

        return super.partitioner(event);
    }

    @Override
    public void registerEventPartitioner(IEventPartitioner eventPartitioner) {
        this.iEventPartitioner = eventPartitioner;
    }
}
