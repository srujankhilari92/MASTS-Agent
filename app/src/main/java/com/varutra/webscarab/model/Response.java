package com.varutra.webscarab.model;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

import java.text.ParseException;

public class Response extends Message {
    
    private String version = null;
    private String status = null;
    private String message = null;
    private Request _request = null;
    private Socket _socket = null;
    private boolean _protocolswitch = false;
    public static String NO_DATA_FROM_SERVER = "No data received from the server";
    
    public Response() {
        setVersion("HTTP/1.0");
    }
    
    public Response(Response resp) {
        this.version = resp.getVersion();
        this.status = resp.getStatus();
        this.message = resp.getMessage();
        setHeaders(resp.getHeaders());
        setContent(resp.getContent());
    }
    
    public void setSocket(Socket socket){
        _socket = socket;
    }
    
    public Socket getSocket(){
        return _socket;
    }
    
    public void read(InputStream is) throws IOException {
        String line = readLine(is);
        if (line == null) {
            throw new IOException(NO_DATA_FROM_SERVER);
        }
        String[] parts = line.split(" ", 3);
        if (parts.length >= 2) {
            setVersion(parts[0]);
            setStatus(parts[1]);
        } else {
            throw new IOException("Invalid response line read from the server: \"" + line + "\"");
        }
        if (parts.length == 3) {
            setMessage(parts[2]);
        } else {
            setMessage("");
        }
        super.read(is);
        if (status.equals("304") || status.equals("204")) {

            setNoBody();
        }
    }
    
    public void parse (String string) throws ParseException {
        parse(new StringBuffer(string));
    }
    
    public void parse(StringBuffer buff) throws ParseException {
        String line = getLine(buff);
        String[] parts = line.split(" ", 3);
        if (parts.length >= 2) {
            setVersion(parts[0]);
            setStatus(parts[1]);
        }
        if (parts.length == 3) {
            setMessage(parts[2]);
        } else {
            setMessage("");
        }
        super.parse(buff);
        if (status.equals("304") || status.equals("204")) {
            setNoBody();
        }
    }
    
    public boolean haveProtocolSwitch(){
        return _protocolswitch;
    }
    
    public void writeSwitchProtocol(OutputStream os) throws Exception {
        writeHeaders(os, "\r\n");
        _protocolswitch = true;
    }
    
    public void writeHeaders(OutputStream os, String crlf) throws IOException {
        os = new BufferedOutputStream(os);
        os.write(new String(version + " " + getStatusLine() + crlf).getBytes());
        super.writeHeaders(os,crlf);
        os.flush();
    }
    
    public void write(OutputStream os) throws Exception {
        write(os, "\r\n");
    }
    
    public void write(OutputStream os, String crlf) throws IOException {
        os = new BufferedOutputStream(os);
        os.write(new String(version + " " + getStatusLine() + crlf).getBytes());
        super.write(os,crlf);
        os.flush();
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getVersion() {
        return version;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getStatusLine() {
        return status + " " + message;
    }
    
    public String toString() {
        return toString("\r\n");
    }
    
    public String toString(String crlf) {
        StringBuffer buff = new StringBuffer();
        buff.append(version + " " + getStatusLine() + crlf);
        buff.append(super.toString(crlf));
        return buff.toString();
    }
    
    public void setRequest(Request request) {
        _request = request;
    }
    
    public Request getRequest() {
        return _request;
    }
        
    public boolean equals(Object obj) {
        if (! (obj instanceof Response)) return false;
        Response resp = (Response) obj;
        if (!getVersion().equals(resp.getVersion())) return false;
        if (!getStatusLine().equals(resp.getStatusLine())) return false;
        return super.equals(obj);
    }
    
}