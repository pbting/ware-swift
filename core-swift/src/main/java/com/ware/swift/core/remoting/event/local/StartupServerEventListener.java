package com.ware.swift.core.remoting.event.local;

import com.ware.swift.core.WareSwiftConfig;
import com.ware.swift.core.WareSwiftExceptionCode;
import com.ware.swift.core.remoting.IRemotingManager;
import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.event.ObjectEvent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 *
 */
public abstract class StartupServerEventListener
        extends AbstractLocalPipelineEventListener<WareSwiftConfig> {

    private final InternalLogger logger = InternalLoggerFactory
            .getInstance(StartupServerEventListener.class);

    public StartupServerEventListener(IRemotingManager remotingManager) {
        super(remotingManager);
    }

    @Override
    public boolean onEvent(ObjectEvent<WareSwiftConfig> event, int listenerIndex) {
        boolean isSuccess = true;
        try {
            startupServer(event.getValue());
            RemotingManager.getRemotingManager().setRaftConfig(event.getValue());
            event.setInterrupt(false);
            remotingManager.getEventLoopGroup().removeListener(this, 1);
        } catch (Exception e) {
            logger.error(WareSwiftExceptionCode.formatExceptionMessage(
                    WareSwiftExceptionCode.REMOTING_STARTUP_SERVER_ERROR_CODE, e.getMessage()),
                    e);
            event.setInterrupt(true);
            isSuccess = false;
        }
        return isSuccess;
    }

    public abstract void startupServer(WareSwiftConfig wareSwiftConfig);
}
