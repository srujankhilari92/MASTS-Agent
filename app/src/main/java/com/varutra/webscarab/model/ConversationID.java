package com.varutra.webscarab.model;

public class ConversationID implements Comparable {
    
    private static Object _lock = new Object();
    private static int _next = 1;
    
    private int _id;
    
    public ConversationID() {
        synchronized(_lock) {
            _id = _next++;
        }
    }
    
    public ConversationID(int id) {
        synchronized (_lock) {
            _id = id;
            if (_id >= _next) {
                _next = _id + 1;
            } else if (_id <= 0) {
                throw new IllegalArgumentException("Cannot use a negative ConversationID");
            } 
        }        
    }
    
    public ConversationID(String id) {
        this(Integer.parseInt(id.trim()));
    }
    
    public static void reset() {
        synchronized(_lock) {
            _next = 1;
        }
    }
    
    protected int getID() {
        return _id;
    }
    
    public String toString() {
        return Integer.toString(_id);
    }
    
    public boolean equals(Object o) {
        if (o == null || ! (o instanceof ConversationID)) return false;
        return _id == ((ConversationID)o).getID();
    }
    
    public int hashCode() {
        return _id;
    }
    
    public int compareTo(Object o) {
        if (o instanceof ConversationID) {
            int thatid = ((ConversationID)o).getID();
            return _id - thatid;
        }
        return 1;
    }
    
}