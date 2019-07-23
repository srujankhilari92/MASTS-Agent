/** 
 * @Class Name :  StartCapturing.java
 */

package com.varutra.masts.proxy;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * @author : Varutra Consulting Pvt. Ltd.
 * @Create On : Jul 10, 2014 12:38:20 PM
 * @License : Copyright © 2014 Varutra Consulting Pvt. Ltd.- All rights
 *          reserved.
 */
public class StartCapturing {

	private SharedPreferences prefs;
	Context context;
	private SimpleDateFormat sdf;
	private String currentDateandTime;
	private String currentScreen;
	private Process sh;
	private DataOutputStream os;
	private BufferedReader bufferRD;
	private String s;
	private String result;

	/**
	 * 
	 */
	public StartCapturing(Context ctx) {
		// TODO Auto-generated constructor stub

		this.context = ctx;
	}

	public void StartSending(Socket socket) {
		// TODO Auto-generated method stub

		try {
			Socket pingSocket = new Socket();
			SocketAddress address = new InetSocketAddress(prefs.getString("IP",
					""), Integer.parseInt(Get_SetIP.getPORT()) + 1);
			pingSocket.connect(address, 10000);
			Get_SetIP.setStrmPORT((Integer.parseInt(Get_SetIP.getPORT()) + 1)
					+ "".trim());
			result = "SUCCESS";

			Send_Return_Message_TO_Client(socket, "PING SUCCESSFULL");
			pingSocket.close();

		} catch (UnknownHostException e) {
			Send_Return_Message_TO_Client(socket, "WRONG ADDRESS");
			result = "WRONG ADDRESS";
		} catch (SocketTimeoutException e) {
			result = "TIMEOUT";
			Send_Return_Message_TO_Client(socket, "TIMEOUT");

		} catch (IOException e) {
			result = "CLOSED";
			Send_Return_Message_TO_Client(socket, "CLOSED");
		}

		try {

			while (prefs.getBoolean("liveStreamingBool", false) == true) {
				// TODO Auto-generated method stub

				sdf = new SimpleDateFormat("ddMMyyyy_HHmmss");
				currentDateandTime = sdf.format(new Date());
				currentScreen = "screen_" + currentDateandTime + ".png";

				sh = Runtime.getRuntime().exec("su", null, null);
				String comando = "/system/bin/screencap -p "
						+ "/sdcard/screens/" + currentScreen + " \n";

				os = new DataOutputStream(sh.getOutputStream());

				os.writeBytes(comando.getBytes("ASCII") + "\n");
				os.flush();
				os.writeBytes("exit\n");
				os.flush();
				os.writeBytes("exit\n");
				os.flush();
				os.close();

				int i = sh.waitFor();

				bufferRD = new BufferedReader(new InputStreamReader(
						sh.getErrorStream()));

				s = bufferRD.readLine();
				if (s == null) {
					try {

						bufferRD.close();
					} catch (IOException ioexception) {
						ioexception.printStackTrace();
					}

				}
				sh.destroy();

				String host = prefs.getString("IP", "");
				int port = Integer.parseInt(Get_SetIP.strmPORT);
				InetAddress address = InetAddress.getByName(host);

				socket = new Socket(address, port);

				try {
					File myFile = new File("/sdcard/screens/" + currentScreen);
					InputStream newFile = new FileInputStream(myFile);
					BufferedInputStream in = new BufferedInputStream(newFile);

					byte[] buffer = new byte[3000000];

					OutputStream out1 = socket.getOutputStream();
					int count;
					while ((count = in.read(buffer)) >= 0) {
						System.out.println(buffer.toString());
						out1.write(buffer, 0, count);
						out1.flush();
					}

					socket.close();
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					Thread.sleep(1000);
					e.printStackTrace();
				} catch (IOException e) {
					Thread.sleep(1000);
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			Send_Error_Message_TO_Client(socket,
					"Exception in StartSending 195");
		}

	}

	@SuppressLint("NewApi")
	void Send_Error_Message_TO_Client(Socket socket, String msg) {
		/**
		 * If Invalid Command Received from Client then Error Message the
		 * response back to the client.
		 */
		try {

			socket.setSendBufferSize(1000);
			socket.setTcpNoDelay(true);

			OutputStream os = socket.getOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(os);
			BufferedWriter bw = new BufferedWriter(osw);
			bw.write(Auth.getTokn() + "#" + msg);
			os.flush();
			bw.flush();
			socket.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	void Send_Return_Message_TO_Client(Socket socket, String msg) {
		/**
		 * If Invalid Command Received from Client then Error Message the
		 * response back to the client.
		 */
		try {
			socket.setSendBufferSize(1000);
			socket.setTcpNoDelay(true);

			OutputStream os = socket.getOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(os);
			BufferedWriter bw = new BufferedWriter(osw);
			bw.write(Auth.getTokn() + "#" + msg);
			os.flush();
			bw.flush();
			socket.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}