package com.rcompton.rhsr;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.rcompton.rhsr.backend.RHSRQuery;
import com.rcompton.rhsr.location.GPSManager;
import com.rcompton.rhsr.location.LocationHelper;

import org.joda.time.DateTime;

import java.io.File;
import java.util.Calendar;
import java.util.Random;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;

public class MainActivity extends Activity {

    private static final int CAMERA_REQ_CODE = 3125;
    String RHSR_MAIN_LOG_TAG = "rhsrMain";
    private RHSRBackendHandler rhsr;
    private String tweetDefault = "report";
    private String closestSpots;

    private double usersLat;
    private double usersLng;

    private LocationControl locationControlTask ;
    private boolean hasLocation = false;
    private LocationHelper locHelper;
    private boolean showGpsDisabledDefaultText = false;
    private boolean gotLocalSurfInfo = false;

    //to pass data to other activity
    public static final String SUBMITTED_REPORT_PHOTO =  "com.rcompton.rhsr.SUBMITTED_REPORT_PHOTO";
    public static final String SUBMITTED_REPORT_MESSAGE = "com.rcompton.rhsr.SUBMITTED_REPORT_MESSAGE";

    private Uri imageUri;
    private boolean tookPhoto = false;
    private static final int PIC_TWITTER_LINK_CHARS = 20; //http://goo.gl/zjvk12
    private String buoyDisplayStr;
    private SharedPreferences mSharedPreferences;
    private boolean tweeted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(RHSR_MAIN_LOG_TAG, "Oncreate lat " + usersLat);
        Log.i(RHSR_MAIN_LOG_TAG, "Oncreate lng " + usersLng);

        setupUI();

        mSharedPreferences = getApplicationContext().getSharedPreferences(MyConstants.PREFSNAME, 0);
        Log.i(RHSR_MAIN_LOG_TAG, mSharedPreferences.getAll().toString());

        tweeted = false;

        GPSManager gpsManager = new GPSManager(MainActivity.this);
        gpsManager.start();
//
//        try {
//            Thread.sleep(1300);
//        } catch (InterruptedException e) {
//            Log.e(RHSR_MAIN_LOG_TAG,"thread sleep",e);
//        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    void setupUI(){
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);    // Removes title bar

