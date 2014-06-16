package com.rcompton.rhsr;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import twitter4j.Twitter;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

public class TwitterOAuthActivity extends Activity implements TwitterOAuthView.Listener {
    private static final String LOG_TAG = "rhsr";

    private static final boolean DUMMY_CALLBACK_URL = true;

    // Twitter
    private static Twitter twitter;
    private static RequestToken requestToken;

    // Shared Preferences
    private static SharedPreferences mSharedPreferences;

    //madness
    private TwitterOAuthView view;
    private boolean oauthStarted;

    private String tweetText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSharedPreferences = getApplicationContext().getSharedPreferences(MyConstants.PREFSNAME, 0);

        // Create an instance of TwitterOAuthView.
        view = new TwitterOAuthView(this);
        setContentView(view);
        oauthStarted = false;
    }


    @Override
    protected void onResume(){
        super.onResume();

        if (oauthStarted){
            return;
        }
        oauthStarted = true;

        // Start Twitter OAuth process. Its result will be notified via
        // TwitterOAuthView.Listener interface.
        view.start(MyConstants.TWITTER_CONSUMER_KEY, MyConstants.TWITTER_CONSUMER_SECRET,
                MyConstants.TWITTER_CALLBACK_URL, DUMMY_CALLBACK_URL, this);
    }

    public void onSuccess(TwitterOAuthView view, AccessToken accessToken){
        // The application has been authorized and an access token
        // has been obtained successfully. Save the access token
        // for later use.
        showMessage("Authorized by " + accessToken.getScreenName());

        SharedPreferences.Editor e = mSharedPreferences.edit();

        // After getting access token, access token secret
        // store them in application preferences
        e.putString(MyConstants.PREF_KEY_OAUTH_TOKEN, accessToken.getToken());
        e.putString(MyConstants.PREF_KEY_OAUTH_SECRET, accessToken.getTokenSecret());
        e.putString(MyConstants.PREF_USERNAME, accessToken.getScreenName());

        // Store login status - true
        e.putBoolean(MyConstants.PREF_KEY_TWITTER_LOGIN, true);
        e.commit(); // save changes

        Log.i("Twitter OAuth Token", "> " + accessToken.getToken() + "\n" + e.toString());
    }

    public void onFailure(TwitterOAuthView view, TwitterOAuthView.Result result)
    {
        // Failed to get an access token.
        showMessage("Failed due to " + result);
    }

    private void showMessage(String message)
    {
        // Show a popup message.
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

}
