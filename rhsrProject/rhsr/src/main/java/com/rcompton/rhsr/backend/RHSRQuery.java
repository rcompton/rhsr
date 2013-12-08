package com.rcompton.rhsr.backend;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by ryan on 9/15/13.
 */
public class RHSRQuery {

    public static String apiRequest(double usersLat, double usersLng) throws IOException {
        URL surfInfoUrl = new URL("http://polar-thicket-8603.herokuapp.com/services/localsurfinfo/"+
                usersLat+","+usersLng);
        InputStream urlStream = surfInfoUrl.openStream();
        return IOUtils.toString(urlStream, "UTF-8");
    }


}
