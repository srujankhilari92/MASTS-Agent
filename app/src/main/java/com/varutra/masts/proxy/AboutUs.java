/**
 * 
 */
package com.varutra.masts.proxy;

import com.varutra.plugin.gui.Proxy_main_activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

/**
 *
 * @author Sudhakar barde (Varutra consulting pvt ltd)
 *
 */

public class AboutUs extends Activity{
	TextView first,second,third,fourth;
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.aboutmastsagent);
		
		first = (TextView) findViewById(R.id.textView1);		
		first.setTag(Html.fromHtml(getString(R.string.aboutus1)));
		
		
		second = (TextView) findViewById(R.id.textView2);		
		second.setTag(Html.fromHtml(getString(R.string.aboutus2)));
		
		third = (TextView) findViewById(R.id.textView3);		
		third.setTag(Html.fromHtml(getString(R.string.aboutus3)));
		
		
		fourth = (TextView) findViewById(R.id.textView4);		
		fourth.setTag(Html.fromHtml(getString(R.string.aboutus4)));
		
		
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub

    	getMenuInflater().inflate(R.menu.v_menu, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case R.id.MASTS_service:			
            finish();
            startActivity(new Intent(AboutUs.this,
					MainActivity.class));
			break;
				default:
			break;
		}

		return super.onOptionsItemSelected(item);
	}
    @Override
	public void onBackPressed() {
    	 finish();
    	 startActivity(new Intent(AboutUs.this,
					MainActivity.class));
    	
    }
}
