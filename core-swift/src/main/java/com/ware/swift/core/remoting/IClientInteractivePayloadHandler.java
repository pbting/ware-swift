package com.ware.swift.core.remoting;

import com.ware.swift.core.WareCoreSwiftPluginLoader;

import java.util.Set;
import java.util.TreeMap;

/**
 * 客户端请求消息处理器
 */
public interface IClientInteractivePayloadHandler {

    /**
     * @param interactive
     */
    void handler(IInteractive interactive) throws RemotingInteractiveException;

    /**
     * 如果你需要覆盖默认的实现，只需要将 order 值大于此值即可
     *
     * @return
     */
    default Integer order() {

        return Integer.MIN_VALUE;
    }

    static IClientInteractivePayloadHandler getInstance(ClassLoader classLoader) {
        Set<IClientInteractivePayloadHandler> interactivePayloadHandlerSet = WareCoreSwiftPluginLoader
                .load(IClientInteractivePayloadHandler.class, classLoader);
        if (interactivePayloadHandlerSet == null) {
            // the default model is CP
            return new DefaultClientInteractivePayloadHandler();
        }
        final TreeMap<Integer, IClientInteractivePayloadHandler> clientInteractivePayloadHandlerTreeMap = new TreeMap<>();
        interactivePayloadHandlerSet.forEach(
                iClientInteractivePayloadHandler -> clientInteractivePayloadHandlerTreeMap
                        .put(iClientInteractivePayloadHandler.order(),
                                iClientInteractivePayloadHandler));

        return clientInteractivePayloadHandlerTreeMap.lastEntry().getValue();
    }
}
