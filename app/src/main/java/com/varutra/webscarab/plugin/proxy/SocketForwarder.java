package com.varutra.webscarab.plugin.proxy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.varutra.webscarab.model.ConnectionDescriptor;

import android.util.Log;

public class SocketForwarder extends Thread {
    
    private static String TAG = SocketForwarder.class.getSimpleName();
    private static boolean LOGD = false;
    
    private InputStream in;
    private OutputStream out;
    private PcapWriter pcapWriter;
    private boolean flip;
    

    public static void connect(String name, Socket clientSocket, Socket serverSocket, boolean captureAsPcap, File storageDir, ConnectionDescriptor connDesc) throws Exception {
        if (clientSocket != null && serverSocket != null && clientSocket.isConnected() && serverSocket.isConnected()){
            clientSocket.setSoTimeout(0);
            serverSocket.setSoTimeout(0);
            PcapWriter pcapWriter = null;
            if (captureAsPcap && storageDir != null){
                String storageFile = storageDir.getAbsolutePath();
                String uid = "";
                if (connDesc != null){
                    uid = connDesc.getId() + "_" + connDesc.getNamespace();
                }
                String pcapFileName = storageFile + "/" + name + "_" + uid + "_" + System.currentTimeMillis() +  ".pcap";
                pcapFileName = pcapFileName.replace("*", "_").replace(":", "_");
                pcapWriter = new PcapWriter(clientSocket, serverSocket, pcapFileName);
            }
            SocketForwarder clientServer = new SocketForwarder(name + "_clientServer", clientSocket.getInputStream(), serverSocket.getOutputStream(), pcapWriter, false);
            SocketForwarder serverClient = new SocketForwarder(name + "_serverClient", serverSocket.getInputStream(), clientSocket.getOutputStream(), pcapWriter, true);
                clientServer.start();
                serverClient.start();
                
                while (clientServer.isAlive()) {
                        try {
                            clientServer.join();
                        } catch (InterruptedException ie) {
                            ie.printStackTrace();
                        }
                }
                while (serverClient.isAlive()) {
                        try {
                            serverClient.join();
                        } catch (InterruptedException ie) {
                            ie.printStackTrace();
                        }
                }
        }else{
            if (LOGD) Log.d(TAG, "skipping socket forwarding because of invalid sockets");
            if (clientSocket != null && clientSocket.isConnected()){
                clientSocket.close();
            }
            if (serverSocket != null && serverSocket.isConnected()){
                serverSocket.close();
            }
        }
        
    }

    public SocketForwarder(String name, InputStream in, OutputStream out, PcapWriter pcapWriter, boolean flip) {
            this.in = in;
            this.out = out;
            this.pcapWriter = pcapWriter;
            this.flip = flip;
            setName(name);
            setDaemon(true);
    }

    public void run() {
            try {
                    byte[] buff = new byte[4096];
                    int got;
                    while ((got = in.read(buff)) > -1){
                        out.write(buff, 0, got);
                        if (pcapWriter != null){
                            byte[] readData = new byte[got];
                            System.arraycopy(buff, 0, readData, 0, got);
                            pcapWriter.writeData(readData, System.currentTimeMillis() * 1000, flip);
                        }
                    }
            } catch (Exception ignore) {
            } finally {
                    try {
                            in.close();
                    } catch (IOException ignore) {
                        ignore.printStackTrace();
                    }
                    try {
                            out.close();
                    } catch (IOException ignore) {
                        ignore.printStackTrace();
                    }
            }
    }

}
