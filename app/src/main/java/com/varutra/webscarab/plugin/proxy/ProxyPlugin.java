package com.varutra.webscarab.plugin.proxy;

import com.varutra.webscarab.httpclient.HTTPClient;

public abstract class ProxyPlugin {
    
    public void setSession(String type, Object store, String session)  {
    }
    
    public void flush() {
    }
    
    
    public abstract String getPluginName();
    
    public abstract HTTPClient getProxyPlugin(HTTPClient in);
    
    public abstract boolean getEnabled();
    
}
