package com.rcompton.rhsr;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;

public class PhotoReportButtonActivity extends Activity {

	private static final String JPEG_FILE_PREFIX = "rhsrPhoto_";
	private static final int IMAGE_CAPTURE_REQUEST_CODE = 1234;
	private static final String TAG = "myLog";

	//used to get the photo I just took
	private String mCurrentPhotoPath = null;

	/**
	 * Calls an external camera program 
	 * @param actionCode
	 * @throws IOException 
	 */
	private void dispatchTakePictureIntent(){

		//create file to save
		File f = null;
		try {
			f = createImageFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//use create intent that will use default camera app
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		//append the file to the camera intent 
		takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));

		//ok...
		startActivityForResult(takePictureIntent, IMAGE_CAPTURE_REQUEST_CODE);
	}

	/**
	 * Save the photo to disk
	 * mCurrentPhotoPath lets me get the photo I just took.
	 * @return
	 * @throws IOException
	 */
	private File createImageFile() throws IOException {
		// Create an image file name
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'").format(new Date());
		String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
		File image = File.createTempFile( imageFileName, ".jpg", getAlbumDir() );
		mCurrentPhotoPath = image.getAbsolutePath();
		return image;
	}


	/* Photo album for this application */
	private String getAlbumName() {
		return getString(R.string.album_name);
	}


	private File getAlbumDir() {
		File storageDir = null;
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			//only works on API 8+
			storageDir = new File( Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_PICTURES ), getAlbumName() );
			if (storageDir != null) {
				if (! storageDir.mkdirs()) {
					if (! storageDir.exists()){
						Log.d("CameraSample", "failed to create directory");
						return null;
					}
				}
			}
		} else {
			Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
		}
		return storageDir;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		 ContentValues values = new ContentValues ();

		    values.put (Media.IS_PRIVATE, 1);
		    values.put (Media.TITLE, "Rhsr Mobile Private Image");
		    values.put (Media.DESCRIPTION, "Surf report.");

		    Uri picUri = null;
			try {
				picUri = Uri.fromFile(createImageFile());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		    //Keep a reference in app for now, we might need it later.
		    //((XeniosMob) getCallingActivity ().getApplication ()).setCamPicUri (picUri);
		    Intent takePicture = new Intent (MediaStore.ACTION_IMAGE_CAPTURE);

		    //May or may not be populated depending on devices.
		    takePicture.putExtra (MediaStore.EXTRA_OUTPUT, picUri);

		    startActivityForResult(takePicture, IMAGE_CAPTURE_REQUEST_CODE);
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
	    if(requestCode == IMAGE_CAPTURE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
	        Log.i(TAG, "Image is saved.");
	    }
	}
	
	
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.photo_report_button, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
