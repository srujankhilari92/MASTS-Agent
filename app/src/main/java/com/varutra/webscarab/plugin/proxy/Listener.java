package com.varutra.webscarab.plugin.proxy;

import java.lang.Runnable;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.Thread;

import java.util.logging.Level;
import java.util.logging.Logger;

import android.util.Log;

import com.varutra.webscarab.model.ConnectionDescriptor;

public class Listener implements Runnable {
    
    private Proxy _proxy;
    private ListenerSpec _spec;
    
    private ServerSocket _serversocket = null;
    private int _socketTimeout = 30000;

    private boolean _stop = false;
    private boolean _stopped = true;
    
    private int _count = 1;
    
    private Logger _logger = Logger.getLogger(this.getClass().getName());
    
    public Listener(Proxy proxy, ListenerSpec spec) {
        _logger.setLevel(Level.FINEST);
        _proxy = proxy;
        _spec = spec;
    }

    public void run() {
        _stop = false;
        _stopped = false;
        Socket sock;
        ConnectionHandler ch;
        Thread thread;
        if (_serversocket == null || _serversocket.isClosed()) {
            try {
                listen();
            } catch (IOException ioe) {
                _logger.severe("Can't listen at " + _spec + ": " + ioe);
                _stopped = true;
                return;
            }
        }
        while (! _stop) {
            try {
                sock = _serversocket.accept();
              
                ConnectionDescriptor connectionDescriptor = null;
                IClientResolver clientResolver = _proxy.getClientResolver();
                String threadName = Thread.currentThread().getName();
                if (clientResolver != null){
                    connectionDescriptor = clientResolver.getClientDescriptorBySocket(sock);
                    if (connectionDescriptor.getId() > -1){
                        threadName = connectionDescriptor.getId() + "_" + connectionDescriptor.getNamespace();
                    }else{
                        threadName = "conn_" + sock.getInetAddress().getHostAddress() + "_" + sock.getPort();
                    }
                    
                }
                ch = new ConnectionHandler(_proxy, sock, _spec.getBase(), _spec.isTransparentProxy(), _spec.isTransparentProxySecure(),
                                           _spec.mustCaptureData(), _spec.useFakeCerts(), _spec.storeSslAsPcap(),
                                           _proxy.getTransparentProxyResolver(), connectionDescriptor);
                thread = new Thread(ch, Thread.currentThread().getName()+"-"+Integer.toString(_count++));
                thread.setName(threadName);
                thread.setDaemon(true);
                thread.start();
            } catch (SocketTimeoutException stex){
            } catch (SocketException sex){
            } catch (IOException e) {
                String exMessage = e.getMessage();
                 _logger.fine("I/O error while waiting for connection: " + exMessage);
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }
        _stopped = true;
        try {
            _serversocket.close();
        } catch (IOException ioe) {
            System.err.println("Error closing socket : " + ioe);
        }
        _logger.info("Not listening on " + _spec);
    }
    
    private void listen() throws IOException {
        InetSocketAddress sa = _spec.getInetSocketAddress();
        _serversocket = new ServerSocket(sa.getPort(), 5, sa.getAddress());
        
        _logger.info("Proxy listening on " + _spec);
        
        try {
            _serversocket.setSoTimeout(_socketTimeout);
        } catch (SocketException se) {
            _logger.warning("Error setting sockettimeout " + se);
            _logger.warning("It is likely that this listener will be unstoppable!");
        }
    }
    
    public boolean stop() {
        _stop = true;
        try {
            _serversocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!_stopped) {
            for (int i=0; i<20; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {}
                if (_stopped) {
                    return true;
                }
            }
            return false;
        } else {
            return true;
        }
    }

    public ListenerSpec getListenerSpec() {
        return _spec;
    }
    
}
