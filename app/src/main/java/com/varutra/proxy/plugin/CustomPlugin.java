package com.varutra.proxy.plugin;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;


import com.varutra.utils.network.ClientResolver;
import com.varutra.webscarab.httpclient.HTTPClient;
import com.varutra.webscarab.model.ConnectionDescriptor;
import com.varutra.webscarab.model.HttpUrl;
import com.varutra.webscarab.model.Request;
import com.varutra.webscarab.model.Response;
import com.varutra.webscarab.plugin.proxy.IClientResolver;
import com.varutra.webscarab.plugin.proxy.ProxyPlugin;

import android.util.Log;

public class CustomPlugin extends ProxyPlugin {
    
    private static boolean LOGD = true;
    private static String TAG = CustomPlugin.class.getName();
    private boolean _enabled = true;
    
    public CustomPlugin() {
    }
    
    public void parseProperties() {
    }
    
    public String getPluginName() {
        return new String("Custom Plugin");
    }
    
    public void setEnabled(boolean bool) {
        _enabled = bool;
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
                String setCookieHeader = "Set-Cookie";
                boolean changeResponse = false;
                ConnectionDescriptor cr = request.getConnectionDescriptor();
                if (cr != null){
                    if (LOGD) Log.d(TAG, "Connection data from: " + cr.getNamespace() + " " + cr.getId());
                }
                
                HttpUrl reqUrl = request.getURL();
                if (reqUrl != null && reqUrl.getHost() != null && reqUrl.getHost().equals("en.wikipedia.org") && reqUrl.getPath().equals("/wiki/Main_Page")){
                    changeResponse = true; 
                }
                
                Response response = _in.fetchResponse(request);
                
                if (changeResponse && response != null && response.getStatus().equals("200")){
                    byte[] responseContentByteArr = response.getContent();
                    if (responseContentByteArr != null){
                        String responseContentStr = new String (responseContentByteArr);
                        String changedResponse = responseContentStr.replace("<title>Wikipedia, the free encyclopedia</title>", "<title>VarutraProxy: Wikipedia, the free encyclopedia </title>");
                        response.setContent(changedResponse.getBytes());
                        if (LOGD) Log.d(TAG, "Response content modified by plugin");
                    }
                }
                
                List<String> headerNames = Arrays.asList(response.getHeaderNames());
                if (headerNames.contains(setCookieHeader)){
                    String newCookies = response.getHeader(setCookieHeader);
                    if (LOGD) Log.d(TAG, "New cookies from server: " + newCookies);
                }
                
                return response;
            }
            return _in.fetchResponse(request);
        }
        
    }
    
}