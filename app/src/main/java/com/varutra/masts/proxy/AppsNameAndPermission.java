package com.varutra.masts.proxy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import com.varutra.masts.proxy.Filename;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Browser;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * @author : Varutra Consulting Pvt. Ltd.
 * @Create On : May 23, 2014 11:10:46 AM
 * @License : Copyright 2014 Varutra Consulting Pvt. Ltd.- All rights
 *          reserved.
 */
public class AppsNameAndPermission {

	private String returnMessage;
	private StringBuffer appNameAndPermissions;
	PackageManager pm;
	ConnectivityManager cm;
	WifiManager wm;
	TelephonyManager tm;
	ActivityManager am;
	Context ctx;
	String path="";
	Thread T1, T2, T3, T4, T5, T6, T7;
	ArrayList<Thread> ThreadList;
	private ArrayList<String> list;
	private StringBuffer listString;
	private Process p;
	private DataOutputStream stdin;
	private BufferedReader bufferRD;
	private ArrayList All = new ArrayList();
	private ArrayList dblistreplace = new ArrayList();
	private ArrayList dblist = new ArrayList();
	private ArrayList databasesList = new ArrayList();
	

	/**
	 * @param pm2
	 */
	public AppsNameAndPermission(PackageManager pm2) {
		// TODO Auto-generated constructor stub
		this.pm = pm2;
	}

	public AppsNameAndPermission(Context c, PackageManager pm2,
			ConnectivityManager cm, WifiManager wm, TelephonyManager tm,
			ActivityManager am) {
		// TODO Auto-generated constructor stub
		this.ctx = c;
		this.pm = pm2;
		this.cm = cm;
		this.wm = wm;
		this.tm = tm;
		this.am = am;
	}

