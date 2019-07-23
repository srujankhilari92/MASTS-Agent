package com.varutra.logger;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import android.os.Message;

public class LogHandler extends Handler {
    
    private static android.os.Handler mHandlerCallback;
    
    private static String messageBuffer;
    
    private static Object messageLock = new Object();

    private long lastSendMessageTs = 0;
    
    public LogHandler(android.os.Handler guiHandlerCallBack){
        mHandlerCallback = guiHandlerCallBack;
        this.setLevel(Level.FINEST);
        messageBuffer = "";
        new Thread(new Runnable() {
            public void run() {
                
                while (true){
                    if (messageBuffer.length() > 0){
                        long ts = System.currentTimeMillis();
                        if ((ts  - 100) > lastSendMessageTs){
                            String sendMessage = LogHandler.getMessageBuffer();
                            if (sendMessage.length() > 0){
                                Message msg = mHandlerCallback.obtainMessage(1, sendMessage);
                                mHandlerCallback.sendMessage(msg);
                                lastSendMessageTs = ts;
                            }
                        }
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        //e.printStackTrace();
                    }
                }
            }
          },"LogHandler.logPumper").start();
    }
    
    @Override
    public void close() {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void flush() {
        // TODO Auto-generated method stub
        
    }
    
    public static String getMessageBuffer(){
        String response = "";
        synchronized (messageLock){
            response = String.copyValueOf(messageBuffer.toCharArray());
            messageBuffer = "";
            
        }
        return response;
    }
    
    @Override
    public void publish(LogRecord record) {
        if (record != null && record.getMessage() != null){
            synchronized (messageLock){
                messageBuffer = record.getMessage()+ "\n" + messageBuffer;
            }
        }
    }
}