package com.varutra.webscarab.model;

import EDU.oswego.cs.dl.util.concurrent.Sync;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;


import com.varutra.webscarab.util.EventListenerList;
import com.varutra.webscarab.util.MRUCache;
import com.varutra.webscarab.util.ReentrantReaderPreferenceReadWriteLock;

import java.io.File;

public class FrameworkModel {
    
    
    private static final Cookie[] NO_COOKIES = new Cookie[0];
    
    
    public static final int CONVERSATION_TYPE_PROXY = 0;
    public static final int CONVERSATION_TYPE_MANUAL = 1;
    public static final int CONVERSATION_TYPE_INTERCEPT = 2;
    
    public static final int CONVERSATION_STATUS_NEW = 0;
    public static final int CONVERSATION_STATUS_REQ_SEND = 1;
    public static final int CONVERSATION_STATUS_RESP_RECEIVED = 2;
    public static final int CONVERSATION_STATUS_ABORTED = 3;

    
    
    private EventListenerList _listenerList = new EventListenerList();
    
    private Map<ConversationID, HttpUrl> _urlCache = new MRUCache<ConversationID, HttpUrl>(200);
    
    private SiteModelStore _store = null;
    
    private FrameworkUrlModel _urlModel;
    private FrameworkConversationModel _conversationModel;
    
    private boolean _modified = false;
    
    private Logger _logger = Logger.getLogger(getClass().getName());
    
    public FrameworkModel() {
        _logger.setLevel(Level.FINEST);
        _conversationModel = new FrameworkConversationModel(this);
        _urlModel = new FrameworkUrlModel();
    }
    
    public void setSession(String type, Object store, String session) throws StoreException {
        try {
            if (type.equals("FileSystem") && store instanceof File) {
                try {
                    _store = new FileSystemStore((File) store);
                } catch (Exception e) {
                    throw new StoreException("Error initialising session : " + e.getMessage());
                }
            }else if (type.equals("Database")){
                _store = (SiteModelStore)store;
            } else {
                throw new StoreException("Unknown store type " + type + " and store " + store);
            }
            _urlModel.fireUrlsChanged();
            _conversationModel.fireConversationsChanged();
            fireCookiesChanged();
        } catch (Exception ie) {
            _logger.severe("Interrupted! " + ie);
        }
        
    }
    
    public boolean haveValidStore(){
        return _store != null;
    }
    
    
    public UrlModel getUrlModel() {
        return _urlModel;
    }
    
    public ConversationModel getConversationModel() {
        return _conversationModel;
    }
    
    public void flush() throws StoreException {
        if (_modified) {
            try {
                try {
                    _store.flush();
                    _modified = false;
                } finally {
                }
            } catch (Exception ie) {
                _logger.severe("Error flushing store " + ie);
            }
        }
    }
    
    public boolean isModified() {
        return _modified;
    }
    
    public ConversationID reserveConversationID() {
        return new ConversationID();
    }
    
    public long createNewConversation(Date when, int type, ConnectionDescriptor connectionDescriptor){
        return _store.createNewConversation(when, type, connectionDescriptor);
    }
    
    public long updateGotRequestConversation(long conversationId, Date when, Request request){
        return _store.updateGotRequestConversation(conversationId, when, request);
    }
    
    public long updateGotResponseConversation(long conversationId, Date when, Request request, Response response){
        return _store.updateGotResponseConversation(conversationId, when, request, response);
    }
    
    public long updateFailedConversation(long conversationId, Date when,  Request request, String reason){
        return _store.updateFailedConversation(conversationId, when, request, reason);
    }
    
    public void addConversation(ConversationID id, Date when, Request request, Response response, String origin) {
        try {
            if (_store != null){
                HttpUrl url = request.getURL();
                addUrl(url); // fires appropriate events
                int index = _store.addConversation(id, when, request, response);
                _store.setConversationProperty(id, "METHOD", request.getMethod());
                _store.setConversationProperty(id, "URL", request.getURL().toString());
                _store.setConversationProperty(id, "STATUS", response.getStatusLine());
                _store.setConversationProperty(id, "WHEN", Long.toString(when.getTime()));
                _store.setConversationProperty(id, "ORIGIN", origin);
                if (response.getContentSize() > 0)
                    _store.setConversationProperty(id, "RESPONSE_SIZE", Integer.toString(response.getContentSize()));
                _conversationModel.fireConversationAdded(id, index); // FIXME
                addUrlProperty(url, "METHODS", request.getMethod());
                addUrlProperty(url, "STATUS", response.getStatusLine());
            }
        } catch (Exception ie) {
            _logger.severe("Exception adding conversation " + ie);
        }
        _modified = true;
    }
    
