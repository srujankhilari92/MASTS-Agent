package com.varutra.masts.proxy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.varutra.plugin.gui.Proxy_main_activity;
import com.varutra.plugin.gui.Proxy_main_activity.ClientThread;
import com.varutra.plugin.gui.Proxy_main_activity.DeleteFilesThread;
import com.varutra.proxy.plugin.CustomPlugin;
import com.varutra.utils.network.ClientResolver;
import com.varutra.webscarab.model.Preferences;
import com.varutra.webscarab.model.StoreException;
import com.varutra.webscarab.plugin.Framework;
import com.varutra.webscarab.plugin.proxy.IClientResolver;
import com.varutra.webscarab.plugin.proxy.Proxy;
import com.varutra.webscarab.plugin.proxy.ProxyPlugin;
import com.varutra.webscarab.store.sql.SqlLiteStore;
import com.varutra.masts.proxy.R;
import com.varutraproxy.utils.NetworkHostNameResolver;
import com.varutraproxy.utils.PreferenceUtils;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

@SuppressLint("ShowToast")
public class MyService extends Service {

	private static final String TAG = "MyService";
	private StartServer startServer;
	public int port = 50000;
	public ServerSocket serverSocket;
	public Socket socket;
	public SharedPreferences prefs;
	public ListFiles listDirs;
	private SnifferStartStop tcpSS;
	private NotificationManager mNM;
	private int NOTIFICATION = R.string.local_service_started;

	public static String SOURCE_FOLDER;

	public String OUTPUT_ZIP_FILE = "";

	@SuppressLint("SdCardPath")
	public String SOURCE_FOLDER_TWO = "/sdcard/POC/";

	public AES aes;
	private PowerManager mgr;
	private WakeLock wakeLock;
	// Proxy_main_activity pro;
	Logout_server logout;
	String logoutop = "";
	private TextView alertBoxTV;

	public static String ListenerStatus = "";
	int preport,postport;
	public static String serverip="";
	public static String serverport="";
	NetworkHostNameResolver networkHostNameResolver = null;
	IClientResolver clientResolver = null;
	public SharedPreferences pref;
	private static Framework framework = null;
	public static boolean proxyStarted = false;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.edit().putBoolean("sendingFile", false).commit();
		listDirs = new ListFiles();
		// pro = new Proxy_main_activity();

