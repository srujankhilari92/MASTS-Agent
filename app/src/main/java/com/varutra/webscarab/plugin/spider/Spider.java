package com.varutra.webscarab.plugin.spider;


import org.htmlparser.Node;

import org.htmlparser.Tag;

import org.htmlparser.NodeFilter;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.filters.OrFilter;
import org.htmlparser.filters.HasAttributeFilter;

import com.varutra.webscarab.httpclient.ConversationHandler;
import com.varutra.webscarab.httpclient.FetcherQueue;
import com.varutra.webscarab.model.ConversationID;
import com.varutra.webscarab.model.Cookie;
import com.varutra.webscarab.model.HttpUrl;
import com.varutra.webscarab.model.NamedValue;
import com.varutra.webscarab.model.Request;
import com.varutra.webscarab.model.Response;
import com.varutra.webscarab.model.StoreException;
import com.varutra.webscarab.model.UrlModel;
import com.varutra.webscarab.parser.Parser;
import com.varutra.webscarab.plugin.Framework;
import com.varutra.webscarab.plugin.Hook;
import com.varutra.webscarab.plugin.Plugin;

import java.util.List;
import java.util.LinkedList;

import java.util.Date;

import java.util.logging.Logger;

import java.net.MalformedURLException;

import java.lang.Thread;

import java.io.IOException;

public class Spider implements Plugin, ConversationHandler {
    
    private SpiderModel _model = null;
    private Framework _framework = null;
    
    private FetcherQueue _fetcherQueue = null;
    private int _threads = 4;
    
    private Thread _runThread = null;
    
    private Logger _logger = Logger.getLogger(getClass().getName());
    
    public Spider(Framework framework) {
        _framework = framework;
        _model = new SpiderModel(_framework.getModel());
        _fetcherQueue = new FetcherQueue("Spider", this, 4, 0);
    }
    
    public SpiderModel getModel() {
        return _model;
    }
    
    public String getPluginName() {
        return new String("Spider");
    }
    
    public void run() {
        _model.setStatus("Started");
        _model.setStopping(false);
        _runThread = Thread.currentThread();
        
        _model.setRunning(true);
        while (!_model.isStopping()) {
            if (!queueRequests()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {}
            } else {
                Thread.yield();
            }
        }
        _fetcherQueue.clearRequestQueue();
        _model.setRunning(false);
        _runThread = null;
        _model.setStatus("Stopped");
    }
    
    private boolean queueRequests() {
        if (_model.getQueuedLinkCount() == 0) return false;
        if (_fetcherQueue.getRequestsQueued() > _threads) return false;
        while (_model.getQueuedLinkCount() > 0 && _fetcherQueue.getRequestsQueued() <= _threads) {
            Link link = _model.dequeueLink();
            if (link == null) {
                _logger.warning("Got a null link from the link queue");
                return false;
            }
            Request request = newGetRequest(link);
            if (_model.getCookieSync()) {
                Cookie[] cookies = _model.getCookiesForUrl(request.getURL());
                if (cookies.length>0) {
                    StringBuffer buff = new StringBuffer();
                    buff.append(cookies[0].getName()).append("=").append(cookies[0].getValue());
                    for (int i=1; i<cookies.length; i++) {
                        buff.append("; ").append(cookies[i].getName()).append("=").append(cookies[i].getValue());
                    }
                    request.setHeader("Cookie", buff.toString());
                }
            }
            _fetcherQueue.submit(request);
        }
        return true;
    }
    
    public void responseReceived(Response response) {
        Request request = response.getRequest();
        if (request == null) {
            _logger.warning("Got a null request from the response!");
            return;
        }
        if (response.getStatus().startsWith("401")) {
            _logger.info("Invalid credentials or authentication required for " + request.getURL());
            _model.setAuthRequired(request.getURL());
            return;
        }
        _framework.addConversation(request, response, "Spider");
        if (_model.getCookieSync()) {
            NamedValue[] headers = response.getHeaders();
            for (int i=0; i<headers.length; i++) {
                if (headers[i].getName().equalsIgnoreCase("Set-Cookie") || headers[i].getName().equalsIgnoreCase("Set-Cookie2")) {
                    Cookie cookie = new Cookie(new Date(), request.getURL(), headers[i].getValue());
                    _model.addCookie(cookie);
                }
            }
        }
    }
    
    public void requestError(Request request, IOException ioe) {
        _logger.info("Requested " + request.getURL() + " got IOException " + ioe.getMessage());
    }
    
    public boolean isBusy() {
        if (!_model.isRunning()) return false;
        return _model.getQueuedLinkCount()>0;
    }
    
    private boolean allowedURL(HttpUrl url) {
        if (isAllowedDomain(url) && !_model.isForbidden(url)) {
            return true;
        }
        return false;
    }
    
    private boolean isAllowedDomain(HttpUrl url) {
        String allowedDomains = _model.getAllowedDomains();
        try {
            return allowedDomains != null && !allowedDomains.equals("") && url.getHost().matches(allowedDomains);
        } catch (Exception e) {
            return false;
        }
    }
    
    public void requestLinksUnder(HttpUrl url) {
        List<Link> links = new LinkedList<Link>();
        queueLinksUnder(url, links, 50);
        while (links.size()>0) _model.queueLink((Link) links.remove(0));
    }
    
