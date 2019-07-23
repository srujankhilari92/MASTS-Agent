package com.varutra.webscarab.model;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;

import java.text.ParseException;

import android.util.Log;


public class Request extends Message {
    
    private String _method = "GET";
    private HttpUrl _url = null;
    private String _version = "HTTP/1.0";
    private boolean _isTransparent = false;
    private boolean _isSecure = false;
    private ConnectionDescriptor _connectionDescriptor= null;
    
    
    public Request() {
    }
    
    public Request(boolean isTransparent, boolean isSecure, ConnectionDescriptor connectionDescriptor) {
        _isTransparent = isTransparent;
        _isSecure = isSecure;
        _connectionDescriptor = connectionDescriptor;
    }
    
    public Request(Request req) {
        _method = req.getMethod();
        _url = req.getURL();
        _version = req.getVersion();
        Log.e("Get Header : ", ""+req.getHeaders());
        setHeaders(req.getHeaders());
        setContent(req.getContent());
    }
    
    public void read(InputStream is) throws IOException {
        read(is, null);
    }
    
    public void read(InputStream is, HttpUrl base) throws IOException {
        String line = null;
        _logger.finer("Base: " + base);
        try {
            line = readLine(is);
            _logger.finest("Request: " + line);
        } catch (SocketTimeoutException ste) {
            return;
        }
        if (line == null || line.equals("")) {
            return;
        }
        if (_isTransparent){
            super.read(is);
            String hostFromHeader = super.getHeader("Host");
            String schema = !_isSecure ? "http://": "https://";
            base = new HttpUrl(schema + hostFromHeader);
        }
        
        String[] parts = line.split(" ");
        if (parts.length == 2 || parts.length == 3) {
            setMethod(parts[0]);
            if (getMethod().equalsIgnoreCase("CONNECT")) {
                String schema = "https";
                setURL(new HttpUrl(schema +"://" + parts[1]));
            } else {
                setURL(new HttpUrl(base, parts[1]));
            }
        } else {
            throw new IOException("Invalid request line reading from the InputStream '"+line+"'");
        }
        if (parts.length == 3) {
            setVersion(parts[2]);
        } else {
            setVersion("HTTP/0.9");
        }
        if (!_isTransparent){
            super.read(is);
        }
        if (_method.equals("CONNECT") || _method.equals("GET") || _method.equals("HEAD") || _method.equals("TRACE")) {
            setNoBody();
        }
    }
    
    public void parse(String string) throws ParseException {
        parse(new StringBuffer(string));
    }
    
    public void parse(StringBuffer buff) throws ParseException {
    	
    	Log.e("In Parse  String Buffer Value : ", ""+buff);
    	
        String line = null;
        line = getLine(buff);
        String[] parts = line.split(" ");
        if (parts.length == 2 || parts.length == 3) {
            setMethod(parts[0]);
            try {
                if (getMethod().equalsIgnoreCase("CONNECT")) {
                    setURL(new HttpUrl("https://" + parts[1] + "/"));
                } else {
                    setURL(new HttpUrl(parts[1]));
                }
            } catch (MalformedURLException mue) {
                throw new ParseException("Malformed URL '" + parts[1] + "' : " + mue, parts[0].length()+1);
            }
        } else {
            throw new ParseException("Invalid request line '" + line + "'", 0);
        }
        if (parts.length == 3) {
            setVersion(parts[2]);
        } else {
            setVersion("HTTP/0.9");
        }
        super.parse(buff);
        if (_method.equals("CONNECT") || _method.equals("GET") || _method.equals("HEAD") || _method.equals("TRACE")) {
            setNoBody();
        }
    }
    
    public void write(OutputStream os) throws IOException {
        write(os,"\r\n");
    }
    
    public void write(OutputStream os, String crlf) throws IOException {
        if (_method == null || _url == null || _version == null) {
            System.err.println("Uninitialised Request!");
            return;
        }
        os = new BufferedOutputStream(os);
        String requestLine = _method+" "+_url+" " + _version + crlf;
        os.write(requestLine.getBytes());
        _logger.finer("Request: " + requestLine);
        super.write(os, crlf);
        os.flush();
    }
    
    public void writeDirect(OutputStream os) throws IOException {
        writeDirect(os, "\r\n");
    }
    
    public void writeDirect(OutputStream os, String crlf) throws IOException {
        if (_method == null || _url == null || _version == null) {
            System.err.println("Uninitialised Request!");
            return;
        }
        os = new BufferedOutputStream(os);
        String requestLine = _method + " " + _url.direct() + " " + _version;
        os.write((requestLine+crlf).getBytes());
        _logger.finer("Request: " + requestLine);
        super.write(os, crlf);
        os.flush();
    }
    
    public void setMethod(String method) {
        _method = method.toUpperCase();
    }
    
    public String getMethod() {
        return _method;
    }
    
    public void setURL(HttpUrl url) {
        _url = url;
    }
    
    public HttpUrl getURL() {
        return _url;
    }
    
    public boolean parameterSearch(String search)
    {
    	String s = getURL().getQuery();
    	if(s == null) return false;
    	return s.toLowerCase().indexOf(search) > -1;
    }
    public void setVersion(String version) {
        _version = version.toUpperCase();
    }
    
    public String getVersion() {
        return _version;
    }
    
    public ConnectionDescriptor getConnectionDescriptor() {
        return _connectionDescriptor;
    }
    
    public String toString() {
        return toString("\r\n");
    }
    
    public String toString(String crlf) {
        if (_method == null || _url == null || _version == null) {
            return "Unitialised Request!";
        }
        StringBuffer buff = new StringBuffer();
        buff.append(_method).append(" ");
        buff.append(_url).append(" ");
        buff.append(_version).append(crlf);
        buff.append(super.toString(crlf));
        return buff.toString();
    }
    
    public boolean equals(Object obj) {
        if (!(obj instanceof Request)) return false;
        Request req = (Request)obj;
        if (!getMethod().equals(req.getMethod())) return false;
        if (!getURL().equals(req.getURL())) return false;
        if (!getVersion().equals(req.getVersion())) return false;
        return super.equals(req);
    }
    
}