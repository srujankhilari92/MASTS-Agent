package com.varutra.webscarab.model;

public class FrameworkListenerCollection {
    
    void add (Class<FrameworkListener> classTemplate, FrameworkListener listener){
    }
    
    void remove(Class<FrameworkListener> classTemplate, FrameworkListener listener){
    }
    
    FrameworkListener[] getListenerList()
    {
    	return new FrameworkListener[0];
    }
    
}
