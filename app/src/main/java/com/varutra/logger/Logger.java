package com.varutra.logger;

import java.util.logging.LogManager;

import android.os.Handler;

public class Logger {
    
    private static boolean LOG_TO_HANDLER = true;
    private static boolean LOG_TO_FILE = false;
    private static boolean LOG_TO_GUI = true;
    
    private static Logger mLogger;
    
    
    public Logger(Handler guiHandler){
        if (LOG_TO_GUI){
            LogHandler logHandler = new LogHandler(guiHandler);
            LogManager.getLogManager().getLogger("").addHandler(logHandler);
        }
    }
}
