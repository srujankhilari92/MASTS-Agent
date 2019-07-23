package com.varutra.webscarab.plugin;

import com.varutra.webscarab.model.ConversationID;
import com.varutra.webscarab.model.Request;
import com.varutra.webscarab.model.Response;
import com.varutra.webscarab.model.StoreException;

public interface Plugin extends Runnable {
    
    String getPluginName();
    
    void setSession(String type, Object store, String session) throws StoreException;
    
    void run();
    
    boolean isRunning();
    
    boolean isBusy();
    
    String getStatus();
    
    boolean stop();
    
    boolean isModified();
    
    void flush() throws StoreException;
    
    void analyse(ConversationID id, Request request, Response response, String origin);
    
    Hook[] getScriptingHooks();
    
    Object getScriptableObject();
    
}
