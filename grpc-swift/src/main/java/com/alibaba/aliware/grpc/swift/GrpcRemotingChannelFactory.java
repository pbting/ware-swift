package com.alibaba.aliware.grpc.swift;

import com.alibaba.aliware.core.swift.remoting.IRemotingManager;
import com.alibaba.aliware.core.swift.remoting.channel.AbstractRemotingChannel;
import com.alibaba.aliware.core.swift.remoting.channel.IRemotingChannelFactory;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;

/**
 *
 */
public class GrpcRemotingChannelFactory implements IRemotingChannelFactory {

    private IRemotingManager remotingManager;

    public GrpcRemotingChannelFactory(IRemotingManager remotingManager) {
        this.remotingManager = remotingManager;
    }

    @Override
    public AbstractRemotingChannel newRemotingChannel(String addressPort,
                                                      String clusterName) {
        // 初始化和每个节点连接
        String[] addressPortArr = addressPort.split("[:]");
        NettyChannelBuilder nettyChannelBuilder = NettyChannelBuilder.forAddress(
                addressPortArr[0].trim(), Integer.valueOf(addressPortArr[1].trim()));

        ManagedChannel channel = nettyChannelBuilder.usePlaintext()
                .flowControlWindow(NettyChannelBuilder.DEFAULT_FLOW_CONTROL_WINDOW)
                .build();
        GrpcRemotingChannel remotingChannel = new GrpcRemotingChannel(channel,
                addressPort);
        remotingChannel.setIdentify(
                remotingManager.getChannelIdentify(addressPort + "@" + clusterName));

        return remotingChannel;
    }

}
