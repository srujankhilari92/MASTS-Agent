package com.varutra.masts.proxy;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

public class Sniffer_Start_Stop {

	private SimpleDateFormat sdf;
	private String currentDateandTime;
	private String currentPcapFile;
	private SharedPreferences prefs;
	private Context context;
	private String comando;
	private DataOutputStream os;
	private BufferedReader bufferedreader;
	private String temp2;
	private Process process1;
	private Process process2;

	/**
	 */
	public Sniffer_Start_Stop(Context ctx) {
		// TODO Auto-generated constructor stub
		this.context = ctx;
		prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
	}

	/**
	 * Method Name: startLog
	 * 
	 * @param socket
	 *            Description:
	 */
	@SuppressLint("SimpleDateFormat")
	public String startTcp() {
		// TODO Auto-generated method stub
		try {
			if (new File("/sdcard/POC").exists()) {
				if (new File("/sdcard/POC/Tcp").exists()) {
					
				} else {
					boolean f = new File("/sdcard/POC/Tcp").mkdir();
					if (f)
						Log.e("Tcp Directory - ", "Created");
				}
			} else {
				boolean f = new File("/sdcard/POC").mkdir();
				if (f)
					
				if (new File("/sdcard/POC/Tcp").exists()) {
					
				} else {
					boolean f2 = new File("/sdcard/POC/Tcp").mkdir();
					if (f2)
						Log.e("Tcp Directory - ", "Created");
				}
			}

			ArrayList<String> pidList = this.checkRunning_LogProcess();

			if (pidList.size() == 0) {
				Log.e("No Process Running", "xxxxxxxxx");

				/**
				 * Get Current Date and Time Format.
				 */
				sdf = new SimpleDateFormat("ddMMyyyy_HHmmss");
				currentDateandTime = sdf.format(new Date());
				currentPcapFile = "tcp_" + currentDateandTime + ".cap";

				prefs.edit().putString("pcapFileName", currentPcapFile)
						.commit();
				prefs.edit()
						.putString("pcapFile",
								"/sdcard/POC/Tcp/" + currentPcapFile).commit();
																				
				Log.e("Current Created pcapFile.txt: ", "/sdcard/POC/Tcp/"
						+ currentPcapFile);

	
				comando = "/data/local/tcpdump-arm -l -w " + "/sdcard/POC/Tcp/"
						+ currentPcapFile + "\n";

				process1 = Runtime.getRuntime().exec(
						new String[] { "su", "-c", "system/bin/sh" });
				os = new DataOutputStream(process1.getOutputStream());
				os.writeBytes(comando + "\n");
				os.flush();
				os.writeBytes("exit\n");
				os.flush();
				os.close();

				Thread.sleep(1000);
				bufferedreader = new BufferedReader(new InputStreamReader(
						process1.getErrorStream()));
				String s = bufferedreader.readLine();

				comando = "ps tcpdump-arm";

				process2 = Runtime.getRuntime().exec(
						new String[] { "su", "-c", "system/bin/sh" });
				os = new DataOutputStream(process2.getOutputStream());
				os.writeBytes(comando + "\n");
				os.flush();
				os.writeBytes("exit\n");
				os.flush();
				os.close();

				bufferedreader = new BufferedReader(new InputStreamReader(
						process2.getInputStream()));

				String temp = null;
				temp = bufferedreader.readLine();
				
				temp = bufferedreader.readLine();
				

				String strRep = temp.trim().replaceAll("\\s{2,}", " ");
				String[] splitStr = strRep.split(" ");
				temp = splitStr[1].trim();
				
				prefs.edit().putInt("PID", Integer.parseInt(temp)).commit();

				if (temp != null) {
					try {
						bufferedreader.close();
						return "" + currentDateandTime;
					} catch (IOException ioexception) {
						ioexception.printStackTrace();
						return null;
					}
				} else {
					return null;
				}

			} else if (pidList.size() > 0) {
				String res = stopTcp();

								/**
				 * Get Current Date and Time Format.
				 */
				sdf = new SimpleDateFormat("ddMMyyyy_HHmmss");
				currentDateandTime = sdf.format(new Date());
				currentPcapFile = "tcp_" + currentDateandTime + ".cap";

				prefs.edit().putString("pcapFileName", currentPcapFile)
						.commit();
				prefs.edit()
						.putString("pcapFile",
								"/sdcard/POC/Tcp/" + currentPcapFile).commit();
																				
				comando = "/data/local/tcpdump-arm -l -w " + "/sdcard/POC/Tcp/"
						+ currentPcapFile + "\n";

				process1 = Runtime.getRuntime().exec(
						new String[] { "su", "-c", "system/bin/sh" });
				os = new DataOutputStream(process1.getOutputStream());
				os.writeBytes(comando + "\n");
				os.flush();
				os.writeBytes("exit\n");
				os.flush();
				os.close();

				Thread.sleep(1000);
				bufferedreader = new BufferedReader(new InputStreamReader(
						process1.getErrorStream()));
				String s = bufferedreader.readLine();

				comando = "ps tcpdump-arm";

				process2 = Runtime.getRuntime().exec(
						new String[] { "su", "-c", "system/bin/sh" });
				os = new DataOutputStream(process2.getOutputStream());
				os.writeBytes(comando + "\n");
				os.flush();
				os.writeBytes("exit\n");
				os.flush();
				os.close();

				bufferedreader = new BufferedReader(new InputStreamReader(
						process2.getInputStream()));

				String temp = null;
				temp = bufferedreader.readLine();
				temp = bufferedreader.readLine();
				
				String strRep = temp.trim().replaceAll("\\s{2,}", " ");
				String[] splitStr = strRep.split(" ");
				temp = splitStr[1].trim();
				
				prefs.edit().putInt("PID", Integer.parseInt(temp)).commit();

				if (temp != null) {
					try {
						bufferedreader.close();
						return "" + currentDateandTime;
					} catch (IOException ioexception) {
						ioexception.printStackTrace();
						return null;
					}
				} else {
					return null;
				}

			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			Log.d("->>>>>>", "Error While Running Tcpdump 245");
			return null;
		}
		Log.d("->>>>>>", "Error While Running Tcpdump 248");
		return null;

	}

	/**
	 * Method Name: stopLog
	 * 
	 * @param socket
	 *            Description:
	 */
	public String stopTcp() {
		// TODO Auto-generated method stub

		String retValue = null;
		try {
			ArrayList<String> pidList = this.checkRunning_LogProcess();
		
			if (pidList.size() >= 1) {
				
				for (String s : pidList) {
					comando = "kill " + s;


					Process suProcessLog3 = Runtime.getRuntime().exec(
							new String[] { "su", "-c", "system/bin/sh" });
					os = new DataOutputStream(suProcessLog3.getOutputStream());
					os.writeBytes(comando + "\n");
					os.flush();
					os.writeBytes("exit\n");
					os.flush();
					os.close();

					
				}
				pidList = this.checkRunning_LogProcess();
				if (pidList.size() >= 1)
					for (String s : pidList) {
						comando = "kill -KILL " + s;

						Process suProcessLog3 = Runtime.getRuntime().exec(
								new String[] { "su", "-c", "system/bin/sh" });
						os = new DataOutputStream(
								suProcessLog3.getOutputStream());
						os.writeBytes(comando + "\n");
						os.flush();
						os.writeBytes("exit\n");
						os.flush();
						os.close();
						
					}

				pidList = this.checkRunning_LogProcess();
				if (pidList.size() > 0)
					return null;
				else
					return "SUCCESS";
			} else {
				return "SUCCESS";
			}

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return retValue;
		}

	}

	public String checkLogProcess() {
		String result = "NOTNULL";
		try {

			comando = "ps tcpdump-arm";

			Process process4 = Runtime.getRuntime().exec(
					new String[] { "su", "-c", "system/bin/sh" });
			os = new DataOutputStream(process4.getOutputStream());
			os.writeBytes(comando + "\n");
			os.flush();
			os.writeBytes("exit\n");
			os.flush();
			os.close();

			bufferedreader = new BufferedReader(new InputStreamReader(
					process4.getInputStream()));

			String temp = bufferedreader.readLine();
			temp = bufferedreader.readLine();
			Log.e("Running Prodess ID", "" + temp);
			process4.destroy();


			if (temp == null)
				return null;
			else
				return result;
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return "EXC";
		}
	}

	public ArrayList<String> checkRunning_LogProcess() {
		ArrayList<String> pidList = new ArrayList<String>();

		try {

			comando = "ps tcpdump-arm";

			Process process4 = Runtime.getRuntime().exec(
					new String[] { "su", "-c", "system/bin/sh" });
			os = new DataOutputStream(process4.getOutputStream());
			os.writeBytes(comando + "\n");
			os.flush();
			os.writeBytes("exit\n");
			os.flush();
			os.close();

			bufferedreader = new BufferedReader(new InputStreamReader(
					process4.getInputStream()));

			String temp = null;
			temp = bufferedreader.readLine();
			Log.e("---->", temp);
			while ((temp = bufferedreader.readLine()) != null) {
				Log.e("Running Process - ", temp);

				String strRep = temp.trim().replaceAll("\\s{2,}", " ");
				String[] splitStr = strRep.split(" ");
				temp = splitStr[1].trim();
				pidList.add(temp);
			}

		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return pidList;
	}

}