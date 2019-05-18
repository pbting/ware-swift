package com.alibaba.aliware.core.swift.remoting.event.remoting;

import com.alibaba.aliware.core.swift.remoting.IInteractive;
import com.alibaba.aliware.core.swift.remoting.RemotingInteractiveException;
import com.alibaba.aliware.core.swift.remoting.RemotingManager;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * 集群节点之间的通信，都是在指定的事件类型之间进行通信。
 * <p>
 * 通过 sdk 客户端发送过来的数据是不带具体的事件类型的或者
 */
public class RemotingDefaultEventListener extends AbstractRemotingPipelineEventListener {

    private static final InternalLogger log = InternalLoggerFactory
            .getInstance(RemotingDefaultEventListener.class);

    public RemotingDefaultEventListener() {
        super();
    }

    @Override
    public boolean onEvent(ObjectEvent<IInteractive> event, int listenerIndex) {
        IInteractive iInteractive = event.getValue();
        if (log.isDebugEnabled()) {
            log.debug(iInteractive.getInteractivePayload().getSource() + ";"
                    + iInteractive.getInteractivePayload().getSink() + "; headers："
                    + iInteractive.getInteractivePayload().getHeadersMap().toString());
        }

        try {
            RemotingManager.getRemotingManager().getClientInteractivePayloadHandler()
                    .handler(iInteractive);
        } catch (RemotingInteractiveException e) {
            log.error("receive a payload cause an exception,", e);
        }
        return true;
    }
}
