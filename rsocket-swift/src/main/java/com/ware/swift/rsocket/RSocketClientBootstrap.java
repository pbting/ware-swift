package com.ware.swift.rsocket;

import io.rsocket.AbstractRSocket;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.DefaultPayload;

public class RSocketClientBootstrap extends RSocketBootstrapSupport<RSocket> {

    private Payload setupPayload;

    public RSocketClientBootstrap(String address, int port) {
        super(address, port);
    }

    @Override
    public RSocket bootstrap() {

        RSocket applyRsocket;

        if (socketRequestHandlerFactory != null) {
            final AbstractRSocket abstractRSocket = socketRequestHandlerFactory
                    .createWithClient();

            applyRsocket = RSocketFactory.connect()
                    .setupPayload(setupPayload != null ? setupPayload
                            : DefaultPayload.create(""))
                    .acceptor(rSocket -> abstractRSocket)
                    .transport(TcpClientTransport.create(address, port)).start().block();
        } else {

            applyRsocket = RSocketFactory.connect()
                    .transport(TcpClientTransport.create(address, port)).start().block();
        }
        return applyRsocket;
    }

    public void setSetupPayload(Payload setupPayload) {
        this.setupPayload = setupPayload;
    }
}
