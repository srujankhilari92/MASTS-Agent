package com.varutra.webscarab.plugin.proxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import android.util.Log;

import netutils.build.IPv4PacketBuilder;
import netutils.build.TCPPacketBuilder;
import netutils.files.pcap.PCapFileWriter;
import netutils.parse.IPv4Address;
import netutils.parse.TCPPacket;

public class PcapWriter {
    
    private static String TAG = PcapWriter.class.getSimpleName();
    private static boolean LOGD = false;
    private static PCapFileWriter pcapFileWriterAll;
    private PCapFileWriter pcapFileWriter;
    private IPv4Address clientAddress;
    private IPv4Address serverAddress;
    private int clientPort;
    private int serverPort;
    private long sequenceNumber;
    
    public static void init(String fileName) throws IOException{
        pcapFileWriterAll = new PCapFileWriter(fileName);
    }
    
    public static void release() throws IOException{
        pcapFileWriterAll = null;
        if (serverThread != null){
            try {
                serverThread.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (serverSocket != null){
                    serverSocket.close();
                    serverSocket = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (serverSocketClient != null){
                    serverSocketClient.close();
                    serverSocketClient = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    
    
    public static synchronized void writeToAll(byte[] data, long time) throws IOException{
        if (pcapFileWriterAll != null){
            pcapFileWriterAll.addPacket(data, time);
            if (serverSocketClient != null && serverSocketClient.isConnected() && isClientConnected){
                if (LOGD) Log.d(TAG, "Send data also to connected client");
                try{
                    serverSocketClient.getOutputStream().write(data);
                }catch(Exception ex){
                    ex.printStackTrace();
                }
            }
        }
    }
    
    private static ServerSocket serverSocket;
    private static Socket serverSocketClient;
    private static boolean isClientConnected = false;
    private static Thread serverThread = null;
    
    
    public PcapWriter(Socket client, Socket server, String fileName) throws Exception{
        pcapFileWriter = new PCapFileWriter(fileName);
        this.clientAddress = new IPv4Address(client.getInetAddress().getHostAddress());
        this.serverAddress = new IPv4Address(server.getInetAddress().getHostAddress());
        clientPort = client.getPort();
        serverPort = server.getPort();
        sequenceNumber = 1;
        if (serverSocket == null){
            serverSocket = new ServerSocket(5400);
            serverThread = new Thread(new Runnable() {
                
                @Override
                public void run() {
                    try{
                        serverThread.setName("PcapThread waiting for connection");
                        serverSocketClient = serverSocket.accept();
                        isClientConnected = true;
                        serverThread.setName("PcapThread " + serverSocketClient.getInetAddress().getHostAddress());
                    }catch(Exception ex){
                        ex.printStackTrace();
                    }
                }
            });
            serverThread.setName("PcapThread");
            serverThread.start();
        }
    }
    
    public synchronized void writeData(byte[] data, long timestamp, boolean flip) throws Exception{
        if (pcapFileWriter != null){
            IPv4PacketBuilder pb = new IPv4PacketBuilder();
            if (flip){
                pb.setSrcAddr(serverAddress);
                pb.setDstAddr(clientAddress);
            }else{
                pb.setSrcAddr(clientAddress);
                pb.setDstAddr(serverAddress);
            }
            
            TCPPacketBuilder tcpbp = new TCPPacketBuilder();
            tcpbp.setL3(pb);
            if (flip){
                tcpbp.setSrcPort(serverPort);
                tcpbp.setDstPort(clientPort);
            }else{
                tcpbp.setSrcPort(clientPort);
                tcpbp.setDstPort(serverPort);
            }
            
            tcpbp.setSeqNum(sequenceNumber);
            
            tcpbp.setPayload(data);
            sequenceNumber = sequenceNumber + data.length;
            tcpbp.setACKFlag(false);
            tcpbp.setSYNFlag(false);
            TCPPacket packet =  tcpbp.createTCPPacket();
            byte[] packetData = packet.getRawBytes();
            pcapFileWriter.addPacket(packetData, timestamp);
            writeToAll(packetData, timestamp);
        }
    }
}
