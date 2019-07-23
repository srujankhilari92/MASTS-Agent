package com.varutra.webscarab.plugin.proxy;

import java.io.IOException;



import com.varutra.webscarab.model.ConversationID;
import com.varutra.webscarab.model.HttpUrl;
import com.varutra.webscarab.plugin.PluginUI;

public interface ProxyUI extends PluginUI {
    
    void proxyAdded(ListenerSpec spec);
    
    void proxyStarted(ListenerSpec spec);
    
    void proxyStartError(ListenerSpec spec, IOException ioe);
    
    void proxyStopped(ListenerSpec spec);
    
    void proxyRemoved(ListenerSpec spec);
    
    void requested(ConversationID id, String method, HttpUrl url);
    
    void received(ConversationID id, String status);
    
    void aborted(ConversationID id, String reason);
    
}
