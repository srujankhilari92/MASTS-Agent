package com.varutra.webscarab.plugin.manualrequest;



import com.varutra.webscarab.model.ConversationModel;
import com.varutra.webscarab.model.Cookie;
import com.varutra.webscarab.model.FrameworkModel;
import com.varutra.webscarab.model.HttpUrl;
import com.varutra.webscarab.plugin.AbstractPluginModel;

public class ManualRequestModel extends AbstractPluginModel {
    
    private FrameworkModel _model;
    
    public ManualRequestModel(FrameworkModel model) {
        _model = model;
    }
    
    public ConversationModel getConversationModel() {
        return _model.getConversationModel();
    }
    
    public Cookie[] getCookiesForUrl(HttpUrl url) {
        return _model.getCookiesForUrl(url);
    }
    
    public void addCookie(Cookie cookie) {
        _model.addCookie(cookie);
    }
    
}
