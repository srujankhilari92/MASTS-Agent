package com.varutra.webscarab.plugin.proxy;

import java.io.IOException;
import java.util.Date;


import com.varutra.webscarab.httpclient.HTTPClient;
import com.varutra.webscarab.model.Cookie;
import com.varutra.webscarab.model.FrameworkModel;
import com.varutra.webscarab.model.NamedValue;
import com.varutra.webscarab.model.Preferences;
import com.varutra.webscarab.model.Request;
import com.varutra.webscarab.model.Response;
import com.varutra.webscarab.plugin.Framework;

public class CookieTracker extends ProxyPlugin {
    
    private FrameworkModel _model = null;
    
    private boolean _injectRequests = false;
    private boolean _readResponses = false;
    
    public CookieTracker(Framework framework) {
        _model = framework.getModel();
        parseProperties();
    }
    
    public void parseProperties() {
        String prop = "CookieTracker.injectRequests";
        String value = Preferences.getPreference(prop, "false");
        _injectRequests = ("true".equalsIgnoreCase( value ) || "yes".equalsIgnoreCase( value ));
        prop = "CookieTracker.readResponses";
        value = Preferences.getPreference(prop, "true");
        _readResponses = ("true".equalsIgnoreCase( value ) || "yes".equalsIgnoreCase( value ));
    }
    
    public String getPluginName() {
        return new String("Cookie Tracker");
    }
    
    public void setInjectRequests(boolean bool) {
        _injectRequests = bool;
        String prop = "CookieTracker.injectRequests";
        Preferences.setPreference(prop,Boolean.toString(bool));
    }

    public boolean getInjectRequests() {
        return _injectRequests;
    }
    
    public void setReadResponses(boolean bool) {
        _readResponses = bool;
        String prop = "CookieTracker.readResponses";
        Preferences.setPreference(prop,Boolean.toString(bool));
    }

    public boolean getReadResponses() {
        return _readResponses;
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
            if (_injectRequests) {
                Cookie[] cookies = _model.getCookiesForUrl(request.getURL());
                if (cookies.length>0) {
                    StringBuffer buff = new StringBuffer();
                    buff.append(cookies[0].getName()).append("=").append(cookies[0].getValue());
                    for (int i=1; i<cookies.length; i++) {
                        buff.append("; ").append(cookies[i].getName()).append("=").append(cookies[i].getValue());
                    }
                    request.setHeader("Cookie", buff.toString());
                }
            }
            Response response = _in.fetchResponse(request);
            if (_readResponses && response != null) {
                NamedValue[] headers = response.getHeaders();
                for (int i=0; i<headers.length; i++) {
                    if (headers[i].getName().equalsIgnoreCase("Set-Cookie") || headers[i].getName().equalsIgnoreCase("Set-Cookie2")) {
                        Cookie cookie = new Cookie(new Date(), request.getURL(), headers[i].getValue());
                        _model.addCookie(cookie);
                    }
                }
            }
            return response;
        }
        
    }

    @Override
    public boolean getEnabled() {
        // TODO Auto-generated method stub
        return _injectRequests || _readResponses;
    }
    
}
