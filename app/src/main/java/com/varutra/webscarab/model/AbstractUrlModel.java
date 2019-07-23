package com.varutra.webscarab.model;

import EDU.oswego.cs.dl.util.concurrent.Sync;


import java.util.logging.Logger;

import com.varutra.webscarab.util.EventListenerList;

public abstract class AbstractUrlModel implements UrlModel {
    
    private EventListenerList _listenerList = new EventListenerList();
    
    private Logger _logger = Logger.getLogger(getClass().getName());
    
    public AbstractUrlModel() {
    }
    
    public abstract int getChildCount(HttpUrl parent);
    
    public abstract int getIndexOf(HttpUrl url);
    
    public abstract HttpUrl getChildAt(HttpUrl parent, int index);
    
    
    public void addUrlListener(UrlListener listener) {
        synchronized(_listenerList) {
            _listenerList.add(UrlListener.class, listener);
        }
    }
    
    public void removeUrlListener(UrlListener listener) {
        synchronized(_listenerList) {
            _listenerList.remove(UrlListener.class, listener);
        }
    }
    
    protected void fireUrlAdded(HttpUrl url, int position) {
        Object[] listeners = _listenerList.getListenerList();
        UrlEvent evt = new UrlEvent(this, url, position);
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==UrlListener.class) {
                try {
                    ((UrlListener)listeners[i+1]).urlAdded(evt);
                } catch (Exception e) {
                    _logger.severe("Unhandled exception: " + e);
                    e.printStackTrace();
                }
            }
        }
    }
    
    protected void fireUrlRemoved(HttpUrl url, int position) {
        Object[] listeners = _listenerList.getListenerList();
        UrlEvent evt = new UrlEvent(this, url, position);
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==UrlListener.class) {
                try {
                    ((UrlListener)listeners[i+1]).urlRemoved(evt);
                } catch (Exception e) {
                    _logger.severe("Unhandled exception: " + e);
                }
            }
        }
    }
    
    protected void fireUrlChanged(HttpUrl url, int position) {
        Object[] listeners = _listenerList.getListenerList();
        UrlEvent evt = new UrlEvent(this, url, position);
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==UrlListener.class) {
                try {
                    ((UrlListener)listeners[i+1]).urlChanged(evt);
                } catch (Exception e) {
                    _logger.severe("Unhandled exception: " + e);
                    e.printStackTrace();
                }
            }
        }
    }
    
    protected void fireUrlsChanged() {
        Object[] listeners = _listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==UrlListener.class) {
                try {
                    ((UrlListener)listeners[i+1]).urlsChanged();
                } catch (Exception e) {
                    _logger.severe("Unhandled exception: " + e);
                    e.printStackTrace();
                }
            }
        }
    }
    
}