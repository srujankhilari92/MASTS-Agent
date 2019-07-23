package com.varutra.webscarab.plugin.manualrequest;


import com.varutra.webscarab.httpclient.HTTPClientFactory;
import com.varutra.webscarab.model.ConversationID;
import com.varutra.webscarab.model.Cookie;
import com.varutra.webscarab.model.FrameworkModel;
import com.varutra.webscarab.model.NamedValue;
import com.varutra.webscarab.model.Request;
import com.varutra.webscarab.model.Response;
import com.varutra.webscarab.model.StoreException;
import com.varutra.webscarab.plugin.Framework;
import com.varutra.webscarab.plugin.Hook;
import com.varutra.webscarab.plugin.Plugin;

import java.io.IOException;

import java.util.Date;

public class ManualRequest implements Plugin {
    
    private ManualRequestUI _ui = null;
    
    private Request _request = null;
    private Response _response = null;
    private Date _responseDate = null;
    
    private Framework _framework;
    private ManualRequestModel _model;
    
    public ManualRequest(Framework framework) {
        _framework = framework;
        _model = new ManualRequestModel(_framework.getModel());
    }
    
    public String getPluginName() {
        return new String("Manual Request");
    }
    
    public ManualRequestModel getModel() {
        return _model;
    }
    
    public void setUI(ManualRequestUI ui) {
        _ui = ui;
        if (_ui != null) _ui.setEnabled(_model.isRunning());
    }
    
    public void setRequest(Request request) {
        _request = request;
        if (_ui != null) {
            _ui.responseChanged(null);
            _ui.requestChanged(request);
        }
    }
    
    public synchronized void fetchResponse() throws IOException {
        if (_request != null) {
            try {
                _model.setBusy(true);
                _model.setStatus("Started, Fetching response");
                long conversationId = _framework.createConversation(_request, new Date(System.currentTimeMillis()), FrameworkModel.CONVERSATION_TYPE_MANUAL, null);
                _framework.gotRequest(conversationId, new Date(System.currentTimeMillis()), _request);
                _response = HTTPClientFactory.getValidInstance().fetchResponse(_request);
                if (_response != null) {
                    _responseDate = new Date();
                    _response.flushContentStream();
                    _framework.gotResponse(conversationId, new Date(System.currentTimeMillis()), _request, _response, false);
                    if (_ui != null) _ui.responseChanged(_response);
                }
            } finally {
                _model.setStatus("Started, Idle");
                _model.setBusy(false);
            }
        }
    }
    
    public void addRequestCookies() {
        if (_request != null) {
            Cookie[] cookies = _model.getCookiesForUrl(_request.getURL());
            if (cookies.length>0) {
                StringBuffer buff = new StringBuffer();
                buff.append(cookies[0].getName()).append("=").append(cookies[0].getValue());
                for (int i=1; i<cookies.length; i++) {
                    buff.append("; ").append(cookies[i].getName()).append("=").append(cookies[i].getValue());
                }
                _request.setHeader(new NamedValue("Cookie", buff.toString()));
                if (_ui != null) _ui.requestChanged(_request);
            }
        }
    }
    
    public void updateCookies() {
        if (_response != null) {
            NamedValue[] headers = _response.getHeaders();
            for (int i=0; i<headers.length; i++) {
                if (headers[i].getName().equalsIgnoreCase("Set-Cookie") || headers[i].getName().equalsIgnoreCase("Set-Cookie2")) {
                    Cookie cookie = new Cookie(_responseDate, _request.getURL(), headers[i].getValue());
                    _model.addCookie(cookie);
                }
            }
        }
    }
    
    public void run() {
        _model.setRunning(true);
        if (_ui != null) _ui.setEnabled(_model.isRunning());
        _model.setStatus("Started, Idle");
    }
    
    public boolean stop() {
        _model.setStopping(true);
        _model.setRunning(false);
        _model.setStopping(false);
        if (_ui != null) _ui.setEnabled(_model.isRunning());
        _model.setStatus("Stopped");
        return ! _model.isRunning();
    }
    
    public void flush() throws StoreException {
    }
    
    public boolean isRunning() {
        return _model.isRunning();
    }
    
    public boolean isBusy() {
        return _model.isBusy();
    }
    
    public String getStatus() {
        return _model.getStatus();
    }
    
    public boolean isModified() {
        return false;
    }
    
    public void analyse(ConversationID id, Request request, Response response, String origin) {
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
