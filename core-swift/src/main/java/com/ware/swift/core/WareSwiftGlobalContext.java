package com.ware.swift.core;

import com.ware.swift.core.remoting.RemotingManager;
import com.ware.swift.event.object.AbstractAsyncEventObject;
import com.ware.swift.event.object.IEventObjectListener;

import java.util.Hashtable;

/**
 *
 */
public class WareSwiftGlobalContext extends AbstractAsyncEventObject {

    public WareSwiftGlobalContext(String executorName, boolean isOptimism) {
        super(executorName, isOptimism);
    }

    private static final WareSwiftGlobalContext WARE_CORE_SWIFT_GLOBAL_CONTEXT = new WareSwiftGlobalContext("ware-swift-executor", true);

    /**
     *
     */
    public static final String RAFT_CONTEXT_ATTR_KEY_CONFIG_PATH = "B";

    /**
     *
     */
    public static final String RAFT_CONTEXT_ATTR_KEY_CONFIG = "C";

    /**
     *
     */
    private Hashtable<String, Object> contextAttrs = new Hashtable<>();

    public static WareSwiftGlobalContext getInstance() {

        return WARE_CORE_SWIFT_GLOBAL_CONTEXT;
    }

    /**
     * @param path
     */
    public void setConfigPath(String path) {
        contextAttrs.put(RAFT_CONTEXT_ATTR_KEY_CONFIG_PATH, path);
    }

    public <V> void putAttribute(String key, V value) {
        assert key != null;
        contextAttrs.put(key, value);
    }

    public <V> V getAttribute(String key) {
        assert key != null;
        return (V) contextAttrs.get(key);
    }

    public <V> V getAttribute(String key, String defaultValue) {
        assert key != null;
        return (V) contextAttrs.getOrDefault(key, defaultValue);
    }

    public NodeInformation getLeader() {
        if (RemotingManager.getRemotingManager().getWareSwiftConfig() == null) {
            return null;
        }
        return RemotingManager.getRemotingManager().getWareSwiftConfig().getLeader();
    }

    @Override
    public void attachListener() {
        // nothing to do
    }

    /**
     * @param objectListener
     * @param eventType
     * @param <V>
     */
    public <V> void registerEventListener(IEventObjectListener<V> objectListener, Integer eventType) {

        addListener(objectListener, eventType);
    }
}
