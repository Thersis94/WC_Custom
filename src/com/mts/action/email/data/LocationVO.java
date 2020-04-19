package com.mts.action.email.data;

// JDK 1.8.x
import java.sql.ResultSet;

// Apache Commons 3
import org.apache.commons.lang3.StringUtils;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: LocationVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Location data for the MTS Event
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 18, 2020
 * @updates:
 ****************************************************************************/
public class LocationVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3264377183314619423L;
	
	// Members
	private int mapZoom;
	private double mapLat;
	private double mapLng;
	private String addressTitle;
	private String addressLine1;
	private String addressLine2;
	private String addressCountry;
	
	/**
	 * 
	 */
	public LocationVO() {
		super();
	}

	/**
	 * @param req
	 */
	public LocationVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public LocationVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the mapZoom
	 */
	public int getMapZoom() {
		return mapZoom;
	}

	/**
	 * @return the mapLat
	 */
	public double getMapLat() {
		return mapLat;
	}

	/**
	 * @return the mapLng
	 */
	public double getMapLng() {
		return mapLng;
	}

	/**
	 * @return the addressTitle
	 */
	public String getAddressTitle() {
		return addressTitle;
	}

	/**
	 * @return the addressLine1
	 */
	public String getAddressLine1() {
		return addressLine1;
	}

	/**
	 * @return the addressLine2
	 */
	public String getAddressLine2() {
		int count = StringUtils.countMatches(addressLine2, ",");
		if (count > 1) {
			int pos = StringUtils.ordinalIndexOf(addressLine2, ",", 2);
			return addressLine2.substring(0, pos);
		}
		
		return addressLine2;
	}

	/**
	 * @return the addressCountry
	 */
	public String getAddressCountry() {
		return addressCountry;
	}

	/**
	 * @param mapZoom the mapZoom to set
	 */
	public void setMapZoom(int mapZoom) {
		this.mapZoom = mapZoom;
	}

	/**
	 * @param mapLat the mapLat to set
	 */
	public void setMapLat(double mapLat) {
		this.mapLat = mapLat;
	}

	/**
	 * @param mapLng the mapLng to set
	 */
	public void setMapLng(double mapLng) {
		this.mapLng = mapLng;
	}

	/**
	 * @param addressTitle the addressTitle to set
	 */
	public void setAddressTitle(String addressTitle) {
		this.addressTitle = addressTitle;
	}

	/**
	 * @param addressLine1 the addressLine1 to set
	 */
	public void setAddressLine1(String addressLine1) {
		this.addressLine1 = addressLine1;
	}

	/**
	 * @param addressLine2 the addressLine2 to set
	 */
	public void setAddressLine2(String addressLine2) {
		this.addressLine2 = addressLine2;
	}

	/**
	 * @param addressCountry the addressCountry to set
	 */
	public void setAddressCountry(String addressCountry) {
		this.addressCountry = addressCountry;
	}

}
