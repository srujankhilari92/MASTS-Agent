package com.varutra.webscarab.model;

import EDU.oswego.cs.dl.util.concurrent.Sync;
import java.util.Date;

public interface ConversationModel {
    
    int getConversationCount();
    
    ConversationID getConversationAt(int index);
    
    int getIndexOfConversation(ConversationID id);
    
    String getConversationOrigin(ConversationID id);
    
    String getConversationProperty(ConversationID id, String property);
    
    void setConversationProperty(ConversationID id, String property, String value);
    
    Date getConversationDate(ConversationID id);
    
    String getRequestMethod(ConversationID id);
    
    HttpUrl getRequestUrl(ConversationID id);
    
    String getResponseStatus(ConversationID id);
    
    Request getRequest(ConversationID id);
    
    Response getResponse(ConversationID id);
    
    void addConversationListener(ConversationListener listener);
    
    void removeConversationListener(ConversationListener listener);
    
}