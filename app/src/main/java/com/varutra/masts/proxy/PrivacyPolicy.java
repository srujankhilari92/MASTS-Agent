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

public class PrivacyPolicy extends Activity{
	TextView first,second,third,fourth,fifth,sixth,seventh,eight,nine,ten,eleven, twelve, thirteen,fourteen,fifteen,sixteen,seventeen;
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.policymastsagent);
		
		first = (TextView) findViewById(R.id.policytextView);		
		first.setText(Html.fromHtml(getString(R.string.policy0)));
		
		
		second = (TextView) findViewById(R.id.policytextView1);		
		second.setText(Html.fromHtml(getString(R.string.policy1)));
		
		third = (TextView) findViewById(R.id.policytextView2);		
		third.setText(Html.fromHtml(getString(R.string.policy2)));
		
		
		fourth = (TextView) findViewById(R.id.policytextView3);		
		fourth.setText(Html.fromHtml(getString(R.string.policy3)));
		
		fifth = (TextView) findViewById(R.id.policytextView4);		
		fifth.setText(Html.fromHtml(getString(R.string.policy4)));
		
		
		sixth = (TextView) findViewById(R.id.policytextView5);		
		sixth.setText(Html.fromHtml(getString(R.string.policy5)));
		
		seventh = (TextView) findViewById(R.id.policytextView6);		
		seventh.setText(Html.fromHtml(getString(R.string.policy6)));
		
		
		eight = (TextView) findViewById(R.id.policytextView7);		
		eight.setText(Html.fromHtml(getString(R.string.policy7)));
		
		nine = (TextView) findViewById(R.id.policytextView8);		
		nine.setText(Html.fromHtml(getString(R.string.policy8)));
		
		
		ten = (TextView) findViewById(R.id.policytextView9);		
		ten.setText(Html.fromHtml(getString(R.string.policy9)));
		
		eleven = (TextView) findViewById(R.id.policytextView10);		
		eleven.setText(Html.fromHtml(getString(R.string.policy10)));
		
		
		twelve = (TextView) findViewById(R.id.policytextView11);		
		twelve.setText(Html.fromHtml(getString(R.string.policy11)));
		
		thirteen = (TextView) findViewById(R.id.policytextView12);		
		thirteen.setText(Html.fromHtml(getString(R.string.policy12)));
		
		
		fourteen = (TextView) findViewById(R.id.policytextView13);		
		fourteen.setText(Html.fromHtml(getString(R.string.policy13)));
		
		fifteen = (TextView) findViewById(R.id.policytextView14);		
		fifteen.setText(Html.fromHtml(getString(R.string.policy14)));
		
		
		sixteen = (TextView) findViewById(R.id.policytextView15);		
		sixteen.setText(Html.fromHtml(getString(R.string.policy15)));
		

		seventeen = (TextView) findViewById(R.id.policytextView16);		
		seventeen.setText(Html.fromHtml(getString(R.string.policy16)));
		
		
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
            startActivity(new Intent(PrivacyPolicy.this,
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
    	 startActivity(new Intent(PrivacyPolicy.this,
					MainActivity.class));
    	
    }
}
