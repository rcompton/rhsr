package com.rcompton.rhsr.backend;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;


public class SubmittedReport {

	String text;
	String link;
	String userName;
	Location userLoc;
	
	public SubmittedReport(String text, String link, String userName,
			Location userLoc) {
		super();
		this.text = text;
		this.link = link;
		this.userName = userName;
		this.userLoc = userLoc;
	}

	public static SubmittedReport fromMongoIndexString(String in){
		try{
			JSONObject inj = new JSONObject(in);
			String text = inj.getString("text");
			String link = inj.getString("link");	
			String userName = inj.getString("userName");
			Location userLoc = Location.fromGeoJson(inj.getString("userLoc"));	
			return new SubmittedReport(text, link, userName, userLoc);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}



	public String toMongoIndexString() throws JSONException{
		JSONObject out = new JSONObject();		
		out.put("text", this.text);
		out.put("link", this.link);
		out.put("userName", this.userName);
		out.put("userLoc", new JSONObject(this.userLoc.toGeoJson()));
		return out.toString();	
	}

	@Override
	public String toString() {
		Gson gson = new Gson();
		return gson.toJson(this);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((link == null) ? 0 : link.hashCode());
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		result = prime * result + ((userLoc == null) ? 0 : userLoc.hashCode());
		result = prime * result
				+ ((userName == null) ? 0 : userName.hashCode());
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
		SubmittedReport other = (SubmittedReport) obj;
		if (link == null) {
			if (other.link != null)
				return false;
		} else if (!link.equals(other.link))
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		if (userLoc == null) {
			if (other.userLoc != null)
				return false;
		} else if (!userLoc.equals(other.userLoc))
			return false;
		if (userName == null) {
			if (other.userName != null)
				return false;
		} else if (!userName.equals(other.userName))
			return false;
		return true;
	}



}
