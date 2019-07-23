package com.varutra.webscarab.plugin.manualrequest;



import com.varutra.webscarab.model.Request;
import com.varutra.webscarab.model.Response;
import com.varutra.webscarab.plugin.PluginUI;

public interface ManualRequestUI extends PluginUI {
    
    void requestChanged(Request request);
    
    void responseChanged(Response response);
    
}
