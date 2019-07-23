package com.varutra.masts.proxy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.varutra.masts.proxy.LogKit_Authentication.Check_Set_System_Files;
import com.varutra.plugin.gui.Proxy_main_activity;
import com.varutra.plugin.gui.Proxy_main_activity.ClientThread;
import com.varutra.masts.proxy.R;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.ProgressBar;
import android.widget.TextView;

public class Logout extends Activity {
	private String uniqueKey;
	public ProgressBar progressbar;
	private Handler handler = new Handler();
	private int progressStatus = 0;
	private TextView alertBoxTV;
	private String myIp;
	private WifiInfo mWifiInfo;
	public SharedPreferences pref;
	private ConnectivityManager cm;
	private WifiManager wm;
	private String Logoutoutput;

	SessionManager session;
	Proxy_main_activity pro;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.logout);
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		boolean isEnableProxy = settings.getBoolean("isEnabled", false);

		pref = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		mWifiInfo = wm.getConnectionInfo();
		pro = new Proxy_main_activity();
		myIp = intToIP(mWifiInfo.getIpAddress());
		progressbar = (ProgressBar) findViewById(R.id.loadingBar);

		final int welcomeScreenDisplay = 3000;

		final Thread welcomeThread = new Thread() {

			int wait = 0;

			@Override
			public void run() {
				try {
					super.run();
					while (wait < welcomeScreenDisplay) {
						sleep(100);
						wait += 100;

						progressStatus += 4;

						handler.post(new Runnable() {
							public void run() {
								progressbar.setProgress(progressStatus);

							}
						});
					}
				} catch (Exception e) {

				} finally {
					
					MainActivity.mainActivity.finish();
					MainActivity.planetList.clear();

					startActivity(new Intent(Logout.this,
							Check_Set_IP_Port.class));
					
					//clear cache data
					
					Thread cThread = new Thread(new CleaseCacheThread());
                    cThread.start();
					
					finish();
				}
			}
		};


		String Url = "http://" + pref.getString("IP", "") + ":"
				+ pref.getString("PORT", "") + "/VMastsWSV";

		String Param1 = myIp;
		String Param2 = "LOGOUT";

		

		Logout_session_stop chkSet = new Logout_session_stop(Url, Param1,
				Param2);
		chkSet.execute(new String[] { Url });

		MainActivity.mainActivity.finish();
		welcomeThread.start();

	}

	public String intToIP(int i) {
		return ((i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
				+ "." + ((i >> 24) & 0xFF));
	}

	private boolean isMyServiceRunning(Context mContext) {
		ActivityManager manager = (ActivityManager) mContext
				.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (com.varutra.masts.proxy.MyService.class.getName().equals(
					service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
	
	public class CleaseCacheThread implements Runnable {

		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				 Context context = Logout.this;
		         File dir = context.getCacheDir();
		         if (dir != null && dir.isDirectory()) {
		            deleteDir(dir);
		         }
		      } catch (Exception e) {
		         // TODO: handle exception
		      }
		}
		
		
	}
	
	 public static boolean deleteDir(File dir) {
	      if (dir != null && dir.isDirectory()) {
	         String[] children = dir.list();
	         for (int i = 0; i < children.length; i++) {
	            boolean success = deleteDir(new File(dir, children[i]));
	            if (!success) {
	               return false;
	            }
	         }
	      }

	      // The directory is now empty so delete it
	      return dir.delete();
	   }

	public class Logout_session_stop extends AsyncTask<String, Void, String> {

		private String url;
		private String param1, param2;

		public Logout_session_stop(String url, String param1, String param2) {
			// TODO Auto-generated constructor stub

			this.url = url;
			this.param1 = param1;
			this.param2 = param2;

		}

		@Override
		protected String doInBackground(String... urls) {
			// TODO Auto-generated method stub

			Logoutoutput = "Welcome Copying Done";

			try {
				for (String url : urls) {
					Logoutoutput = getOutputFromUrl(url);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				Logoutoutput = null;
			}

			return Logoutoutput;

		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);


		}

		private String getOutputFromUrl(String url) {

			String op = "MASTS";

			HttpResponse httpResponse;
			try {
				DefaultHttpClient httpClient = new DefaultHttpClient();
				HttpPost httpPost = new HttpPost(url);
				List<BasicNameValuePair> nameValuePairs = new ArrayList<BasicNameValuePair>(
						1);
				nameValuePairs.add(new BasicNameValuePair("ip", this.param1));
				nameValuePairs
						.add(new BasicNameValuePair("status", this.param2));

				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				httpResponse = httpClient.execute(httpPost);
				HttpEntity httpEntity = httpResponse.getEntity();
				op = EntityUtils.toString(httpEntity);

			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				op = "MASTS";

			} catch (IOException e) {
				// TODO Auto-generated catch block
				op = "MASTS";

			} catch (Exception e) {
				// TODO Auto-generated catch block
				op = "MASTS";

			}
			return op;
		}

	}
}