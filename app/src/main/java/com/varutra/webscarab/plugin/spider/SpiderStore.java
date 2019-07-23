package com.varutra.webscarab.plugin.spider;

import com.varutra.webscarab.model.StoreException;
public interface SpiderStore {
    
    void writeUnseenLinks(Link[] links) throws StoreException;
    
    Link[] readUnseenLinks() throws StoreException;
    
    void writeSeenLinks(String[] links) throws StoreException;
    
    String[] readSeenLinks() throws StoreException;
    
}
