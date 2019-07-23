package com.varutra.webscarab.model;

import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import EDU.oswego.cs.dl.util.concurrent.Sync;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.varutra.webscarab.util.EventListenerList;
import com.varutra.webscarab.util.MRUCache;

public abstract class FilteredUrlModel extends AbstractUrlModel {
    
    protected UrlModel _urlModel;
    private Set _filteredUrls = null;
    private Set _implicitUrls = null;
    
    private MRUCache _cache = new MRUCache(16);
    
    protected EventListenerList _listenerList = new EventListenerList();
    
    protected Logger _logger = Logger.getLogger(getClass().getName());
    
    private boolean _updating = false;
    
    private int hit, miss = 0;
    
    public FilteredUrlModel(UrlModel urlModel) {
        _logger.setLevel(Level.INFO);
        _urlModel = urlModel;
        try {
            updateFilteredUrls();
            _urlModel.addUrlListener(new Listener());
        } catch (Exception ie) {
            _logger.warning("Interrupted waiting for the read lock! " + ie.getMessage());
        } finally {
        }
    }
    
    
    protected void initFilters() {
        _filteredUrls = new HashSet();
        _implicitUrls = new HashSet();
    }
    
    protected abstract boolean shouldFilter(HttpUrl url);
    
    protected boolean isFiltered(HttpUrl url) {
        return _filteredUrls != null && _filteredUrls.contains(url);
    }
    
    protected void setFiltered(HttpUrl url, boolean filtered) {
        if (filtered) {
            _filteredUrls.add(url);
        } else {
            _filteredUrls.remove(url);
        }
    }
    
    public boolean isImplicit(HttpUrl url) {
        return _implicitUrls.contains(url);
    }
    
    protected void setImplicit(HttpUrl url, boolean filtered) {
        if (_implicitUrls == null) _implicitUrls = new HashSet();
        if (filtered) {
            _implicitUrls.add(url);
        } else {
            _implicitUrls.remove(url);
        }
    }
    
    private boolean isVisible(HttpUrl url) {
        return isImplicit(url) || ! isFiltered(url);
    }
    
    public int getIndexOf(HttpUrl url) {
        int index = Collections.binarySearch(getFilteredChildren(url), url);
        return index < 0 ? -1 : index;
    }
    
    public HttpUrl getChildAt(HttpUrl url, int index) {
        return (HttpUrl) getFilteredChildren(url).get(index);
    }
    
    private void updateFilteredUrls() {
        initFilters();
        recurseTree(null);
    }
    
    private ArrayList getFilteredChildren(HttpUrl parent) {
        ArrayList childList = (ArrayList) _cache.get(parent);
        if (childList != null) {
            hit++;
            return childList;
        }
        try {
            childList = new ArrayList();
            int count = _urlModel.getChildCount(parent);
            for (int i=0; i<count; i++) {
                HttpUrl child = _urlModel.getChildAt(parent, i);
                if (isVisible(child)) 
                    childList.add(child);
            }
            if (count > 0) {
                miss++;
                _logger.fine("Hit=" + hit + ", miss=" + miss + " parent = " + parent + " count="+count);
                _cache.put(parent, childList);
            }
            return childList;
        } catch (Exception ie) {
            _logger.warning("Interrupted waiting for the read lock! " + ie.getMessage());
        } finally {
        }
        return null;
    }
    
    public int getChildCount(HttpUrl url) {
        return getFilteredChildren(url).size();
    }
    
    private void recurseTree(HttpUrl parent) {
        int count = _urlModel.getChildCount(parent);
        for (int i=0; i<count; i++) {
            HttpUrl url = _urlModel.getChildAt(parent, i);
            if (shouldFilter(url)) {
                setFiltered(url, true);
            } else {
                grow(url);
            }
            recurseTree(url);
        }
    }
    
    private void grow(HttpUrl url) {
        HttpUrl[] path = url.getUrlHierarchy();
        for (int i=0; i<path.length-1; i++) {
            if (! isVisible(path[i])) {
                setImplicit(path[i], true);
                if (i==0) { 
                    _cache.remove(null);
                } else {
                    _cache.remove(path[i-1]);
                }
                if (!_updating)
                    fireUrlAdded(path[i], -1); //FIXME
            }
        }
        _cache.remove(url.getParentUrl());
        if (!_updating)
            fireUrlAdded(url, 0); // FIXME
    }
    
    private void prune(HttpUrl url) {
        _cache.remove(url.getParentUrl());
        if (!_updating)
            fireUrlRemoved(url, -1); // FIXME
        HttpUrl[] path = url.getUrlHierarchy();
        for (int i=path.length-2; i>=0; i--) {
            if (isImplicit(path[i]) && getChildCount(path[i])==0) {
                setImplicit(path[i], false);
                if (i==0) { 
                    _cache.remove(null);
                } else {
                    _cache.remove(path[i-1]);
                }
                if (!_updating)
                    fireUrlRemoved(path[i], -1); // FIXME
            }
        }
    }
    
    public void reset() {
        _cache.clear();
        _updating = true;
        updateFilteredUrls();
        _updating = false;
        fireUrlsChanged();
    }
    
    private class Listener implements UrlListener {
        
        public Listener() {
        }
        
        public void urlsChanged() {
            reset();
        }
        
        public void urlAdded(UrlEvent evt) {
            HttpUrl url = evt.getUrl();
            if (! shouldFilter(url)) {
                grow(url);
            } else {
                setFiltered(url, true);
            }
        }
        
        public void urlChanged(UrlEvent evt) {
            HttpUrl url = evt.getUrl();
            if (shouldFilter(url)) { 
                if (isVisible(url)) { 
                    if (getChildCount(url)>0) { 
                        setFiltered(url, true);
                        setImplicit(url, true);
                        if (!_updating)
                            fireUrlChanged(url, -1); // FIXME
                    } else { 
                        setFiltered(url, true);
                        prune(url);
                    }
                } 
            } else { 
                if (! isVisible(url)) { 
                    setFiltered(url, false);
                    grow(url);
                } else if (!_updating) {
                    fireUrlChanged(url, -1); // FIXME
                }
            }
        }
        
        public void urlRemoved(UrlEvent evt) {
            HttpUrl url = evt.getUrl();
            if (isVisible(url)) {
                prune(url);
            } else {
                setFiltered(url, false);
            }
        }
        
    }
    
}