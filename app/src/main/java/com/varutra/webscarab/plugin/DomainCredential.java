package com.varutra.webscarab.plugin;

public class DomainCredential {
    
    private String _host;
    private String _domain;
    private String _username;
    private String _password;
    
    public DomainCredential(String host, String domain, String username, String password) {
        _host = host;
        _domain = domain;
        _username = username;
        _password = password;
    }
    
    public String getHost() {
        return _host;
    }
    
    public String getDomain() {
        return _domain;
    }
    
    public String getUsername() {
        return _username;
    }
    
    public String getPassword() {
        return _password;
    }
    
}
