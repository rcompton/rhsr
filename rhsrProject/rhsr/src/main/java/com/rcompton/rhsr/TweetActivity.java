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
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_tweet);
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//
//        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//        StrictMode.setThreadPolicy(policy);
//
//        //pull the text from the main intent
//        Intent mainIntent = getIntent();
//        tweetText = mainIntent.getStringExtra(MainActivity.SUBMITTED_REPORT_MESSAGE);
//
//        //display report
//        EditText report = (EditText)findViewById(R.id.txtUpdateStatus);
//        report.setText(tweetText);
//
//        cd = new ConnectionDetector(getApplicationContext());
//
//        // Check if Internet present
//        if (!cd.isConnectingToInternet()) {
//            Log.e(LOG_TAG,"no internet");
//
//            // Internet Connection is not present
//            //alert.showAlertDialog(MainActivity.this, "Internet Connection Error",
//            //        "Please connect to working Internet connection", false);
//            // stop executing code by return
//            return;
//        }
//
//        // Check if twitter keys are set
//        if(TWITTER_CONSUMER_KEY.trim().length() == 0 || TWITTER_CONSUMER_SECRET.trim().length() == 0){
//            // Internet Connection is not present
//           // alert.showAlertDialog(MainActivity.this, "Twitter oAuth tokens", "Please set your twitter oauth tokens first!", false);
//            // stop executing code by return
//            return;
//        }
//
//        // All UI elements
//        btnLoginTwitter = (Button) findViewById(R.id.btnLoginTwitter);
//        btnUpdateStatus = (Button) findViewById(R.id.btnUpdateStatus);
//        btnLogoutTwitter = (Button) findViewById(R.id.btnLogoutTwitter);
//        txtUpdate = (EditText) findViewById(R.id.txtUpdateStatus);
//        lblUpdate = (TextView) findViewById(R.id.lblUpdate);
//        lblUserName = (TextView) findViewById(R.id.lblUserName);
//
//        // Shared Preferences
//        mSharedPreferences = getApplicationContext().getSharedPreferences("MyPrefs", 0);
//
//        Log.i(LOG_TAG, mSharedPreferences.getAll().toString());
//
//        /**
//         * Twitter login button click event will call loginToTwitter() function
//         * */
//        btnLoginTwitter.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View arg0) {
//                // Call login twitter function
//                loginToTwitter();
//            }
//        });
//
//
//        /** This if conditions is tested once is
//         * redirected from twitter page. Parse the uri to get oAuth
//         * Verifier
//         * */
//        if (!isTwitterLoggedInAlready()) {
//            Uri uri = getIntent().getData();
//            if (uri != null && uri.toString().startsWith(TWITTER_CALLBACK_URL)) {
//                // oAuth verifier
//                String verifier = uri
//                        .getQueryParameter(URL_TWITTER_OAUTH_VERIFIER);
//
//                try {
//                    // Get the access token
//                    AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verifier);
//
//                    // Shared Preferences
//                    SharedPreferences.Editor e = mSharedPreferences.edit();
//
//                    // After getting access token, access token secret
//                    // store them in application preferences
//                    e.putString(PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
//                    e.putString(PREF_KEY_OAUTH_SECRET,
//                            accessToken.getTokenSecret());
//                    // Store login status - true
//                    e.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
//                    e.commit(); // save changes
//
//                    Log.e("Twitter OAuth Token", "> " + accessToken.getToken());
//
//                    // Hide login button
//                    btnLoginTwitter.setVisibility(View.GONE);
//
//                    // Show Update Twitter
//                    lblUpdate.setVisibility(View.VISIBLE);
//                    txtUpdate.setVisibility(View.VISIBLE);
//                    btnUpdateStatus.setVisibility(View.VISIBLE);
//                    btnLogoutTwitter.setVisibility(View.VISIBLE);
//
//                    // Getting user details from twitter
//                    // For now i am getting his name only
//                    long userID = accessToken.getUserId();
//                    User user = twitter.showUser(userID);
//                    String username = user.getName();
//
//                    // Displaying in xml ui
//                    lblUserName.setText(Html.fromHtml("<b>Welcome " + username + "</b>"));
//                } catch (Exception e) {
//                    // Check log for login errors
//                    Log.e("Twitter Login Error", "> " + e.getMessage());
//                }
//            }
//        }
//
//        /**
//         * Button click event to Update Status, will call updateTwitterStatus()
//         * function
//         * */
//        btnUpdateStatus.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                // Call update status function
//                // Get the status from EditText
//                String status = txtUpdate.getText().toString();
//
//                // Check for blank text
//                if (status.trim().length() > 0) {
//                    // update status
//                    new updateTwitterStatus().execute(status);
//                } else {
//                    // EditText is empty
//                    Toast.makeText(getApplicationContext(),
//                            "Please enter status message", Toast.LENGTH_SHORT)
//                            .show();
//                }
//            }
//        });

