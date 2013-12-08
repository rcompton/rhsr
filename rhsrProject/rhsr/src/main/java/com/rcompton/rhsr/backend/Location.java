package com.rcompton.rhsr.backend;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.*;

public class Location {

	double latitude;
	double longitude;

	public Location(double latitude, double longitude) {
		super();
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public Location() {
		super();
		this.latitude = 0.0;
		this.longitude = 0.0;
	}

	public static double distFrom(double lat1, double lng1, double lat2, double lng2) {
	    double earthRadius = 6371;//km
	    double dLat = Math.toRadians(lat2-lat1);
	    double dLng = Math.toRadians(lng2-lng1);
	    double sindLat = Math.sin(dLat / 2);
	    double sindLng = Math.sin(dLng / 2);
	    double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
	            * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));
	    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
	    double dist = earthRadius * c;

	    return dist;
	    }

	public double getLatitude() {
		return latitude;
	}
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	public double getLongitude() {
		return longitude;
	}
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}



	public String toGeoJson() {
		try{
			JSONObject output = new JSONObject();
			output.put("type","Point");
			JSONArray longLat = new JSONArray();
			longLat.put(this.longitude);
			longLat.put(this.latitude);
			output.put("coordinates", longLat);
			return output.toString();
		}catch(JSONException e){
			e.printStackTrace();
		}
		return null;
	}

	public static Location fromGeoJson(String geoJSON) {
		try{
			JSONObject in = new JSONObject(geoJSON);
			double lat = in.getJSONArray("coordinates").getDouble(1);
			double lng = in.getJSONArray("coordinates").getDouble(0);
			return new Location(lat,lng);
		}catch(JSONException e){
			e.printStackTrace();
		}
		return null;
	}
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(latitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(longitude);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Location other = (Location) obj;
		if (Double.doubleToLongBits(latitude) != Double
				.doubleToLongBits(other.latitude))
			return false;
		if (Double.doubleToLongBits(longitude) != Double
				.doubleToLongBits(other.longitude))
			return false;
		return true;
	}

	/**
	 * not geoJSON
	 */
	@Override
	public String toString() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}

}
