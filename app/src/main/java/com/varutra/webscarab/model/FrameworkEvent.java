package com.varutra.webscarab.model;

import java.util.EventObject;

public class FrameworkEvent extends EventObject {
    
	private static final long serialVersionUID = 6301623751009629601L;
	private ConversationID _id = null;
    private HttpUrl _url = null;
    private Cookie _cookie = null;
    private String _property = null;
    
    public FrameworkEvent(Object source, ConversationID id, String property) {
        super(source);
        _id = id;
        _property = property;
    }
    
    public FrameworkEvent(Object source, HttpUrl url, String property) {
        super(source);
        _url = url;
        _property = property;
    }
    
    public FrameworkEvent(Object source, Cookie cookie) {
        super(source);
        _cookie = cookie;
    }
    
    public ConversationID getConversationID() {
        return _id;
    }
    
    public HttpUrl getUrl() {
        return _url;
    }
    
    public Cookie getCookie() {
        return _cookie;
    }
    
    public String getPropertyName() {
        return _property;
    }
    
}