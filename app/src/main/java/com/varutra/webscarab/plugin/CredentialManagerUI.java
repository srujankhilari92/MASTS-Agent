package com.varutra.webscarab.plugin;

public interface CredentialManagerUI {
    
    void requestCredentials(String host, String[] challenges);
    
}
