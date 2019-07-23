package com.varutra.webscarab.plugin;

import com.varutra.webscarab.model.ConversationID;
import com.varutra.webscarab.model.Request;
import com.varutra.webscarab.model.Response;

public class ScriptableConversation {
    
	private ConversationID _id;
    private Request _request;
    private Response _response;
    private String _origin;
    
    private boolean _cancelled = false;
    private boolean _analyse = true;
    
    /** Creates a new instance of ScriptableConversation */
    public ScriptableConversation(ConversationID id, Request request, Response response, String origin) {
    	_id = id;
        _request = request;
        _response = response;
        _origin = origin;
    }
    
    public ConversationID getId() {
    	return _id;
    }
    
    public Request getRequest() {
        return new Request(_request); 
    }
    
    public Response getResponse() {
        return new Response(_response);
    }
    
    public String getOrigin() {
        return _origin;
    }
    
    public void setCancelled(boolean cancelled) {
        _cancelled = cancelled;
    }
    
    public boolean isCancelled() {
        return _cancelled;
    }
    
    public void setAnalyse(boolean analyse) {
        _analyse = analyse;
    }
    
    public boolean shouldAnalyse() {
        return _analyse;
    }
    
}
