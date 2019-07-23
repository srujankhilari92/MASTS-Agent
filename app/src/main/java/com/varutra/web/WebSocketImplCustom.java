package com.varutra.web;

import java.net.Socket;
import java.util.List;

import org.java_websocket.WebSocketImpl;
import org.java_websocket.WebSocketListener;
import org.java_websocket.drafts.Draft;

public class WebSocketImplCustom extends WebSocketImpl{

    private boolean isInitialised = false;
    private boolean showRuntimeEvents = false;
    private boolean showStoredEvents = false;
    private String socketId;
    
    public WebSocketImplCustom(WebSocketListener listener, Draft draft,
            Socket sock) {
        super(listener, draft, sock);
    }
    
    public WebSocketImplCustom(WebSocketListener listener, List<Draft> d, Socket sock) {
        super(listener, d, sock);
    }

    public boolean isInitialised() {
        return isInitialised;
    }

    public void setInitialised(boolean isInitialised) {
        this.isInitialised = isInitialised;
    }

    public boolean isShowRuntimeEvents() {
        return showRuntimeEvents;
    }

    public void setShowRuntimeEvents(boolean showRuntimeEvents) {
        this.showRuntimeEvents = showRuntimeEvents;
    }
    public boolean isShowStoredEvents() {
        return showStoredEvents;
    }

    public void setShowStoredEvents(boolean showStoredEvents) {
        this.showStoredEvents = showStoredEvents;
    }

    public String getSocketId() {
        return socketId;
    }

    public void setSocketId(String socketId) {
        this.socketId = socketId;
    }
    
}
