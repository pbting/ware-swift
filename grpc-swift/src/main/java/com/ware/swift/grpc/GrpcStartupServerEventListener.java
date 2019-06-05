package com.ware.swift.grpc;

import com.ware.swift.core.WareSwiftConfig;
import com.ware.swift.core.remoting.IRemotingManager;
import com.ware.swift.core.remoting.event.local.StartupServerEventListener;
import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * 使用 Grpc 来启动 Server
 */
public class GrpcStartupServerEventListener extends StartupServerEventListener {

    public GrpcStartupServerEventListener(IRemotingManager remotingManager) {
        super(remotingManager);
    }

    @Override
    public void startupServer(WareSwiftConfig wareCoreSwiftConfig) {
        InetSocketAddress inetSocketAddress = wareCoreSwiftConfig.getNodeInformation()
                .getInetSocketAddress();
        NettyServerBuilder nettyServerBuilder = NettyServerBuilder
                .forAddress(inetSocketAddress);
        Server server = null;
        try {
            server = nettyServerBuilder.addService(new RaftInteractiveServiceGrpcImpl())
                    .flowControlWindow(NettyServerBuilder.DEFAULT_FLOW_CONTROL_WINDOW)
                    .build().start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (remotingManager instanceof LeaderFollowerGrpcRemotingManager) {
            remotingManager.setServer(server);
        }

    }
}
