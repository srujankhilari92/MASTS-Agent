package com.varutra.webscarab.model;

import EDU.oswego.cs.dl.util.concurrent.Sync;

public interface UrlModel {

    int getChildCount(HttpUrl parent);
    
    HttpUrl getChildAt(HttpUrl parent, int index);
    
    int getIndexOf(HttpUrl url);
    
    void addUrlListener(UrlListener listener);
    
    void removeUrlListener(UrlListener listener);
    
}