    public String getConversationOrigin(ConversationID id) {
        return getConversationProperty(id, "ORIGIN");
    }
    
    public Date getConversationDate(ConversationID id) {
        try {
            try {
                String when = getConversationProperty(id, "WHEN");
                if (when == null) return null;
                try {
                    long time = Long.parseLong(when);
                    return new Date(time);
                } catch (NumberFormatException nfe) {
                    System.err.println("NumberFormatException parsing date for Conversation " + id + ": " + nfe);
                    return null;
                }
            } finally {
            }
        } catch (Exception ie) {
            _logger.severe("Interrupted! " + ie);
            return null;
        }
    }
    
    
    public HttpUrl getRequestUrl(ConversationID conversation) {
        try {
            try {
                if (_urlCache.containsKey(conversation))
                    return (HttpUrl) _urlCache.get(conversation);
                
                String url = getConversationProperty(conversation, "URL");
                try {
                    HttpUrl httpUrl = new HttpUrl(url);
                    _urlCache.put(conversation, httpUrl);
                    return httpUrl;
                } catch (MalformedURLException mue) {
                    System.err.println("Malformed URL for Conversation " + conversation + ": " + mue);
                    return null;
                }
            } finally {
            }
        } catch (Exception ie) {
            _logger.severe("Interrupted! " + ie);
            return null;
        }
    }
    
    public void setConversationProperty(ConversationID conversation, String property, String value) {
        try {
            _store.setConversationProperty(conversation, property, value);
            _conversationModel.fireConversationChanged(conversation, 0); // FIXME
            fireConversationPropertyChanged(conversation, property);
        } catch (Exception ie) {
            _logger.severe("Interrupted! " + ie);
        }
        _modified = true;
    }
    
    public boolean addConversationProperty(ConversationID conversation, String property, String value) {
        boolean change = false;
        try {
            change = _store.addConversationProperty(conversation, property, value);
            if (change) {
                _conversationModel.fireConversationChanged(conversation, 0); // FIXME
                fireConversationPropertyChanged(conversation, property);
            }
        } catch (Exception ie) {
            _logger.severe("Interrupted! " + ie);
        }
        _modified = _modified || change;
        return change;
    }
    
    public String getConversationProperty(ConversationID conversation, String property) {
        String[] values = getConversationProperties(conversation, property);
        if (values == null || values.length == 0) return null;
        if (values.length == 1) return values[0];
        StringBuffer value = new StringBuffer(values[0]);
        for (int i=1; i<values.length; i++) value.append(", ").append(values[i]);
        return value.toString();
    }
    
    public String getRequestMethod(ConversationID id) {
        return getConversationProperty(id, "METHOD");
    }
    
    public String getResponseStatus(ConversationID id) {
        return getConversationProperty(id, "STATUS");
    }
    
    public String[] getConversationProperties(ConversationID conversation, String property) {
        try {
            try {
                return _store.getConversationProperties(conversation, property);
            } finally {
            }
        } catch (Exception ie) {
            _logger.severe("Interrupted! " + ie);
            return null;
        }
    }
    
    private void addUrl(HttpUrl url) {
        try {
            try {
                if (_store != null && !_store.isKnownUrl(url)) {
                    HttpUrl[] path = url.getUrlHierarchy();
                    for (int i=0; i<path.length; i++) {
                        if (!_store.isKnownUrl(path[i])) {
                            if (!_store.isKnownUrl(path[i])) {
                                _store.addUrl(path[i]);
                                _urlModel.fireUrlAdded(path[i], 0); // FIXME
                                _modified = true;
                            } else { 
                            }
                        }
                    }
                }
            } finally {
            }
        } catch (Exception ie) {
            _logger.severe("Interrupted! " + ie);
        }
    }
    
    public void setUrlProperty(HttpUrl url, String property, String value) {
        addUrl(url);
        try {
            _store.setUrlProperty(url, property, value);
            _urlModel.fireUrlChanged(url, 0); // FIXME
            fireUrlPropertyChanged(url, property);
        } catch (Exception ie) {
            _logger.severe("Interrupted! " + ie);
        }
        _modified = true;
    }
    
