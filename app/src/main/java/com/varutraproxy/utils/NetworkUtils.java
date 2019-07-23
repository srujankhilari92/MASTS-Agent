package com.varutraproxy.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;

import android.util.Log;

public class NetworkUtils {
    
    private static String TAG = NetworkUtils.class.getName();
    private static Logger _logger = Logger.getLogger(NetworkUtils.class.getName());
    
    public static List<String> getLocalIpAddress() {
        List<String> networkAdapterIps = new ArrayList<String>();
        
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        networkAdapterIps.add(inetAddress.getHostAddress());
                    }
                }
            }
        } catch (SocketException ex) {
            _logger.warning(ex.toString());
            Log.e(TAG, ex.toString());
        }
        return networkAdapterIps;
    }
}
