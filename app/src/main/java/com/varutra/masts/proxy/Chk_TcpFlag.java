package com.varutra.masts.proxy;

public class Chk_TcpFlag {

	public static boolean tcpflag = false;
	public static boolean monitorflag = false;
	public static boolean logcatflag = false;

	public static boolean isLogcatflag() {
		return logcatflag;
	}

	public static void setLogcatflag(boolean logcatflag) {
		Chk_TcpFlag.logcatflag = logcatflag;
	}

	public static boolean isMonitorflag() {
		return monitorflag;
	}

	public static void setMonitorflag(boolean monitorflag) {
		Chk_TcpFlag.monitorflag = monitorflag;
	}

	public static boolean isTcpflag() {
		return tcpflag;
	}

	public static void setTcpflag(boolean tcpflag) {
		Chk_TcpFlag.tcpflag = tcpflag;
	}

}