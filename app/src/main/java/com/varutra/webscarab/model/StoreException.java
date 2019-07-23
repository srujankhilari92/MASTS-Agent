package com.varutra.webscarab.model;

public class StoreException extends java.lang.Exception {
    
	private static final long serialVersionUID = -3216060604426546272L;

    public StoreException() {
    }
    
    
    public StoreException(String msg) {
        super(msg);
    }
    
    public StoreException(String msg, Throwable cause) {
        super(msg, cause);
    }
    
}
