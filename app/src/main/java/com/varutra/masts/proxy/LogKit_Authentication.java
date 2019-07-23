package com.varutra.masts.proxy;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.varutra.masts.proxy.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

public class LogKit_Authentication extends Activity implements OnClickListener {
	private EditText username;
	private EditText password;
	private CheckBox remenberme;
	private Button login, cancel;
	private Get_SetIP getSet_IPPort_Obj;
	private Validation valid;
	public SharedPreferences pref;
	public AlertDialogManager alert;
	SessionManager session;
	private boolean isInternetPresent = false;
	private ConnectionDetector cd;
	private String uniqueKey;

	private ConnectivityManager cm;
	private WifiManager wm;
	public AppsNameAndPermission appname;
	private String myIp;
	private WifiInfo mWifiInfo;
	public Vibrator vibrate;
	
	private Process p;
	public AES aes;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);

		setContentView(R.layout.login_new_six_new);
		pref = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		mWifiInfo = wm.getConnectionInfo();
		vibrate = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
		aes = new AES();


		session = new SessionManager(getApplicationContext());
		alert = new AlertDialogManager();
		cd = new ConnectionDetector(getApplicationContext());
		myIp = intToIP(mWifiInfo.getIpAddress());

		getSet_IPPort_Obj = new Get_SetIP();
		valid = new Validation();
		username = (EditText) findViewById(R.id.userName_Edit);
		password = (EditText) findViewById(R.id.passWord_Edit);
		remenberme = (CheckBox) findViewById(R.id.checkBox1);
		

		
		if (pref.getString("username", "").length() != 0 & pref.getString("password", "").length() != 0) {
			
			String decuserName = CodeReviewing.decrypt2(aes.getKey1(), aes.getKey2(), pref.getString("username", ""));
			String decpassWord = CodeReviewing.decrypt2(aes.getKey1(), aes.getKey2(), pref.getString("password", ""));
			username.setText(decuserName);
			password.setText(decpassWord);
		}
		int mastsagentid = android.os.Process.myPid();
        Log.e("MASTS Agent LogKit Authentication process id:", "id-"+mastsagentid);
		login = (Button) findViewById(R.id.loginBtn);
		cancel = (Button) findViewById(R.id.cancelBtn);

		login.setOnClickListener(this);
		cancel.setOnClickListener(this);

		/*try {
			p = Runtime.getRuntime().exec("su");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			alert.showAlertDialog(
					this,
					"Warning!",
					"Your device is not rooted.\n Please root your device prior using this application.",
					false);

		}*/
	}

	public String intToIP(int i) {
		return ((i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
				+ "." + ((i >> 24) & 0xFF));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub

		menu.add("Setting")
				.setIntent(
						new Intent(LogKit_Authentication.this,
								Check_Set_IP_Port.class));
		super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public void onClick(View v) {

		vibrate.vibrate(50);

		// TODO Auto-generated method stub
		if (v.getId() == R.id.loginBtn) {

			String userName = username.getText().toString().trim();
			String passWord = password.getText().toString().trim();
			
			if(remenberme.isChecked()){
				
				String encusername = CodeReviewing.encrypt(aes.getKey1(), aes.getKey2(), userName);
				String encpassword = CodeReviewing.encrypt(aes.getKey1(), aes.getKey2(), passWord);
				
				pref.edit().putString("username", encusername).commit();
				pref.edit().putString("password", encpassword).commit();
				
			}
			
			if ((userName.length() > 0 && userName.length() <= 15)) {
				if ((passWord.length() > 0 && passWord.length() <= 15)) {
					if ((valid.validateName(userName) == true)) {
						if ((valid.validateName(passWord) == true)) {
							if (pref.getString("IP", "").trim().length() != 0
									&& pref.getString("PORT", "").trim()
											.length() != 0) {
								isInternetPresent = cd.isConnectingToInternet();

								if (isInternetPresent) {
									Log.e("Internet Connection",
											"after checking internet connectiion");

									String  encuser = CodeReviewing.encrypt(aes.getKey1(), aes.getKey2(), userName);
									pref.edit().putString("user", encuser)
											.commit();

									
									uniqueKey = CodeReviewing.GenerateKey(
											username.getText().toString()
													.trim(), password.getText()
													.toString().trim());
									
									String Url = "http://"
											+ pref.getString("IP", "") + ":"
											+ pref.getString("PORT", "")
											+ "/VMastsWSV";
									String Param1 = userName;
									String Param2 = passWord;
									String Param3 = uniqueKey;
									String Param4 = myIp;
									String Param5 = "LOGIN";

									

									Check_Set_System_Files chkSet = new Check_Set_System_Files(
											this, Url, Param1, Param2, Param3,
											Param4, Param5);
									chkSet.execute(new String[] { Url });

								} else {
									alert.showAlertDialog(
											LogKit_Authentication.this,
											"No Internet Connection!",
											"Sorry, no internet connectivity available. Please reconnect and try again.",
											false);
								}
							} else {
								alert.showAlertDialog(
										this,
										"Access Denied!",
										"Please specify IP address and port number from Menu -> Settings",
										false);
							}
						} else {
							alert.showAlertDialog(
									this,
									"Access Denied!",
									"Invalid username or password! Please try again.",
									false);

						}
					} else {
						alert.showAlertDialog(
								this,
								"Access Denied!",
								"Invalid username or password! Please try again.",
								false);
					}
				} else {
					alert.showAlertDialog(this, "Access Denied!",
							"Invalid username or password! Please try again.", false);

				}
			} else {
				alert.showAlertDialog(this, "Access Denied!",
						"Invalid username or password! Please try again.", false);

			}

		}
		if (v.getId() == R.id.cancelBtn) {
			username.setText("");
			password.setText("");
			finish();
		}

	}

	public class Check_Set_System_Files extends AsyncTask<String, Void, String> {

		private String comando;
		private Process suProcess;
		private DataOutputStream os;
		private BufferedReader bufferedreader;
		private String s, op;
		private String data = "/data/";
		private String data_local = "/data/local/";
		private String system = "/system/";
		private String system_bin = "/system/bin/";
		public LogKit_Authentication ctx;
		public Context context;
		private ProgressDialog loadingBar;
		private int apiLEVEL = Integer
				.valueOf(android.os.Build.VERSION.SDK_INT);
		private InputStream stream;
		private OutputStream output;
		private CodeReviewing ckd;
		private String url;
		private String param1, param2, param3, param4, param5;
		private Thread chmodData;
		private Thread chmodDataLocal;
		private Thread chmodSystem;
		private Thread chmodSystemBin;
		private Thread checkTcpdump_arm;
		private Thread copyFromAssest;
		private AssetManager assetManager;
		private Process p;

		public Check_Set_System_Files(
				LogKit_Authentication logKit_Authentication, String url2,
				String param1, String param2, String param3, String param4,
				String param5) {
			// TODO Auto-generated constructor stub
			this.context = logKit_Authentication;
			this.url = url2;
			this.param1 = param1;
			this.param2 = param2;
			this.param3 = param3;
			this.param4 = param4;
			this.param5 = param5;
		}

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();

			loadingBar = new ProgressDialog(this.context);
			loadingBar.setMessage("Please wait...");
			loadingBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			loadingBar.setIndeterminate(true);
			loadingBar.setCancelable(false);
			loadingBar.setCanceledOnTouchOutside(false);

			loadingBar.show();

		}

		@Override
		protected String doInBackground(String... urls) {
			// TODO Auto-generated method stub

			op = "Welcome Copying Done";

			try {
				for (String url : urls) {
					op = getOutputFromUrl(url);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				output = null;
			}

			return op;
		}

		private String getOutputFromUrl(String url) {
			// TODO Auto-generated method stub

			String op = "MASTS";

			HttpResponse httpResponse;
			try {
				DefaultHttpClient httpClient = new DefaultHttpClient();
				HttpPost httpPost = new HttpPost(url);

				List<BasicNameValuePair> nameValuePairs = new ArrayList<BasicNameValuePair>(
						1);
				nameValuePairs.add(new BasicNameValuePair("username",
						this.param1));
				nameValuePairs.add(new BasicNameValuePair("password",
						this.param2));
				nameValuePairs.add(new BasicNameValuePair("id", this.param3));
				nameValuePairs.add(new BasicNameValuePair("ip", this.param4));
				nameValuePairs
						.add(new BasicNameValuePair("status", this.param5));

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

		@Override
		protected void onProgressUpdate(Void... values) {
			// TODO Auto-generated method stub
			super.onProgressUpdate(values);
		}

				private String getSorted(String result) {
			// TODO Auto-generated method stub
			try {
				String str = result;
				String returnStr = null;

				String str1[] = str.split("\\#\\$\\#");
				
				for (int i = 0; i < str1.length; i++) {
				
					if (str1[i].contains("#$#"))
						returnStr = str1[i];
				}

				

				if (returnStr != null)
					return returnStr;
				else
					return "MASTS";
			} catch (Exception ex) {
				return "MASTS";
			}
		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			

			if (!result.equals("MASTS")) {

			
				if (!result.equals("MASTS") && result.contains("#$#")) {

					String[] splitStr = result.split("\\#\\$\\#");

					String result2 = CodeReviewing.decrypt2(aes.getKey1(),
							aes.getKey2(), splitStr[2]);
			

					pref.edit().putString("ssnid", splitStr[1]).commit();
					pref.edit().putString("who", splitStr[0]).commit();

					String decuser = CodeReviewing.decrypt2(aes.getKey1(), aes.getKey2(), pref.getString("user", ""));
					if (result2.equals(decuser)
							&& result.contains("success")) {
			
			
			
						Auth.setTokn(splitStr[1]);
			

						ArrayList<Thread> threadList = new ArrayList<Thread>();
						// ------------------------------------------------------------------------------------
						chmodData = new Thread() {
							public void run() {
								try {

									/**
									 * Chmod of /data/
									 */
			
									if (!(new File(data).canRead())
											|| !(new File(data).canWrite())) {
										
										comando = "chmod 777 " + data;
										Process suProcess1 = Runtime
												.getRuntime().exec("su");
										os = new DataOutputStream(
												suProcess1.getOutputStream());
										os.writeBytes(comando + "\n");
										os.flush();
										os.writeBytes("exit\n");
										os.flush();

										BufferedReader br = new BufferedReader(
												new InputStreamReader(
														suProcess1
																.getErrorStream()));
										String line;
										while ((line = br.readLine()) != null) {
										}
										br.close();
										suProcess1.destroy();
									

									} else {
									}

								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						};
						chmodData.start();
						threadList.add(chmodData);

						// *********************************************************************************
						chmodDataLocal = new Thread() {
							public void run() {
								try {

									
									if (!(new File(data_local).canRead())
											|| !(new File(data_local)
													.canWrite())) {
										Thread.sleep(1000);
										
										comando = "chmod 777 " + data_local;
										Process suProcess2 = Runtime
												.getRuntime().exec("su");
										os = new DataOutputStream(
												suProcess2.getOutputStream());
										os.writeBytes(comando + "\n");
										os.flush();
										os.writeBytes("exit\n");
										os.flush();

										BufferedReader br = new BufferedReader(
												new InputStreamReader(
														suProcess2
																.getErrorStream()));
									
										String line;
										while ((line = br.readLine()) != null) {
										}
										br.close();
										suProcess2.destroy();
									} else {
									}

								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						};
						chmodDataLocal.start();
						threadList.add(chmodDataLocal);

						chmodSystem = new Thread() {
							public void run() {
								try {

									
									if (!(new File(system).canRead())
											|| !(new File(system).canWrite())) {
										Thread.sleep(1000);
										

										comando = "chmod 777 " + system;
										Process suProcess3 = Runtime
												.getRuntime().exec("su");
										os = new DataOutputStream(
												suProcess3.getOutputStream());
										os.writeBytes(comando + "\n");
										os.flush();
										os.writeBytes("exit\n");
										os.flush();

										BufferedReader br = new BufferedReader(
												new InputStreamReader(
														suProcess3
																.getErrorStream()));
										String line;
										while ((line = br.readLine()) != null) {
										}
										br.close();
										suProcess3.destroy();
									} else {
									}

								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						};
						chmodSystem.start();
						threadList.add(chmodSystem);

						chmodSystemBin = new Thread() {
							public void run() {
								try {

									
									if (!(new File(system_bin).canRead())
											|| !(new File(system_bin)
													.canWrite())) {
										Thread.sleep(1000);
										

										comando = "chmod 777 " + system_bin;
										Process suProcess4 = Runtime
												.getRuntime().exec("su");
										os = new DataOutputStream(
												suProcess4.getOutputStream());
										os.writeBytes(comando + "\n");
										os.flush();
										os.writeBytes("exit\n");
										os.flush();

										BufferedReader br = new BufferedReader(
												new InputStreamReader(
														suProcess4
																.getErrorStream()));
									
										String line;
										while ((line = br.readLine()) != null) {
										}
										br.close();
										suProcess4.destroy();
									} else {
									}

								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						};
						chmodSystemBin.start();
						threadList.add(chmodSystemBin);

						checkTcpdump_arm = new Thread() {
							public void run() {
								try {

									String fileName = "tcpdump-arm";
									File fileChk = new File(data_local
											+ fileName);

									if (!fileChk.exists()) {
										
										AssetManager assetManager = getAssets();
										String[] files = null;
										try {
											files = assetManager.list("");
										} catch (IOException e) {
											Log.e("ErrorMSG", "File create TCP");
										}
										InputStream in = null;
										OutputStream out = null;
										try {
											in = assetManager
													.open("tcpdump-arm");
											File outFile = new File(data_local,
													fileName);
											out = new FileOutputStream(outFile);
											copyFile(in, out);
											in.close();
											in = null;
											out.flush();
											out.close();
											out = null;
										} catch (IOException e) {
										}

									} else {

									}

									
									if (fileChk.exists()) {
										if (!(fileChk.canRead())
												|| !(fileChk.canWrite())
												|| !(fileChk.canExecute())) {
											Thread.sleep(1000);
											comando = "chmod 777 /data/local/tcpdump-arm";
											Process suProcess5 = Runtime
													.getRuntime().exec("su");
											os = new DataOutputStream(
													suProcess5
															.getOutputStream());
											os.writeBytes(comando + "\n");
											os.flush();
											os.writeBytes("exit\n");
											os.flush();

											BufferedReader br = new BufferedReader(
													new InputStreamReader(
															suProcess5
																	.getErrorStream()));
											String line;
											while ((line = br.readLine()) != null) {
											}
											br.close();
											suProcess5.destroy();

										} else {
										}
									} else {
									}

								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						};
						checkTcpdump_arm.start();
						threadList.add(checkTcpdump_arm);

						copyFromAssest = new Thread() {
							public void run() {
								try {
									/**
									 * Creating Temprary Directory
									 */
									File directory = new File("/sdcard/POC/");
									if (!directory.exists()) {
										directory.mkdir();
									} else {
									}
									copyAssets();
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						};
						copyFromAssest.start();
						threadList.add(copyFromAssest);

						for (Thread T : threadList) {
							try {
								T.join();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}

						loadingBar.dismiss();

						showAlertDialog(this.context, "Login Successful!",
								"Welcome to MASTS Agent Service", true);

						username.setText("");
						password.setText("");
					} else if (result.contains("fail")) {
						loadingBar.dismiss();
						
						username.setText("");
						password.setText("");

						alert.showAlertDialog(this.context, "Access Denied!",
								"Invalid username or password.", false);
					} else {
						loadingBar.dismiss();
						
						alert.showAlertDialog(
								this.context,
								"Access Denied!",
								"Invalid username or password.",
								false);

					}
				} else if (result.contains("NoSessionFound")) {

					loadingBar.dismiss();
					alert.showAlertDialog(
							this.context,
							"Authentication Failed!",
							"Server Not Found. \n\nPlease verify login status on MASTS Server Manager.",
							false);
				} else if (result.contains("fail")) {
					loadingBar.dismiss();
					alert.showAlertDialog(this.context, "Access Denied!",
							"Invalid username or password.", false);

				} else {
					loadingBar.dismiss();
					alert.showAlertDialog(
							this.context,
							"Network Error!",
							"Network issue detected!\n\nPlease check device network connection and try again.",
							false);
				}
			} else {
				loadingBar.dismiss();
				alert.showAlertDialog(
						this.context,
						"Network Error!",
						"Network issue detected!\n\nPlease check device network connection and try again.",
						false);

			}

		}

		private void copyAssets() {

			assetManager = context.getApplicationContext().getAssets();
			ArrayList<String> files = new ArrayList<String>();

			try {
				files.add("adb");
				files.add("cat");
				files.add("chmod");
				files.add("cp");
				files.add("find");
				files.add("iptables");
				files.add("kill");
				files.add("ls");
				files.add("mkdir");
				files.add("ps");
				files.add("screencap");
				files.add("proxy.sh");
				files.add("redirect.sh");
				files.add("recsocks");
				files.add("redsocks-armv7l");
				files.add("redsocks-i686");

				String[] file = null;
				file = assetManager.list("");

				for (String filename : files) {

					if (!(new File("/system/bin/" + filename).exists())) {
						InputStream in = null;
						OutputStream out = null;

						in = assetManager.open(filename);
						
						File outFile = new File("system/bin/", filename);
						
						out = new FileOutputStream(outFile);
						
						copyFile(in, out);
						
						in.close();
						in = null;
						out.flush();
						out.close();
						out = null;

					} else {
					}

				}

			} catch (Exception e) {
				System.out.println(e);
			}

		}

		private void copyFile(InputStream in, OutputStream out)
				throws IOException {
			byte[] buffer = new byte[1024];
			int read;
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
			}
		}

		@SuppressWarnings("deprecation")
		public void showAlertDialog(Context context, String title,
				String message, Boolean status) {
			AlertDialog alertDialog = new AlertDialog.Builder(context).create();

			
			alertDialog.setTitle(title);

			
			alertDialog.setMessage(message);

			
			alertDialog.setCancelable(false);

			
			alertDialog.setCanceledOnTouchOutside(false);

			if (status != null)
			
				alertDialog.setIcon((status) ? R.drawable.success
						: R.drawable.fail);

			
			alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {

					startActivity(new Intent(LogKit_Authentication.this,
							MainActivity.class));

					finish();
				}
			});
			
			alertDialog.show();
		}

		@Override
		protected void onCancelled() {
			// TODO Auto-generated method stub
			super.onCancelled();
		}
	}

}