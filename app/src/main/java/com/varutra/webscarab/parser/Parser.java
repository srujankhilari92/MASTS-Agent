package com.varutra.webscarab.parser;



import com.varutra.webscarab.model.HttpUrl;
import com.varutra.webscarab.model.Message;
import com.varutra.webscarab.util.MRUCache;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class Parser {
    
    private static List<ContentParser> _parsers = new ArrayList<ContentParser>();
    
    private static MRUCache<Message, Object> _cache = new MRUCache<Message, Object>(8);
    
    static {
        _parsers.add(new HTMLParser());
    }
    
    private Parser() {
    }
    
    public static Object parse(HttpUrl url, Message message) {
        if (_cache.containsKey(message)) {
            return _cache.get(message);
        }
        Iterator<ContentParser> it = _parsers.iterator();
        Object parsed = null;
        ContentParser parser;
        while(it.hasNext()) {
            parser = it.next();
            parsed = parser.parseMessage(url, message);
            if (parsed != null) break;
        }
        _cache.put(message, parsed);
        return parsed;
    }
    
}
