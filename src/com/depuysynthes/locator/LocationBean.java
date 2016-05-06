package com.depuysynthes.locator;

// Java 7
import java.io.Serializable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/****************************************************************************
 * <b>Title: </b>LocationBean.java <p/>
 * <b>Project: </b>WC_Custom <p/>
 * <b>Description: </b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2016<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0<p/>
 * @since Feb 10, 2016<p/>
 *<b>Changes: </b>
 * Feb 10, 2016: David Bargerhuff: Created class.
 ****************************************************************************/
public class LocationBean implements Serializable {
		
	/**
	 * 
	 */
	private static final long serialVersionUID = 836327441951736728L;

	/**
	 * Populates bean fields from JSON.
	 * @param rs
	 */
    public LocationBean(JsonElement jsonElement) {
    	latitude = new Double(0.0);
    	longitude = new Double(0.0);
    	this.setData(jsonElement);
    }
    
    // location data
    private int surgeonId;
    private int clinicId;
    private String clinicName;
    private int locationId;
    private String locationName; // in case we ever need it.
    private String address;
    private String address2;
    private String city;
    private String state;
    private String zip;
    private String country;
    private String phoneNumber;
    private int statusId;
    private Double latitude;
    private Double longitude;
    private String uniqueId;
    private double distance;

    /**
     * 
     * @param rs
     */
    private void setData(JsonElement jsonElement) {
    	JsonObject loc = jsonElement.getAsJsonObject();
    	surgeonId = (loc.has("surgeonId") ? loc.get("surgeonId").getAsInt() : 0);
    	clinicId = (loc.has("clinicId") ? loc.get("clinicId").getAsInt() : 0);
    	clinicName = (loc.has("clinicName") ? loc.get("clinicName").getAsString() : null);
    	locationId = (loc.has("locationId") ? loc.get("locationId").getAsInt() : 0);
    	locationName = (loc.has("locationName") ? loc.get("locationName").getAsString() : null);
    	address = (loc.has("address") ? loc.get("address").getAsString() : null);
    	address2 = (loc.has("address2") ? loc.get("address2").getAsString() : null);
    	city = (loc.has("city") ? loc.get("city").getAsString() : null);
    	state =  (loc.has("state") ? loc.get("state").getAsString() : null);
    	zip =  (loc.has("zip") ? loc.get("zip").getAsString() : null);
    	country = (loc.has("country") ? loc.get("country").getAsString() : null);
    	phoneNumber = (loc.has("phoneNumber") ? loc.get("phoneNumber").getAsString() : null);
    	statusId = (loc.has("statusId") ? loc.get("statusId").getAsInt() : 0);
    	latitude = (loc.has("latitude") ? loc.get("latitude").getAsDouble() : 0.0);
    	longitude = (loc.has("longitude") ? loc.get("longitude").getAsDouble() : 0.0);
    	uniqueId = (loc.has("uniqueId") ? loc.get("uniqueId").getAsString() : null);
    	distance = (loc.has("distance") ? loc.get("distance").getAsDouble() : 0.0);
    }
    
	/**
	 * @return the surgeonId
	 */
	public int getSurgeonId() {
		return surgeonId;
	}
	/**
	 * @param surgeonId the surgeonId to set
	 */
	public void setSurgeonId(int surgeonId) {
		this.surgeonId = surgeonId;
	}
	/**
	 * @return the clinicId
	 */
	public int getClinicId() {
		return clinicId;
	}
	/**
	 * @param clinicId the clinicId to set
	 */
	public void setClinicId(int clinicId) {
		this.clinicId = clinicId;
	}
	/**
	 * @return the clinicName
	 */
	public String getClinicName() {
		return clinicName;
	}
	/**
	 * @param clinicName the clinicName to set
	 */
	public void setClinicName(String clinicName) {
		this.clinicName = clinicName;
	}
	/**
	 * @return the locationId
	 */
	public int getLocationId() {
		return locationId;
	}
	/**
	 * @param locationId the locationId to set
	 */
	public void setLocationId(int locationId) {
		this.locationId = locationId;
	}
	/**
	 * @return the locationName
	 */
	public String getLocationName() {
		return locationName;
	}
	/**
	 * @param locationName the locationName to set
	 */
	public void setLocationName(String locationName) {
		this.locationName = locationName;
	}
	/**
	 * @return the uniqueId
	 */
	public String getUniqueId() {
		return uniqueId;
	}
	/**
	 * @param uniqueId the uniqueId to set
	 */
	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}
	/**
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}
	/**
	 * @param address the address to set
	 */
	public void setAddress(String address) {
		this.address = address;
	}
	/**
	 * @return the address2
	 */
	public String getAddress2() {
		return address2;
	}
	/**
	 * @param address2 the address2 to set
	 */
	public void setAddress2(String address2) {
		this.address2 = address2;
	}
	/**
	 * @return the city
	 */
	public String getCity() {
		return city;
	}
	/**
	 * @param city the city to set
	 */
	public void setCity(String city) {
		this.city = city;
	}
	/**
	 * @return the state
	 */
	public String getState() {
		return state;
	}
	/**
	 * @param state the state to set
	 */
	public void setState(String state) {
		this.state = state;
	}

	/**
	 * @return the zip
	 */
	public String getZip() {
		return zip;
	}

	/**
	 * @param zip the zip to set
	 */
	public void setZip(String zip) {
		this.zip = zip;
	}

	/**
	 * @return the country
	 */
	public String getCountry() {
		return country;
	}
	/**
	 * @param country the country to set
	 */
	public void setCountry(String country) {
		this.country = country;
	}
	
	/**
	 * @return the phoneNumber
	 */
	public String getPhoneNumber() {
		return phoneNumber;
	}

	/**
	 * @param phoneNumber the phoneNumber to set
	 */
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	/**
	 * @return the distance
	 */
	public double getDistance() {
		return distance;
	}
	/**
	 * @param distance the distance to set
	 */
	public void setDistance(double distance) {
		this.distance = distance;
	}
	/**
	 * @return the statusId
	 */
	public int getStatusId() {
		return statusId;
	}
	/**
	 * @param statusId the statusId to set
	 */
	public void setStatusId(int statusId) {
		this.statusId = statusId;
	}
	/**
	 * @return the latitude
	 */
	public Double getLatitude() {
		return latitude;
	}
	/**
	 * @param latitude the latitude to set
	 */
	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}
	/**
	 * @return the longitude
	 */
	public Double getLongitude() {
		return longitude;
	}
	/**
	 * @param longitude the longitude to set
	 */
	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}
 	
}