//        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);    // Removes notification bar

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        getWindow().setBackgroundDrawableResource(getRandomBackground());
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        setContentView(R.layout.activity_main);

        TextView ronText = (TextView) findViewById(R.id.ronTextMain);
        ronText.setText(Html.fromHtml(
                "Art: <a href=\"https://www.facebook.com/pages/Art-of-Ron-Croci/212905852073946\">Ron Croci</a>"
        ));
        ronText.setMovementMethod(LinkMovementMethod.getInstance());

        TextView dataText = (TextView) findViewById(R.id.dataTextMain);
        dataText.setText(Html.fromHtml(
                "Data: <a href=\"http://web.archive.org/web/20130121163929/http://www.listphile.com/Open_Surf_Atlas\">OpenSurfAtlas</a>" +
                        ", <a href=\"http://code.google.com/p/ndbc-buoy4j\">NOAA</a>" +
                        ", <a href=\"http://www.wunderground.com/weather/api\">wunderground api</a>"
        ));
        dataText.setMovementMethod(LinkMovementMethod.getInstance());

    }

    public static int getRandomBackground(){
        int[] images = new int[]{R.drawable.surfa4,
                R.drawable.s13,
                R.drawable.s32,
                R.drawable.surfgirl,
                R.drawable.surn12,
        };
        Random random = new Random();
        int whichImage = Math.abs(random.nextInt())%images.length;
        return images[whichImage];
    }

    @Override
    protected void onStart(){
        super.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.i(RHSR_MAIN_LOG_TAG, "onResume");

        EditText editText = (EditText)findViewById(R.id.editText0);

        if(hasLocation){ //has location
            if(!gotLocalSurfInfo){
                gotLocalSurfInfo = herokuConnect();
            }
        }

        if(showGpsDisabledDefaultText){
            editText.setText("enable GPS for local surf info", TextView.BufferType.EDITABLE);
        }

        if(gotLocalSurfInfo){
            Log.i(RHSR_MAIN_LOG_TAG,"gotLocalSurfInfo lat "+usersLat);
            Log.i(RHSR_MAIN_LOG_TAG,"gotLocalSurfInfo lng "+usersLng);

            //default report
            editText.setText(tweetDefault, TextView.BufferType.EDITABLE);

            //close spots
            TextView foundSpot = (TextView) findViewById(R.id.foundSpot);
            foundSpot.setMovementMethod(new ScrollingMovementMethod());
            foundSpot.setText(closestSpots);

            //close buoys
            TextView buoyDisplayText = (TextView) findViewById(R.id.buoyDisplay);
            buoyDisplayText.setMovementMethod(new ScrollingMovementMethod());
            buoyDisplayText.setText(buoyDisplayStr);
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        Log.i(RHSR_MAIN_LOG_TAG, "on pause");
    }

    @Override
    public void onStop(){
        super.onStop();
        Log.i(RHSR_MAIN_LOG_TAG, "on stop");
        if(locHelper != null)
            locHelper.stopLocationUpdates();
        if(locationControlTask != null)
            locationControlTask.cancel(true);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.i(RHSR_MAIN_LOG_TAG, "on destroy");

        if(locHelper != null)
            locHelper.stopLocationUpdates();
        if(locationControlTask != null)
            locationControlTask.cancel(true);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     *  User clicked download button.
     *  pauses UI until GPS is done
     */
    public void downloadButton(View view){

        //get gps location
        if(!hasLocation){
            //check for GPS
            LocationManager mlocManager = (LocationManager) MainActivity.this.getSystemService(Context.LOCATION_SERVICE);

            //location help setup
            locHelper = new LocationHelper();
            locHelper.getLocation(this.getApplicationContext(), locationResult);
            locationControlTask = new LocationControl();

            //GPS on, get to it.
            if (mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationControlTask.execute(this);
            }else{
                showGpsDisabledDefaultText = true;
            }
        }



    }

    /** Called when the user clicks the Photo Report button */
    public void addPhotoButton(View view) {
        capturePhoto();
    }


    /** Called when the user clicks the Tweet button */
    public void tweetButton(View view) {
        EditText editText = (EditText) findViewById(R.id.editText0);
        String submittedReport = editText.getText().toString();

        Log.i(RHSR_MAIN_LOG_TAG, "submittedReport length (must be under 120): "+submittedReport.length());

        if((submittedReport.length()+PIC_TWITTER_LINK_CHARS) > 140){
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage("Max characters allowed for Twitter: 140 \n" +
                    "Your report: "+(submittedReport.length()+PIC_TWITTER_LINK_CHARS))
                    .setCancelable(false)
                    .setPositiveButton("Edit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            WindowManager.LayoutParams wmlp = alertDialog.getWindow().getAttributes();
            wmlp.gravity = Gravity.TOP | Gravity.LEFT;
            wmlp.x = 100;   //x position
            wmlp.y = 100;   //y position
            alertDialog.show();
        }else{
              sendTweet(submittedReport);
//            Intent intent = new Intent(this, TweetActivity.class);
//            intent.putExtra(SUBMITTED_REPORT_MESSAGE, submittedReport);
//            if(tookPhoto)
//                intent.putExtra(SUBMITTED_REPORT_PHOTO, imageUri.getPath());
//            startActivity(intent);
        }
    }


    private void sendTweet(String tweetText){
        Log.i(RHSR_MAIN_LOG_TAG, tweetText);
        ConnectionDetector connectionDetector = new ConnectionDetector(this.getApplicationContext());
        if(connectionDetector.isConnectingToInternet()){
            if(isTwitterLoggedInAlready()){
                if(!tweeted){
                    showMessage("logged in as "+ mSharedPreferences.getString(MyConstants.PREF_USERNAME,""));
                    UpdateTwitterStatus updateTwitterStatus = new UpdateTwitterStatus();
                    updateTwitterStatus.setTweetText(tweetText);
                    updateTwitterStatus.setTweetPhoto(imageUri);
                    updateTwitterStatus.execute();
                }
            }else{
                Intent intent = new Intent(this, TwitterOAuthActivity.class);
                startActivity(intent);
            }
        }else{
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage("No network connection...");
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            WindowManager.LayoutParams wmlp = alertDialog.getWindow().getAttributes();
            wmlp.gravity = Gravity.TOP | Gravity.LEFT;
            wmlp.x = 100;   //x position
            wmlp.y = 100;   //y position
            alertDialog.show();
        }
    }


    /**
     * Check user already logged in your application using twitter Login flag is
     * fetched from Shared Preferences
     * */
    private boolean isTwitterLoggedInAlready() {
        // return twitter login status from Shared Preferences
        Log.i(RHSR_MAIN_LOG_TAG, MyConstants.PREF_KEY_TWITTER_LOGIN);
        return mSharedPreferences.getBoolean(MyConstants.PREF_KEY_TWITTER_LOGIN, false);

    }

    private void showMessage(String message) {
        // Show a popup message.
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * connect to my api
     */
    private boolean herokuConnect(){
        Log.i(RHSR_MAIN_LOG_TAG,"begin heroku connect function");

        if(usersLat == 0.0 && usersLng == 0.0){
            Log.e(RHSR_MAIN_LOG_TAG, "called heroku with 0 0");
            return false;
        }

        if(!hasLocation){
            Log.e(RHSR_MAIN_LOG_TAG, "called heroku with no location");
            return false;
        }

        String responseStr = "no data";
        try{
            responseStr  = new PolarThicketCaller().execute(usersLat,usersLng).get();
            rhsr = new RHSRBackendHandler(responseStr);
            Toast.makeText(getApplicationContext(), "lat="+usersLat+" lng="+usersLng, Toast.LENGTH_LONG).show();
        }catch(Exception e){
            Log.i(RHSR_MAIN_LOG_TAG, "failed to download heroku app response");
        }

        /** parse the heroku response **/
        try{

            if(rhsr.kmToClosestSpot() > 3.0){
                tweetDefault = "no known spots within "+rhsr.kmToClosestSpot()+"km\n" +
                        "swell: "+rhsr.getSwellInfo() + "\n" +
                        "wind: "+rhsr.getWindInfo().split("Gust")[0].replace("From the","") + "\n" +
                        "tide: "+rhsr.getTideInfo() + "\n" +
                        "water: "+rhsr.getClosestBuoyTemp()+"\n"+
                        " http://goo.gl/zjvk12";
            }else{
                tweetDefault = rhsr.getClosestSpotsAndDistances().get(0).split("\t")[0] + "\n"+
                        "swell: "+rhsr.getSwellInfo() + "\n" +
                        "wind: "+rhsr.getWindInfo().split("Gust")[0].replace("From the","") + "\n" +
                        "tide: "+rhsr.getTideInfo() + "\n" +
                        "water: "+rhsr.getClosestBuoyTemp()+"\n"+
                        " http://goo.gl/zjvk12";
            }

            //closest known spots and descriptions appear below
            if(rhsr.getClosestSpotsAndDescriptions().size() >= 5 ){
                closestSpots = "closest known spots: \n\n" +
                        rhsr.getClosestSpotsAndDescriptions().get(0) + "\n\n" +
                        rhsr.getClosestSpotsAndDescriptions().get(1) + "\n\n" +
                        rhsr.getClosestSpotsAndDescriptions().get(2) + "\n\n" +
                        rhsr.getClosestSpotsAndDescriptions().get(3) + "\n\n" +
                        rhsr.getClosestSpotsAndDescriptions().get(4);

            }else{
                closestSpots = "closest known spots: \n\n" + rhsr.getClosestSpotsAndDescriptions().get(0);
            }

            //buoy data below that
            buoyDisplayStr = rhsr.getNearbyBuoyDataForDisplay();

            return true;
        }catch (Exception e){
            Log.i(RHSR_MAIN_LOG_TAG, "failed to parse heroku app response");
        }
        return false;

    }

    /**
     * Background task to handle connection to heroku
     */
    private class PolarThicketCaller extends AsyncTask<Double, Integer, String> {
        ProgressDialog pdLoading = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //this method will be running on UI thread
            pdLoading.setMessage("\tDownloading local surf data...");
            pdLoading.setCancelable(false);
            pdLoading.show();
        }

        @Override
        protected String doInBackground(Double... latlng) {
            try {
                return RHSRQuery.apiRequest(latlng[0], latlng[1]);
            } catch (Exception e) {
                Log.i(RHSR_MAIN_LOG_TAG, "failed background heroku download");
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            pdLoading.setMessage("\tGot local surf info");
            pdLoading.dismiss();
        }
    }


    /**
     * Wait GPS fix
     */
    private class LocationControl extends AsyncTask<Context, Void, Void>{
        private final ProgressDialog dialog = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            dialog.setMessage("waiting for GPS fix...");
            dialog.setCanceledOnTouchOutside(false);

            Window window = dialog.getWindow();
            WindowManager.LayoutParams wlp = window.getAttributes();
            wlp.gravity = Gravity.TOP;
            //wlp.alpha = 1;
            wlp.screenBrightness = 1;
            wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            window.setAttributes(wlp);

            dialog.show();
        }

        @Override
        protected Void doInBackground(Context... params){
            Long t = Calendar.getInstance().getTimeInMillis();
            Long MAX_WAIT_MILLIS = 35000L;
            while (!hasLocation && Calendar.getInstance().getTimeInMillis() - t < MAX_WAIT_MILLIS) {
                try {
                    Log.i(RHSR_MAIN_LOG_TAG, "sleeping for hasLocation "+hasLocation + " until "+MAX_WAIT_MILLIS +" : "+ (Calendar.getInstance().getTimeInMillis() - t) );
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.e(RHSR_MAIN_LOG_TAG,"failed thread wait on gps fix",e);
                }
            };
            Log.i(RHSR_MAIN_LOG_TAG, "done waiting for GPS fix.. hasLocation: "+hasLocation + " until "+MAX_WAIT_MILLIS +" t: "+ (Calendar.getInstance().getTimeInMillis() - t) );
            dialog.dismiss();

            return null;
        }

        @Override
        protected void onPostExecute(Void unused){
            super.onPostExecute(unused);

            Log.i(RHSR_MAIN_LOG_TAG, "heroku connect with "+usersLat + " "+ usersLng);
            Log.i(RHSR_MAIN_LOG_TAG, "done with heroku connect, "+tweetDefault);

            //back to UI...
            onResume();
        }

    }

    /**
     * callback from GPS control. I have no idea how this works...
     */
    public LocationHelper.LocationResult locationResult = new LocationHelper.LocationResult()
    {
        @Override
        public void gotLocation(final Location location)
        {
            Log.i(RHSR_MAIN_LOG_TAG, "gotLocation callback worked!");
            hasLocation = true;
            usersLat = location.getLatitude();
            usersLng = location.getLongitude();
        }
    };


    //start camera
    public void capturePhoto() {
        String fname = android.os.Environment.DIRECTORY_DCIM + "rhsrPhoto"+ new DateTime() +".jpg";
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        File photoFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), fname
                );
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
        imageUri = Uri.fromFile(photoFile);
        startActivityForResult(intent, CAMERA_REQ_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CAMERA_REQ_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    Log.i(RHSR_MAIN_LOG_TAG," took photo");
                    tookPhoto = true;

                    //I guess I need to tell it explicitly
                    Log.i(RHSR_MAIN_LOG_TAG, "call onResume after photo taken");
                    onResume();

                }else{
                    Log.i(RHSR_MAIN_LOG_TAG,"photo fail!");
                }

        }
        return;

    }

    public void clearSharedPrefs(MenuItem mview){
        SharedPreferences mSharedPreferences = getApplicationContext().getSharedPreferences(MyConstants.PREFSNAME, 0);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.remove(MyConstants.PREF_USERNAME);
        editor.remove(MyConstants.PREF_KEY_TWITTER_LOGIN);
        editor.remove(MyConstants.PREF_KEY_OAUTH_SECRET);
        editor.remove(MyConstants.PREF_KEY_OAUTH_TOKEN);
        editor.clear();
        editor.commit();
        Toast.makeText(this, "logged out of Twitter", Toast.LENGTH_LONG).show();
    }


    /**
     * Class to update status
     **/
    class UpdateTwitterStatus extends AsyncTask<String, String, String> {

        ProgressDialog pDialog;
        private String tweetLink;
        private String tweetText;
        private Uri tweetPhoto;

        /**
         * Before starting background thread Show Progress Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Posting to Twitter...");
            //pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            Window window = pDialog.getWindow();
            WindowManager.LayoutParams wlp = window.getAttributes();
            wlp.gravity = Gravity.TOP;
            wlp.alpha = 1;
            wlp.screenBrightness = 1;
            wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            window.setAttributes(wlp);
            pDialog.show();
        }

        /**
         * getting Places JSON
         * */
        protected String doInBackground(String... args) {
            // Log.d("Tweet Text", "> " + args[0]);
            //String status = args[0]; //use global tweetText
            try {
                Long t = Calendar.getInstance().getTimeInMillis();
                Long MAX_WAIT_MILLIS = 135000L;
                while (!tweeted && Calendar.getInstance().getTimeInMillis() - t < MAX_WAIT_MILLIS) {

                    ConfigurationBuilder builder = new ConfigurationBuilder();
                    builder.setOAuthConsumerKey(MyConstants.TWITTER_CONSUMER_KEY);
                    builder.setOAuthConsumerSecret(MyConstants.TWITTER_CONSUMER_SECRET);

                    // Access Token
                    String access_token = mSharedPreferences.getString(MyConstants.PREF_KEY_OAUTH_TOKEN, "");
                    // Access Token Secret
                    String access_token_secret = mSharedPreferences.getString(MyConstants.PREF_KEY_OAUTH_SECRET, "");

                    AccessToken accessToken = new AccessToken(access_token, access_token_secret);
                    Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);

                    // Update status
                    StatusUpdate statusUpdate = null;

                    statusUpdate = new StatusUpdate(tweetText);
                    if(tookPhoto) {
                        File photoFile = new File(tweetPhoto.getPath());
                        statusUpdate.setMedia(photoFile);
                    }
                    twitter4j.Status postedResponse = twitter.updateStatus(statusUpdate);

                    Log.i(RHSR_MAIN_LOG_TAG, "success! "+postedResponse.getText());
                    tweetLink = "https://twitter.com/"+postedResponse.getId()+"/status/"+ postedResponse.getId();
                    Log.i(RHSR_MAIN_LOG_TAG, "> " + tweetLink);
                    tweeted = true;

                    Log.i(RHSR_MAIN_LOG_TAG, "now big bro report it");
                }

            } catch (Exception e) {
                // Error in updating status
                Log.d(RHSR_MAIN_LOG_TAG,"twitter error async",e);
            }
            return null;
        }

        /**
         * After completing background task Dismiss the progress dialog and show
         * the data in UI Always use runOnUiThread(new Runnable()) to update UI
         * from background thread, otherwise you will get error
         * **/
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after getting all products
            pDialog.dismiss();
            tweeted = true;


            // Linkify the message
            final SpannableString alertLink = new SpannableString(tweetLink);
            Linkify.addLinks(alertLink, Linkify.ALL);

            //bring up a dialog
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
            alertDialogBuilder.setMessage(alertLink)
                    .setCancelable(false)
                    .setPositiveButton("OK",new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
            ((TextView)alertDialog.findViewById(android.R.id.message))
                                  .setMovementMethod(LinkMovementMethod.getInstance());

            Log.i(RHSR_MAIN_LOG_TAG,"tweeted!");
        }

        public String getTweetLink() {
            return tweetLink;
        }

        public void setTweetLink(String tweetLink) {
            this.tweetLink = tweetLink;
        }

        public String getTweetText() {
            return tweetText;
        }

        public void setTweetText(String tweetText) {
            this.tweetText = tweetText;
        }

        public Uri getTweetPhoto() {
            return tweetPhoto;
        }

        public void setTweetPhoto(Uri tweetPhoto) {
            this.tweetPhoto = tweetPhoto;
        }
    }


}
