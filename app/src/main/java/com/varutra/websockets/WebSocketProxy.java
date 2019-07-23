package com.varutra.websockets;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;


public abstract class WebSocketProxy {
    
    private static final Logger logger = Logger.getLogger(WebSocketProxy.class.getSimpleName());

    public enum State {  
        CONNECTING, OPEN, CLOSING, CLOSED, // ready state
        EXCLUDED, INCLUDED; // no WebSocket state, used for new black- or whitelisted channels
    }
    private static Comparator<WebSocketObserver> observersComparator;
    
    protected State state;
    
    protected Timestamp start;

    protected Timestamp end;

    protected Map<InputStream, WebSocketMessage> unfinishedMessages;

    protected final Socket localSocket;
    
    protected final Socket remoteSocket;

    private WebSocketListener remoteListener;
    
    private WebSocketListener localListener;

    private Vector<WebSocketObserver> observerList;


    private long handshakeReference;

    private final String host;

    private final int port;

    private final Long channelId;
    
    private AtomicInteger messageIdGenerator;

    private boolean isForwardOnly;
    
    private boolean isClientMode;
    
    public static WebSocketProxy create(String version, Socket localSocket, Socket remoteSocket, String subprotocol, Map<String, String> extensions) throws WebSocketException {
        logger.info("Create WebSockets proxy for version '" + version + "'.");
        WebSocketProxy wsProxy = null;
        
        // TODO: provide a registry for WebSocketProxy versions
        if (version.equals("13")) {
            wsProxy = new WebSocketProxyV13(localSocket, remoteSocket);
            
            if (subprotocol != null) {
                // TODO: do something with this subprotocol
            }
            
            if (extensions != null && extensions.size() > 0) {
                // TODO: do something with these extensions
            }
        } else {
            throw new WebSocketException("Unsupported Sec-WebSocket-Version '"
                    + version + "' provided in factory method!");
        }
        
        return wsProxy;
    }

