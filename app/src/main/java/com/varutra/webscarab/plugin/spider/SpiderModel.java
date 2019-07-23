package com.varutra.webscarab.plugin.spider;



import com.varutra.webscarab.model.Cookie;
import com.varutra.webscarab.model.FilteredUrlModel;
import com.varutra.webscarab.model.FrameworkModel;
import com.varutra.webscarab.model.HttpUrl;
import com.varutra.webscarab.model.NamedValue;
import com.varutra.webscarab.model.Preferences;
import com.varutra.webscarab.model.UrlModel;
import com.varutra.webscarab.plugin.AbstractPluginModel;

import java.util.List;
import java.util.LinkedList;
import java.util.logging.Logger;

public class SpiderModel extends AbstractPluginModel {
    
    private FrameworkModel _model;
    private SpiderUrlModel _urlModel;
    
    private List<Link> _linkQueue = new LinkedList<Link>();
    
    private String _allowedDomains = null;
    private String _forbiddenPaths = null;
    private boolean _recursive = false;
    private boolean _cookieSync = false;
    
    private NamedValue[] _extraHeaders = null;
    
    private Logger _logger = Logger.getLogger(getClass().getName());
    
    public SpiderModel(FrameworkModel model) {
        _model = model;
        _urlModel = new SpiderUrlModel(_model.getUrlModel());
        parseProperties();
    }
    
    public UrlModel getUrlModel() {
        return _urlModel;
    }
    
    public boolean isUnseen(HttpUrl url) {
        return _model.getUrlProperty(url, "METHODS") == null;
    }
    
    public boolean isForbidden(HttpUrl url) {
        if (_forbiddenPaths != null && !_forbiddenPaths.equals("")) {
            try {
                return url.toString().matches(getForbiddenPaths());
            } catch (Exception e) {
            }
        }
        return false;
    }
    
    public void addUnseenLink(HttpUrl url, HttpUrl referer) {
        if (url == null) {
            return;
        }
        if (isUnseen(url)) {
            String first = _model.getUrlProperty(url, "REFERER");
            if (first == null || first.equals("")) {
                _model.setUrlProperty(url, "REFERER", referer.toString());
            }
        }
    }
    
    public void queueLink(Link link) {
        try {
            _linkQueue.add(link);
        } catch (Exception ie) {
            _logger.warning("Interrupted waiting for the read lock! " + ie.getMessage());
        } finally {
        }
    }
    
    public Link dequeueLink() {
        Link link = null;
        try {
            if (_linkQueue.size() > 0) 
                link = _linkQueue.remove(0);
            if (_linkQueue.size() == 0) {
                setStatus("Idle");
            } else {
                setStatus(_linkQueue.size() + " links remaining");
            }
        } catch (Exception ie) {
            _logger.warning("Interrupted waiting for the read lock! " + ie.getMessage());
        } finally {
        }
        return link;
    }
    
    public void clearLinkQueue() {
        try {
            _linkQueue.clear();
        } catch (Exception ie) {
            _logger.warning("Interrupted waiting for the read lock! " + ie.getMessage());
        } finally {
            
        }
    }
    
    public int getQueuedLinkCount() {
        try {
            return _linkQueue.size();
        } catch (Exception ie) {
            _logger.warning("Interrupted waiting for the read lock! " + ie.getMessage());
        } finally {
             }
        return 0;
    }
    
    public Cookie[] getCookiesForUrl(HttpUrl url) {
        return _model.getCookiesForUrl(url);
    }
    
    public void addCookie(Cookie cookie) {
        _model.addCookie(cookie);
    }
    
    public void parseProperties() {
        String prop = "Spider.domains";
        String value = Preferences.getPreference(prop, ".*localhost.*");
        setAllowedDomains(value);
        
        prop = "Spider.excludePaths";
        value = Preferences.getPreference(prop, "");
        setForbiddenPaths(value);
        
        prop = "Spider.synchroniseCookies";
        value = Preferences.getPreference(prop, "true");
        setCookieSync(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
        
        prop = "Spider.recursive";
        value = Preferences.getPreference(prop, "false");
        setRecursive(value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes"));
    }
    
    public String getReferer(HttpUrl url) {
        return _model.getUrlProperty(url, "REFERER");
    }
    
    public void setExtraHeaders(NamedValue[] headers) {
        _extraHeaders = headers;
    }
    
    public NamedValue[] getExtraHeaders() {
        return _extraHeaders;
    }
    
    public void setRecursive(boolean bool) {
        _recursive = bool;
        String prop = "Spider.recursive";
        Preferences.setPreference(prop,Boolean.toString(bool));
    }
    
    public boolean getRecursive() {
        return _recursive;
    }
    
    public void setCookieSync(boolean enabled) {
        _cookieSync = enabled;
        String prop = "Spider.synchroniseCookies";
        Preferences.setPreference(prop,Boolean.toString(enabled));
    }
    
    public boolean getCookieSync() {
        return _cookieSync;
    }
    
    public void setAllowedDomains(String regex) {
        _allowedDomains = regex;
        String prop = "Spider.domains";
        Preferences.setPreference(prop,regex);
    }
    
    public String getAllowedDomains() {
        return _allowedDomains;
    }
    
    public void setForbiddenPaths(String regex) {
        _forbiddenPaths = regex;
        String prop = "Spider.excludePaths";
        Preferences.setPreference(prop,regex);
        _urlModel.reset();
    }
    
    public String getForbiddenPaths() {
        return _forbiddenPaths;
    }
    
    public void setAuthRequired(HttpUrl url) {
        _model.setUrlProperty(url, "AUTHREQUIRED", Boolean.toString(true));
    }
    
    private class SpiderUrlModel extends FilteredUrlModel {
        
        public SpiderUrlModel(UrlModel model) {
            super(model);
        }
        
        public boolean shouldFilter(HttpUrl url) {
            return ! isUnseen(url) || isForbidden(url);
        }
        
    }
    
}
