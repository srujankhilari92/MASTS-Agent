/** 
 * @Class Name :  SnifferSS.java
 */

package com.varutra.masts.proxy;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * @author : Varutra Consulting Pvt. Ltd.
 * @Create On : Oct 18, 2014 5:49:16 PM
 * @License : Copyright © 2014 Varutra Consulting Pvt. Ltd.- All rights
 *          reserved.
 */

public class SnifferSS {

	private SimpleDateFormat sdf;
	private String currentDateandTime;
	private String currentLogFile;
	private SharedPreferences prefs;
	private Context context;
	private String comando;
	private static Process suProcessLog;
	private DataOutputStream os;
	private BufferedReader bufferedreader;
	private String s;
	private DataInputStream datainputstream;
	private String temp, temp2;
	private String currentPack;
	private Process suProcess;
	private boolean retval;
	private File file;

	/**
	 */
	public SnifferSS(Context ctx) {
		// TODO Auto-generated constructor stub
		this.context = ctx;
		prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
	}

	/**
	 * Method Name: startSniffer
	 * 
	 * @param socket
	 *            Description:
	 */
	@SuppressLint("SimpleDateFormat")
	public void startSniffer(Socket socket) throws InterruptedException {
		// TODO Auto-generated method stub
		try {
			Log.e("TCPDumpChecking() ", "welcome");
			/**
			 * Get Current Date and Time Format.
			 */
			sdf = new SimpleDateFormat("ddMMyyyy_HHmmss");
			currentDateandTime = sdf.format(new Date());
			currentPack = "output_" + currentDateandTime + ".pcap";
			prefs.edit().putString("pcapFile", currentPack).commit();

			Log.e("Starting TcpDump : ", "" + currentPack);

			comando = "/data/local/tcpdump-arm -l -w /data/local/"
					+ currentPack + "\n";

			suProcessLog = Runtime.getRuntime().exec(
					new String[] { "su", "-c", "system/bin/sh" });
			os = new DataOutputStream(suProcessLog.getOutputStream());
			os.writeBytes(comando + "\n");
			os.flush();
			os.writeBytes("exit\n");
			os.flush();
			os.close();

			Thread.sleep(1000);

			bufferedreader = new BufferedReader(new InputStreamReader(
					suProcessLog.getErrorStream()));

			s = bufferedreader.readLine();
			
			if (s == null) {
				try {

					bufferedreader.close();

					Send_Return_Message_TO_Client(socket,
							"Sniffer is Not Started");

				} catch (IOException ioexception) {
					Log.d("->>>>>>", "Error While Changing Mode");
					ioexception.printStackTrace();
				}

			} else {
				Process process2 = Runtime.getRuntime().exec("ps tcpdump-arm");
				// read the output of ps
				bufferedreader = new BufferedReader(new InputStreamReader(
						process2.getInputStream()));
				temp = bufferedreader.readLine();
				temp = bufferedreader.readLine();

				String strRep = temp.trim().replaceAll("\\s{2,}", " ");
				String[] splitStr = strRep.split(" ");
				temp = splitStr[1].trim();

				int pid = Integer.parseInt(temp);

				process2.destroy();
				bufferedreader.close();
				prefs.edit().putString("processPID", pid + "".trim()).commit();

				Send_Return_Message_TO_Client(socket, "Sniffer is Started");
				System.out.println("Sniffer is Started");

			}

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Send_Return_Message_TO_Client(socket,
					"SnifferStarted Thread InterruptedException Exception 153");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Send_Return_Message_TO_Client(socket,
					"SnifferStarted Monitoring Started Exception 158");

		}

	}

