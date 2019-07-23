package com.varutra.webscarab.parser;


import org.htmlparser.Parser;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import com.varutra.webscarab.model.HttpUrl;
import com.varutra.webscarab.model.Message;

import java.util.logging.Logger;
public class HTMLParser implements ContentParser {
    
    private Logger _logger = Logger.getLogger(this.getClass().getName());
    
    public HTMLParser() {
    }
    public Object parseMessage(HttpUrl url, Message message) {
        String contentType = message.getHeader("Content-Type");
        if (contentType == null || !contentType.matches("text/html.*")) {
            return null;
        }
        byte[] content = message.getContent();
        if (content == null || content.length == 0) {
            return null;
        }
        Parser parser = Parser.createParser(new String(content), null);
        try {
            NodeList nodelist = parser.extractAllNodesThatMatch(new NodeFilter() {
		public boolean accept(Node node) {
                    return true;
                }
            });
            return nodelist;
        } catch (ParserException pe) {
            _logger.severe(pe.toString());
            return null;
        }
    }
    
}
