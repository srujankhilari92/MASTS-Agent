/** 
 * @Class Name :  LogcatSetGet.java
 */

package com.varutra.masts.proxy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * @author : Varutra Consulting Pvt. Ltd.
 * @Create On : Aug 4, 2014 6:27:32 PM
 * @License : Copyright © 2014 Varutra Consulting Pvt. Ltd.- All rights
 *          reserved.
 */
public class LogcatSetGet {

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
	private String temp;
	private String temp2;
	private BufferedReader bufferedreader2;
	private Process process1;
	private Process process2;

	/**
	 */
	public LogcatSetGet(Context ctx) {
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
	public String startLog() {
		// TODO Auto-generated method stub
		try {
			if (new File("/sdcard/POC").exists()) {

				if (new File("/sdcard/POC/Logcat").exists()) {

				} else {
					boolean f = new File("/sdcard/POC/Logcat").mkdir();
					if (f)
						Log.e("Directory - ", "Created");
				}
			} else {
				boolean f = new File("/sdcard/POC").mkdir();
				if (f)
					
				if (new File("/sdcard/POC/Logcat").exists()) {
					Log.e("Logcat - Directory - ", "Exist");
				} else {
					boolean f2 = new File("/sdcard/POC/Logcat").mkdir();
					if (f2)
						Log.e("Logcat - Directory - ", "Created");
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
				currentLogFile = "logfile_" + currentDateandTime + ".txt";

				prefs.edit().putString("logFileName", currentLogFile).commit();
																				
				prefs.edit()
						.putString("logFile",
								"/sdcard/POC/Logcat/" + currentLogFile)
						.commit();

				Log.e("Current Created logcatFile.txt: ", "/sdcard/POC/Logcat/"
						+ currentLogFile);

				comando = "logcat -f /sdcard/POC/Logcat/" + currentLogFile;

				process1 = Runtime.getRuntime().exec(
						new String[] { "su", "-c", "system/bin/sh" });
				os = new DataOutputStream(process1.getOutputStream());
				os.writeBytes(comando + "\n");
				os.flush();
				os.writeBytes("exit\n");
				os.flush();
				os.close();
				Log.e("Command Executed Successfully: ", "" + comando);

				

				comando = "ps logcat";

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
				

				prefs.edit().putInt("PID", Integer.parseInt(temp));

				return "LogCat Monitoring Started " + currentDateandTime;

			} else if (pidList.size() > 0) {

				

				String res = stopLog();
				/**
				 * Get Current Date and Time Format.
				 */
				sdf = new SimpleDateFormat("ddMMyyyy_HHmmss");
				currentDateandTime = sdf.format(new Date());
				currentLogFile = "logfile_" + currentDateandTime + ".txt";

				prefs.edit().putString("logFileName", currentLogFile).commit();
				prefs.edit()
						.putString("logFile",
								"/sdcard/POC/Logcat/" + currentLogFile)
						.commit();

				Log.e("Current Created logcatFile.txt: ", "/sdcard/POC/Logcat/"
						+ currentLogFile);

				comando = "logcat -f /sdcard/POC/Logcat/" + currentLogFile;

				process1 = Runtime.getRuntime().exec(
						new String[] { "su", "-c", "system/bin/sh" });
				os = new DataOutputStream(process1.getOutputStream());
				os.writeBytes(comando + "\n");
				os.flush();
				os.writeBytes("exit\n");
				os.flush();
				os.close();
				

				comando = "ps logcat";

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
				
				prefs.edit().putInt("PID", Integer.parseInt(temp));

				return "LogCat Monitoring Started " + currentDateandTime;

			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return null;
		}
		return null;

	}

	public String stopLog() {
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
			return retValue = "EXC";
		}
	}

	public String checkLogProcess() {
		String result = "NOTNULL";
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
			
			comando = "ps logcat";

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
		
			while ((temp = bufferedreader.readLine()) != null) {
				

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