package com.varutra.webscarab.plugin.proxy;

import java.net.Socket;
import java.net.InetAddress;
import java.io.IOException;

import com.varutra.webscarab.model.Request;
import com.varutra.webscarab.model.Response;

public class ScriptableConnection {
    
    private Socket _socket = null;
    
    private Request _request = null;
    private Response _response = null;
    
    public ScriptableConnection(Socket socket) {
        _socket = socket;
    }
    
    public InetAddress getAddress() {
        return _socket.getInetAddress();
    }
    
    public void closeConnection() {
        try {
            _socket.close();
        } catch (IOException ioe) {}
    }
    
    public void setRequest(Request request) {
        _request = request;
    }
    
    public Request getRequest() {
        return _request;
    }
    
    public void setResponse(Response response) {
        _response = response;
    }
    
    public Response getResponse() {
        return _response;
    }
    
}