		int mastsagentid = android.os.Process.myPid();
		Log.e("MASTS Agent MyService oncrate process id:", "id-"+mastsagentid);
		logout = new Logout_server();
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			// TODO Auto-generated catch block

		}

		startServer = new StartServer(this);
		startServer.execute(new String[] {});

		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		showNotification();

		mgr = (PowerManager) this.getSystemService(MyService.POWER_SERVICE);
		wakeLock = mgr
				.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
		wakeLock.acquire();
		prefs.edit().putBoolean("WAKEFLAG", true).commit();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub

		return START_STICKY;
	}

	@Override
	public void onStart(Intent intent, int startId) {


	}

	@Override
	public void onDestroy() {
		Toast.makeText(this, "MASTS Agent Service terminated!",
				Toast.LENGTH_LONG).show();


		try {
			new BeforDestroy();

			startServer.cancel(true);
			serverSocket.close();

			if (wakeLock.isHeld()) {
				wakeLock.release();
				prefs.edit().putBoolean("WAKEFLAG", false).commit();
			}


		} catch (IOException e) {
			// TODO Auto-generated catch block

		}


		mNM.cancel(NOTIFICATION);

		Toast.makeText(this, R.string.local_service_stop, Toast.LENGTH_SHORT)
		.show();

	}

	@SuppressWarnings("deprecation")
	private void showNotification() {
		getText(R.string.local_service_started);

		Notification notification = new Notification(
				R.drawable.mastlogonotification,
				"MASTS Agent Service started!", System.currentTimeMillis());

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, MainActivity.class), 0);

		notification.setLatestEventInfo(this,
				getText(R.string.local_service_started),
				"MASTS Agent Service is running!", contentIntent);

		mNM.notify(NOTIFICATION, notification);
	}

	class StartServer extends AsyncTask<String, Void, String> {

		Context context;
		public String returnMessage;
		byte[] mybytearray;
		public int pid;
		public Process p;
		private ListFiles listfile;
		public BufferedReader br;
		private StartCapturing monitorService;
		public TextView alertBoxTV;
		public AES aes;
		private String checkP = null;

		public StartServer(Context mobile_ServerActivity) {
			// TODO Auto-generated constructor stub

			this.context = mobile_ServerActivity;
			aes = new AES();
			new CodeReviewing();
		}

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();

			try {

				p = Runtime.getRuntime().exec("su");
				Log.e("", "" + "");
			} catch (IOException e) {
				// TODO Auto-generated catch block

			}
		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);

			if (result.equals("LOGOUT")) {

				try {
					socket.setTcpNoDelay(true);
					OutputStream os = socket.getOutputStream();
					OutputStreamWriter osw = new OutputStreamWriter(os);
					BufferedWriter bw = new BufferedWriter(osw);
					bw.write("LOGOUT_SUCCESS");
					os.flush();
					bw.flush();
					socket.close();
					Log.e("Logout success messagg: ", "Send Complete");

				} catch (Exception ex) {

				}
			}

		}

		@SuppressLint("ShowToast")
		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub
			try {
				while (!isCancelled()) {
					try {
						socket = serverSocket.accept();
						InputStream is = socket.getInputStream();
						if (prefs.getBoolean("sendingFile", false) == false) {
							InputStreamReader isr = new InputStreamReader(is);
							br = new BufferedReader(isr);

							String receivedMessage = null;
							receivedMessage = br.readLine();

							String[] command = receivedMessage.split("\\#");
							String tokn1 = CodeReviewing.decrypt2(
									aes.getKey1(), aes.getKey2(),
									Auth.getTokn());
							String tokn2 = CodeReviewing.decrypt2(
									aes.getKey1(), aes.getKey2(), command[0]
											.toString().trim());

							if (tokn1.equals(tokn2.trim())) {

								if (command.length == 1) {
									Send_Error_Message_TO_Client(socket,
											"String is Empty");
								} else {
									receivedMessage = command[1].toString()
											.trim();
									String[] type = receivedMessage
											.split("\\$");

									if (type.length > 1)
										if (type[1].equals("SORTTYPE")
												|| type[1].equals("SORTNAME")
												|| type[1].equals("SORTTIME")
												|| type[1].equals("SORTDATE")
												|| type[1].equals("SORTSIZE")
												|| type[1].equals("SORTDB")) {

											type[1] = type[1].trim();

										} else {
											type[1] = "SORTTYPE";
										}

									if (receivedMessage.startsWith("PASS")) {

										String[] chk = receivedMessage
												.split("\\$");

										for (int i = 0; i < chk.length; i++)
											Log.e("chk[" + i + "]", chk[i]);

										Send_Return_Message_TO_Client(socket,
												chk[1]);

									} else if (receivedMessage
											.startsWith("CHECKDEVICE")) {
										try {
											Send_Return_Message_TO_Client(
													socket, "CHECKDEVICE");

										} catch (Exception e) {
											// TODO: handle exception

											Send_Return_Message_TO_Client(
													socket, "CHECKDEVICE");

										}

									} else if (receivedMessage
											.startsWith("CREATE")) {
										String[] chk = receivedMessage
												.split("\\$");

										publishProgress("CREATE");

										if (chk[1].startsWith("FILE")) {
											Log.e("", " In CREATE FILE");
											listfile = new ListFiles();
											listfile.createFILE(socket, chk[2]
													.toString().trim());

										} else if (chk[1]
												.startsWith("DIRECTORY")) {
											Log.e("", " In CREATE DIRECTORY ");
											listfile = new ListFiles();
											listfile.createDIRECTORY(socket,
													chk[2].toString().trim());
										} else {
											Send_Error_Message_TO_Client(
													socket,
													"Invalid Create Command - "
															+ receivedMessage);
										}

									} else if (receivedMessage
											.startsWith("SNAPSHOT")) {
										Log.e("", " In SNAPSHOT ");
										publishProgress("SNAPSHOT");

										PackageManager pm = getPackageManager();
										WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
										TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
										ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
										ActivityManager am = (ActivityManager) context
												.getSystemService(Context.ACTIVITY_SERVICE);

										AppsNameAndPermission appNameP = new AppsNameAndPermission(
												this.context, pm, cm, wm, tm,
												am);
										appNameP.CompletSnapShot(socket);

									}else if(receivedMessage
											.startsWith("SANPSHOTDEVNET")){

										Log.e("", " In SANPSHOTDEVNET ");
										publishProgress("SANPSHOTDEVNET");

										PackageManager pm = getPackageManager();
										WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
										TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
										ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
										ActivityManager am = (ActivityManager) context
												.getSystemService(Context.ACTIVITY_SERVICE);

										AppsNameAndPermission appNameP = new AppsNameAndPermission(
												this.context, pm, cm, wm, tm,
												am);
										appNameP.ScanningAppInfo(socket);



									} else if (receivedMessage
											.startsWith("TCPSTART")) {
										Log.e("", " In TCPSTART ");
										publishProgress("TCP STARTED");

										try {
											Sniffer_Start_Stop logCat = new Sniffer_Start_Stop(
													this.context);
											String result = logCat.startTcp();
											if (result == null) {
												Log.e("Return from TCP",
														"Error");
												Send_Error_Message_TO_Client(
														socket,
														"Sniffer Not Started");
											} else {
												Log.e("Return from TCP - ",
														result);
												Send_Return_Message_TO_Client(
														socket,
														"Sniffer Started - "
																+ result);
											}
										} catch (Exception e) {

											Send_Error_Message_TO_Client(
													socket,
													"Exception in TCPSTART");
										}
									} else if (receivedMessage
											.startsWith("TCPSTOP")) {
										Log.e("", " In TCPSTOP ");
										publishProgress("TCP STOPPED");
										try {
											Sniffer_Start_Stop sniffer = new Sniffer_Start_Stop(
													this.context);

											if (sniffer.checkLogProcess() != null) {
												checkP = sniffer.stopTcp();

												if (checkP != null) {

													String fname = prefs
															.getString(
																	"pcapFileName",
																	"").trim();

													if (fname != null) {

														String filePath = "/sdcard/POC/Tcp/"
																+ prefs.getString(
																		"pcapFileName",
																		"")
																		.trim();

														if (new File(
																filePath.trim())
														.exists()) {
															listfile = new ListFiles();

															listfile.Copy_Source_to_Dest_Directory(
																	socket,
																	filePath.trim(),
																	SOURCE_FOLDER_TWO);

															MyService.SOURCE_FOLDER = SOURCE_FOLDER_TWO
																	+ filePath
																	.substring(filePath
																			.lastIndexOf("/") + 1);

															if (new File(
																	MyService.SOURCE_FOLDER)
															.exists()) {
															} else {
															}

															listfile.Change_Mod_File_Directory(MyService.SOURCE_FOLDER); // File

															if (new File(
																	MyService.SOURCE_FOLDER)
															.exists()) {
																listfile.getFile(
																		socket,
																		MyService.SOURCE_FOLDER);
																listfile.rm_File_Directory(MyService.SOURCE_FOLDER);
																MyService.SOURCE_FOLDER = "";

															} else
																Send_Error_Message_TO_Client(
																		socket,
																		MyService.SOURCE_FOLDER
																		+ "File Not Exist Send Fail 494");

														} else
															Send_Error_Message_TO_Client(
																	socket,
																	MyService.SOURCE_FOLDER
																	+ "File Not Exist Send Fail 494");
													} else {
														Send_Error_Message_TO_Client(
																socket,
																MyService.SOURCE_FOLDER
																+ "File Not Exist Send Fail 494");
													}
												} else {
													Send_Error_Message_TO_Client(
															socket,
															MyService.SOURCE_FOLDER
															+ "Error in Process Killing");
												}
											} else {
												Send_Return_Message_TO_Client(
														socket,
														"Start_Sniffer_First");
											}

										} catch (Exception ex) {

											Send_Return_Message_TO_Client(
													socket,
													"Start_Sniffer_First");
										}
									} else if (receivedMessage
											.startsWith("STARTLOGCAT")) {

										Log.e("", " In STARTLOGCAT ");
										publishProgress("LOGCAT STARTED");

										// TODO Auto-generated method stub
										try {
											LogcatSetGet logCat = new LogcatSetGet(
													this.context);

											String result = logCat.startLog();
											if (result == null) {
												Send_Error_Message_TO_Client(
														socket,
														"Not Allowed - Please Stop LOGCAT First");

											} else {
												Send_Return_Message_TO_Client(
														socket, result);
											}

										} catch (Exception e) {

										}

									} else if (receivedMessage
											.startsWith("STOPLOGCAT")) {
										Log.e("", " In STOPLOGCAT ");
										try {
											publishProgress("LOGCAT STOPPED");

											LogcatSetGet logCat = new LogcatSetGet(
													this.context);

											if (logCat.checkLogProcess() != null) {

												checkP = logCat.stopLog();

												if (checkP != null) {
													String fname = prefs
															.getString(
																	"logFileName",
																	null)
																	.trim();

													if (fname != null) {
														String filePath = "/sdcard/POC/Logcat/"
																+ prefs.getString(
																		"logFileName",
																		"")
																		.trim();

														if (new File(
																filePath.trim())
														.exists()) {

															listfile = new ListFiles();

															listfile.Copy_Source_to_Dest_Directory(
																	socket,
																	filePath.trim(),
																	SOURCE_FOLDER_TWO);

															MyService.SOURCE_FOLDER = SOURCE_FOLDER_TWO
																	+ filePath
																	.substring(filePath
																			.lastIndexOf("/") + 1);

															if (new File(
																	MyService.SOURCE_FOLDER)
															.exists()) {
															} else {
															}

															listfile.Change_Mod_File_Directory(MyService.SOURCE_FOLDER);

															if (new File(
																	MyService.SOURCE_FOLDER)
															.exists()) {
																listfile.getFile(
																		socket,
																		MyService.SOURCE_FOLDER);
																listfile.rm_File_Directory(MyService.SOURCE_FOLDER);
																MyService.SOURCE_FOLDER = "";

															} else
																Send_Error_Message_TO_Client(
																		socket,
																		MyService.SOURCE_FOLDER
																		+ "File Not Exist Send Fail 579");
														} else {
															Send_Error_Message_TO_Client(
																	socket,
																	MyService.SOURCE_FOLDER
																	+ "File Not Exist Send Fail 579");

														}
													} else {
														Send_Error_Message_TO_Client(
																socket,
																MyService.SOURCE_FOLDER
																+ "File Not Exist Send Fail 579");

													}
												} else {

													Send_Error_Message_TO_Client(
															socket,
															MyService.SOURCE_FOLDER
															+ "Error in Process Killing");

												}
											} else {
												Log.e("In LogProcess Check Else",
														"Start Logcat First");
												Send_Return_Message_TO_Client(
														socket,
														"Start_Logcat_First");
											}

										} catch (Exception ex) {

											Send_Error_Message_TO_Client(
													socket,
													"Start_Logcat_First");
										}

									} else if (receivedMessage
											.startsWith("STARTMONITOR")) {
										Log.e("", " In STARTMONITOR ");
										publishProgress("MONITOR STARTED");

										if (Chk_TcpFlag.isMonitorflag() == false) {
											monitorService = new StartCapturing(
													getApplicationContext());
											monitorService.StartSending(socket);
											Chk_TcpFlag.setMonitorflag(true);
											Send_Return_Message_TO_Client(
													socket,
													"TCP Started and Running");

										} else {
											Send_Error_Message_TO_Client(
													socket,
													"Not Allowed - Please Stop Monitoring First ");

										}
									} else if (receivedMessage
											.startsWith("STOPMONITOR")) {
										Log.e("", " In STOPMONITOR ");
										publishProgress("MONITOR STOPPED");

										if (Chk_TcpFlag.isMonitorflag() == true) {
											prefs.edit()
											.putBoolean(
													"liveStreamingBool",
													false).commit();
											Chk_TcpFlag.setMonitorflag(false);

										} else {
											Send_Error_Message_TO_Client(
													socket,
													"Not Allowed Please Start Monitoring First");

										}
									} else if (receivedMessage
											.startsWith("TCPEXPORT")) {
										Log.e("", " In TCPEXPORT ");
										publishProgress("TCP EXPORTED");

										tcpSS = new SnifferStartStop(
												getApplicationContext());
										tcpSS.TcpExport(socket);

									} else if (receivedMessage
											.startsWith("SAVE")) {
										Log.e("", " In SAVE ");
										publishProgress("SAVE");

										String s[] = receivedMessage
												.split("\\$");
										if (s[1].isEmpty()) {
											Send_Error_Message_TO_Client(
													socket,
													s[1]
															+ " - Source or Destination is Empty");
										} else {
											String count = null;
											StringBuffer lineBuffer = new StringBuffer();

											try {
												while ((count = br.readLine()) != null) {
													if (count
															.contains("\\#\\#\\#")) {
														break;
													}

													lineBuffer.append(count);
													lineBuffer.append("\n");
												}
												System.out
												.println("Read Complete");
											} catch (Exception ex) {

											}
											listfile = new ListFiles();
											listfile.Save_File(socket, s[1]
													.toString().trim(),
													lineBuffer);

										}
									} else if (receivedMessage
											.startsWith("DELETE")) {
										Log.e("", " In DELETE ");
										publishProgress("DELETE");

										String s[] = receivedMessage
												.split("\\$");

										if (s[1].isEmpty()) {
											Send_Error_Message_TO_Client(
													socket, "Source is Empty");
										} else {
											listfile = new ListFiles();
											listfile.rm_File(socket, s[1]
													.toString().trim());
										}

									} else if (receivedMessage
											.startsWith("COPY")) {
										Log.e("", " In COPY ");
										publishProgress("COPY");

										String s[] = receivedMessage
												.split("\\$");
										if (s[1].isEmpty() || s[2].isEmpty()) {
											Send_Error_Message_TO_Client(
													socket,
													"Source or Destination is Empty");
										} else {
											listfile = new ListFiles();
											listfile.Copy_Source_to_Dest(
													socket, s[1].toString()
													.trim(), s[2]
															.toString().trim());
										}
									} else if (receivedMessage
											.startsWith("PUSHFILE")) {
										String s[] = receivedMessage
												.split("\\$");
										Log.e("IN PUSHFILE ", "");
										publishProgress("PUSHFILE");

										if ((s[1].isEmpty() || s[1]
												.equals(null))
												&& (s[2].isEmpty() || s[2]
														.equals(null))) {
											Send_Error_Message_TO_Client(
													socket,
													s[1]
															+ " OR "
															+ s[2]
																	+ " - Source or Destination is Empty");
										} else {
											listfile = new ListFiles();
											listfile.Push_File(socket, s[1]
													.toString().trim(), s[2]
															.toString().trim(),
															this.context);
										}
									} else if (receivedMessage
											.startsWith("ISAVAILABLE")) {
										String s[] = receivedMessage
												.split("\\$");
										Log.e("IN ISAVAILABLE ", "");
										if ((s[1].isEmpty() || s[1]
												.equals(null))
												&& (s[2].isEmpty() || s[2]
														.equals(null))) {
											Send_Error_Message_TO_Client(
													socket,
													s[1]
															+ " OR "
															+ s[2]
																	+ " - Source or Destination is Empty");
										} else {
											listfile = new ListFiles();
											listfile.IsAvailable(socket, s[1]
													.toString().trim(), s[2]
															.toString().trim(),
															this.context);
										}
									} else if (receivedMessage
											.startsWith("MOVE")) {
										Log.e("", " In MOVE ");
										publishProgress("MOVE");

										String s[] = receivedMessage
												.split("\\$");
										if (s[1].isEmpty() || s[2].isEmpty()) {
											Send_Error_Message_TO_Client(
													socket,
													"Source or Destination is Empty");
										} else {
											listfile = new ListFiles();
											listfile.moveFile(socket, s[1]
													.toString().trim(), s[2]
															.toString().trim());

										}

									} else if (receivedMessage
											.startsWith("DOWNLOAD")) {
										Log.e("", " In DOWNLOAD ");
										publishProgress("APK DOWNLOADED");

										String s[] = receivedMessage
												.split("\\$");
										if (s[1].isEmpty()) {
											Send_Error_Message_TO_Client(
													socket,
													"Source or Destination is Empty");
										} else if (new File(s[1].trim())
										.isFile()) {

											if (new File(SOURCE_FOLDER_TWO)
											.exists()) {

											} else {
												File dir = new File(
														SOURCE_FOLDER_TWO);
												dir.mkdir();
											}

											listfile = new ListFiles();

											listfile.Copy_Source_to_Dest_Directory(
													socket, s[1].toString()
													.trim(),
													SOURCE_FOLDER_TWO);

											MyService.SOURCE_FOLDER = SOURCE_FOLDER_TWO
													+ s[1].substring(s[1]
															.lastIndexOf("/") + 1);

											if (new File(
													MyService.SOURCE_FOLDER)
											.exists()) {
											} else {
											}

											listfile.Change_Mod_File_Directory(MyService.SOURCE_FOLDER); // File

											if (new File(
													MyService.SOURCE_FOLDER)
											.exists()) {
												listfile.getFile(socket,
														MyService.SOURCE_FOLDER);
												listfile.rm_File_Directory(MyService.SOURCE_FOLDER);
												MyService.SOURCE_FOLDER = "";

											} else
												Send_Error_Message_TO_Client(
														socket,
														s[1].trim()
														+ " File Not Exist.");

										} else if (new File(s[1].trim())
										.isDirectory()) {
											Log.e("", " In Dir DOWNLOAD ");
											publishProgress("DIRECTORY DOWNLOADED");

											listfile = new ListFiles();

											listfile.Copy_Source_to_Dest_Directory(
													socket, s[1].toString()
													.trim(),
													SOURCE_FOLDER_TWO);

											MyService.SOURCE_FOLDER = SOURCE_FOLDER_TWO
													+ s[1].substring(s[1]
															.lastIndexOf("/") + 1);

											listfile.Change_Mod_File_Directory(MyService.SOURCE_FOLDER);

											OUTPUT_ZIP_FILE = SOURCE_FOLDER_TWO
													+ s[1].substring(s[1]
															.lastIndexOf("/") + 1)
															+ ".zip";

											listfile.generateFileList(new File(
													MyService.SOURCE_FOLDER));
											listfile.zipIt(OUTPUT_ZIP_FILE);

											if (new File(OUTPUT_ZIP_FILE)
											.exists()) {
												listfile.getFile(socket,
														OUTPUT_ZIP_FILE);
												listfile.rm_File_Directory(MyService.SOURCE_FOLDER);
												listfile.rm_File_Directory(OUTPUT_ZIP_FILE);
												MyService.SOURCE_FOLDER = "";
												OUTPUT_ZIP_FILE = "";
											} else
												Send_Error_Message_TO_Client(
														socket,
														s[1].trim()
														+ " File Not Exist.");
										} else {
											Send_Error_Message_TO_Client(
													socket,
													"Invalid File/Directory Name "
															+ s[1]);
										}

									}else if (receivedMessage
											.startsWith("CURRENTSCREEN")) {

										Log.e("", " In CURRENTSCREEN ");
										publishProgress("CURRENT SCREEN");

										listfile = new ListFiles(this.context);
										listfile.Get_Current_Screen(this.context);

										String screenPath = prefs.getString(
												"currentScreen", "");
										File f = new File(screenPath);
										if (f.exists()
												&& !screenPath.equals("")) {
											listfile.getFile(socket,
													f.toString());
											listfile.rm_File_Directory(f
													.toString());

										} else {
											Send_Error_Message_TO_Client(
													socket,
													"File Does Not Exist");
										}

									} else if (receivedMessage
											.startsWith("STARTLISTENER")) { // Changing to start the VPORXY rather then checking the status - 17.11.2017

										////////

										try{

											// boolean value = ((ToggleButton)v).isChecked();
											// if (value && !proxyStarted){}else if (proxyStarted){}

											Log.e("Start Proxy", "First time");
											proxyStarted = true;
											pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

											serverip = pref.getString("IP","").trim();        
											serverport = pref.getString("PORT", "").trim();

											preport = Integer.parseInt(serverport)-1;
											postport = Integer.parseInt(serverport)+1;

											Thread thread = new Thread()
											{
												@Override
												public void run() {
													Preferences.init(getApplicationContext());
													if (isDeviceRooted()){
														ipTablesForTransparentProxy(true);
													}
													framework = new Framework(getApplicationContext());
													setStore(getApplicationContext());
													networkHostNameResolver = new NetworkHostNameResolver(getApplicationContext());
													clientResolver = new ClientResolver(getApplicationContext());
													Proxy proxy = new Proxy(framework, networkHostNameResolver, clientResolver);
													framework.addPlugin(proxy);
													if (true){
														ProxyPlugin plugin = new CustomPlugin();
														proxy.addPlugin(plugin);
													}
													proxy.run();
													proxyStarted = true;

													// logger.fine("Android os proxy should point to localhost 9008");
												}
											};
											thread.setName("Starting proxy");
											thread.start();

										}catch(Exception ex){
											ex.printStackTrace();
										}

										/////////

										Log.e("STARTLISTENER", "STARTLISTENER");
										Log.e("Proxy Status StartListener", "" + proxyStarted);


										if (proxyStarted) {

											Send_Error_Message_TO_Client(
													socket, "Listener Started");

											ListenerStatus = "REQUEST_CAPTURE_START";

											Log.e("InSIDE GET Request :",
													ListenerStatus);

										} else {

											Send_Error_Message_TO_Client(
													socket,
													"Please Start Proxy On MASTS Agent");
										}

									}
									else if (receivedMessage
											.startsWith("STOPLISTENER")) {
										try
										{
											//proxyStarted = false;
											Thread thread = new Thread()
											{
												@Override
												public void run() {
													if (isDeviceRooted()){
														ipTablesForTransparentProxy(false);
													}
													if (framework != null){
														framework.stop();
													}
													if (networkHostNameResolver != null){
														networkHostNameResolver.cleanUp();
													}
													networkHostNameResolver = null;
													framework = null;

													File file = PreferenceUtils.getDataStorageDir(getApplicationContext());
													String rootDirName = null;
													if (file != null){
														rootDirName = file.getAbsolutePath() + "/content";
													}
													File rootDir = new File(rootDirName);
													File[] contentFiles = null;
													if (file.exists()) {

														contentFiles = rootDir.listFiles();
													}
													SqlLiteStore database = SqlLiteStore.getInstance(getApplicationContext(), rootDirName);
													database.clearHttpDatabase();

													DeleteFilesThread deleteFilesThread = new DeleteFilesThread(contentFiles);
													deleteFilesThread.start();
													Log.e("Deleted capture Data", "Deleted");

													Thread cThread = new Thread(new ClientThread());
													cThread.start();


												}
											};
											thread.setName("Stoping proxy");
											thread.start();
										}catch(Exception e){e.printStackTrace();}

									}
									else if (receivedMessage
											.startsWith("LOGOUT")) {

										logoutop = "LOGOUT";
										Log.e("In Logout Command", "LOGOUT");
										return "LOGOUT";

									} else if (receivedMessage
											.startsWith("DATABASE")) {

										Log.e("", " In DATABASE ");
										publishProgress("DATABASE");

										listfile = new ListFiles();
										listfile.List_ALL_DB_Files(socket);

									} else if (receivedMessage
											.startsWith("RENAME")) {

										Log.e("", " In RENAME ");
										publishProgress("FILE RENAME");

										String chkStr[] = receivedMessage
												.split("\\$");

										listfile = new ListFiles();
										listfile.renameFILE_DIRECTORY(socket,
												chkStr[1].trim(),
												chkStr[2].trim());

									} else if (new File(type[0].trim())
									.exists()) {

										if (new File(receivedMessage.trim())
										.isFile()) {
											publishProgress("FILE ACCESS");
											listfile = new ListFiles();

											listfile.Copy_Source_to_Dest_Directory(
													socket, receivedMessage
													.toString().trim(),
													SOURCE_FOLDER_TWO);

											MyService.SOURCE_FOLDER = SOURCE_FOLDER_TWO
													+ receivedMessage
													.substring(receivedMessage
															.lastIndexOf("/") + 1);

											listfile.Change_Mod_File_Directory(MyService.SOURCE_FOLDER);

											if (new File(
													MyService.SOURCE_FOLDER)
											.exists()) {

												listfile.getFile(socket,
														MyService.SOURCE_FOLDER);
												listfile.rm_File_Directory(MyService.SOURCE_FOLDER);
												MyService.SOURCE_FOLDER = "";

											} else
												Send_Error_Message_TO_Client(
														socket,
														" file Not Exist.");

										} else if (new File(type[0].trim())
										.isDirectory()) {
											Log.e("", " In IsDIR ");

											listfile = new ListFiles();
											listfile.readDir(socket,
													type[0].trim() + "$"
															+ type[1].trim());

										} else {
											Send_Error_Message_TO_Client(
													socket,
													"888 = Please Send Valid Command - "
															+ receivedMessage);

										}
									} else {
										Send_Error_Message_TO_Client(socket,
												"677 MYService = Please Send Valid Command!"
														+ receivedMessage);
									}
								}
							} else {
								Session_Expire_ReLoginRequired(socket,
										command[0].toString().trim());
								publishProgress(MainActivity.mainActivity
										.getApplicationContext());
							}

						} else {
							publishProgress("FILE INSERTED");

							prefs = PreferenceManager
									.getDefaultSharedPreferences(this.context);
							String file = prefs.getString("pushFile", "");
							if (!file.equals("")) {
								Log.e("", " In WritingToFILE ");
								listfile = new ListFiles();
								listfile.Write_ToFile(socket, this.context);
								File rmfile = new File(file);
								if (rmfile.exists())
									listfile.rm_File_Directory(rmfile
											.toString());
							} else {
								Log.e("", " Push File does not Exist ");

							}
						}
					} catch (Exception e) {
						// TODO: handle exception

						Send_Error_Message_TO_Client(socket,
								"Invalid_Command_Received_931");
					}
				}
			} catch (Exception e) {

				Send_Error_Message_TO_Client(socket, "Invalid Command 686");
			}
			return null;
		}

		/**
		 * Method Name: publishProgress
		 * 
		 * @param applicationContext
		 *            Description:
		 */
		private void publishProgress(Context applicationContext) {
			// TODO Auto-generated method stub

		}

		/**
		 * Method Name: publishProgress
		 * 
		 * @param string
		 *            Description:
		 */
		private void publishProgress(String string) {
			// TODO Auto-generated method stub
			try {

				String uppercaseString = string.toUpperCase();
				MainActivity.planetList.add(0, uppercaseString);
				MainActivity.listAdapter.notifyDataSetChanged();
				MainActivity.status_list.setSelection(MainActivity.listAdapter
						.getCount() - 1);
			} catch (Exception e) {
				// TODO: handle exception

			}
		}

		@SuppressLint("NewApi")
		void Send_Error_Message_TO_Client(Socket socket, String msg) {

			try {
				socket.setTcpNoDelay(true);
				OutputStream os = socket.getOutputStream();
				OutputStreamWriter osw = new OutputStreamWriter(os);
				BufferedWriter bw = new BufferedWriter(osw);
				bw.write(Auth.getTokn() + "#" + msg);
				os.flush();
				bw.flush();
				socket.close();

			} catch (Exception ex) {

			}

		}

		void Session_Expire_ReLoginRequired(Socket socket, String msg) {
			try {

				socket.setTcpNoDelay(true);
				OutputStream os = socket.getOutputStream();
				OutputStreamWriter osw = new OutputStreamWriter(os);
				BufferedWriter bw = new BufferedWriter(osw);
				bw.write(msg + "#" + "Re_LoginRequiredOnDevice");
				os.flush();
				bw.flush();
				socket.close();

			} catch (Exception ex) {

			}

		}

		public void Send_Return_Message_TO_Client(Socket socket, String msg) {
			try {
				socket.setTcpNoDelay(true);

				OutputStream os = socket.getOutputStream();
				OutputStreamWriter osw = new OutputStreamWriter(os);
				BufferedWriter bw = new BufferedWriter(osw);
				bw.write(Auth.getTokn() + "#" + msg);
				os.flush();
				bw.flush();
				socket.close();

			} catch (Exception ex) {

			}

		}
	}
	/**
	 * Checks if the device is rooted.
	 * 
	 * @return <code>true</code> if the device is rooted, <code>false</code>
	 * otherwise.
	 */
	public static boolean isDeviceRooted() {

		boolean trueReturn = true;
		String buildTags = android.os.Build.TAGS;
		if (buildTags != null && buildTags.contains("test-keys")) {
			return trueReturn;
		}
		try {

			{
				File file = new File("/system/app/Superuser.apk");
				if (file.exists()) {
					return trueReturn;
				}
			}

			{
				String[] suPlaces = { "/sbin/", "/system/bin/", "/system/xbin/", "/data/local/xbin/",
						"/data/local/bin/", "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/" };
				for (String suPlace : suPlaces) {
					File file = new File(suPlace + "su");
					if (file.exists()) {
						return trueReturn;
					}
				}
			}
		} catch (Throwable e1) {

		}
		return false;
	}


	private void ipTablesForTransparentProxy(boolean activate){
		int processId = getApplicationInfo().uid;
		String excludedUid = String.valueOf(processId);
		String action = "A";
		String chainName = "spplugin";
		String chainName1 = "sppluginOutput";
		List<String> rules = new ArrayList<String>();

		if (activate){
			action = "A";
			String createChainRule = "iptables --new " + chainName; rules.add(createChainRule);
			String createNatChainRule = "iptables -t nat --new " + chainName; rules.add(createNatChainRule);
			String createNatChainRule1 = "iptables -t nat --new " + chainName1; rules.add(createNatChainRule1);
		}else{
			action = "D";
			String dettachChainRule = "iptables -D INPUT -j " + chainName; rules.add(dettachChainRule);
			String dettachNatChainRule = "iptables -t nat -D PREROUTING -j " + chainName; rules.add(dettachNatChainRule);
			String dettachNatChainRule1 = "iptables -t nat -D OUTPUT -j " + chainName1; rules.add(dettachNatChainRule1);
		}

		String accept80Rule = "iptables -" + action + " " + chainName + " -p tcp --dport 0:79 -j ACCEPT "; rules.add(accept80Rule);
		String redirect80Rule = "iptables -" + action + " " + chainName + " -t nat -p tcp --dport 0:79 -j REDIRECT --to-port 8009 ";rules.add(redirect80Rule);
		String exclude80UidRule = "iptables -t nat -" + action + " " + chainName1 + " -m owner ! --uid-owner " + excludedUid + " -p tcp --dport 0:79 -j DNAT --to 127.0.0.1:8009 ";rules.add(exclude80UidRule);

		String accept80Rule1 = "iptables -" + action + " " + chainName + " -p tcp --dport 80 -j ACCEPT "; rules.add(accept80Rule1);
		String redirect80Rule1 = "iptables -" + action + " " + chainName + " -t nat -p tcp --dport 80 -j REDIRECT --to-port 8009 ";rules.add(redirect80Rule1);
		String exclude80UidRule1 = "iptables -t nat -" + action + " " + chainName1 + " -m owner ! --uid-owner " + excludedUid + " -p tcp --dport 80 -j DNAT --to 127.0.0.1:8009 ";rules.add(exclude80UidRule1);

		String accept80Rule2 = "iptables -" + action + " " + chainName + " -p tcp --dport 81:442 -j ACCEPT "; rules.add(accept80Rule2);
		String redirect80Rule2 = "iptables -" + action + " " + chainName + " -t nat -p tcp --dport 81:442 -j REDIRECT --to-port 8009 ";rules.add(redirect80Rule2);
		String exclude80UidRule2 = "iptables -t nat -" + action + " " + chainName1 + " -m owner ! --uid-owner " + excludedUid + " -p tcp --dport 81:442 -j DNAT --to 127.0.0.1:8009 ";rules.add(exclude80UidRule2);

		String accept443Rule3 = "iptables -" + action + " " + chainName + " -p tcp --dport 443 -j ACCEPT ";rules.add(accept443Rule3);
		String redirect443Rule3 = "iptables -" + action + " " + chainName + " -t nat -p tcp --dport 443 -j REDIRECT --to-port 8010 ";rules.add(redirect443Rule3);
		String exclude443UidRule3 = "iptables -t nat -" + action + " " + chainName1 + " -m owner ! --uid-owner " + excludedUid + " -p tcp --dport 443 -j DNAT --to 127.0.0.1:8010 ";rules.add(exclude443UidRule3);

		String accept80Rule4 = "iptables -" + action + " " + chainName + " -p tcp --dport 444:45000 -j ACCEPT "; rules.add(accept80Rule4);
		String redirect80Rule4 = "iptables -" + action + " " + chainName + " -t nat -p tcp --dport 444:45000 -j REDIRECT --to-port 8009 ";rules.add(redirect80Rule4);
		String exclude80UidRule4 = "iptables -t nat -" + action + " " + chainName1 + " -m owner ! --uid-owner " + excludedUid + " -p tcp --dport 444:45000 -j DNAT --to 127.0.0.1:8009 ";rules.add(exclude80UidRule4);




		if (activate){
			String attachChainRule = "iptables -A INPUT -j " + chainName; rules.add(attachChainRule);
			String attachNatChainRule = "iptables -t nat -A PREROUTING -j " + chainName; rules.add(attachNatChainRule);
			String attachNatChainRule1 = "iptables -t nat -A OUTPUT -j " + chainName1; rules.add(attachNatChainRule1);
		}else{

			String deleteChainRule = "iptables --delete-chain " + chainName; rules.add(deleteChainRule);
			String deleteNatChainRule = "iptables -t nat --delete-chain " + chainName; rules.add(deleteNatChainRule);
			String deleteNatChainRule1 = "iptables -t nat --delete-chain " + chainName1; rules.add(deleteNatChainRule1);
		}
		Process p;
		try {
			p = Runtime.getRuntime().exec(new String[]{"su", "-c", "sh"});

			DataOutputStream stdin = new DataOutputStream(p.getOutputStream());
			DataInputStream stdout = new DataInputStream(p.getInputStream());
			InputStream stderr = p.getErrorStream();

			for (String rule : rules) {
				//logger.finest(rule);
				stdin.writeBytes(rule + "\n");
				stdin.writeBytes("echo $?\n");
				Thread.sleep(100);
				byte[] buffer = new byte[4096];
				int read = 0;
				String out = new String();
				String err = new String();
				while(true){
					read = stdout.read(buffer);
					out += new String(buffer, 0, read);
					if(read<4096){
						break;
					}
				}
				while(stderr.available() > 0){
					read = stderr.read(buffer);
					err += new String(buffer, 0, read);
					if(read < 4096){
						break;
					}
				}
				//if (out != null && out.trim().length() > 0) logger.finest(out);
				//if (err != null && err.trim().length() > 0) logger.finest(err);
			}
		} catch (Exception e) {
			e.printStackTrace();
			//logger.finest("Error executing rules: " + e.getMessage());
		}
	}
	 public static void setStore(Context context){
	        if (framework != null){
	            try {
	                File file =  PreferenceUtils.getDataStorageDir(context);
	                if (file != null){
	                    File rootDir = new File(file.getAbsolutePath() + "/content");
	                    if (!rootDir.exists()){
	                        rootDir.mkdir();
	                    }
	                    framework.setSession("Database", SqlLiteStore.getInstance(context, rootDir.getAbsolutePath()), "");
	                }
	            } catch (StoreException e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
	            }
	        }
	    }
	
	public class DeleteFilesThread extends Thread{
		private File[] filesToDelete;
		public DeleteFilesThread(File[] files){
			filesToDelete = files;
		}

		public void run() {
			if (filesToDelete != null){
				for (File contentFile : filesToDelete) {
					try{
						contentFile.delete();
						// logger.finest("File deleted: " + contentFile.getAbsolutePath());
					}catch (Exception ex){
						Log.e(TAG, ex.getMessage());
					}
				}
			}
		}
	}
	public class ClientThread implements Runnable {

		public void run() {

			Log.e("REQUEST THREAD CALL	", "OK");
			try {

				InetAddress inet = InetAddress.getByName(pref.getString("IP","").trim());
				Socket s=new Socket(inet,45000);
				OutputStream os = s.getOutputStream();
				PrintWriter pw = new PrintWriter(os, true);
				pw.write("Listner Stop");
				pw.close();
				s.close();
				//bufferrequest.setLength(0);
				Log.e("Stop Listener", "When Proxy Stop");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	} 
}

