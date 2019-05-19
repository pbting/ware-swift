package com.ware.swift.event.common;

/**
 * 幂等操作器
 * Created by yijunzhang on 14-10-22.
 */
public abstract class IdempotentConfirmer {
    private int retry = 3;

    protected IdempotentConfirmer(int retry) {
        this.retry = retry;
    }

    public IdempotentConfirmer() {
    }

    public abstract boolean execute();

    public boolean run() {
        while (retry-- > 0) {
            try {
                boolean isOk = execute();
                if (isOk){
                    return true;
                }
            } catch (Exception e) {
                Log.error(e.getMessage(), e);
                continue;
            }
        }
        return false;
    }
}

