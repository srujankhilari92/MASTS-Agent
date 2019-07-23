package com.varutra.websockets;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


import com.varutra.webscarab.model.Response;
import com.varutra.webscarab.store.sql.SqlLiteStore;

 
public class ExtensionWebSocket {
    
    private static final Logger logger = Logger.getLogger(ExtensionWebSocket.class.getName());
    
    public static final int HANDSHAKE_LISTENER = 10;
    
    public static final String NAME = "ExtensionWebSocket";

    private ExecutorService listenerThreadPool;

    private Map<String, WebSocketObserver> allChannelObservers;

    private Map<Long, WebSocketProxy> wsProxies;


    private WebSocketStorage storageObserver;

    
    public ExtensionWebSocket(SqlLiteStore store) {
        allChannelObservers = new HashMap<String, WebSocketObserver>();
        wsProxies = new HashMap<Long, WebSocketProxy>();
        storageObserver = new WebSocketStorage(store);
        allChannelObservers.put(WebSocketStorage.class.getName(), storageObserver);
    }
    
    

    
    public void unload() {
        
        for (Entry<Long, WebSocketProxy> wsEntry : wsProxies.entrySet()) {
            WebSocketProxy wsProxy = wsEntry.getValue();
            wsProxy.shutdown();
        }
    }

    
    public void addAllChannelObserver(String name, WebSocketObserver observer) {
        allChannelObservers.put(name, observer);
    }
    
    public void removeAllChannelObserver(String name, WebSocketObserver observer) {
        allChannelObservers.remove(name);
    }



    public boolean onHandshakeResponse(long handshakeReference, Response httpResponse, Socket inSocket, Socket outWebSocket, InputStream outWebInputStream) {

        boolean keepSocketOpen = false;
        
        logger.info("Got WebSockets upgrade request. Handle socket connection over to WebSockets extension.");
        
        Socket outSocket = outWebSocket;
        InputStream outReader = outWebInputStream;
        addWebSocketsChannel(handshakeReference, httpResponse, inSocket, outSocket, outReader);
        
        return keepSocketOpen;
    }

    
    public void addWebSocketsChannel(long historyId, Response httpResponse, Socket localSocket, Socket remoteSocket, InputStream remoteReader) {
        try {            

            String source = (localSocket != null) ? localSocket.getInetAddress().toString() + ":" + localSocket.getPort() : "VarutraProxy";
            String destination = remoteSocket.getInetAddress() + ":" + remoteSocket.getPort();
            
            logger.info("Got WebSockets channel from " + source + " to " + destination);
            
            Map<String, String> wsExtensions = parseWebSocketExtensions(httpResponse);
            String wsProtocol = parseWebSocketSubProtocol(httpResponse);
            String wsVersion = parseWebSocketVersion(httpResponse);
    
            WebSocketProxy wsProxy = null;
            wsProxy = WebSocketProxy.create(wsVersion, localSocket, remoteSocket, wsProtocol, wsExtensions);
            
            for (WebSocketObserver observer : allChannelObservers.values()) {
                // TODO here we could also have map so we can dynamically remove observers
                wsProxy.addObserver(observer);
            }
            
            wsProxy.setHandshakeReference(historyId);
            
            // TODO Varutrap some regular expression what to have in ignore list 
            wsProxy.startListeners(getListenerThreadPool(), remoteReader);
            
            synchronized (wsProxies) {
                wsProxies.put(wsProxy.getChannelId(), wsProxy);
            }
        } catch (Exception e) {
            if (localSocket != null && !localSocket.isClosed()) {
                try {
                    localSocket.close();
                } catch (IOException e1) {
                    logger.info(e.getMessage());
                }
            }
            
            if (remoteReader != null) {
                try {
                    remoteReader.close();
                } catch (IOException e1) {
                    logger.info(e.getMessage());
                }
            }
            
            if (remoteSocket != null && !remoteSocket.isClosed()) {
                try {
                    remoteSocket.close();
                } catch (IOException e1) {
                    logger.info(e.getMessage());
                }
            }
            logger.info("Adding WebSockets channel failed due to: '" + e.getClass() + "' " + e.getMessage());
            return;
        }
    }

    private Map<String, String> parseWebSocketExtensions(Response msg) {
        Vector<String> extensionHeaders = null;
        // TODO Varutrap this is not used so can be null currently

        if (extensionHeaders == null) {
            return null;
        }
        
        Map<String, String> wsExtensions = new LinkedHashMap<String, String>();
        for (String extensionHeader : extensionHeaders) {
            for (String extension : extensionHeader.split(",")) {
                String key = extension.trim();
                String params = "";
                
                int paramsIndex = key.indexOf(";");
                if (paramsIndex != -1) {
                    key = extension.substring(0, paramsIndex).trim();
                    params = extension.substring(paramsIndex + 1).trim();
                }
                
                wsExtensions.put(key, params);
            }
        }
        
        
        return wsExtensions;
    }

    private String parseWebSocketSubProtocol(Response msg) {
        String subProtocol = msg.getHeader(
                WebSocketProtocol.HEADER_PROTOCOL);
        return subProtocol;
    }

    private String parseWebSocketVersion(Response msg) {
        String version = msg.getHeader(
                WebSocketProtocol.HEADER_VERSION);
        
        if (version == null) {
            version = msg.getHeader(WebSocketProtocol.HEADER_VERSION);
            
            if (version == null) {
                logger.info("No " + WebSocketProtocol.HEADER_VERSION + " header was provided - try version 13");
                version = "13";
            }
        }
        
        return version;
    }

    private ExecutorService getListenerThreadPool() {
        if (listenerThreadPool == null) {
            listenerThreadPool = Executors.newCachedThreadPool();
        }
        return listenerThreadPool;
    }


    public boolean isConnected(long channelId) {
        synchronized (wsProxies) {
            if (wsProxies.containsKey(channelId)) {
                return wsProxies.get(channelId).isConnected();
            }
        }
        return false;
    }
    
    public boolean sendMessage(long channelId, WebSocketMessageDTO message, boolean notify) throws IOException{
        if (isConnected(channelId)){
            WebSocketProxy proxy =  wsProxies.get(channelId);
            if (proxy != null){
                WebSocketMessage msg = proxy.sendAndNotify(message, notify);
                if (msg != null && !notify){
                    storageObserver.insertMessage(msg.getDTO());
                }
                return true;
            }
        }
        return false;
    }
    
    public Map<Long, String> getConnectedProxies(){
        Map<Long, String> proxies = new LinkedHashMap<Long, String>();
        for(WebSocketProxy proxy : wsProxies.values()){
            if (proxy.isConnected()){
                proxies.put(proxy.getChannelId(), proxy.toString());
            }
        }
        return proxies;
    }
    
}