    private void queueLinksUnder(HttpUrl url, List<Link> links, int max) {
        String referer;
        if (_model.isUnseen(url)) {
        	if (! _model.isForbidden(url) && !url.toString().matches(_framework.getDropPattern())) {
                referer = _model.getReferer(url);
                links.add(new Link(url, referer));
            } else {
                _logger.warning("Skipping forbidden path " + url);
            }
        }
        if (links.size() == max) return;
        UrlModel urlModel = _model.getUrlModel();
        int count = urlModel.getChildCount(url);
        for (int i=0; i<count; i++) {
            HttpUrl child = urlModel.getChildAt(url, i);
            queueLinksUnder(child, links, max);
            if (links.size() == max) return;
        }
    }
    
    public void requestLinks(HttpUrl[] urls) {
        Link link;
        for (int i=0; i<urls.length; i++) {
            String referer = _model.getReferer(urls[i]);
            link = new Link(urls[i], referer);
            _model.queueLink(link);
        }
    }
    
    public void clearQueue() {
        _model.clearLinkQueue();
    }
    
    private Request newGetRequest(Link link) {
        HttpUrl url = link.getURL();
        String referer = link.getReferer();
        Request req = new Request();
        req.setMethod("GET");
        req.setURL(url);
        req.setVersion("HTTP/1.0"); // 1.1 or 1.0?
        if (referer != null) {
            req.setHeader("Referer", referer);
        }
        req.setHeader("Host", url.getHost() + ":" + url.getPort());
        if (req.getVersion().equals("HTTP/1.0")) 
            req.setHeader("Connection", "Keep-Alive");
        NamedValue[] headers = _model.getExtraHeaders();
        if (headers != null && headers.length > 0) {
            for (int i=0; i< headers.length; i++) {
                if (headers[i] != null)
                    req.addHeader(headers[i]);
            }
        }
        return req;
    }
    
    public void setExtraHeaders(NamedValue[] headers) {
        _model.setExtraHeaders(headers);
    }
    
    public NamedValue[] getExtraHeaders() {
        return _model.getExtraHeaders();
    }
    
    public void flush() throws StoreException {
        // we do not manage our own store
    }
    
    public boolean stop() {
        if (isBusy()) return false;
        _model.setStopping(true);
        try {
            _runThread.join(5000);
        } catch (InterruptedException ie) {
            _logger.severe("Interrupted stopping " + getPluginName());
        }
        return !_model.isRunning();
    }
    
    public String getStatus() {
        return _model.getStatus();
    }
    
    public void analyse(ConversationID id, Request request, Response response, String origin) {
        HttpUrl base = request.getURL();
        if (response.getStatus().equals("302")) {
            String location = response.getHeader("Location");
            if (location != null) {
                try {
                    HttpUrl url = new HttpUrl(base, location);
                    _model.addUnseenLink(url, base);
                } catch (MalformedURLException mue) {
                    _logger.warning("Badly formed Location header : " + location);
                }
            } else {
                _logger.warning("302 received, but no Location header!");
            }
            return;
        }
        Object parsed = Parser.parse(base, response);
        if (parsed != null && parsed instanceof NodeList) { // the parsed content is HTML
            NodeList nodelist = (NodeList) parsed;
            processHtml(base, nodelist);
        } 
    }
    
    private void processHtml(HttpUrl base, NodeList nodelist) {
        NodeFilter filter = new HasAttributeFilter("href");
        filter = new OrFilter(filter, new HasAttributeFilter("src"));
        filter = new OrFilter(filter, new HasAttributeFilter("onclick"));
        filter = new OrFilter(filter, new HasAttributeFilter("onblur"));
        try {
            NodeList links = nodelist.extractAllNodesThatMatch(filter);
            for (NodeIterator ni = links.elements(); ni.hasMoreNodes(); ) {
                Node node = ni.nextNode();
                if (node instanceof Tag) {
                    boolean got = false;
                    Tag tag = (Tag) node;
                    String src = tag.getAttribute("src");
                    if (src != null) {
                        processLink(base, src);
                        got = true;
                    }
                    String href = tag.getAttribute("href");
                    if (href != null) {
                        processLink(base, href);
                        got = true;
                    }
                    if (!got) {
                    }
                }
            }
        } catch (ParserException pe) {
            _logger.warning("ParserException : " + pe);
        }
    }
    
    private void processLink(HttpUrl base, String link) {
        if (link.startsWith("http://") || link.startsWith("https://")) {
            try {
                HttpUrl url = new HttpUrl(link);
                _model.addUnseenLink(url, base);
            } catch (MalformedURLException mue) {
                _logger.warning("Malformed link : " + link);
            }
        } else if (link.toLowerCase().startsWith("mailto:")) {
            // do nothing
        } else if (link.toLowerCase().startsWith("javascript:")) {
            processScript(base, link.substring(10));
        } else if (link.matches("^[a-zA-Z]+://.*")) {
            _logger.info("Encountered an unhandled url scheme " + link);
        } else {
            _logger.fine("Creating a new relative URL with " + base + " and " + link + " '");
            try {
                HttpUrl url = new HttpUrl(base, link);
                _model.addUnseenLink(url, base);
            } catch (MalformedURLException mue) {
                _logger.warning("Bad relative URL (" + base.toString() + ") : " + link);
            }
        }
    }
    
    private void processScript(HttpUrl base, String script) {
        if (script.startsWith("window.open(")) {
            _logger.info("Script opens a window : " + script);
        } else if (script.startsWith("location.href")) {
            _logger.info("Script sets location : " + script);
        }
    }
    
    public boolean isModified() {
        return false; 
    }
    
    public boolean isRunning() {
        return _model.isRunning();
    }
    
    public void setSession(String type, Object store, String session) throws StoreException {
    }
    
    public Object getScriptableObject() {
        return null;
    }
    
    public Hook[] getScriptingHooks() {
        return new Hook[0];
    }
    
}
