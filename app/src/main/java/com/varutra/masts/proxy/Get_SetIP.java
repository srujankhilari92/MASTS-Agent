package com.varutra.masts.proxy;

public class Get_SetIP {

	public static String IP;
	public static String PORT;
	public static String strmPORT;

	public static String getStrmPORT() {
		return strmPORT;
	}

	public static void setStrmPORT(String strmPORT) {
		Get_SetIP.strmPORT = strmPORT;
	}

	public static String getIP() {
		return IP;
	}

	public static void setIP(String iP) {
		IP = iP;
	}

	public static String getPORT() {
		return PORT;
	}

	public static void setPORT(String pORT) {
		PORT = pORT;
	}

}