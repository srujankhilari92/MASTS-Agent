package com.varutra.webscarab.model;

import java.util.EventListener;

public interface ConversationListener extends EventListener {
    
    void conversationAdded(ConversationEvent evt);
    
    void conversationChanged(ConversationEvent evt);
    
    void conversationRemoved(ConversationEvent evt);
    
    void conversationsChanged();
    
}
