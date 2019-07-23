package com.varutra.webscarab.model;

import java.util.ArrayList;
import java.net.MalformedURLException;

public class HttpUrl implements Comparable {
    
    private static final HttpUrl[] nullPath = new HttpUrl[0];
    
    private String _scheme;
    private String _host;
    private int _port;
    private String _path;
    private String _fragment = null;
    private String _query = null;
    
    private int _hashcode;
    
    public HttpUrl(String url) throws MalformedURLException {
        if (url.indexOf('\n') > -1 || url.indexOf(' ') > -1)
            throw new MalformedURLException("Illegal characters in url: " + url);
        parseUrl(url);
        _hashcode = this.toString().hashCode();
    }
    
    public HttpUrl(HttpUrl url, String relative) throws MalformedURLException {
        if (relative.indexOf('\n') > -1 || relative.indexOf(' ') > -1)
            throw new MalformedURLException("Illegal characters in relative : " + relative);
        if (url == null || relative.startsWith("http://") || relative.startsWith("https://")) { 
            parseUrl(relative);
            _hashcode = this.toString().hashCode();
            return;
        }
        _scheme = url.getScheme();
        _host = url.getHost();
        _port = url.getPort();
        if (relative.startsWith("/")) { 
            _path = relative;
        } else {
            _path = relativePath(url.getPath(), relative);
        }
        splitFragQuery();
        _path = _path.replaceAll(" ", "%20");
        if (_query != null) _query = _query.replace(' ', '+');
        if (_fragment != null) _fragment = _fragment.replaceAll(" ", "%20");
        _hashcode = this.toString().hashCode();
    }
    
    private void parseUrl(String url) throws MalformedURLException {
        int pos = url.indexOf("://");
        if (pos == -1)
            throw new MalformedURLException("An URL must have a scheme!");
        _scheme = url.substring(0, pos).toLowerCase();
        if (!_scheme.equals("http") && !_scheme.equals("https"))
            throw new MalformedURLException("This class only supports HTTP or HTTPS schemes: '"+_scheme+"'");
        int prev = pos + 3;
        pos = url.indexOf("/", prev);
        if (pos == -1)
            pos = url.length();
        String hp = url.substring(prev, pos);
        int colon = hp.indexOf(":");
        if (colon == -1) {
            _host = hp;
            if (_scheme.equals("http")) {
                _port = 80;
            } else if (_scheme.equals("https")) {
                _port = 443;
            }
        } else {
            try {
                _host = hp.substring(0, colon);
                _port = Integer.parseInt(hp.substring(colon + 1));
            } catch (NumberFormatException nfe) {
                throw new MalformedURLException("Error parsing the port number: " + nfe);
            }
        }
        if ("".equals(_host))
            throw new MalformedURLException("Host cannot be empty");
        if (_port < 1 || _port > 65535)
            throw new MalformedURLException("Port out of range: " + _port);
        if (pos == url.length()) {
            _path = "/";
        } else {
            _path = url.substring(pos);
            splitFragQuery();
        }
    }
    
    private String relativePath(String oldPath, String relative) {
        if (!oldPath.endsWith("/")) { // trim the file part
            oldPath = parentPath(oldPath);
        }
        
        while (relative.startsWith("../") || relative.startsWith("./")) {
            if (relative.startsWith("./")) { // trim meaningless self-ref
                relative = relative.substring(2);
            } else {
                relative = relative.substring(3);
                if (oldPath.length()>1) {
                    oldPath = parentPath(oldPath);
                }
            }
        }
        return oldPath + relative;
    }
    
    private void splitFragQuery() {
        int hash = _path.indexOf("#");
        if (hash > -1) _path = _path.substring(0, hash);
        
        int ques = _path.indexOf("?");
        if (ques > -1) {
            _query = _path.substring(ques + 1);
            _path = _path.substring(0, ques);
        }
        int semi = _path.indexOf(";");
        if (semi > -1) {
            _fragment = _path.substring(semi + 1);
            _path = _path.substring(0, semi);
        }
    }
    
