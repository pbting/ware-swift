package com.ware.swift.grpc;

import com.ware.swift.core.remoting.IRequestStreamCallbackRegistrator;
import com.ware.swift.core.remoting.channel.AbstractRemotingChannelModel;
import com.ware.swift.proto.InteractivePayload;
import com.ware.swift.proto.InteractiveServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

/**
 *
 */
public class GrpcRemotingChannel extends AbstractRemotingChannelModel {
    private volatile ManagedChannel managedChannel;

    public GrpcRemotingChannel(ManagedChannel managedChannel, String addressPort, JoinType joinType) {
        super(addressPort, joinType);
        this.managedChannel = managedChannel;
    }

    public ManagedChannel getManagedChannel() {
        return managedChannel;
    }

    public InteractiveServiceGrpc.InteractiveServiceStub getStub() {
        return InteractiveServiceGrpc.newStub(this.getManagedChannel());
    }

    public InteractiveServiceGrpc.InteractiveServiceBlockingStub getBlockingStub() {
        return InteractiveServiceGrpc.newBlockingStub(this.getManagedChannel());
    }

    @Override
    public boolean isNetUnavailable(Exception e) {
        return GrpcNetExceptionUtils.isNetUnavailable(e);
    }

    @Override
    public void close() {
        managedChannel.resetConnectBackoff();
    }

    @Override
    public void fireAndForget(InteractivePayload inBound) {
        requestResponse(inBound);
    }

    @Override
    public InteractivePayload requestResponse(InteractivePayload inBound) {
        InteractiveServiceGrpc.InteractiveServiceBlockingStub stub = getBlockingStub();
        InteractivePayload response = stub.requestResponse(inBound);
        return response;
    }

    /**
     * 返回的是一个: AbstractGrpcRequestStreamObserver,业务层必须调用一次 (IRemotingCallStreamObserver)}
     * (IRemotingCallStreamObserver)} 来处理接收服务端数据时的处理
     *
     * @param inBound
     * @return
     */
    @Override
    public IRequestStreamCallbackRegistrator requestStream(InteractivePayload inBound) {
        InteractiveServiceGrpc.InteractiveServiceStub stub = getStub();
        AbstractGrpcRequestStreamObserver streamObserver = new AbstractGrpcRequestStreamObserver() {
            @Override
            public void onNext(InteractivePayload value) {
                callStreamObserver.onNext(value);
            }
        };
        stub.requestStream(inBound, streamObserver);
        return streamObserver;
    }

    @Override
    public <OUT_BOUND, IN_BOUND> OUT_BOUND requestChannel(IN_BOUND inBound) {

        return (OUT_BOUND) getStub()
                .requestChannel((StreamObserver<InteractivePayload>) inBound);
    }
}
