package com.varutra.webscarab.model;

import java.util.EventListener;

public interface UrlListener extends EventListener {

    void urlAdded(UrlEvent evt);
    
    void urlChanged(UrlEvent evt);
    
    void urlRemoved(UrlEvent evt);
    
    void urlsChanged();
    
}