    public boolean addUrlProperty(HttpUrl url, String property, String value) {
        boolean change = false;
        addUrl(url);
        try {
            change = _store.addUrlProperty(url, property, value);
            if (change) {
                _urlModel.fireUrlChanged(url, 0);
                fireUrlPropertyChanged(url, property);
            }
        } catch (Exception ie) {
            _logger.severe("Interrupted! " + ie);
        }
        _modified = _modified || change;
        return change;
    }
    
    public String[] getUrlProperties(HttpUrl url, String property) {
        try {
           try {
                return _store.getUrlProperties(url, property);
            } finally {
            }
        } catch (Exception ie) {
            _logger.severe("Interrupted! " + ie);
            return null;
        }
    }
    
    public String getUrlProperty(HttpUrl url, String property) {
        String[] values = getUrlProperties(url, property);
        if (values == null || values.length == 0) return null;
        if (values.length == 1) return values[0];
        StringBuffer value = new StringBuffer(30);
        value.append(values[0]);
        for(int i=1; i< values.length; i++) value.append(", ").append(values[i]);
        return value.toString();
    }
    
    public Request getRequest(ConversationID conversation) {
        try {
            try {
                return _store.getRequest(conversation);
            } finally {
            }
        } catch (Exception ie) {
            _logger.severe("Interrupted! " + ie);
            return null;
        }
    }
    
    public Response getResponse(ConversationID conversation) {
        try {
            try {
                return _store.getResponse(conversation);
            } finally {
            }
        } catch (Exception ie) {
            _logger.severe("Interrupted! " + ie);
            return null;
        }
    }
    
    public void addModelListener(FrameworkListener listener) {
        synchronized(_listenerList) {
            _listenerList.add(FrameworkListener.class, listener);
        }
    }
    
    public void removeModelListener(FrameworkListener listener) {
        synchronized(_listenerList) {
            _listenerList.remove(FrameworkListener.class, listener);
        }
    }
    
    public int getCookieCount() {
        if (_store == null) return 0;
        try {
            try {
                return _store.getCookieCount();
            } finally {
            }
        } catch (Exception ie) {
            _logger.severe("Interrupted! " + ie);
            return 0;
        }
    }
    
    public int getCookieCount(String key) {
        try {
            try {
                return _store.getCookieCount(key);
            } finally {
            }
        } catch (Exception ie) {
            _logger.severe("Interrupted! " + ie);
            return 0;
        }
    }
    
    public String getCookieAt(int index) {
        try {
            try {
                return _store.getCookieAt(index);
            } finally {
            }
        } catch (Exception ie) {
            _logger.severe("Interrupted! " + ie);
            return null;
        }
    }
    
    public Cookie getCookieAt(String key, int index) {
        try {
            try {
                return _store.getCookieAt(key, index);
            } finally {
            }
        } catch (Exception ie) {
            _logger.severe("Interrupted! " + ie);
            return null;
        }
    }
    
    public int getIndexOfCookie(Cookie cookie) {
        try {
            try {
                return _store.getIndexOfCookie(cookie);
            } finally {
            }
        } catch (Exception ie) {
            _logger.severe("Interrupted! " + ie);
            return 0;
        }
    }
    
    public int getIndexOfCookie(String key, Cookie cookie) {
        try {
            try {
                return _store.getIndexOfCookie(key, cookie);
            } finally {
            }
        } catch (Exception ie) {
            _logger.severe("Interrupted! " + ie);
            return 0;
        }
    }
    
    public Cookie getCurrentCookie(String key) {
        try {
            try {
                int count = _store.getCookieCount(key);
                return _store.getCookieAt(key, count-1);
            } finally {
            }
        } catch (Exception ie) {
            _logger.severe("Interrupted! " + ie);
            return null;
        }
    }
    
    public void addCookie(Cookie cookie) {
        try {
            boolean added = _store.addCookie(cookie);
            if (! added) { 
            } else {
                _modified = true;
                fireCookieAdded(cookie);
            }
        } catch (Exception ie) {
            _logger.severe("Interrupted! " + ie);
        }
    }
    
    public void removeCookie(Cookie cookie) {
        try {
            boolean deleted = _store.removeCookie(cookie);
            if (deleted) {
                _modified = true;
                fireCookieRemoved(cookie);
            } else {
            }
        } catch (Exception ie) {
            _logger.severe("Interrupted! " + ie);
        }
    }
    
