package com.ram.action.data;

// SMT Base Libs
import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.Location;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: InventoryLocationVO.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> manages the location information for the inventory event
 * Class extends the Geocode location object, adding a few additional variables to the class
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Feb 13, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class InventoryLocationVO extends GeocodeLocation {
	// Member Variables
	protected String inventoryLocationId = null;
	protected String locationName = null;
	protected String profileId = null;
	protected UserDataVO contact = null;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param addr
	 * @param city
	 * @param state
	 * @param zip
	 */
	public InventoryLocationVO(String addr, String city, String state, String zip) {
		super(addr, city, state, zip);
	}

	/**
	 * @param fullAddress
	 */
	public InventoryLocationVO(String fullAddress) {
		super(fullAddress);
	}

	/**
	 * @param l
	 */
	public InventoryLocationVO(Location l) {
		super(l);
	}

	/**
	 * 
	 */
	public InventoryLocationVO() {
		super();
	}
	
	/**
	 * 
	 * @param rs
	 */
	public InventoryLocationVO(ResultSet rs) {
		super();
		this.setData(rs);
	}
	
	/**
	 * 
	 * @param rs
	 */
	public InventoryLocationVO(ActionRequest req) {
		super();
		this.setData(req);
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return StringUtil.getToString(this);
	}

	/**
	 * Assigns the value of a field in the row set to a particular member
	 * variable
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		inventoryLocationId = db.getStringVal("inventory_location_id", rs);
		locationName = db.getStringVal("location_nm", rs);
		address = db.getStringVal("address_txt", rs);
		address2 = db.getStringVal("address2_txt", rs);
		city = db.getStringVal("city_nm", rs);
		state = db.getStringVal("state_cd", rs);
		country = db.getStringVal("country_cd", rs);
		zipCode = db.getStringVal("zip_cd", rs);
		this.setMatchCode(db.getStringVal("match_cd", rs));
		latitude = db.getDoubleVal("latitude_no", rs);
		longitude = db.getDoubleVal("longitude_no", rs);
		profileId = db.getStringVal("profile_id", rs);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.gis.Location#setData(com.siliconmtn.http.SMTServletRequest)
	 */
	public void setData(ActionRequest req) {
		super.setData(req);
		
		profileId = req.getParameter("profileId");
		inventoryLocationId = req.getParameter("inventoryLocationId");
		locationName = req.getParameter("locationName");
		this.setContact(new UserDataVO(req));
	}

	/**
	 * @return the inventoryLocationId
	 */
	public String getInventoryLocationId() {
		return inventoryLocationId;
	}

	/**
	 * @param inventoryLocationId the inventoryLocationId to set
	 */
	public void setInventoryLocationId(String inventoryLocationId) {
		this.inventoryLocationId = inventoryLocationId;
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
	 * @return the profileId
	 */
	public String getProfileId() {
		return profileId;
	}

	/**
	 * @param profileId the profileId to set
	 */
	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	/**
	 * @return the contact
	 */
	public UserDataVO getContact() {
		return contact;
	}

	/**
	 * @param contact the contact to set
	 */
	public void setContact(UserDataVO contact) {
		this.contact = contact;
	}
	
	/**
	 * 
	 * @return
	 */
	public boolean hasContactAssigned() {
		if (StringUtil.checkVal(profileId).length() == 0) return false;
		else return true;
	}
}
