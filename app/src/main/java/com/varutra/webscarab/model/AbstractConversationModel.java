package com.varutra.webscarab.model;

import EDU.oswego.cs.dl.util.concurrent.Sync;

import java.util.logging.Logger;
import java.util.Date;

import com.varutra.webscarab.util.EventListenerList;

public abstract class AbstractConversationModel implements ConversationModel {
    
    private FrameworkModel _model;
    
    private EventListenerList _listenerList = new EventListenerList();
    
    private Logger _logger = Logger.getLogger(getClass().getName());
    
    public AbstractConversationModel(FrameworkModel model) {
        _model = model;
    }
    
    public abstract int getConversationCount();
    
    public abstract ConversationID getConversationAt(int index);
    
    public abstract int getIndexOfConversation(ConversationID id);
    
    
    public String getConversationOrigin(ConversationID id) {
        return _model.getConversationOrigin(id);
    }
    
    public Date getConversationDate(ConversationID id) {
        return _model.getConversationDate(id);
    }
    
    public String getRequestMethod(ConversationID id) {
        return _model.getRequestMethod(id);
    }
    
    public String getConversationProperty(ConversationID id, String property) {
        return _model.getConversationProperty(id, property);
    }
    
    public void setConversationProperty(ConversationID id, String property, String value) {
        _model.setConversationProperty(id, property, value);
    }
    
    public String getResponseStatus(ConversationID id) {
        return _model.getResponseStatus(id);
    }
    
    public HttpUrl getRequestUrl(ConversationID id) {
        return _model.getRequestUrl(id);
    }
    
    public Request getRequest(ConversationID id) {
        return _model.getRequest(id);
    }
    
    public Response getResponse(ConversationID id) {
        return _model.getResponse(id);
    }
    
    public void removeConversationListener(ConversationListener listener) {
        synchronized(_listenerList) {
            _listenerList.remove(ConversationListener.class, listener);
        }
    }
    
    public void addConversationListener(ConversationListener listener) {
        synchronized(_listenerList) {
            _listenerList.add(ConversationListener.class, listener);
        }
    }
    
    protected void fireConversationAdded(ConversationID id, int position) {
        Object[] listeners = _listenerList.getListenerList();
        ConversationEvent evt = new ConversationEvent(this, id, position);
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ConversationListener.class) {
                try {
                    ((ConversationListener)listeners[i+1]).conversationAdded(evt);
                } catch (Exception e) {
                    _logger.severe("Unhandled exception: " + e);
                }
            }
        }
    }
    
    protected void fireConversationRemoved(ConversationID id, int position) {
        Object[] listeners = _listenerList.getListenerList();
        ConversationEvent evt = new ConversationEvent(this, id, position);
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]== ConversationListener.class) {
                try {
                    ((ConversationListener)listeners[i+1]).conversationRemoved(evt);
                } catch (Exception e) {
                    _logger.severe("Unhandled exception: " + e);
                }
            }
        }
    }
    
    protected void fireConversationChanged(ConversationID id, int position) {
        Object[] listeners = _listenerList.getListenerList();
        ConversationEvent evt = new ConversationEvent(this, id, position);
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ConversationListener.class) {
                try {
                    ((ConversationListener)listeners[i+1]).conversationChanged(evt);
                } catch (Exception e) {
                    _logger.severe("Unhandled exception: " + e);
                }
            }
        }
    }
    
    protected void fireConversationsChanged() {
        Object[] listeners = _listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ConversationListener.class) {
                try {
                    ((ConversationListener)listeners[i+1]).conversationsChanged();
                } catch (Exception e) {
                    _logger.severe("Unhandled exception: " + e);
                }
            }
        }
    }
    
}