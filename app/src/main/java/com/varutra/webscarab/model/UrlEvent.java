package com.varutra.webscarab.model;

import java.util.EventObject;

public class UrlEvent extends EventObject {
    
	private static final long serialVersionUID = -2563329935372684632L;
	private HttpUrl _url;
    private int _position;
    
    public UrlEvent(Object source, HttpUrl url, int position) {
        super(source);
        _url = url;
        _position = position;
    }
    
    public HttpUrl getUrl() {
        return _url;
    }
    
    public int getPosition() {
        return _position;
    }
}