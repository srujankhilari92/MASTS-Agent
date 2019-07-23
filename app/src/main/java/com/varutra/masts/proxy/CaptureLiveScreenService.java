package com.varutra.masts.proxy;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.varutra.masts.proxy.R;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

@SuppressLint("ShowToast")
public class CaptureLiveScreenService extends Service {

	private static final String TAG = "CaptureLiveScreenService";
	private StartServerStreaming startServer;
	public int port = 55555;
	public ServerSocket serverSocket;
	public Socket socket;
	public SharedPreferences prefs;
	public static String SOURCE_FOLDER;

	public String OUTPUT_ZIP_FILE = "";
	@SuppressLint("SdCardPath")
	public String SOURCE_FOLDER_TWO = "/sdcard/POC/";

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		prefs.edit().putBoolean("liveStreamingBool", true).commit();

		startServer = new StartServerStreaming(this);
		startServer.execute(new String[] {});

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

		Toast.makeText(this, "Mobile Suite Agent Service Stopped",
				Toast.LENGTH_LONG).show();
		
		try {
			prefs.edit().putBoolean("liveStreamingBool", false).commit();
			startServer.cancel(true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}

		Toast.makeText(this, R.string.local_service_stop, Toast.LENGTH_SHORT)
				.show();

	}

	class StartServerStreaming extends AsyncTask<String, Void, String> {
		Context context;
		public String returnMessage;
		byte[] mybytearray;
		public int pid;
		public Process p;
		public BufferedReader br;
		private SimpleDateFormat sdf;
		private Object currentDateandTime;
		private String currentScreen;
		private Process sh;
		private DataOutputStream os;
		private BufferedReader bufferRD;
		private String s;

		public StartServerStreaming(
				CaptureLiveScreenService mobile_ServerActivity) {
			// TODO Auto-generated constructor stub

			this.context = mobile_ServerActivity;
		}

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();

		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);

		}

		@SuppressLint({ "ShowToast", "SimpleDateFormat" })
		@Override
		protected String doInBackground(String... params) {
			// TODO Auto-generated method stub

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

					os.writeBytes(comando + "\n");
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
					int port = Integer.parseInt("55555");
					InetAddress address = InetAddress.getByName(host);

					socket = new Socket(address, port);
					try {

						File myFile = new File("/sdcard/screens/"
								+ currentScreen);
						InputStream newFile = new FileInputStream(myFile);
						BufferedInputStream in = new BufferedInputStream(
								newFile);

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
						//e.printStackTrace();
					} catch (IOException e) {
						Thread.sleep(1000);
						// TODO Auto-generated catch block
						//e.printStackTrace();
					}
				}

			} catch (Exception e) {
				// TODO: handle exception
				//e.printStackTrace();
				Send_Error_Message_TO_Client(socket,
						"Exception in doInBackground 323");
			}

			return null;
		}

		@SuppressLint("SimpleDateFormat")
		public void saveBitmap(Bitmap bitmap) {

			sdf = new SimpleDateFormat("ddMMyyyy_HHmmss");
			currentDateandTime = sdf.format(new Date());
			currentScreen = "screen_" + currentDateandTime + ".png";
		
			String filePath = Environment.getExternalStorageDirectory()
					+ File.separator + "screens/" + currentScreen;
			File imagePath = new File(filePath);
			FileOutputStream fos;
			try {
				fos = new FileOutputStream(imagePath);
				bitmap.compress(CompressFormat.PNG, 100, fos);
				fos.flush();
				fos.close();

			} catch (FileNotFoundException e) {
				Log.e("MASS", e.getMessage(), e);
			} catch (IOException e) {
				Log.e("MASS", e.getMessage(), e);
			}
		}

		@SuppressLint("NewApi")
		void Send_Error_Message_TO_Client(Socket socket, String msg) {

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

		void Send_Return_Message_TO_Client(Socket socket, String msg) {

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

				Log.e("Return Message : ", msg + " Send Complet");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
}