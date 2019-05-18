package com.alibaba.aliware.grpc.swift.event.swift;

/**
 *
 * @param <E> 条件满足后负责执行业务逻辑的执行器: executor
 * @param <R> 业务执行完后的返回值: result
 * @param <S> 产生 condtion 的事件源: source
 */
public abstract class ConditionSupport<E,R,S> {

    protected E executor ;
    public ConditionSupport(E executor){
        this.executor = executor;
    }

    /**
     *
     * @param src
     * @return
     */
    protected abstract boolean accept(S src) ;

    /**
     *
     * @param src
     * @return
     */
    public R conditionPerform(S src){
        if (accept(src)){
            return onTrueExecute(this.executor,src);
        }

        return onFalseExecute(this.executor,src);
    }

    /**
     *
     * @param executor
     * @param src
     * @return
     */
    protected abstract R onTrueExecute(E executor,S src);

    /**
     *
     * @param executor
     * @param src
     * @return
     */
    protected abstract R onFalseExecute(E executor,S src);
}