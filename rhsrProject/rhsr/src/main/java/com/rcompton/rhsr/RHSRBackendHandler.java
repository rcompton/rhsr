package com.rcompton.rhsr;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ryan on 9/15/13.
 */
public class RHSRBackendHandler {

    private JSONObject responseJson;

    public RHSRBackendHandler(String responseStr) throws IOException,JSONException {
        try{
            responseJson = new JSONObject(responseStr);
        }catch(Exception e){
            responseJson = null;
        }
    }

    public String getTideInfo() {
        try {
            return responseJson.getString("tideInfo");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "no tide data";
    }

    public String getWindInfo()  {
        try {
            return responseJson.getString("windInfo");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "no wind data";
    }


    public double kmToClosestSpot(){
        double out = 20000;
        try{
            JSONArray spots = responseJson.getJSONArray("nearbySurfSpots");
            out = spots.getJSONObject(0).getDouble("distanceKm");
        }catch (Exception e){
            e.printStackTrace();
        }
        return out;
    }

    /**
     * Ideal default surf report
     * @return
     */
    public List<String> getClosestSpotsAndDistances() {
        List<String> out = new ArrayList<String>();
        try{
            JSONArray spots = responseJson.getJSONArray("nearbySurfSpots");
            for(int i=0; i < spots.length(); i++){
                String spotAndDis = spots.getJSONObject(i).getString("name")+"\t"+
                        spots.getJSONObject(i).getDouble("distanceKm");
                out.add(spotAndDis);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return out;
    }

    /**
     * Figure the text to display to the user under the EditText
     * @return
     */
    public List<String> getClosestSpotsAndDescriptions() {
        List<String> out = new ArrayList<String>();
        try{
            JSONArray spots = responseJson.getJSONArray("nearbySurfSpots");
            for(int i=0; i < spots.length(); i++){
                String spotAndDis = spots.getJSONObject(i).getString("name")+",  "+
                        spots.getJSONObject(i).getDouble("distanceKm") + "km,  " +
                        spots.getJSONObject(i).getString("description");
                out.add(spotAndDis);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return out;
    }

    public String getSwellInfo() {
        try{
            JSONArray stations = responseJson.getJSONObject("buoyData").getJSONArray("stations");
            for(int i=0; i<stations.length(); i++){
                JSONObject station = stations.getJSONObject(i);
                if(station.has("waveDirection") && station.has("waveHeight")){
                    try{
                        return station.getString("waveHeight")+ " " +
                                station.getString("waveDirection").split("\\s+")[0];
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return "no swell data";
    }

    public String getClosestBuoyName() {
        try{
            JSONArray stations = responseJson.getJSONObject("buoyData").getJSONArray("stations");
            for(int i=0; i<stations.length(); i++){
                JSONObject station = stations.getJSONObject(i);
                if(station.has("waveDirection") && station.has("waveHeight")){
                    try{
                        return station.getString("title")+ "; " +
                                station.getString("distance").split("\\s+")[0];
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "no buoy data";
    }

    public String getClosestBuoyTemp() {
        try{
            JSONArray stations = responseJson.getJSONObject("buoyData").getJSONArray("stations");
            for(int i=0; i<stations.length(); i++){
                JSONObject station = stations.getJSONObject(i);
                if(station.has("waveDirection") && station.has("waveHeight")){
                    try{
                        String temp = Integer.parseInt(station.getString("waterTemperature")
                                .substring(0,2)) + "F";
                        return temp;
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return "no temperature data";
    }


    public String getNearbyBuoyDataForDisplay(){
        String output = "buoy data:\n\n";
        try{
            JSONArray stations = responseJson.getJSONObject("buoyData").getJSONArray("stations");
            for(int i=0; i < Math.min(5,stations.length()); i++){
                JSONObject station = stations.getJSONObject(i);
                if(station.has("waveDirection") && station.has("waveHeight")
                        && station.has("wavePeriodMean") && station.has("wavePeriodDominant")){
                    try{
                        output = output + station.getString("title") + "\n" +
                                station.getString("distance") + "\n" +
                                "wave height: "+station.getString("waveHeight")+"\n"+
                                "wave direction: "+station.getString("waveDirection") +"\n"+
                                "avg period: "+station.getString("wavePeriodMean")+"\n"+
                                "dominant period: "+station.getString("wavePeriodDominant") +"\n"+
                                "water temperature: "+station.getString("waterTemperature").substring(0,2) + "F\n";
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return output;
    }
}
