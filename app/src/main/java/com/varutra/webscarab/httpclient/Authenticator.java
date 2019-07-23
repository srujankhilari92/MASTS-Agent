package com.varutra.webscarab.httpclient;

import com.varutra.webscarab.model.HttpUrl;
public interface Authenticator {

    String getCredentials(HttpUrl url, String[] challenges);
    
    String getProxyCredentials(String hostname, String[] challenges);
    
}
