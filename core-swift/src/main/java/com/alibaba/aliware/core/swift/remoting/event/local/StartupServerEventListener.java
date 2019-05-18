package com.alibaba.aliware.core.swift.remoting.event.local;

import com.alibaba.aliware.core.swift.WareCoreSwiftConfig;
import com.alibaba.aliware.core.swift.WareCoreSwiftExceptionCode;
import com.alibaba.aliware.core.swift.remoting.IRemotingManager;
import com.alibaba.aliware.core.swift.remoting.RemotingManager;
import com.alibaba.aliware.grpc.swift.event.swift.ObjectEvent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 *
 */
public abstract class StartupServerEventListener
        extends AbstractLocalPipelineEventListener<WareCoreSwiftConfig> {

    private final InternalLogger logger = InternalLoggerFactory
            .getInstance(StartupServerEventListener.class);

    public StartupServerEventListener(IRemotingManager remotingManager) {
        super(remotingManager);
    }

    @Override
    public boolean onEvent(ObjectEvent<WareCoreSwiftConfig> event, int listenerIndex) {
        boolean isSuccess = true;
        try {
            startupServer(event.getValue());
            RemotingManager.getRemotingManager().setRaftConfig(event.getValue());
            event.setInterruptor(false);
            remotingManager.getEventLoopGroup().removeListener(this, 1);
        } catch (Exception e) {
            logger.error(WareCoreSwiftExceptionCode.formatExceptionMessage(
                    WareCoreSwiftExceptionCode.REMOTING_STARTUP_SERVER_ERROR_CODE, e.getMessage()),
                    e);
            event.setInterruptor(true);
            isSuccess = false;
        }
        return isSuccess;
    }

    public abstract void startupServer(WareCoreSwiftConfig raftConfig);
}
