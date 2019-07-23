package com.varutra.webscarab.model;

import java.util.Date;
import java.util.logging.Logger;


public class Cookie {
    
    private Date _date = null;
    private String _name = null;
    private String _value = null;
    private String _key = null;
    private String _comment = null;
    private String _domain = null;
    private String _path = null;
    private String _maxage = null;
    private boolean _secure = false;
    private String _version = null;
    private boolean _httponly = false;
    
    private Logger _logger = Logger.getLogger(getClass().getName());
    
    public Cookie(Date date, HttpUrl url, String setHeader) {
        _date = date;
        _domain = url.getHost();
        _path = url.getPath();
        int index = _path.lastIndexOf("/");
        if (index > 0) {
            _path = _path.substring(0,index);
        } else {
            _path = "/";
        }
        parseHeader(setHeader);
        _key = _domain + _path + " " + _name;
    }
    
    
    public Cookie(Date date, String setHeader) {
        _date = date;
        parseHeader(setHeader);
        _key = _domain + _path + " " + _name;
    }
    
    private void parseHeader(String setHeader) {
        if (setHeader == null) {
            throw new NullPointerException("You may not pass a null value for setHeader");
        }
        String[] parts = setHeader.split(" *; *");
        if (parts.length < 1) {
            throw new IllegalArgumentException("The setHeader must have at least one part to it!");
        }
        String[] av = parts[0].split("=",2);
        if (av.length != 2) {
            throw new IllegalArgumentException("The header passed in must at least contain the name and value '" +parts[0] + "'");
        }
        _name = av[0];
        _value = av[1];
        for (int i=1; i<parts.length; i++) {
            if (parts[i].equalsIgnoreCase("secure")) {
                _secure = true;
            } else if (parts[i].equalsIgnoreCase("httponly")) {
                    _httponly = true;
            } else {
                av = parts[i].split("=", 2);
                if (av.length != 2) {
                    _logger.warning("Unknown cookie attribute '" + parts[i] + "'");
                } else if (av[0].equalsIgnoreCase("Comment")) {
                    _comment = av[1];
                } else if (av[0].equalsIgnoreCase("Domain")) {
                    _domain = av[1];
                } else if (av[0].equalsIgnoreCase("Path")) {
                    _path = av[1];
                } else if (av[0].equalsIgnoreCase("Max-Age")) {
                    _maxage = av[1];
                } else if (av[0].equalsIgnoreCase("Version")) {
                    _version = av[1];
                }
            }
        }
    }
    
    public String getKey() {
        return _key;
    }
    
    public Date getDate() {
        return _date;
    }
    
    public String getName() {
        return _name;
    }
    
    public String getValue() {
        return _value;
    }

    public String getDomain() {
        return _domain;
    }
    
    public String getMaxAge() {
        return _maxage;
    }
    
    public String getPath() {
        return _path;
    }
    
    public boolean getSecure() {
        return _secure;
    }
    
    public boolean getHTTPOnly() {
        return _httponly;
    }
    
    public String getVersion() {
        return _version;
    }
    
    public String getComment() {
        return _comment;
    }
    
    public String setCookie() {
        StringBuffer buf = new StringBuffer();
        buf.append(_name + "=" + _value);
        if (_comment != null) {
            buf.append("; Comment=" + _comment);
        }
        if (_domain != null) {
            buf.append("; Domain=" + _domain);
        }
        if (_maxage != null) {
            buf.append("; Max-Age=" + _maxage);
        }
        if (_path != null) {
            buf.append("; Path=" + _path);
        }
        if (_secure) {
            buf.append("; Secure");
        }
        if (_httponly) {
            buf.append("; httponly");
        }
        if (_version != null) {
            buf.append("; Version=" + _version);
        }
        return buf.toString();
    }
    
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append(_date.getTime()).append(" ");
        buff.append(setCookie());
        return buff.toString();
    }

}