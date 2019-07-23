package com.varutra.webscarab.plugin.proxy;

import com.varutra.webscarab.model.Request;
import com.varutra.webscarab.model.Response;

public interface ManualEditUI {
    
    Request editRequest(Request request);
    
    Response editResponse(Request request, Response response);
    
}
