package com.ware.swift.core.remoting.event.local;

import com.ware.swift.core.WareCoreSwiftConfig;
import com.ware.swift.core.WareCoreSwiftExceptionCode;
import com.ware.swift.core.remoting.IRemotingManager;
import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.event.ObjectEvent;
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
