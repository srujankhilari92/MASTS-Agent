package com.varutra.masts.proxy;

import com.varutra.masts.proxy.R;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

public class VulnSplashScreen extends Activity {

	public ProgressBar progressbar;
	private Handler handler = new Handler();
	private int progressStatus = 0;
	private Thread welcomeThread;
	private boolean backPress = false;
	private RelativeLayout rl;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.welcome_screen);
		rl = (RelativeLayout) findViewById(R.id.splashScreenRL);

		progressbar = (ProgressBar) findViewById(R.id.loadingBar);

		final int welcomeScreenDisplay = 3000;

		welcomeThread = new Thread() {

			int wait = 0;

			@Override
			public void run() {
				try {
					super.run();
					TransitionDrawable transition = (TransitionDrawable) rl
							.getBackground();
					transition.startTransition(1000);
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
					if (backPress == false)
						startActivity(new Intent(VulnSplashScreen.this,
								Check_Set_IP_Port.class));
					finish();
				}
			}
		};
		welcomeThread.start();
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		backPress = true;
		try {
			welcomeThread.destroy();
		} catch (Exception ex) {
			finish();
		}
	}
}