    public WebSocketProxy(Socket localSocket, Socket remoteSocket) {
        if (localSocket == null) {
            isClientMode = true;
        } else {
            isClientMode = false;
        }
        
        this.localSocket = localSocket;
        this.remoteSocket = remoteSocket;
        
        unfinishedMessages = new HashMap<InputStream, WebSocketMessage>();
        observerList = new Vector<WebSocketObserver>();
        
        synchronized (this) {
            channelId = System.currentTimeMillis();
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        messageIdGenerator = new AtomicInteger(0);
        host = remoteSocket.getInetAddress().getHostName();
        port = remoteSocket.getPort();
        
        isForwardOnly = false;
    }
    
    protected void setState(State newState) {
        if (state == newState) {
            return;
        }
        
        switch (newState) {
        case OPEN:
            start = new Timestamp(Calendar.getInstance().getTimeInMillis());
            break;
        case CLOSED:
            end = new Timestamp(Calendar.getInstance().getTimeInMillis());
            break;
        default:
        }
        
        state = newState;
        
        if (!isForwardOnly) {
            notifyStateObservers(state);
        }
    }

    public void startListeners(ExecutorService listenerThreadPool, InputStream remoteReader) throws WebSocketException {
        setState(State.CONNECTING);
        
        if (localSocket != null && (localSocket.isClosed() || !localSocket.isConnected())) {
            throw new WebSocketException("local socket is closed or not connected");
        }
        
        if (remoteSocket.isClosed() || !remoteSocket.isConnected()) {
            throw new WebSocketException("remote socket is closed or not connected");
        }
        
        try {
            if (localSocket != null) {
                localSocket.setSoTimeout(0); // infinite timeout
                localSocket.setTcpNoDelay(true);
                localSocket.setKeepAlive(true);
            }
            
            remoteSocket.setSoTimeout(0);
            remoteSocket.setTcpNoDelay(true);
            remoteSocket.setKeepAlive(true);
        } catch (SocketException e) {
            throw new WebSocketException(e);
        }
        
        logger.info("Start listeners for channel '" + toString() + "'.");
        
        try {
            remoteListener = createListener(remoteSocket, remoteReader, "remote");
            localListener = createListener(localSocket, "local");
        } catch (WebSocketException e) {
            shutdown();
            throw e;
        }
        
        setState(State.OPEN);
        
        listenerThreadPool.execute(remoteListener);
        listenerThreadPool.execute(localListener);
    }
    
    private WebSocketListener createListener(Socket readEnd, InputStream reader, String side) throws WebSocketException {
        try {
            OutputStream writer = null;
            Socket writeSocket = getOppositeSocket(readEnd);
            if (writeSocket != null) {
                writer = writeSocket.getOutputStream();
            }

            String name = "WS-Listener (" + side + ") '" + toString() + "'";
            
            return new WebSocketListener(this, reader, writer, name);
        } catch (IOException e) {
            throw new WebSocketException("Failed to start listener due to: " + e.getMessage());
        }
    }

    private WebSocketListener createListener(Socket readEnd, String side) throws WebSocketException {
        try {
            InputStream reader = null;
            if (readEnd != null) {
                reader = new BufferedInputStream(readEnd.getInputStream());
            }
            
            return createListener(readEnd, reader, side);
        } catch (IOException e) {
            throw new WebSocketException("Failed to start listener due to: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (isClientMode && localListener.isFinished() && !remoteListener.isFinished()) {
            return;
        }
        
        setState(State.CLOSING);
        
        int closedCount = 0;
        
        if (localListener != null && !localListener.isFinished()) {
            localListener.stop();
        } else {
            closedCount++;
        }
        
        if (remoteListener != null && !remoteListener.isFinished()) {
            remoteListener.stop();
        } else {
            closedCount++;
        }

        if (closedCount == 2) {
            logger.info("close WebSockets");
            
            try {
                if (localSocket != null) {
                    localSocket.close();
                }
            } catch (IOException e) {
                logger.info(e.getMessage());
            }
            
            try {
                remoteSocket.close();
            } catch (IOException e) {
                logger.info(e.getMessage());
            }
            
            setState(State.CLOSED);
        }
    }

    public boolean isConnected() {
        if (state != null && state.equals(State.OPEN)) {
            return true;
        }
        return false;
    }

    public void processRead(InputStream in, OutputStream out, byte frameHeader) throws IOException {
        WebSocketMessage message = null;
    
        int opcode = (frameHeader & 0x0F); // last 4 bits represent opcode
        String readableOpcode = WebSocketMessage.opcode2string(opcode);
        
        logger.info("Process WebSocket frame: " + opcode + " (" + readableOpcode + ")");
        
        if (WebSocketMessage.isControl(opcode)) {
            message = createWebSocketMessage(in, frameHeader);
        } else {
            
            boolean shouldContinueMessage = unfinishedMessages.containsKey(in);
            if (opcode == WebSocketMessage.OPCODE_CONTINUATION) {
                if (shouldContinueMessage) {
                    message = unfinishedMessages.remove(in);
                    message.readContinuation(in, frameHeader);
                } else {
                    handleInvalidContinuation(in, out, frameHeader);                    
                    return;
                }
            } else {
                message = createWebSocketMessage(in, frameHeader);
            }
            
            if (!message.isFinished()) {
                unfinishedMessages.put(in, message);
            }
        }
        
        if (isForwardOnly || notifyMessageObservers(message)) {
            message.forward(out);
        }    
    }

    private void handleInvalidContinuation(InputStream in, OutputStream out, byte frameHeader) throws IOException {
        logger.info("Got continuation frame, but there is no message to continue - forward frame in any case!");
        
        WebSocketMessage message = createWebSocketMessage(in, frameHeader);
        if (!isForwardOnly) {
            if (!notifyMessageObservers(message)) {
                logger.info("Ignore observer's wish to skip forwarding as we have received an invalid frame!");
            }
        }
        message.forward(out);
    }

    protected abstract WebSocketMessage createWebSocketMessage(InputStream in, byte frameHeader) throws IOException;

    protected abstract WebSocketMessage createWebSocketMessage(WebSocketMessageDTO message) throws WebSocketException;

    protected Socket getOppositeSocket(Socket socket) {
        Socket oppositeSocket;
        if (socket == localSocket) {
            oppositeSocket = remoteSocket;
        } else {
            oppositeSocket = localSocket;
        }
        return oppositeSocket;
    }

    public boolean isForwardOnly() {
        return isForwardOnly;
    }

    public void setForwardOnly(boolean shouldBeForwardOnly) {
        if (isForwardOnly == shouldBeForwardOnly) {
            return;
        }
        
        if (isForwardOnly && !shouldBeForwardOnly) {
            logger.info(toString() + " is re-included in storage & UI!");
            
            isForwardOnly = false;
            notifyStateObservers(State.INCLUDED);
        } else if (!isForwardOnly && shouldBeForwardOnly) {
            logger.info(toString() + " is excluded from storage & UI!");

            isForwardOnly = true;
            notifyStateObservers(State.EXCLUDED);
        }
    }
    
    protected boolean notifyMessageObservers(WebSocketMessage message) {
        for (WebSocketObserver observer : observerList) {
            try {
                if (!observer.onMessageFrame(channelId, message)) {
                    return false;
                }
            } catch (Exception e) {
                logger.info(e.getMessage());
            }
        }
        return true;
    }

    protected void notifyStateObservers(State state) {
        for (WebSocketObserver observer : observerList) {
            observer.onStateChange(state, this);
        }
    }
    
    public void addObserver(WebSocketObserver observer) {
        observerList.add(observer);
        Collections.sort(observerList, getObserversComparator());
    }
    
    public void removeObserver(WebSocketObserver observer) {
        observerList.remove(observer);
    }
    
    private static Comparator<WebSocketObserver> getObserversComparator() {
        if(observersComparator == null) {
            createObserversComparator();
        }
        
        return observersComparator;
    }
    
    private static synchronized void createObserversComparator() {
        if (observersComparator == null) {
            observersComparator = new Comparator<WebSocketObserver>() {
                
                @Override
                public int compare(WebSocketObserver o1, WebSocketObserver o2) {
                    int order1 = o1.getObservingOrder();
                    int order2 = o2.getObservingOrder();
                    
                    if (order1 < order2) {
                        return -1;
                    } else if (order1 > order2) {
                        return 1;
                    }
                    
                    return 0;
                }
            };
        }
    }

    public Long getChannelId() {
        return channelId;
    }
    
    public int getIncrementedMessageCount() {
        return messageIdGenerator.incrementAndGet();
    }
    
    public long getHandshakeReference() {
        return handshakeReference;
    }
    
    public void setHandshakeReference(long handshakeReference) {
        this.handshakeReference = handshakeReference;
    }

    public WebSocketChannelDTO getDTO() {
        WebSocketChannelDTO dto = new WebSocketChannelDTO();
        dto.id = getChannelId();
        dto.host = host;
        dto.port = port;
        dto.startTimestamp = (start != null) ? start.getTime() : null;
        dto.endTimestamp = (end != null) ? end.getTime() : null;
        
        dto.historyId = getHandshakeReference();
        
        return dto;
    }
    
    @Override
    public String toString() {
        return host + ":" + port + " (#" + channelId + ")";
    }

    public WebSocketMessage sendAndNotify(WebSocketMessageDTO msg, boolean notify) throws IOException {
        logger.info("send custom message");
        WebSocketMessage message = createWebSocketMessage(msg);
        
        OutputStream out;
        if (msg.isOutgoing) {
            out = localListener.getOutputStream();
        } else {
            out = remoteListener.getOutputStream();
        }
    
        if (message.forward(out) && notify) {
            notifyMessageObservers(message);
        }
        return message;
    }

    public boolean isClientMode() {
        return isClientMode;
    }
}