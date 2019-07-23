package com.varutra.webscarab.plugin.proxy;



import com.varutra.webscarab.httpclient.HTTPClient;
import com.varutra.webscarab.model.Preferences;
import com.varutra.webscarab.model.Request;
import com.varutra.webscarab.model.Response;
import com.varutra.webscarab.plugin.proxy.ProxyPlugin;

import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
public class RevealHidden extends ProxyPlugin {
    
    private boolean _enabled = false;
    
    public RevealHidden() {
        parseProperties();
    }
    
    public void parseProperties() {
        String prop = "RevealHidden.enabled";
        _enabled = Preferences.getPreferenceBoolean(prop, false);
    }
    
    public String getPluginName() {
        return new String("Reveal Hidden");
    }
    

    public boolean getEnabled() {
        return _enabled;
    }
    
    public HTTPClient getProxyPlugin(HTTPClient in) {
        return new Plugin(in);
    }    
    
    private class Plugin implements HTTPClient {
    
        private HTTPClient _in;
        
        public Plugin(HTTPClient in) {
            _in = in;
        }
        
        public Response fetchResponse(Request request) throws IOException {
            Response response = _in.fetchResponse(request);
            if (_enabled) {
                String ct = response.getHeader("Content-Type");
                if (ct != null && ct.matches("text/.*") && !ct.matches("text/javascript")) {
                    byte[] content = response.getContent();
                    if (content != null) {
                        response.setContent(revealHidden(content));
                        response.addHeader("X-RevealHidden", "possibly modified");
                    }
                }
            }
            return response;
        }
        
        private byte[] revealHidden(byte[] content) {
            Pattern inputPattern = Pattern.compile("(<input.+?type\\s*=\\s*[\"']{0,1})hidden([\"']{0,1}.+?>)", Pattern.CASE_INSENSITIVE);
            Matcher inputMatcher = inputPattern.matcher(new String(content));
            StringBuffer outbuf = new StringBuffer();
            
            while(inputMatcher.find()) {
                String input = inputMatcher.group();
                String name = "noname";
                
                Pattern namePattern = Pattern.compile("name=[\"']{0,1}(\\w+)[\"']{0,1}", Pattern.CASE_INSENSITIVE);
                Matcher nameMatcher = namePattern.matcher(input);
                if (nameMatcher.find() && nameMatcher.groupCount() == 1){
                    name = nameMatcher.group(1);
                }
                input = inputMatcher.group(1) + "text" + inputMatcher.group(2);
                inputMatcher.appendReplacement(outbuf, constructReplacement(name, input));
            }
            inputMatcher.appendTail(outbuf);
            return outbuf.toString().getBytes();
        }

        private String constructReplacement(final String name, final String input) {
            final StringBuffer result = new StringBuffer();
            result.append("<div style=\"background: pink; border: red 1px solid; padding: 2px; margin:4px; text-align: left;\">");
            result.append("<p style=\"color: red; text-align: left; margin-top: 0px; font-size: xx-small;\">Hidden Input Field</p>");
            result.append("<p style=\"text-align: center; color: black; margin: 0px; font-size: normal;\">");
            result.append("[").append(name).append("]").append("&nbsp;").append(input);
            result.append("</p>");
            result.append("<p style=\"color: red; text-align: right; margin-bottom: 0px; font-size: xx-small;\">Revealed by VarutraProxy</p>");
            result.append("</div>");
            return result.toString();
        }

    }

}