    public String getScheme() {
        return _scheme;
    }
    public String getHost() {
        return _host;
    }
    
    public int getPort() {
        return _port;
    }
    
    public String getPath() {
        return _path;
    }
    
    public String getFragment() {
        return _fragment;
    }
    
    public String getQuery() {
        return _query;
    }
    
    public String getSHPP() {
        StringBuffer buff = new StringBuffer();
        buff.append(_scheme).append("://");
        buff.append(_host).append(":").append(_port);
        buff.append(_path);
        return buff.toString();
    }
    
    public String getParameters() {
        if (_fragment == null && _query == null) return null;
        StringBuffer buff = new StringBuffer();
        if (_fragment != null) buff.append(";").append(_fragment);
        if (_query != null) buff.append("?").append(_query);
        return buff.toString();
    }
    
    private String parentPath(String path) {
        int secondlast = path.lastIndexOf("/",path.length()-2);
        return path.substring(0,secondlast+1);
    }
    
    public HttpUrl getParentUrl() {
        if (_scheme.equals("")) throw new NullPointerException("Should not be trying to get the parent of NULL URL");
        try {
            if (_fragment != null || _query != null) {
                return new HttpUrl(getSHPP());
            } else if (_path != null && _path.length() > 1) {
                String url = getSHPP();
                int secondLast = url.lastIndexOf("/",url.length()-2);
                return new HttpUrl(url.substring(0, secondLast+1));
            } else {
                return null;
            }
        } catch (MalformedURLException mue) {
            System.err.println("Malformed URL calculating parent path of " + toString());
            return null;
        }
    }
    
    public HttpUrl[] getUrlHierarchy() {
        ArrayList list = new ArrayList();
        list.add(this);
        HttpUrl url = getParentUrl();
        while (url != null) {
            list.add(0, url);
            url = url.getParentUrl();
        }
        return (HttpUrl[]) list.toArray(nullPath);
    }
    
    public String toString() {
        if (_scheme.equals("")) return "NULL URL";
        StringBuffer buff = new StringBuffer();
        buff.append(_scheme).append("://");
        buff.append(_host).append(":").append(_port);
        return direct(buff).toString();
    }
    
    public StringBuffer direct(StringBuffer buff) {
        buff.append(_path);
        if (_fragment != null) buff.append(";").append(_fragment);
        if (_query != null) buff.append("?").append(_query);
        return buff;
    }
    
    public String direct() {
        return direct(new StringBuffer()).toString();
    }
    public boolean equals(Object o) {
        if (! (o instanceof HttpUrl)) return false;
        if (_hashcode != o.hashCode()) return false;
        return compareTo(o) == 0;
    }
    
    public int compareTo(Object o) {
        if (o == null) return 1;
        
        if (! (o instanceof HttpUrl)) throw new ClassCastException("Can only compare HttpUrls, not a " + o.getClass().getName());
        
        HttpUrl url = (HttpUrl) o;
        int result;
        
        result = _scheme.compareTo(url.getScheme());
        if (result != 0) return result;
        
        result = _host.compareTo(url.getHost());
        if (result != 0) return result;
        
        result = _port - url.getPort();
        if (result != 0) return result;
        
        result = _path.compareTo(url.getPath());
        if (result != 0) return result;
        
        if (_fragment == null) {
            if (url.getFragment() == null) { result = 0; }
            else { result = -1; }
        } else {
            if (url.getFragment() == null) { result = 1; }
            else { result = _fragment.compareTo(url.getFragment()); }
        }
        if (result != 0) return result;
        
        if (_query == null) {
            if (url.getQuery() == null) { result = 0; }
            else { result = -1; }
        } else {
            if (url.getQuery() == null) { result = 1; }
            else { result = _query.compareTo(url.getQuery()); }
        }
        return result;
    }
    
    public int hashCode() {
        return _hashcode;
    }
}