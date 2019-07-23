package com.varutra.webscarab.plugin.spider;

import com.varutra.webscarab.model.HttpUrl;

public class Link {
    
    private HttpUrl _url;
    private String _referer;
    
    public Link(HttpUrl url, String referer) {
        _url = url;
        _referer = referer;
    }
    
    public HttpUrl getURL() {
        return _url;
    }
    
    public String getReferer() {
        return _referer;
    }
    
    public String toString() {
        return _url.toString() + " via " + _referer;
    }
}
