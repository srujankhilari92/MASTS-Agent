package com.varutra.webscarab.plugin.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import com.varutra.webscarab.model.HttpUrl;

public class ListenerSpec implements Comparable {

    private String _address;
    private int _port;
    private HttpUrl _base = null;
    private boolean _primaryProxy = false;
    private boolean _transparentProxy = false;
    private boolean _transparentProxySecure = false;
    private boolean _captureData = true;
    private boolean _useFakeCerts = false;
    private boolean _storeSslAsPcap = false;

    private InetSocketAddress _sockAddr = null;
    
    public ListenerSpec(String address, int port, HttpUrl base, boolean primaryProxy, boolean transparentProxy, boolean transparentProxySecure, 
                                    boolean captureData, boolean useFakeCerts, boolean storeSslAsPcap) {
        if (address == null) {
            address = "*";
        }
        _address = address;
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 0 and 65536");
        }
        _port = port;
        _base = base;
        _primaryProxy = primaryProxy;
        _transparentProxy = transparentProxy;
        _transparentProxySecure = transparentProxySecure;
        _captureData = captureData;
        _useFakeCerts = useFakeCerts;
        if (_captureData == false && storeSslAsPcap){
            _storeSslAsPcap = true;
        }
    }
    
    public String getAddress() {
        return _address;
    }
    
    public int getPort() {
        return _port;
    }
    
    public HttpUrl getBase() {
        return _base;
    }
    
    public boolean isPrimaryProxy() {
        return _primaryProxy;
    }
    
    public boolean isTransparentProxy() {
        return _transparentProxy;
    }
    
    public boolean isTransparentProxySecure() {
        return _transparentProxySecure;
    }
    
    public boolean mustCaptureData(){
        return _captureData;
    }
    
    public boolean useFakeCerts(){
        return _useFakeCerts;
    }
    public boolean storeSslAsPcap(){
        return _storeSslAsPcap;
    }
    
    public String getKey() {
        return _address + ":" + _port;
    }
    
    public int hashCode() {
        return toString().hashCode();
    }
    
    public String toString() {
        return _address + ":" + _port + (_base != null ? " => " + _base : "") + (_primaryProxy ? " Primary" : "");
    }
    
    public boolean equals(Object obj) {
        return toString().equals(obj.toString());
    }
    
    public InetSocketAddress getInetSocketAddress() {
        if (_sockAddr == null) {
            _sockAddr = new InetSocketAddress(_address, _port);
        }
        return _sockAddr;
    }
    
    public void verifyAvailable() throws IOException {
        InetSocketAddress sa = getInetSocketAddress();
        ServerSocket serversocket = new ServerSocket(sa.getPort(), 5, sa.getAddress());
        serversocket.close();
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o) {
        return toString().compareTo(o.toString());
    }
    
    
}
