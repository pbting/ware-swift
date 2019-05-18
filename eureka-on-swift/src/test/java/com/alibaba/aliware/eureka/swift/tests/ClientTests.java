package com.alibaba.aliware.eureka.swift.tests;

import com.alibaba.aliware.core.swift.NodeInformation;
import com.alibaba.aliware.core.swift.remoting.IRemotingManager;
import com.alibaba.aliware.core.swift.remoting.RemotingInteractiveConstants;
import com.alibaba.aliware.core.swift.remoting.channel.AbstractRemotingChannel;
import com.alibaba.aliware.core.swift.remoting.ClusterDataSyncManager;
import com.alibaba.aliware.core.swift.remoting.conspart.ServiceInstance;
import com.alibaba.aliware.grpc.swift.GrpcRemotingChannel;
import com.alibaba.aliware.rsocket.swift.*;
import com.alibaba.aliware.rsocket.swift.handler.RSocketRequestHandlerSupport;
import com.alibaba.aliware.swift.proto.InteractivePayload;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ClientTests {

    private final static int DATA_TOTAL_COUNT = 1000;
    private final static int MULTI_THREAD_COUNT = 10;
    private final static IRemotingManager remo = new LeaderFollowerRSocketRemotingManager();

    public static AbstractRemotingChannel newGrpcRemotingChannel(String addressPort,
                                                                 String clusterName) {
        String[] addressPortArr = addressPort.split("[:]");
        NettyChannelBuilder nettyChannelBuilder = NettyChannelBuilder.forAddress(
                addressPortArr[0].trim(), Integer.valueOf(addressPortArr[1].trim()));
        ManagedChannel channel = nettyChannelBuilder.usePlaintext()
                .flowControlWindow(NettyChannelBuilder.DEFAULT_FLOW_CONTROL_WINDOW)
                .build();
        GrpcRemotingChannel remotingChannel = new GrpcRemotingChannel(channel,
                addressPort);
        remotingChannel.setIdentify(clusterName + "@" + addressPort + "@" + clusterName);
        return remotingChannel;
    }

    public static AbstractRemotingChannel newRSocketRemotingChannel(String addressPort,
                                                                    String clusterName) {
        String[] addressPortArr = addressPort.split("[:]");

        RSocketClientBootstrap clientBootstrap = new RSocketClientBootstrap(
                addressPortArr[0], Integer.valueOf(addressPortArr[1]));

        clientBootstrap
                .setSocketRequestHandlerFactory(new IRSocketRequestHandlerFactory() {
                    @Override
                    public RSocketRequestHandlerSupport createWithClient() {
                        return new RSocketClientSideInboundHandler(
                                RSocketRequestHandlerSupport.RSocketRequestHandlerRole.CLIENT);
                    }
                });
        while (true) {
            try {
                AbstractRemotingChannel remotingChannel = new RSocketRemotingChannel(
                        addressPort, clientBootstrap);
                remotingChannel.setIdentify("RAFT@" + addressPort + "@" + clusterName);
                return remotingChannel;
            } catch (Exception e) {
                System.err.println(addressPort + " is unavliable...");
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e1) {
                }
            }
        }
    }

    /**
     * DefaultClientInteractivePayloadHandler#getLeaderWithRSocket
     */
    @Test
    public void getLeaderWithRSocket() {
        System.err.println(getLeaderRSocketRequest().toString());
    }

    @Test
    public void getLeaderWithGrpc() {
        System.err.println(getLeaderGrpcRequest().toString());
    }

    public NodeInformation getLeaderRSocketRequest() {
        AbstractRemotingChannel abstractRemotingChannel = newRSocketRemotingChannel(
                "127.0.0.1:19092", "DEFAULT");
        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        builder.setSink("getLeader");

        InteractivePayload responsePayload = abstractRemotingChannel
                .requestResponse(builder.build());
        NodeInformation leader = (NodeInformation) RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                .decodeResult(responsePayload.getPayload().toByteArray(),
                        NodeInformation.class);
        System.err.println(leader.toString());
        return leader;
    }

    public NodeInformation getLeaderGrpcRequest() {
        AbstractRemotingChannel abstractRemotingChannel = newGrpcRemotingChannel(
                "127.0.0.1:19092", "DEFAULT");
        InteractivePayload.Builder builder = InteractivePayload.newBuilder();
        builder.setSink("getLeader");

        InteractivePayload responsePayload = abstractRemotingChannel
                .requestResponse(builder.build());
        NodeInformation leader = (NodeInformation) RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                .decodeResult(responsePayload.getPayload().toByteArray(),
                        NodeInformation.class);
        System.err.println(leader.toString());
        return leader;
    }

    public void addSomeDataWithRSocket() {
        NodeInformation leader = new ClientTests().getLeaderRSocketRequest();
        System.err.println("get the leader is:" + leader.toString());
        AbstractRemotingChannel abstractRemotingChannel = newRSocketRemotingChannel(
                leader.getAddressPort(), "DEFAULT");
        for (int i = 0; i < DATA_TOTAL_COUNT; i++) {
            InteractivePayload.Builder builder = InteractivePayload.newBuilder();
            builder.setSink("dataSync");
            ServiceInstance serviceInstance = new ServiceInstance();
            serviceInstance.setServiceId(
                    randaomeServiceName("rsocker", "order.service") + (i + 1));
            serviceInstance.setHost("127.0.0.1");
            serviceInstance.setPort(9090);
            serviceInstance.setScheme("tcp://");
            serviceInstance.setSecure(true);
            serviceInstance.setVersion(new AtomicLong().incrementAndGet());
            builder.putHeaders(ClusterDataSyncManager.HEADER_KEY_REMOTING_DOMAIN_CLASS,
                    serviceInstance.getClass().getName());
            builder.setPayload(ByteString
                    .copyFrom(RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                            .encodingResult(serviceInstance)));

            InteractivePayload responsePayload = abstractRemotingChannel
                    .requestResponse(builder.build());
            System.err.println(
                    serviceInstance.getServiceId() + ":" + serviceInstance.getVersion()
                            + "; " + responsePayload.getPayload().toStringUtf8());
        }
    }

    @Test
    public void boolInit() {

        Properties properties = new Properties();
        properties.setProperty("aaa", "true");
        System.err.println(Boolean.valueOf(properties.getProperty("aaa")));
    }

    @Test
    public void multiThreadDataSyncWithRSocket() throws Exception {

        for (int i = 0; i < MULTI_THREAD_COUNT; i++) {
            new Thread(() -> {
                try {
                    leaderFollowerdataSyncWithRSocket();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }

        System.in.read();
    }

    @Test
    public void decenterationDataSyncWithRSocket() throws Exception {
        AtomicLong syncCount = new AtomicLong();
        List<String> addressPortList = new LinkedList<>();
        addressPortList.add("127.0.0.1:19091");
        addressPortList.add("127.0.0.1:19092");
        addressPortList.add("127.0.0.1:19093");
        HashMap<String, AbstractRemotingChannel> remotingChannels = new HashMap<>();
        while (true) {
            int index = new Random().nextInt(addressPortList.size());
            String addressPort = addressPortList.get(index);
            AbstractRemotingChannel abstractRemotingChannel = remotingChannels
                    .get(addressPort);
            System.err.println("\tremoting channel :" + addressPort);
            if (abstractRemotingChannel == null) {
                AbstractRemotingChannel newRSocketRemotingChannel = newRSocketRemotingChannel(
                        addressPortList.get(index), "DEFAULT");
                remotingChannels.put(addressPort, newRSocketRemotingChannel);
                abstractRemotingChannel = newRSocketRemotingChannel;
            }
            InteractivePayload.Builder builder = InteractivePayload.newBuilder();
            builder.setSink("dataSync");
            ServiceInstance serviceInstance = new ServiceInstance();
            serviceInstance.setServiceId(randaomeServiceName("rsocket", "consumer"));
            serviceInstance.setHost("127.0.0.1");
            serviceInstance.setPort(9090);
            serviceInstance.setScheme("tcp://");
            serviceInstance.setSecure(true);
            serviceInstance.setVersion(syncCount.incrementAndGet());
            builder.putHeaders(ClusterDataSyncManager.HEADER_KEY_REMOTING_DOMAIN_CLASS,
                    serviceInstance.getClass().getName());
            builder.setPayload(ByteString
                    .copyFrom(RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                            .encodingResult(serviceInstance)));

            InteractivePayload responsePayload;
            try {
                responsePayload = abstractRemotingChannel
                        .requestResponse(builder.build());
                System.err.println(syncCount.get() + "; "
                        + responsePayload.getPayload().toStringUtf8());
            } catch (Exception e) {
                e.printStackTrace();
            }
            TimeUnit.SECONDS.sleep(1);
        }
    }

    @Test
    public void leaderFollowerdataSyncWithRSocket() throws Exception {
        addSomeDataWithRSocket();
        AtomicLong syncCount = new AtomicLong();
        NodeInformation leader = getLeaderRSocketRequest();
        System.err.println("get the leader is:" + leader.toString());
        AbstractRemotingChannel abstractRemotingChannel = newRSocketRemotingChannel(
                leader.getAddressPort(), "DEFAULT");
        while (true) {
            InteractivePayload.Builder builder = InteractivePayload.newBuilder();
            builder.setSink("dataSync");
            ServiceInstance serviceInstance = new ServiceInstance();
            serviceInstance.setServiceId("rsocket.order.consumer");
            serviceInstance.setHost("127.0.0.1");
            serviceInstance.setPort(9090);
            serviceInstance.setScheme("tcp://");
            serviceInstance.setSecure(true);
            serviceInstance.setVersion(syncCount.incrementAndGet());
            builder.putHeaders(ClusterDataSyncManager.HEADER_KEY_REMOTING_DOMAIN_CLASS,
                    serviceInstance.getClass().getName());
            builder.setPayload(ByteString
                    .copyFrom(RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                            .encodingResult(serviceInstance)));

            InteractivePayload responsePayload = abstractRemotingChannel
                    .requestResponse(builder.build());
            System.err.println(
                    syncCount.get() + "; " + responsePayload.getPayload().toStringUtf8());

            TimeUnit.SECONDS.sleep(5);
        }
    }

    public String randaomeServiceName(String prefix, String suffix) {

        return prefix + "." + UUID.randomUUID().toString().substring(0, 4) + "." + suffix;
    }

    public AbstractRemotingChannel addSomeDataWithGrpc() {
        NodeInformation leader = new ClientTests().getLeaderGrpcRequest();
        System.err.println("get the leader is:" + leader.toString());
        AbstractRemotingChannel abstractRemotingChannel = newGrpcRemotingChannel(
                leader.getAddressPort(), "DEFAULT");
        for (int i = 0; i < DATA_TOTAL_COUNT; i++) {
            InteractivePayload.Builder builder = InteractivePayload.newBuilder();
            builder.setSink("dataSync");
            ServiceInstance serviceInstance = new ServiceInstance();
            serviceInstance
                    .setServiceId(randaomeServiceName("grpc", "order.service") + (i + 1));
            serviceInstance.setHost("127.0.0.1");
            serviceInstance.setPort(9090);
            serviceInstance.setScheme("tcp://");
            serviceInstance.setSecure(true);
            serviceInstance.setVersion(new AtomicLong().incrementAndGet());
            builder.putHeaders(ClusterDataSyncManager.HEADER_KEY_REMOTING_DOMAIN_CLASS,
                    serviceInstance.getClass().getName());
            builder.setPayload(ByteString
                    .copyFrom(RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                            .encodingResult(serviceInstance)));

            InteractivePayload responsePayload = abstractRemotingChannel
                    .requestResponse(builder.build());
            System.err.println(
                    serviceInstance.getServiceId() + ":" + serviceInstance.getVersion()
                            + "; " + responsePayload.getPayload().toStringUtf8());
        }

        return abstractRemotingChannel;
    }

    @Test
    public void dataSyncWithGrpc() throws Exception {
        AtomicLong syncCount = new AtomicLong();
        NodeInformation leader = new ClientTests().getLeaderGrpcRequest();
        System.err.println("get the leader is:" + leader.toString());
        AbstractRemotingChannel abstractRemotingChannel = addSomeDataWithGrpc();
        while (true) {
            InteractivePayload.Builder builder = InteractivePayload.newBuilder();
            builder.setSink("dataSync");
            ServiceInstance serviceInstance = new ServiceInstance();
            serviceInstance.setServiceId("grpc.order.consumer");
            serviceInstance.setHost("127.0.0.1");
            serviceInstance.setPort(9090);
            serviceInstance.setScheme("tcp://");
            serviceInstance.setSecure(true);
            serviceInstance.setVersion(syncCount.incrementAndGet());
            builder.putHeaders(ClusterDataSyncManager.HEADER_KEY_REMOTING_DOMAIN_CLASS,
                    serviceInstance.getClass().getName());
            builder.setPayload(ByteString
                    .copyFrom(RemotingInteractiveConstants.OBJECT_ENCODING_HANDLER
                            .encodingResult(serviceInstance)));

            InteractivePayload responsePayload = abstractRemotingChannel
                    .requestResponse(builder.build());
            System.err.println(
                    syncCount.get() + "; " + responsePayload.getPayload().toStringUtf8());

            TimeUnit.SECONDS.sleep(10);
        }
    }
}
