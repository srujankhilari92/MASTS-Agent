package com.varutra.webscarab.plugin;

public class DigestCredential {
    
    private String _host;
    private String _realm;
    private String _username;
    private String _password;
    
    public DigestCredential(String host, String realm, String username, String password) {
        _host = host;
        _realm = realm;
        _username = username;
        _password = password;
    }
    
    public String getHost() {
        return _host;
    }
    
    public String getRealm() {
        return _realm;
    }
    
    public String getUsername() {
        return _username;
    }
    
    public String getPassword() {
        return _password;
    }
    
}
