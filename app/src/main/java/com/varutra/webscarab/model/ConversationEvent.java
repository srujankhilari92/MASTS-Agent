package com.varutra.webscarab.model;

import java.util.EventObject;

public class ConversationEvent extends EventObject {
    
	private static final long serialVersionUID = 5382638131336063659L;
	private ConversationID _id;
    private int _position;
    
    public ConversationEvent(Object source, ConversationID id, int position) {
        super(source);
        _id = id;
        _position = position;
    }
    
    public ConversationID getConversationID() {
        return _id;
    }
    
    public int getPosition() {
        return _position;
    }
    
}