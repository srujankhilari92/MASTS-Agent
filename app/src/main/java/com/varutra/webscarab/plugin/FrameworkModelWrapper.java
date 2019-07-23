package com.varutra.webscarab.plugin;

import java.util.Date;

import com.varutra.webscarab.model.ConversationID;
import com.varutra.webscarab.model.Cookie;
import com.varutra.webscarab.model.FrameworkModel;
import com.varutra.webscarab.model.HttpUrl;
import com.varutra.webscarab.model.Request;
import com.varutra.webscarab.model.Response;

public class FrameworkModelWrapper {

    private FrameworkModel _frameworkModel;
    
    public FrameworkModelWrapper(FrameworkModel frameworkModel) {
        this._frameworkModel = frameworkModel;
    }
    
    public String getConversationOrigin(ConversationID id) {
        return _frameworkModel.getConversationOrigin(id);
    }
    
    public Date getConversationDate(ConversationID id) {
        return _frameworkModel.getConversationDate(id);
    }
    
    
    public HttpUrl getRequestUrl(ConversationID conversation) {
        return _frameworkModel.getRequestUrl(conversation);
    }
    
    public void setConversationProperty(ConversationID conversation, String property, String value) {
        _frameworkModel.setConversationProperty(conversation, property, value);
    }
    
    public boolean addConversationProperty(ConversationID conversation, String property, String value) {
        return _frameworkModel.addConversationProperty(conversation, property, value);
    }
    
    public String getConversationProperty(ConversationID conversation, String property) {
        return getConversationProperty(conversation, property);
    }
    
    public String getRequestMethod(ConversationID id) {
        return _frameworkModel.getRequestMethod(id);
    }
    
    public String getResponseStatus(ConversationID id) {
        return _frameworkModel.getResponseStatus(id);
    }
    
    public String[] getConversationProperties(ConversationID conversation, String property) {
        return _frameworkModel.getConversationProperties(conversation, property);
    }
    
    public void setUrlProperty(HttpUrl url, String property, String value) {
        _frameworkModel.setUrlProperty(url, property, value);
    }
    
    public boolean addUrlProperty(HttpUrl url, String property, String value) {
        return _frameworkModel.addUrlProperty(url, property, value);
    }
    
    public String[] getUrlProperties(HttpUrl url, String property) {
        return _frameworkModel.getUrlProperties(url, property);
    }
    
    public String getUrlProperty(HttpUrl url, String property) {
        return _frameworkModel.getUrlProperty(url, property);
    }
    
    public Request getRequest(ConversationID conversation) {
        return _frameworkModel.getRequest(conversation);
    }
    
    public Response getResponse(ConversationID conversation) {
        return _frameworkModel.getResponse(conversation);
    }
    
    public int getCookieCount() {
        return _frameworkModel.getCookieCount();
    }
    
    public int getCookieCount(String key) {
        return _frameworkModel.getCookieCount(key);
    }
    
    public String getCookieAt(int index) {
        return _frameworkModel.getCookieAt(index);
    }
    
    public Cookie getCookieAt(String key, int index) {
        return _frameworkModel.getCookieAt(key, index);
    }
    
    public int getIndexOfCookie(Cookie cookie) {
        return _frameworkModel.getIndexOfCookie(cookie);
    }
    
    public int getIndexOfCookie(String key, Cookie cookie) {
        return _frameworkModel.getIndexOfCookie(key, cookie);
    }
    
    public Cookie getCurrentCookie(String key) {
        return _frameworkModel.getCurrentCookie(key);
    }
    
    public void addCookie(Cookie cookie) {
        _frameworkModel.addCookie(cookie);
    }
    
    public void removeCookie(Cookie cookie) {
        _frameworkModel.removeCookie(cookie);
    }
    
    public Cookie[] getCookiesForUrl(HttpUrl url) {
        return _frameworkModel.getCookiesForUrl(url);
    }

}
