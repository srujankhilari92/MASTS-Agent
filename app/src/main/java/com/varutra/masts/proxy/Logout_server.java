package com.varutra.masts.proxy;

import com.varutra.masts.proxy.Logout.Logout_session_stop;
import com.varutra.masts.proxy.R;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

public class Logout_server extends Activity {
	public ProgressBar progressbar;
	private int progressStatus = 0;
	private Handler handler = new Handler();

	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.logout);
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		boolean isEnableProxy = settings.getBoolean("isEnabled", false);
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
					System.out.println("EXc=" + e);
				} finally {
					MainActivity.mainActivity.finish();
					MainActivity.planetList.clear();
					startActivity(new Intent(Logout_server.this,
							LogKit_Authentication.class));
					finish();
				}
			}
		};
		MainActivity.mainActivity.finish();
		welcomeThread.start();

	}

}