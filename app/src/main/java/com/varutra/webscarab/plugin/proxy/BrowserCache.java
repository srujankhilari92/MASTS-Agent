package com.varutra.webscarab.plugin.proxy;

import java.io.IOException;

import com.varutra.webscarab.httpclient.HTTPClient;
import com.varutra.webscarab.model.Preferences;
import com.varutra.webscarab.model.Request;
import com.varutra.webscarab.model.Response;

public class BrowserCache extends ProxyPlugin {
    
    private boolean _enabled = false;
    
    public BrowserCache() {
        parseProperties();
    }
    
    public void parseProperties() {
        String prop = "BrowserCache.enabled";
        _enabled = Preferences.getPreferenceBoolean(prop, false);
    }
    
    public String getPluginName() {
        return new String("Browser Cache");
    }
    
    public void setEnabled(boolean bool) {
        _enabled = bool;
        String prop = "BrowserCache.enabled";
        Preferences.setPreference(prop,Boolean.toString(bool));
    }

    public boolean getEnabled() {
        return _enabled;
    }
    
    public HTTPClient getProxyPlugin(HTTPClient in) {
        return new Plugin(in);
    }    
    
    private class Plugin implements HTTPClient {
    
        private HTTPClient _in;
        
        public Plugin(HTTPClient in) {
            _in = in;
        }
        
        public Response fetchResponse(Request request) throws IOException {
            if (_enabled) {
                request.deleteHeader("ETag");
                request.deleteHeader("If-Modified-Since");
                request.deleteHeader("If-None-Match");
            }
            return _in.fetchResponse(request);
        }
        
    }
    
}