//}




//    /**
//     * Function to login twitter
//     * */
//    private void loginToTwitter() {
//        // Check if already logged in
//        if (!isTwitterLoggedInAlready()) {
//            ConfigurationBuilder builder = new ConfigurationBuilder();
//            builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
//            builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
//            Configuration configuration = builder.build();
//
//            TwitterFactory factory = new TwitterFactory(configuration);
//            twitter = factory.getInstance();
//
//            try {
//                requestToken = twitter.getOAuthRequestToken(TWITTER_CALLBACK_URL);
//                this.startActivity(new Intent(Intent.ACTION_VIEW, Uri
//                        .parse(requestToken.getAuthenticationURL())));
//            } catch (TwitterException e) {
//                Log.e(LOG_TAG, "???", e);
//            }
//        } else {
//            // user already logged into twitter
//            Toast.makeText(getApplicationContext(),
//                    "Already Logged into twitter", Toast.LENGTH_LONG).show();
//        }
//    }




//    @Override
//    protected void onResume()
//    {
//        super.onResume();
//
//        if (oauthStarted){
//            return;
//        }
//
//        oauthStarted = true;
//
//        // Start Twitter OAuth process. Its result will be notified via
//        // TwitterOAuthView.Listener interface.
//        view.start(MyConstants.TWITTER_CONSUMER_KEY, MyConstants.TWITTER_CONSUMER_SECRET,
//                MyConstants.TWITTER_CALLBACK_URL, DUMMY_CALLBACK_URL, this);
//    }
//
//    public void onSuccess(TwitterOAuthView view, AccessToken accessToken)
//    {
//        // The application has been authorized and an access token
//        // has been obtained successfully. Save the access token
//        // for later use.
//        showMessage("Authorized by " + accessToken.getScreenName());
//    }
//
//
//    public void onFailure(TwitterOAuthView view, TwitterOAuthView.Result result)
//    {
//        // Failed to get an access token.
//        showMessage("Failed due to " + result);
//    }
//
//
//    private void showMessage(String message)
//    {
//        // Show a popup message.
//        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
//    }

//
///**
// * Function to update status
// * */
//class updateTwitterStatus extends AsyncTask<String, String, String> {
//
//    /**
//     * Before starting background thread Show Progress Dialog
//     * */
//    @Override
//    protected void onPreExecute() {
//        super.onPreExecute();
//        pDialog = new ProgressDialog(TweetActivity.this);
//        pDialog.setMessage("Updating to twitter...");
//        pDialog.setIndeterminate(false);
//        pDialog.setCancelable(false);
//        pDialog.show();
//    }
//
//    /**
//     * getting Places JSON
//     * */
//    protected String doInBackground(String... args) {
//        Log.d("Tweet Text", "> " + args[0]);
//        String status = args[0];
//        try {
//            ConfigurationBuilder builder = new ConfigurationBuilder();
//            builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
//            builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
//
//            // Access Token
//            String access_token = mSharedPreferences.getString(PREF_KEY_OAUTH_TOKEN, "");
//            // Access Token Secret
//            String access_token_secret = mSharedPreferences.getString(PREF_KEY_OAUTH_SECRET, "");
//
//            AccessToken accessToken = new AccessToken(access_token, access_token_secret);
//            Twitter twitter = new TwitterFactory(builder.build()).getInstance(accessToken);
//
//            // Update status
//            twitter4j.Status response = twitter.updateStatus(status);
//
//            Log.d("Status", "> " + response.getText());
//        } catch (TwitterException e) {
//            // Error in updating status
//            Log.d("Twitter Update Error", e.getMessage());
//        }
//        return null;
//    }
//
//    /**
//     * After completing background task Dismiss the progress dialog and show
//     * the data in UI Always use runOnUiThread(new Runnable()) to update UI
//     * from background thread, otherwise you will get error
//     * **/
//    protected void onPostExecute(String file_url) {
//        // dismiss the dialog after getting all products
//        pDialog.dismiss();
//        // updating UI from Background Thread
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                Toast.makeText(getApplicationContext(),
//                        "Status tweeted successfully", Toast.LENGTH_SHORT)
//                        .show();
//                // Clearing EditText field
//                txtUpdate.setText("");
//            }
//        });
//    }
//
//}


