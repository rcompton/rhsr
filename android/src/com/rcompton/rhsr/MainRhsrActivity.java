package com.rcompton.rhsr;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;

public class MainRhsrActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_rhsr);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_rhsr, menu);
		return true;
	}

    /**
     * Photo report button
     */
    public void photoReportButton(View view){
    	Intent intent = new Intent(this, PhotoReportButtonActivity.class);
    	
        startActivity(intent);
    }
	
}
