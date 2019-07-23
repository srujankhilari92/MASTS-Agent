
package com.varutra.webscarab.httpclient;

import java.io.IOException;

import com.varutra.webscarab.model.Request;
import com.varutra.webscarab.model.Response;

public interface ConversationHandler {
    
    void responseReceived(Response response);
    
    void requestError(Request request, IOException ioe);
    
}
