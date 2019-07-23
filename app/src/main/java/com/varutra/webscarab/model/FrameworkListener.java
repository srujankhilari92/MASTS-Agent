package com.varutra.webscarab.model;

public interface FrameworkListener extends java.util.EventListener {
    
    void cookieAdded(FrameworkEvent evt);
    
    void cookieRemoved(FrameworkEvent evt);
    
    void cookiesChanged();
    
    void conversationPropertyChanged(FrameworkEvent evt);
    
    void urlPropertyChanged(FrameworkEvent evt);
    
}