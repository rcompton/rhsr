package com.rcompton.rhsr;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Calendar;
import java.util.Random;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

public class TweetActivity extends Activity{

    private static final String LOG_TAG = "rhsrTweet";

    // Twitter
    private static Twitter twitter;
    private static RequestToken requestToken;

    // Shared Preferences
    private static SharedPreferences mSharedPreferences;

    // Alert Dialog Manager
    //AlertDialogManager alert = new AlertDialogManager();

    private String tweetText;
    private String tweetLink;
    private File tweetPhoto;
    private boolean tookPhoto = false;
    private boolean tweeted = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupUI();

        //pull the text from the main intent
        Intent mainIntent = getIntent();
        tweetText = mainIntent.getStringExtra(MainActivity.SUBMITTED_REPORT_MESSAGE);

        if(mainIntent.hasExtra(MainActivity.SUBMITTED_REPORT_PHOTO)){
            tookPhoto = true;
            tweetPhoto = new File(mainIntent.getStringExtra(MainActivity.SUBMITTED_REPORT_PHOTO));
        }

        //for login cred
        mSharedPreferences = getApplicationContext().getSharedPreferences(MyConstants.PREFSNAME, 0);
        Log.i(LOG_TAG, mSharedPreferences.getAll().toString());

//        //display report
//        TextView report = (TextView) findViewById(R.id.yourReport);
//        report.setText(tweetText);

        //if connected, send tweet
        ConnectionDetector connectionDetector = new ConnectionDetector(this.getApplicationContext());
        if(connectionDetector.isConnectingToInternet()){
            if(isTwitterLoggedInAlready()){
                if(!tweeted){
                    showMessage("logged in as "+ mSharedPreferences.getString(MyConstants.PREF_USERNAME,""));
                    new updateTwitterStatus().execute();
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


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    void setupUI(){
//        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);    // Removes notification bar

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);    // Removes title bar
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        getWindow().setBackgroundDrawableResource(MainActivity.getRandomBackground());
        setContentView(R.layout.activity_tweet);


        TextView ronText = (TextView) findViewById(R.id.ronTextPost);
        ronText.setText(Html.fromHtml(
                "Art: <a href=\"https://www.facebook.com/pages/Art-of-Ron-Croci/212905852073946\">Ron Croci</a>"
        ));
        ronText.setMovementMethod(LinkMovementMethod.getInstance());

        TextView dataText = (TextView) findViewById(R.id.dataTextPost);
        dataText.setText(Html.fromHtml(
                "Data: <a href=\"http://web.archive.org/web/20130121163929/http://www.listphile.com/Open_Surf_Atlas\">OpenSurfAtlas</a>" +
                        ", <a href=\"http://code.google.com/p/ndbc-buoy4j\">NOAA</a>" +
                        ", <a href=\"http://www.wunderground.com/weather/api\">wunderground api</a>"
        ));
        dataText.setMovementMethod(LinkMovementMethod.getInstance());

    }

    @Override
    public void onResume(){
        super.onResume();

        TextView link = (TextView) findViewById(R.id.tweetLink);
        link.setText("Your report is available: \n\n"+tweetLink);
        link.setTextSize(12);
        link.setMovementMethod(LinkMovementMethod.getInstance());

    }


    /**
     * Check user already logged in your application using twitter Login flag is
     * fetched from Shared Preferences
     * */
    private boolean isTwitterLoggedInAlready() {
        // return twitter login status from Shared Preferences
        Log.i(LOG_TAG, MyConstants.PREF_KEY_TWITTER_LOGIN);
        return mSharedPreferences.getBoolean(MyConstants.PREF_KEY_TWITTER_LOGIN, false);

    }

    private void showMessage(String message) {
        // Show a popup message.
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }


    /**
     * Function to update status
     * */
    class updateTwitterStatus extends AsyncTask<String, String, String> {

        ProgressDialog pDialog;
        /**
         * Before starting background thread Show Progress Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(TweetActivity.this);
            pDialog.setMessage("Posting to Twitter...");
            //pDialog.setIndeterminate(false);
            //pDialog.setCancelable(false);
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
                    if(tookPhoto)
                        statusUpdate.setMedia(tweetPhoto);
                    twitter4j.Status postedResponse = twitter.updateStatus(statusUpdate);

                    //Log.i(RHSR_PHOTO_BUTTON, "success! "+postedResponse.getText());
                    tweetLink = "https://twitter.com/"+postedResponse.getId()+"/status/"+ postedResponse.getId();
                    Log.d("Status", "> " + postedResponse.getText());
                    tweeted = true;

                    Log.i(LOG_TAG, "now big bro report it");
                }

            } catch (Exception e) {
                // Error in updating status
                Log.d(LOG_TAG,"twitter error async",e);
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
            onResume(); //I have to call it to get the textview to refresh
        }

    }

}


