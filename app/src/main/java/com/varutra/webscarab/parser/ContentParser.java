package com.varutra.webscarab.parser;

import com.varutra.webscarab.model.HttpUrl;
import com.varutra.webscarab.model.Message;

public interface ContentParser {
    
    Object parseMessage(HttpUrl url, Message message);
    
}
