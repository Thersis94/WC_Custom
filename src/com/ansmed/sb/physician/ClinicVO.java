package com.ansmed.sb.physician;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.Location;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.gis.MapLocationVO;

/****************************************************************************
 * <b>Title</b>: ClinicVO.java<p/>
 * <b>Description: </b> Value object that stores a record for the clinic 
 * information.  The GeocodeLocation object is extended because the location
 * information for each clinic will be geocoded
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Apr 26, 2007
 ****************************************************************************/
public class ClinicVO extends GeocodeLocation {
	private static final long serialVersionUID = 1l;
	private String clinicId = null;
	private String clinicName = null;
	private String address2 = null;
	private String address3 = null;
	private Integer locationTypeId = new Integer(0);
	private String locationTypeName = null;
	private Integer locatorDisplay = new Integer(0);
	private List<PhoneVO> phones = null;
	private Integer manualGeocodeFlag = Integer.valueOf(0);
	
	/**
	 * 
	 */
	public ClinicVO() {
		super();
		phones = new ArrayList<PhoneVO>();
	}
	
	public ClinicVO(ResultSet rs) {
		super();
		phones = new ArrayList<PhoneVO>();
		setData(rs);
	}
	
	
	public ClinicVO(ActionRequest req) {
		super();
		phones = new ArrayList<PhoneVO>();
		setData(req);
	}
	
	public String getEncodedAddress() {
		return super.getURLEncodedAddress(",");
	}
	
	
	public void setData(ActionRequest req) {
		StringEncoder se = new StringEncoder();
		clinicId = req.getParameter("clinicId");
		clinicName = se.decodeValue(req.getParameter("clinicName"));
		address = se.decodeValue(req.getParameter("address"));
		address2 = se.decodeValue(req.getParameter("address2"));
		address3 = se.decodeValue(req.getParameter("address3"));
		city = se.decodeValue(req.getParameter("city"));
		state = req.getParameter("physState");
		zipCode = req.getParameter("physZipCode");
		locationTypeId = Convert.formatInteger(req.getParameter("locationTypeId"));
		locatorDisplay = Convert.formatInteger(req.getParameter("locatorDisplay"));
		manualGeocodeFlag = Convert.formatInteger(req.getParameter("manualGeocodeFlag"));
	}
	
	/**
	 * Gets the address info as a Location Object
	 * @return
	 */
	public Location getLocation() {
		Location loc = new Location();
		loc.setAddress(address);
		loc.setCity(city);
		loc.setState(state);
		loc.setZipCode(zipCode);
		
		return loc;
	}

	
	/**
	 * Assigns a location object to the individual variables
	 * @param loc
	 */
	public void setLocation(Location loc) {
		this.setAddress(loc.getAddress());
		this.setCity(loc.getCity());
		this.setState(loc.getState());
		this.setZipCode(loc.getZipCode());
		this.setCassValidated(loc.isCassValidated() ? 1 : 0);
	}
	
	/**
	 * Adds a row of data from the db to the bean
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		clinicId = db.getStringVal("clinic_id", rs);
		clinicName = db.getStringVal("clinic_nm", rs);
		address = db.getStringVal("address_txt", rs);
		address2 = db.getStringVal("address2_txt", rs);
		address3 = db.getStringVal("address3_txt", rs);
		city = db.getStringVal("city_nm", rs);
		state = db.getStringVal("state_cd", rs);
		country = db.getStringVal("country_cd", rs);
		zipCode = db.getStringVal("zip_cd", rs);
		latitude = db.getDoubleVal("latitude_no", rs);
		longitude = db.getDoubleVal("longitude_no", rs);
		setMatchCode(db.getStringVal("geo_match_cd", rs));
		locationTypeId = db.getIntegerVal("location_type_id", rs);
		locatorDisplay = db.getIntegerVal("locator_display_flg", rs);
		locationTypeName = db.getStringVal("type_nm", rs);
		manualGeocodeFlag = db.getIntVal("manual_geocode_flg", rs);
		
		// Add the main phone number
		String pn = db.getStringVal("phone_number_txt", rs);
		PhoneVO vo = new PhoneVO("2", pn, country);
		this.addPhone(vo);
		
	}
	
	/**
	 * Bulds a map marker for the google maps
	 * @return
	 */
	public String getMapMarker() {
		StringBuffer sb = new StringBuffer();
		sb.append("<span style=\"text-align:left;\">");
		sb.append(clinicName).append("<br/>");
		if (address != null && address.length() > 0) sb.append(address).append("<br/>");
		sb.append(city).append(", ").append(state).append("");
		sb.append("</span>");
		return sb.toString();
	}
	
