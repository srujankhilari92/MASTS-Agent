package com.varutra.webscarab.model;

import java.util.Date;

public interface SiteModelStore {
    
    
    long createNewConversation(Date when, int type, ConnectionDescriptor connectionDescriptor);
    

    long updateGotRequestConversation(long  conversationId, Date when, Request request);
    
    long updateGotResponseConversation(long  conversationId, Date when, Request request, Response response);
    
    long updateFailedConversation(long  conversationId, Date when,  Request request, String reason); 
    
    int addConversation(ConversationID id, Date when, Request request, Response response);
    
    void setConversationProperty(ConversationID id, String property, String value);
    
    boolean addConversationProperty(ConversationID id, String property, String value);
    
    String[] getConversationProperties(ConversationID id, String property);
    
    int getIndexOfConversation(HttpUrl url, ConversationID id);
    
    int getConversationCount(HttpUrl url);
    
    ConversationID getConversationAt(HttpUrl url, int index);
    
    
    void addUrl(HttpUrl url);
    
    boolean isKnownUrl(HttpUrl url);
    
    void setUrlProperty(HttpUrl url, String property, String value);
    
    boolean addUrlProperty(HttpUrl url, String property, String value);
    
    String[] getUrlProperties(HttpUrl url, String property);
    
    public int getChildCount(HttpUrl url);
    
    public HttpUrl getChildAt(HttpUrl url, int index);
    
    public int getIndexOf(HttpUrl url);
    
    void setRequest(ConversationID id, Request request);
    
    Request getRequest(ConversationID id);
    
    void setResponse(ConversationID id, Response response);
    
    Response getResponse(ConversationID id);
    
    
    int getCookieCount();
    
    int getCookieCount(String key);
    
    String getCookieAt(int index);
    
    Cookie getCookieAt(String key, int index);
    
    Cookie getCurrentCookie(String key);
    
    int getIndexOfCookie(Cookie cookie);
    
    int getIndexOfCookie(String key, Cookie cookie);
    
    boolean addCookie(Cookie cookie);
    
    boolean removeCookie(Cookie cookie);
    
    void flush() throws StoreException;
}