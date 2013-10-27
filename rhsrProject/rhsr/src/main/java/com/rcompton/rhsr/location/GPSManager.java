package com.rcompton.rhsr.location;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public class GPSManager {

    private Activity activity;
    private LocationManager mlocManager;
    private LocationListener gpsListener;

    public GPSManager(Activity activity) {
        this.activity = activity;
    }

    public void start() {
        mlocManager = (LocationManager) activity
                .getSystemService(Context.LOCATION_SERVICE);

        if (mlocManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            setUp();
            findLoc();
        } else {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                    activity);
            alertDialogBuilder
                    .setMessage("GPS is disabled in your device," +
                            " you'll need it to get local surf info." +
                            " Enable GPS?")
                    .setCancelable(false)
                    .setPositiveButton("Enable GPS",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    Intent callGPSSettingIntent = new Intent(
                                            android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                    activity.startActivity(callGPSSettingIntent);
                                }
                            });
            alertDialogBuilder.setNegativeButton("Cancel",
                    new DialogInterface.OnClickListener() {
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

        }
    }

    public void setUp() {
        gpsListener = new GPSListener(activity, mlocManager);
    }

    public double[] findLoc() {
        mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1, gpsListener);

        if (mlocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) == null)
            Toast.makeText(activity, "LAST Location null", Toast.LENGTH_SHORT).show();
        else {
            try{
                gpsListener.onLocationChanged(mlocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));

            }catch(Exception e){
                Log.e("rhsrgps", "fail gps"+e.getMessage());
            }
        }

        return null;

    }
}