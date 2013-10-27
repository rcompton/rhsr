package com.rcompton.rhsr;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;

import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;


public class PhotoReportButtonActivity extends Activity {

    private final static String  RHSR_PHOTO_BUTTON ="rhsrPhotoButton";
    private final static int CAMERA_REQ_CODE = 123456;
    private final static int TWITTER_REQ_CODE = 1234567;


    private Uri imageUri;
    private Bitmap photoBitMap = null;

    private String tweetText;
    private boolean gotBGBitMap = false;

    // Login button
    Button btnLoginTwitter;
    ImageButton btnUpdateTwitter;

    //t4j
    private Twitter twitter;
    private RequestToken requestToken;

    // Shared Preferences
    private static SharedPreferences mSharedPreferences;

    //network on main
    StrictMode.ThreadPolicy policy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_report_button_activity);
        //setupActionBar();
        Log.i(RHSR_PHOTO_BUTTON, "photo button oncreate");


        //yeah f async
        policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        mSharedPreferences = getApplicationContext().getSharedPreferences("MyPref", 0);

        Log.i(RHSR_PHOTO_BUTTON,mSharedPreferences.getAll().toString());

        //pull the text from the main intent
        Intent mainIntent = getIntent();
        tweetText = mainIntent.getStringExtra(MainActivity.SUBMITTED_REPORT_MESSAGE);

        //start camera and take photo
        //leaves you in onActivityResult
        // waits in onPause()
       // capturePhoto();
    }

    @Override
    protected void onStart(){
        super.onStart();
        Log.i(RHSR_PHOTO_BUTTON, "photo button onstart");
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.i(RHSR_PHOTO_BUTTON, "photo button onresume");


        mSharedPreferences = getApplicationContext().getSharedPreferences("MyPref", 0);

        Log.i(RHSR_PHOTO_BUTTON,mSharedPreferences.getAll().toString());

        //display report
        TextView report = (TextView)findViewById(R.id.textViewPhotoActivity);
        report.setText(tweetText);

        //got photo
        if(gotBGBitMap){
            Log.i(RHSR_PHOTO_BUTTON, "try to display "+imageUri.getPath());
            try{
                ImageView tweetPhoto = (ImageView)findViewById(R.id.imageViewPhotoActivity);
                tweetPhoto.setImageBitmap(photoBitMap);
            } catch (Exception e) {
                Log.i(RHSR_PHOTO_BUTTON, "no bitmap loaded... " + e.getMessage());
            }
        }else {
            Log.i(RHSR_PHOTO_BUTTON, "no photo");
        }


        btnLoginTwitter = (Button)findViewById(R.id.twitterLoginButton);
        if(isTwitterLoggedInAlready())
            btnLoginTwitter.setVisibility(View.INVISIBLE);
        /**
         * Twitter login button click event will call loginToTwitter() function
         * */
        btnLoginTwitter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                // Call login twitter function
                loginToTwitter();
            }
        });

        /** This if conditions is tested once is
         * redirected from twitter page. Parse the uri to get oAuth
         * Verifier
         * */
        if (!isTwitterLoggedInAlready()) {
            Uri uri = getIntent().getData();
            if (uri != null && uri.toString().startsWith(MyConstants.TWITTER_CALLBACK_URL)) {
                // oAuth verifier
                String verifier = uri.getQueryParameter(MyConstants.URL_TWITTER_OAUTH_VERIFIER);
                try {
                    // Get the access token
                    AccessToken accessToken = twitter.getOAuthAccessToken(requestToken, verifier);
                    Log.w(RHSR_PHOTO_BUTTON,accessToken.toString());

                    Log.e("Twitter OAuth Token", "> " + accessToken.getToken());

                    // Hide login button
                    btnLoginTwitter.setVisibility(View.GONE);

                    // Show Update Twitter
                    btnUpdateTwitter.setVisibility(View.VISIBLE);

                    // Getting user details from twitter
                    // For now i am getting his name only
                    //long userID = accessToken.getUserId();
                    //User user = twitter.showUser(userID);
                    //String username = user.getName();

                    // Displaying in xml ui
                    //lblUserName.setText(Html.fromHtml("<b>Welcome " + username + "</b>"));
                } catch (Exception e) {
                    // Check log for login errors
                    Log.e(RHSR_PHOTO_BUTTON, "> ", e);
                }
            }
        }else{
            /**
             * Button click event to Update Status, will call updateTwitterStatus()
             * function
             * */
            btnUpdateTwitter = (ImageButton)findViewById(R.id.twitterUpdateButton);
            btnUpdateTwitter.setVisibility(View.VISIBLE);

            btnUpdateTwitter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Call update status function
                    // Get the status from EditText
                    //String status = tweetText;
                    String status = UUID.randomUUID().toString();

                    // Check for blank text
                    if (status.trim().length() > 0) {
                        // update status
                        new updateTwitterStatus().execute(status);
                    } else {
                        // EditText is empty
                        Toast.makeText(getApplicationContext(),
                                "Please enter status message", Toast.LENGTH_SHORT)
                                .show();
                    }
                }
            });

        }

    }

    @Override
    public void onPause(){
        super.onPause();
        Log.i(RHSR_PHOTO_BUTTON, "photo paused");
    }    boolean isFirstResume = true;


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