	String getAppNameAndPermission(Socket socket) {
		returnMessage = "";

		

		appNameAndPermissions = new StringBuffer();

		List<ApplicationInfo> packages = pm
				.getInstalledApplications(PackageManager.GET_META_DATA);

		for (ApplicationInfo applicationInfo : packages) {
		
			try {
				PackageInfo packageInfo = pm.getPackageInfo(
						applicationInfo.packageName,
						PackageManager.GET_PERMISSIONS);
				appNameAndPermissions.append(pm
						.getApplicationLabel(packageInfo.applicationInfo)
						+ "<br>" + packageInfo.packageName + ":<br>");
			
				String[] requestedPermissions = packageInfo.requestedPermissions;
				if (requestedPermissions != null) {
					for (int i = 0; i < requestedPermissions.length; i++) {
						Log.d("test", requestedPermissions[i]);
						appNameAndPermissions.append(requestedPermissions[i]
								+ "<br>");
					}
					appNameAndPermissions.append("<br>");
				}
			} catch (NameNotFoundException e) {
				//e.printStackTrace();
			} catch (Exception ex) {
				

			}

		}
		returnMessage = appNameAndPermissions + "";
		return returnMessage;

	}
	void ScanningAppInfo(Socket socket){
		

		returnMessage = "";

		ThreadList = new ArrayList<Thread>();

		List<ApplicationInfo> packages = pm
				.getInstalledApplications(PackageManager.GET_META_DATA);

		// TODO Auto-generated method stub
    	
		tm.getDeviceId();

		StringBuffer networkInfo = new StringBuffer();

		String phoneType = "";
		if (tm.getPhoneType() == 0) {
			phoneType = "NONE";
		} else if (tm.getPhoneType() == 1) {
			phoneType = "GSM";
		}
		if (tm.getPhoneType() == 2) {
			phoneType = "CDMA";
		}
		if (tm.getPhoneType() == 3) {
			phoneType = "SIP";
		}

		networkInfo.append("" + 
				"Os Version: "
				+ android.os.Build.VERSION.RELEASE
				+ "<br>"
				+ "Unique ID: "
				+ Secure.getString(this.ctx.getContentResolver(),
						Secure.ANDROID_ID)
				+ "<br>"
				+ 
				"Patches: " + android.os.Build.VERSION.SDK_INT + "<br>"
				+ "Operating System: " + tm.getSimOperatorName() + "<br>"
				+ "Sim Country: " + tm.getSimCountryIso() + "<br>"
				+ "Network Operator: " + tm.getNetworkOperatorName() + "<br>"
				+ "Phone Type: " + phoneType + "<br>" + "Sim Operator: "
				+ tm.getSimOperator() + "<br>" + "Sim Serial Num: "
				+ tm.getSimSerialNumber() + "<br>" + "Sim State: "
				+ tm.getSimState() + "<br>" + "Subsciber Id: "
				+ tm.getSubscriberId() + "<br>" + "Roaming Status: "
				+ tm.isNetworkRoaming() + "<br>");

		networkInfo.append("#1#2#3#4#5#6#7#8#9#0#");

		networkInfo.append("" + 
				"Name: " + android.os.Build.PRODUCT + "<br>" + "Device Type: "
				+ android.os.Build.MODEL + "<br>" + "CPU Type: "
				+ android.os.Build.CPU_ABI + "<br>" + "CPU Type2: "
				+ android.os.Build.CPU_ABI2 + "<br>" + "Manufacturer: "
				+ android.os.Build.MANUFACTURER + "<br>" + "Phone Number: "
				+ tm.getLine1Number() + "<br>" + "IMEI Number: "
				+ tm.getDeviceId() + "<br>" + "Device Name: "
				+ android.os.Build.MODEL + "<br>");

		networkInfo.append("#1#2#3#4#5#6#7#8#9#0#");

		NetworkInfo networks = cm.getActiveNetworkInfo();
		WifiInfo mWifiInfo = wm.getConnectionInfo();

		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
				.getDefaultAdapter();
		String bluetoothState = "";
		if (mBluetoothAdapter == null) {
			
			bluetoothState = "Device Does not Support BT";
		} else {
			if (!mBluetoothAdapter.isEnabled()) {
			
				bluetoothState = "BT is not Enable.";
			} else if (mBluetoothAdapter.isEnabled()) {
				bluetoothState = "BT is Enable.";
			}
		}

		networkInfo.append("Network State: " + networks.getState() + "<br>"
				+ "Network Type: " + networks.getType() + "<br>"
				+ "Network Subtype: " + networks.getSubtype() + "<br>"
				+ "IP Address: " + getLocalIpAddress() + "<br>" + "Host Name: "
				+ getLocalHostName() + "<br>" + "Host Address: "
				+ getLocalIpAddress() + "<br>" +

				"Wifi Ip Address: " + intToIP(mWifiInfo.getIpAddress())
				+ "<br>" + "Wifi Mac Address: " + mWifiInfo.getMacAddress()
				+ "<br>" + "Wifi SSID: " + mWifiInfo.getSSID() + "<br>"
				+ "Wifi BSSID: " + mWifiInfo.getBSSID() + "<br>"
				+ "Bluetooth Status: " + bluetoothState + "<br>");

		networkInfo.append("#1#2#3#4#5#6#7#8#9#0#");


		for (Thread T : ThreadList) {
			try {
				T.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}
		returnMessage = Auth.getTokn() + "#" + networkInfo + "#1#2#3#4#5#6#7#8#9#0#";

		Send_Result_Message_TO_Client(socket, returnMessage);
        
		
	
	}
	void CompletSnapShot(Socket socket) {
		returnMessage = "";

		StringBuffer appNameAndPermissions = new StringBuffer();
		ThreadList = new ArrayList<Thread>();

		List<ApplicationInfo> packages = pm
				.getInstalledApplications(PackageManager.GET_META_DATA);

	
		for (ApplicationInfo applicationInfo : packages) {
		
			try {
				PackageInfo packageInfo = pm.getPackageInfo(
						applicationInfo.packageName,
						PackageManager.GET_PERMISSIONS);
				appNameAndPermissions.append(pm
						.getApplicationLabel(packageInfo.applicationInfo)
						+ "<br>" + packageInfo.packageName + ":<br>");
				
				String[] requestedPermissions = packageInfo.requestedPermissions;
				if (requestedPermissions != null) {
					for (int i = 0; i < requestedPermissions.length; i++) {
				
						appNameAndPermissions.append(requestedPermissions[i]
								+ "<br>");
					}
					appNameAndPermissions.append("<br>");
				}
			} catch (NameNotFoundException e) {
				//e.printStackTrace();
			} catch (Exception ex) {
				

			}

		}
		appNameAndPermissions.append("#1#2#3#4#5#6#7#8#9#0#");

		// TODO Auto-generated method stub
    	
		tm.getDeviceId();

		StringBuffer networkInfo = new StringBuffer();

		String phoneType = "";
		if (tm.getPhoneType() == 0) {
			phoneType = "NONE";
		} else if (tm.getPhoneType() == 1) {
			phoneType = "GSM";
		}
		if (tm.getPhoneType() == 2) {
			phoneType = "CDMA";
		}
		if (tm.getPhoneType() == 3) {
			phoneType = "SIP";
		}

		networkInfo.append("" + 
				"Os Version: "
				+ android.os.Build.VERSION.RELEASE
				+ "<br>"
				+ "Unique ID: "
				+ Secure.getString(this.ctx.getContentResolver(),
						Secure.ANDROID_ID)
				+ "<br>"
				+ 
				"Patches: " + android.os.Build.VERSION.SDK_INT + "<br>"
				+ "Operating System: " + tm.getSimOperatorName() + "<br>"
				+ "Sim Country: " + tm.getSimCountryIso() + "<br>"
				+ "Network Operator: " + tm.getNetworkOperatorName() + "<br>"
				+ "Phone Type: " + phoneType + "<br>" + "Sim Operator: "
				+ tm.getSimOperator() + "<br>" + "Sim Serial Num: "
				+ tm.getSimSerialNumber() + "<br>" + "Sim State: "
				+ tm.getSimState() + "<br>" + "Subsciber Id: "
				+ tm.getSubscriberId() + "<br>" + "Roaming Status: "
				+ tm.isNetworkRoaming() + "<br>");

		networkInfo.append("#1#2#3#4#5#6#7#8#9#0#");

		networkInfo.append("" + 
				"Name: " + android.os.Build.PRODUCT + "<br>" + "Device Type: "
				+ android.os.Build.MODEL + "<br>" + "CPU Type: "
				+ android.os.Build.CPU_ABI + "<br>" + "CPU Type2: "
				+ android.os.Build.CPU_ABI2 + "<br>" + "Manufacturer: "
				+ android.os.Build.MANUFACTURER + "<br>" + "Phone Number: "
				+ tm.getLine1Number() + "<br>" + "IMEI Number: "
				+ tm.getDeviceId() + "<br>" + "Device Name: "
				+ android.os.Build.MODEL + "<br>");

		networkInfo.append("#1#2#3#4#5#6#7#8#9#0#");

		NetworkInfo networks = cm.getActiveNetworkInfo();
		WifiInfo mWifiInfo = wm.getConnectionInfo();

		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
				.getDefaultAdapter();
		String bluetoothState = "";
		if (mBluetoothAdapter == null) {
			
			bluetoothState = "Device Does not Support BT";
		} else {
			if (!mBluetoothAdapter.isEnabled()) {
			
				bluetoothState = "BT is not Enable.";
			} else if (mBluetoothAdapter.isEnabled()) {
				bluetoothState = "BT is Enable.";
			}
		}

		networkInfo.append("Network State: " + networks.getState() + "<br>"
				+ "Network Type: " + networks.getType() + "<br>"
				+ "Network Subtype: " + networks.getSubtype() + "<br>"
				+ "IP Address: " + getLocalIpAddress() + "<br>" + "Host Name: "
				+ getLocalHostName() + "<br>" + "Host Address: "
				+ getLocalIpAddress() + "<br>" +

				"Wifi Ip Address: " + intToIP(mWifiInfo.getIpAddress())
				+ "<br>" + "Wifi Mac Address: " + mWifiInfo.getMacAddress()
				+ "<br>" + "Wifi SSID: " + mWifiInfo.getSSID() + "<br>"
				+ "Wifi BSSID: " + mWifiInfo.getBSSID() + "<br>"
				+ "Bluetooth Status: " + bluetoothState + "<br>");

		networkInfo.append("#1#2#3#4#5#6#7#8#9#0#");

	
		
		StringBuffer nameAndPackages = new StringBuffer();

		for (ApplicationInfo applicationInfos : packages) {
		
			try {
				PackageInfo packageInfo = pm.getPackageInfo(
						applicationInfos.packageName,
						PackageManager.GET_PERMISSIONS);
				nameAndPackages.append(pm
						.getApplicationLabel(packageInfo.applicationInfo)
						+ "<br>");

			} catch (NameNotFoundException e) {
				//e.printStackTrace();
			} catch (Exception ex) {
	

			}

		}// end of For

		StringBuffer browserHistory = new StringBuffer();

		browserHistory.append("#1#2#3#4#5#6#7#8#9#0#" + "");
															
		String[] proj = new String[] { Browser.BookmarkColumns.TITLE,
				Browser.BookmarkColumns.URL };
		String sel = Browser.BookmarkColumns.BOOKMARK + " = 0";
																
		Cursor mCur = ctx.getContentResolver().query(Browser.BOOKMARKS_URI,
				proj, sel, null, null);
		mCur.moveToFirst();
		String title = "";
		String url = "";
		if (mCur.moveToFirst() && mCur.getCount() > 0) {
			boolean cont = true;
			while (mCur.isAfterLast() == false && cont) {
				title = mCur.getString(mCur
						.getColumnIndex(Browser.BookmarkColumns.TITLE));
				url = mCur.getString(mCur
						.getColumnIndex(Browser.BookmarkColumns.URL));
				
				browserHistory.append("Browser Title : " + title + "<br>" + url
						+ "<br>");
				mCur.moveToNext();
			}
		}


		StringBuffer browserNames = new StringBuffer();
		browserNames.append("");
		ArrayList<String> allLaunchers = new ArrayList<String>();

		Intent allApps = new Intent(Intent.ACTION_MAIN);
		List<ResolveInfo> allAppList = pm.queryIntentActivities(allApps, 0);
		for (int i = 0; i < allAppList.size(); i++)
			allLaunchers.add(allAppList.get(i).activityInfo.packageName);

		Intent myApps = new Intent(Intent.ACTION_VIEW);
		myApps.setData(Uri.parse("http://www.varutra.com"));
		List<ResolveInfo> myAppList = pm.queryIntentActivities(myApps, 0);
		for (int i = 0; i < myAppList.size(); i++) {
			if (allLaunchers
					.contains(myAppList.get(i).activityInfo.packageName)) {
				browserNames.append(myAppList.get(i).activityInfo.packageName
						+ "<br>");
				
			}
		}

		for (Thread T : ThreadList) {
			try {
				T.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}

		   //updated snapshot
		    getAllList("/data/data");
			getDbList(dblistreplace);
			getFeatureList(dblist);
			
		String data_data = getDatabaseList(databasesList);
		
		returnMessage = Auth.getTokn() + "#" + networkInfo +"" + appNameAndPermissions + ""
				+ browserNames + browserHistory + "#1#2#3#4#5#6#7#8#9#0#"
				+ data_data;

		All.clear();
        dblistreplace.clear();
        dblist.clear();
        databasesList.clear();
		Send_Result_Message_TO_Client(socket, returnMessage);
        
		
	}
	   private String getDatabaseList(ArrayList lis){
	    	
	    	for(int i=0; i<lis.size();i++){
	    		String[] splited = lis.get(i).toString().split("\\s+");
	    	    for(int j=0;j<splited.length;j++){
	    	    	if(j==0){

	    	    		String filename = splited[j].substring(splited[j].lastIndexOf("/")+1, splited[j].length());
	    	    		listString.append("#$#File:"+filename);
	    	    		listString.append("#$#Path:"+splited[j]);
	    	    		
	    	    				
	    	    	}else if(j==1){
	    	    	
	    	    		listString.append("#$#Permission:"+splited[j]);
	    	    		
	    	    	}else if(j==4){
	    	    		
	    	    		listString.append("#$#Size:"+splited[j]);
	    	    		
	    	    	}else{
	    
	    	    	}
	    			
	    	    }
	    	    
	    	    listString.append("#####");
	    	}
	    	
	     
	    	return listString+" ";
	    }
	    private void getDbList(ArrayList lis){

	    		for(int i=0; i<lis.size(); i++){
	        		
	        		String str = lis.get(i).toString().replace(":", "/");
	        		dblist.add(str);
	        	}
	    	
	    }

	    private void getFeatureList(ArrayList lis){

			for (int i = 0; i < lis.size(); i++) {

				try {

					listString = new StringBuffer();				
					p = Runtime.getRuntime().exec(
							new String[] { "su", "-c", "system/bin/sh" });      
					stdin = new DataOutputStream(p.getOutputStream());
					stdin.writeBytes("ls -l " + lis.get(i).toString() + "\n");
					stdin.flush();
					stdin.writeBytes("exit\n");
					stdin.flush();

					bufferRD = new BufferedReader(new InputStreamReader(
							p.getInputStream()));
					String line = null;

					while ((line = bufferRD.readLine()) != null) {

						databasesList.add(lis.get(i).toString() + " "
								+ line.toString());
						Log.e("databasesList", "databasesList : " + "Path:"
								+ lis.get(i).toString() + "Permission and size:"
								+ line.toString());
						Thread.sleep(500);
					}
				} catch (Exception e) {
					//e.printStackTrace();
				}

			}

		}
	    
	    private void getAllList(String directory){

			try {
				if (new File(directory).exists()
						&& new File(directory).isDirectory()) {

					listString = new StringBuffer();
					p = Runtime.getRuntime().exec(
							new String[] { "su", "-c", "system/bin/sh" });
					stdin = new DataOutputStream(p.getOutputStream());
					stdin.writeBytes("ls -R " + directory + "\n");
					stdin.flush();
					stdin.writeBytes("exit\n");
					stdin.flush();

					bufferRD = new BufferedReader(new InputStreamReader(
							p.getInputStream()));
					String line = null;

					while ((line = bufferRD.readLine()) != null) {
						All.add(line.toString());
						
					}

					for (int i = 0; i < All.size(); i++) {

						
						Filename myHomePage = new Filename(All.get(i).toString(),
								'/', '.');

						if (myHomePage.extension().equals("db")|| myHomePage.extension().equals("xml") || myHomePage.extension().equals("log")) {
							
								
							String str = All.get(i-1).toString().replace(":", "");
							
							
							
							p = Runtime.getRuntime().exec(
									new String[] { "su", "-c", "system/bin/sh" });
							stdin = new DataOutputStream(p.getOutputStream());
							stdin.writeBytes("ls -R " + str + "\n");
							stdin.flush();
							stdin.writeBytes("exit\n");
							stdin.flush();

							bufferRD = new BufferedReader(new InputStreamReader(
									p.getInputStream()));
							String line1 = null;

							while ((line1 = bufferRD.readLine()) != null) {
								
								Log.e("", "Check: "+line1.toString());
								
								Filename myHomePage1 = new Filename(line1.toString(),
										'/', '.');
								if(myHomePage1.extension().equals("db")|| myHomePage1.extension().equals("xml") || myHomePage1.extension().equals("log")) {
								
									dblistreplace.add(path+ line1.toString());
									
								}else{
									
									if(line1.toString().contains(":"))
									{
										path=line1.toString();
									}else{
										
										
									}
								  
								}
								Thread.sleep(500);		
							}
							
							Thread.sleep(100);
					
						} else {
		
						}

					}

				} else {

		
				}
			} catch (NullPointerException e) {
			
				//e.printStackTrace();
			} catch (Exception ex) {
				ex.printStackTrace();
			}

		}

	/**
	 * Method Name: GetData_dataFiles
	 * 
	 * @return Description:
	 */
	private String GetData_dataFiles(String directory) {
		// TODO Auto-generated method stub
		
		try {
			if (new File(directory).exists()
					&& new File(directory).isDirectory()) {

				listString = new StringBuffer();

				p = Runtime.getRuntime().exec(
						new String[] { "su", "-c", "system/bin/sh" });
				stdin = new DataOutputStream(p.getOutputStream());

				
				stdin.writeBytes("ls -R " + directory + "\n"); 
																
				stdin.flush();
				stdin.writeBytes("exit\n"); 
				stdin.flush();

				bufferRD = new BufferedReader(new InputStreamReader(
						p.getInputStream()));
				String line = null;

				while ((line = bufferRD.readLine()) != null) {
					listString.append(line.toString() + "<br>");

				}
			} else {
				
			}
		} catch (NullPointerException e) {
			// TODO: handle exception
			//e.printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return listString + "";
	}

	void Send_Result_Message_TO_Client(Socket socket, String msg) {
		/**
		 * If Invalid Command Received from Client then Error Message the
		 * response back to the client.
		 */
		try {
			socket.setSendBufferSize(10000);
			socket.setTcpNoDelay(true);

			OutputStream os = socket.getOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(os);
			BufferedWriter bw = new BufferedWriter(osw);
			bw.write(msg);
			os.flush();
			bw.flush();
			socket.close();
			Log.e("Result Message: ", "Send Complet");
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

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
			bw.write(msg);
			os.flush();
			bw.flush();
			socket.close();

			Log.e("Error Message: ", "Send Complet");
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	@SuppressWarnings("static-access")
	public String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isSiteLocalAddress()) {
						return inetAddress.getLocalHost().getHostAddress();
					}
				}
			}
		} catch (Exception ex) {

		}
		return null;
	}

	@SuppressWarnings("static-access")
	public String getLocalHostName() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isSiteLocalAddress()) {
						return inetAddress.getLocalHost().getHostName();
					}
				}
			}
		} catch (Exception ex) {
		
		}
		return null;
	}

	public String intToIP(int i) {
		return ((i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
				+ "." + ((i >> 24) & 0xFF));
	}
}