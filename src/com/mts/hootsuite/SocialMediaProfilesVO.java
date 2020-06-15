package com.mts.hootsuite;

import java.util.ArrayList;
import java.util.HashMap;

/****************************************************************************
 * <b>Title</b>: SocialMediaProfiles.java
 * <b>Project</b>: Hootsuite
 * <b>Description: </b> VO that holds a list of social media profile VOs. Can return either a hashmap, with all of the social media ids or a specific id by passing the name of the social network.
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author justinjeffrey
 * @version 3.0
 * @since May 19, 2020
 * @updates:
 ****************************************************************************/
public class SocialMediaProfilesVO {
	
	ArrayList<SocialMediaProfileVO> data = new ArrayList<>();
	String error;
	String error_description;
	
	/**
	 * Returns the social profile id of the profile name passed into it.
	 * @param name of the social profile you would like the id for
	 * @return the id of the social profile you requested
	 */
	public String getIdByName(String name) {
		String id = "";
		for (SocialMediaProfileVO profile: data) {
			if(profile.getType().equalsIgnoreCase(name)) {
				id = profile.getId();
			}
		}
		return id;
	}
	
	/**
	 * Used for getting a hashmap of all of the social media profiles.
	 * @returna HashMap with all of the social media profiles
	 */
	public HashMap<String, String> getAllSocialIds() {
		HashMap<String, String> ids = new HashMap<>();
		for (SocialMediaProfileVO profile: data) {
			ids.put(profile.getType(), profile.getId());
		}
		return ids;
	}

	/**
	 * @return the data
	 */
	public ArrayList<SocialMediaProfileVO> getData() {
		return data;
	}

	/**
	 * @param data the data to set
	 */
	public void setData(ArrayList<SocialMediaProfileVO> data) {
		this.data = data;
	}

	/**
	 * @return the error
	 */
	public String getError() {
		return error;
	}

	/**
	 * @param error the error to set
	 */
	public void setError(String error) {
		this.error = error;
	}

	/**
	 * @return the error_description
	 */
	public String getError_description() {
		return error_description;
	}

	/**
	 * @param error_description the error_description to set
	 */
	public void setError_description(String error_description) {
		this.error_description = error_description;
	}
}