	/**
	 * Method Name: stopSniffer
	 * 
	 * @param socket
	 *            Description:
	 */
	public String stopSniffer(Socket socket) {
		// TODO Auto-generated method stub

		String retValue = null;

		try {
			Thread.sleep(1000);

			comando = "ps tcpdump-arm";
			Log.e("Check Process ID", "1");
			Process suProcessLog2 = Runtime.getRuntime().exec(
					new String[] { "su", "-c", "system/bin/sh" });
			os = new DataOutputStream(suProcessLog2.getOutputStream());
			os.writeBytes(comando + "\n");
			os.flush();
			os.writeBytes("exit\n");
			os.flush();
			os.close();

			datainputstream = new DataInputStream(
					suProcessLog2.getInputStream());
			temp = datainputstream.readLine();

			temp = datainputstream.readLine();
			suProcessLog2.destroy();
			if (temp == null) {

				retValue = null;
				return retValue;
			} else {
				String strRep = temp.trim().replaceAll("\\s{2,}", " ");
				String[] splitStr = strRep.split(" ");
				temp = splitStr[1].trim();


				comando = "kill -KILL " + temp;

				Process suProcessLog3 = Runtime.getRuntime().exec(
						new String[] { "su", "-c", "system/bin/sh" });
				os = new DataOutputStream(suProcessLog3.getOutputStream());
				os.writeBytes(comando + "\n");
				os.flush();
				os.writeBytes("exit\n");
				os.flush();
				os.close();

				BufferedReader bufferedreader = new BufferedReader(
						new InputStreamReader(suProcessLog3.getErrorStream()));
				String line = bufferedreader.readLine();
				if (line == null) {
					try {
						bufferedreader.close();
					} catch (IOException ioexception) {
						ioexception.printStackTrace();
					}
				}
				suProcessLog3.destroy();

				comando = "ps tcpdump-arm";

				Process suProcessLog4 = Runtime.getRuntime().exec(
						new String[] { "su", "-c", "system/bin/sh" });
				os = new DataOutputStream(suProcessLog4.getOutputStream());
				os.writeBytes(comando + "\n");
				os.flush();
				os.writeBytes("exit\n");
				os.flush();
				os.close();

				datainputstream = new DataInputStream(
						suProcessLog4.getInputStream());
				temp2 = datainputstream.readLine();

				temp2 = datainputstream.readLine();

				suProcessLog4.destroy();
				if (temp2 != null) {
					stopSniffer(socket);
				} else {
					retValue = null;
					return retValue;
				}

			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return retValue = "EXC";
	
		}
		return retValue;
	}

	public String checkLogProcess() {
		String result = null;
		try {
			comando = "ps logcat";

			Process process4 = Runtime.getRuntime().exec(
					new String[] { "su", "-c", "system/bin/sh" });
			os = new DataOutputStream(process4.getOutputStream());
			os.writeBytes(comando + "\n");
			os.flush();
			os.writeBytes("exit\n");
			os.flush();
			os.close();
			datainputstream = new DataInputStream(process4.getInputStream());
			process4.destroy();
			String temp3 = datainputstream.readLine();

			temp3 = datainputstream.readLine();
	

			if (temp3 == null)
				return null;
		} catch (Exception e) {
			// TODO: handle exception
			return "EXC";
		}
		return result;
	}

	/**
	 * Method Name: stopLogKILL
	 * 
	 * @param socket
	 *            Description:
	 */
	public void stopLogKILL(Socket socket) {
		// TODO Auto-generated method stub
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			comando = "ps logcat";

			Process process1 = Runtime.getRuntime().exec(
					new String[] { "su", "-c", "system/bin/sh" });
			os = new DataOutputStream(process1.getOutputStream());
			os.writeBytes(comando + "\n");
			os.flush();
			os.writeBytes("exit\n");
			os.flush();
			os.close();
			datainputstream = new DataInputStream(process1.getInputStream());
			process1.destroy();
			String temp = datainputstream.readLine();
			
			temp = datainputstream.readLine();
			temp = temp.replaceAll("^root *([0-9]*).*", "$1");

			if (temp == null) {
				Send_Return_Message_TO_Client(socket,
						"LogCat Stop No Log Process Found");
			} else {


				comando = "kill -KILL " + temp;

				Process process2 = Runtime.getRuntime().exec(
						new String[] { "su", "-c", "system/bin/sh" });
				os = new DataOutputStream(process2.getOutputStream());
				os.writeBytes(comando + "\n");
				os.flush();
				os.writeBytes("exit\n");
				os.flush();
				os.close();

				BufferedReader bufferedreader = new BufferedReader(
						new InputStreamReader(process2.getErrorStream()));
				process2.destroy();
				String line = bufferedreader.readLine();
				if (line == null) {
					try {
						bufferedreader.close();

					} catch (IOException ioexception) {
						ioexception.printStackTrace();
					}
				}

				comando = "ps logcat";

				Process process3 = Runtime.getRuntime().exec(
						new String[] { "su", "-c", "system/bin/sh" });
				os = new DataOutputStream(process3.getOutputStream());
				os.writeBytes(comando + "\n");
				os.flush();
				os.writeBytes("exit\n");
				os.flush();
				os.close();
				datainputstream = new DataInputStream(process3.getInputStream());
				process3.destroy();
				String temp2 = datainputstream.readLine();
				Log.e("readLine()", "" + temp2);

				temp2 = datainputstream.readLine();
				temp2 = temp2.replaceAll("^root *([0-9]*).*", "$1");
				if (temp2 != null) {
					stopSniffer(socket);
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			Send_Return_Message_TO_Client(socket, "Exception in LogStop");
		}
	}

	void TcpExport(Socket socket) {

		String fileName = prefs.getString("pcapFile", "").trim();
		if (!fileName.isEmpty()) {
			try {
				suProcess = Runtime.getRuntime().exec("su");

				comando = ("chmod 777 /data/local/" + fileName);
				os = new DataOutputStream(suProcess.getOutputStream());
				os.writeBytes(comando + "\n");
				os.flush();
				os.writeBytes("exit\n");
				os.flush();
				try {
					if (255 == suProcess.waitFor()) {
						retval = true;
					} else {
						retval = false;
			
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
				file = new File("/data/local/" + fileName);

				if (!file.exists()) {
					Log.e("File Not Exist", "----->");
				} else {
					Log.e("File Exist", "----->");
				}
				System.out.println("Connecting...");

				int count;
				byte[] buffer = new byte[1024];

		
				socket.setTcpNoDelay(true);
				OutputStream out = socket.getOutputStream();
				BufferedInputStream in = new BufferedInputStream(
						new FileInputStream(file));
				while ((count = in.read(buffer)) >= 0) {
					out.write(buffer, 0, count);
					out.flush();
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				socket.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			Send_Return_Message_TO_Client(socket, " .pcap file not available");
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

			Log.e("Error Message: ", "Send Complet");
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