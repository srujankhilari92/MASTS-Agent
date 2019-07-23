package com.varutra.webscarab.plugin.fragments;

import java.util.EventListener;

import com.varutra.webscarab.model.ConversationID;
import com.varutra.webscarab.model.HttpUrl;

public interface FragmentListener extends EventListener {
    
    void fragmentAdded(HttpUrl url, ConversationID id, String type, String key);
    
    void fragmentAdded(String type, String key, int position);
    
    void fragmentsChanged();
    
}
