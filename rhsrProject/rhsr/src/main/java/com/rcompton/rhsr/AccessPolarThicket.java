package com.rcompton.rhsr;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.net.URL;

class AccessPolarThicket extends AsyncTask<String, Void, String> {



    @Override
    protected String doInBackground(String[] urlIn) {
        try {
            URL url= new URL(urlIn[0]);
            InputStream urlStream = url.openStream();
            return IOUtils.toString(urlStream, "UTF-8");
        } catch (Exception e) {
            Log.i("URL",e.toString());
        }
        return null;
    }


}