	/**
	 * Returns a Map Location object for placing this location on a Map
	 * @return
	 */
	public MapLocationVO getMapLocation() {
		MapLocationVO mLoc = new MapLocationVO();
		mLoc.setAddress(getAddress());
		mLoc.setCity(getCity());
		mLoc.setState(getState());
		mLoc.setZipCode(getZipCode());
		mLoc.setLatitude(getLatitude());
		mLoc.setLongitude(getLongitude());
		//mLoc.setLocationDesc(getMapMarker());
		mLoc.setLocationDesc(getClinicName());
		
		return mLoc;
	}

	/**
	 * @return the address2
	 */
	public String getAddress2() {
		return address2;
	}

	/**
	 * @return the address3
	 */
	public String getAddress3() {
		return address3;
	}

	/**
	 * @return the clinicName
	 */
	public String getClinicName() {
		return clinicName;
	}

	/**
	 * @param address2 the address2 to set
	 */
	public void setAddress2(String address2) {
		this.address2 = address2;
	}

	/**
	 * @param address3 the address3 to set
	 */
	public void setAddress3(String address3) {
		this.address3 = address3;
	}

	/**
	 * @param clinicName the clinicName to set
	 */
	public void setClinicName(String clinicName) {
		this.clinicName = clinicName;
	}

	/**
	 * @return the phones
	 */
	public List<PhoneVO> getPhones() {
		return phones;
	}
	
	/**
	 * Returns the fax phone number
	 * @return
	 */
	public String getFaxNumber() {
		return this.getPhoneNumber(PhoneVO.FAX_PHONE);
	}
	
	/**
	 * Returns the work phone number
	 * @return
	 */
	public String getWorkNumber() {
		if (this.getPhoneNumber(PhoneVO.WORK_PHONE) != null) {
			return this.getPhoneNumber(PhoneVO.WORK_PHONE);
		} else {
			return this.getPhoneNumber("WORK_PHONE");
		}
	}
	
	/**
	 * Returns the phone number for the provided type
	 * @param type
	 * @return
	 */
	public String getPhoneNumber(String type) {
		if (phones == null || phones.size() == 0 || StringUtil.checkVal(type).length() == 0) 
			return null;
		
		String phone = null;
		for (int i = 0; i < phones.size(); i++) {
			PhoneVO vo = phones.get(i);
			if (type.equalsIgnoreCase(vo.getPhoneType())) {
				phone = vo.getPhoneNumber();
			}
		}
		
		return phone;
	}

	/**
	 * @param phones the phones to set
	 */
	public void setPhones(List<PhoneVO> phones) {
		this.phones = phones;
	}
	
	
	public void addPhone(PhoneVO vo) {
		phones.add(vo);
	}
	
	/**
	 * Adds a phone number for the clinic from the DB
	 * @param rs
	 */
	public void addPhone(ResultSet rs) {
		DBUtil db = new DBUtil();
		String pn = db.getStringVal("phone_number_txt", rs);
		String type = db.getStringVal("phone_type_id", rs);
		String cntry = db.getStringVal("phone_country_cd", rs);
		PhoneVO vo = new PhoneVO(type, pn, cntry);
		vo.setTypeName(db.getStringVal("type_nm", rs));
		if (vo.isValidPhone()) phones.add(vo);
	}

	public String getClinicId() {
		return clinicId;
	}

	public void setClinicId(String clinicId) {
		this.clinicId = clinicId;
	}
	
	/**
	 * Finds the wqork phone number
	 * @return Work phone number.  Null if not found
	 */
	public String getWorkPhone() {
		String ph = null;
		for (int i=0; i < phones.size(); i++) {
			PhoneVO pvo = phones.get(i);
			if ("WORK_PHONE".equalsIgnoreCase(pvo.getPhoneType()))
				ph = pvo.getPhoneNumber();
		}
		
		return ph;
	}
	
	/**
	 * @return the locatorDisplay
	 */
	public Integer getLocatorDisplay() {
		return locatorDisplay;
	}

	/**
	 * @param locatorDisplay the locatorDisplay to set
	 */
	public void setLocatorDisplay(Integer locatorDisplay) {
		this.locatorDisplay = locatorDisplay;
	}

	/**
	 * @return the locationTypeId
	 */
	public Integer getLocationTypeId() {
		return locationTypeId;
	}

	/**
	 * @param locationTypeId the locationTypeId to set
	 */
	public void setLocationTypeId(Integer locationTypeId) {
		this.locationTypeId = locationTypeId;
	}

	public String getLocationTypeName() {
		return locationTypeName;
	}

	public void setLocationTypeName(String locationTypeName) {
		this.locationTypeName = locationTypeName;
	}

	/**
	 * @return the manualGeocode
	 */
	public Integer getManualGeocodeFlag() {
		return manualGeocodeFlag;
	}

	/**
	 * @param manualGeocode the manualGeocode to set
	 */
	public void setManualGeocodeFlag(Integer manualGeocodeFlag) {
		this.manualGeocodeFlag = manualGeocodeFlag;
	}
}