    public Cookie[] getCookiesForUrl(HttpUrl url) {
        try {
            try {
                List<Cookie> cookies = new ArrayList<Cookie>();
                
                String host = url.getHost();
                String path = url.getPath();
                
                int size = getCookieCount();
                for (int i=0; i<size; i++) {
                    String key = getCookieAt(i);
                    Cookie cookie = getCurrentCookie(key);
                    String domain = cookie.getDomain();
                    if (host.equals(domain) || (domain.startsWith(".") && host.endsWith(domain))) {
                        if (path.startsWith(cookie.getPath())) {
                            cookies.add(cookie);
                        }
                    }
                }
                return cookies.toArray(NO_COOKIES);
            } finally {
            }
        } catch (Exception ie) {
            _logger.severe("Interrupted! " + ie);
            return NO_COOKIES;
        }
    }
    
    protected void fireCookieAdded(Cookie cookie) {
        Object[] listeners = _listenerList.getListenerList();
        FrameworkEvent evt = new FrameworkEvent(this, cookie);
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==FrameworkListener.class) {
                try {
                    ((FrameworkListener)listeners[i+1]).cookieAdded(evt);
                } catch (Exception e) {
                    _logger.severe("Unhandled exception: " + e);
                }
            }
        }
    }
    
    protected void fireCookieRemoved(Cookie cookie) {
        Object[] listeners = _listenerList.getListenerList();
        FrameworkEvent evt = new FrameworkEvent(this, cookie);
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==FrameworkListener.class) {
                try {
                    ((FrameworkListener)listeners[i+1]).cookieRemoved(evt);
                } catch (Exception e) {
                    _logger.severe("Unhandled exception: " + e);
                }
            }
        }
    }
    
    protected void fireCookiesChanged() {
        Object[] listeners = _listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==FrameworkListener.class) {
                try {
                    ((FrameworkListener)listeners[i+1]).cookiesChanged();
                } catch (Exception e) {
                    _logger.severe("Unhandled exception: " + e);
                }
            }
        }
    }
    
    protected void fireConversationPropertyChanged(ConversationID id, String property) {
        Object[] listeners = _listenerList.getListenerList();
        FrameworkEvent evt = new FrameworkEvent(this, id, property);
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==FrameworkListener.class) {
                try {
                    ((FrameworkListener)listeners[i+1]).conversationPropertyChanged(evt);
                } catch (Exception e) {
                    _logger.severe("Unhandled exception: " + e);
                }
            }
        }
    }
    
    protected void fireUrlPropertyChanged(HttpUrl url, String property) {
        Object[] listeners = _listenerList.getListenerList();
        FrameworkEvent evt = new FrameworkEvent(this, url, property);
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==FrameworkListener.class) {
                try {
                    ((FrameworkListener)listeners[i+1]).urlPropertyChanged(evt);
                } catch (Exception e) {
                    _logger.severe("Unhandled exception: " + e);
                }
            }
        }
    }
    
    private class FrameworkUrlModel extends AbstractUrlModel {
        
        
        public int getChildCount(HttpUrl parent) {
            if (_store == null) return 0;
            try {
                try {
                    return _store.getChildCount(parent);
                } finally {
                }
            } catch (Exception ie) {
                _logger.severe("Interrupted! " + ie);
                return 0;
            }
        }
        
        public int getIndexOf(HttpUrl url) {
            try {
                try {
                    return _store.getIndexOf(url);
                } finally {
                }
            } catch (Exception ie) {
                _logger.severe("Interrupted! " + ie);
                return -1;
            }
        }
        
        public HttpUrl getChildAt(HttpUrl parent, int index) {
            try {
                try {
                    return _store.getChildAt(parent, index);
                } finally {
                }
            } catch (Exception ie) {
                _logger.severe("Interrupted! " + ie);
                return null;
            }
        }
        
    }
    
    private class FrameworkConversationModel extends AbstractConversationModel {
        
        public FrameworkConversationModel(FrameworkModel model) {
            super(model);
        }
        
        
        public ConversationID getConversationAt(int index) {
            try {
                try {
                    return _store.getConversationAt(null, index);
                } finally {
                }
            } catch (Exception ie) {
                _logger.severe("Interrupted! " + ie);
                return null;
            }
        }
        
        public int getConversationCount() {
            if (_store == null) return 0;
            try {
                try {
                    return _store.getConversationCount(null);
                } finally {
                }
            } catch (Exception ie) {
                _logger.severe("Interrupted! " + ie);
                return 0;
            }
        }
        
        public int getIndexOfConversation(ConversationID id) {
            try {
                try {
                    return _store.getIndexOfConversation(null, id);
                } finally {
                }
            } catch (Exception ie) {
                _logger.severe("Interrupted! " + ie);
                return 0;
            }
        }
    }
    
}