//    //simple call to t4j, checks for internet connection and sends a tweet.
//    protected boolean sendTweetToRyansAccount(String tweetText, Uri anImageUri){
//
//        //assert we've got internet
//        ConnectionDetector connectionDetector = new ConnectionDetector(getApplicationContext());
//        if(!connectionDetector.isConnectingToInternet()){
//            TextView textView = new TextView(this);
//            textView.setTextSize(40);
//            textView.setText("bruddah I tink you need da internet");
//
//            //setContentView(R.layout.photo_report_button_activity);
//            setContentView(textView);
//            return false;
//        }
//
//        //lazy fix for network on main
//        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//        StrictMode.setThreadPolicy(policy);
//
//        String CONSUMER_KEY = "DiJ0O1WJSwphr19vuA";
//        String CONSUMER_SECRET = "cdOeuS8dcj8wZAhqI2lIG40RgbW7WA4s0idwZ4iYmS8";
//        Twitter twitter = TwitterFactory.getSingleton();
//        twitter.setOAuthConsumer(CONSUMER_KEY,CONSUMER_SECRET);
//        AccessToken accessToken = new AccessToken("13036062-0QfetCQZxpI5AhF2PXSeO6o7NGPnirRnIXe1xGTLc",
//                "PRrgnD49ncyTStSP6VCL1nZkeq0fFQwlwkdWf0s");
//        twitter.setOAuthAccessToken(accessToken);
//
//        StatusUpdate statusUpdate = null;
//        try{
//            statusUpdate = new StatusUpdate(tweetText);
//            statusUpdate.setMedia(new File(anImageUri.getPath()));
//            Status status = twitter.updateStatus(statusUpdate);
//            Log.i(RHSR_PHOTO_BUTTON, "success! "+status.getText());
//            //tweetLink = "https://twitter.com/"+status.getId()+
//            //           "/status/"+ status.getId();
//            return true;
//        }catch(Exception e){
//            StringWriter sw = new StringWriter();
//            PrintWriter pw = new PrintWriter(sw);
//            e.printStackTrace(pw);
//            Log.e(RHSR_PHOTO_BUTTON, sw.toString());
//        }
//        return false;
//    }

    //start camera
    public void capturePhoto() {
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        File photoFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),
                "rhsrPhoto"+UUID.randomUUID().toString()+".jpg");
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
                    Log.i(RHSR_PHOTO_BUTTON," took photo");
                    try {
                        photoBitMap = decodeFileToBitMap(new File(imageUri.getPath()));
                        if(photoBitMap != null){
                            gotBGBitMap = true;
                            Log.i(RHSR_PHOTO_BUTTON, "got bitmap...");
                        }
                    } catch (Exception e) {
                        Log.i(RHSR_PHOTO_BUTTON, "get photo bitmap fail");
                    }

                    //I guess I need to tell it explicitly
                    Log.i(RHSR_PHOTO_BUTTON, "call onResume after photo taken");
                    onResume();

                    //Bitmap smallImage = (Bitmap) data.getExtras().get("data");
                    //this.imageView.setImageBitmap(smallImage);
                    //Toast.makeText(getApplicationContext(),"ok"+imageUri.toString(),Toast.LENGTH_SHORT);
                    //Uri selectedImageUri = imageUri;
                    //boolean tweetSuccess = sendTweetToRyansAccount(tweetText,selectedImageUri);
                    //if(tweetSuccess){
                    //    Log.i(PBUTTON_LOG_TAG, "tweet with photo success!!!!!!!");
                    //}
                }else{
                    Log.i(RHSR_PHOTO_BUTTON,"photo fail!");
                }

        }
        return;

    }

    //decodes image and scales it to reduce memory consumption
    private Bitmap decodeFileToBitMap(File f){
        try {
            //Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f),null,o);

            //The new size we want to scale to
            final int REQUIRED_SIZE=150;

            //Find the correct scale value. It should be the power of 2.
            int scale=1;
            while(o.outWidth/scale/2>=REQUIRED_SIZE && o.outHeight/scale/2>=REQUIRED_SIZE)
                scale*=2;

            //Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize=scale;

            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {}
        return null;
    }


    // private boolean resumeHasRun = false;


//        if (!resumeHasRun) {
//            resumeHasRun = true;
//            return;
//        }

//        Log.i(PBUTTON_LOG_TAG,"onResume");
    //this.imageView = (ImageView)this.findViewById(R.id.imageView0);

//        Intent mainIntent = getIntent();
//        tweetText = mainIntent.getStringExtra(MainActivity.SUBMITTED_REPORT_MESSAGE);
//        TextView textView = new TextView(this);
//        textView.setTextSize(20);
//        textView.setText(tweetText);
//        setContentView(textView);

    // ImageView image = (ImageView) findViewById(R.id.imageView1);
    //this.imageView = (ImageView)this.findViewById(R.id.imageView0);

    //imageView.setImageURI(imageUri);
    //imageView.getLayoutParams().height = 20;
    //setContentView(imageView);


    //       Log.i(PBUTTON_LOG_TAG,"photo report text set.");


    // }


    /**
     * Function to login twitter
     * */
    private void loginToTwitter() {
        Log.i(RHSR_PHOTO_BUTTON,"login to twitter....");
        // Check if already logged in
        if (!isTwitterLoggedInAlready()) {
            ConfigurationBuilder builder = new ConfigurationBuilder();
            builder.setOAuthConsumerKey(MyConstants.TWITTER_CONSUMER_KEY);
            builder.setOAuthConsumerSecret(MyConstants.TWITTER_CONSUMER_SECRET);
            Configuration configuration = builder.build();

            TwitterFactory factory = new TwitterFactory(configuration);
            twitter = factory.getInstance();

            try {
                requestToken = twitter
                        .getOAuthRequestToken(MyConstants.TWITTER_CALLBACK_URL);
                this.startActivityForResult(new Intent(Intent.ACTION_VIEW, Uri.parse(requestToken.getAuthenticationURL())),
                                                TWITTER_REQ_CODE);
            } catch (TwitterException e) {
                Log.e(RHSR_PHOTO_BUTTON, "???", e);
            }
        } else {
            // user already logged into twitter
            Toast.makeText(getApplicationContext(),
                    "Already Logged into twitter", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Check user already logged in your application using twitter Login flag is
     * fetched from Shared Preferences
     * */
    private boolean isTwitterLoggedInAlready() {
        // return twitter login status from Shared Preferences
        return mSharedPreferences.getBoolean(MyConstants.PREF_KEY_TWITTER_LOGIN, false);
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
            pDialog = new ProgressDialog(PhotoReportButtonActivity.this);
            pDialog.setMessage("Updating to twitter...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        /**
         * getting Places JSON
         * */
        protected String doInBackground(String... args) {
            Log.d("Tweet Text", "> " + args[0]);
            //String status = args[0]; //use global tweetText
            try {
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
                if(gotBGBitMap)
                    statusUpdate.setMedia(new File(imageUri.getPath()));
                twitter4j.Status postedResponse = twitter.updateStatus(statusUpdate);
                Log.i(RHSR_PHOTO_BUTTON, "success! "+postedResponse.getText());
                String tweetLink = "https://twitter.com/"+postedResponse.getId()+"/status/"+ postedResponse.getId();

                Log.d("Status", "> " + postedResponse.getText());
            } catch (TwitterException e) {
                // Error in updating status
                Log.d("Twitter Update Error", e.getMessage());
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
            // updating UI from Background Thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            "Status tweeted successfully", Toast.LENGTH_SHORT)
                            .show();
//                    // Clearing EditText field
//                    txtUpdate.setText("");
                }
            });
        }

    }

}

