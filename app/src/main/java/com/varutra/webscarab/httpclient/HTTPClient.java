package com.varutra.webscarab.httpclient;

import com.varutra.webscarab.model.Request;
import com.varutra.webscarab.model.Response;

import java.io.IOException;

public interface HTTPClient {
    
    Response fetchResponse(Request request) throws IOException;
    
}
