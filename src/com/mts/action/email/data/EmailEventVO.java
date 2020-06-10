package com.mts.action.email.data;

import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: EmailEventVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> CHANGE ME!!
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 18, 2020
 * @updates:
 ****************************************************************************/
public class EmailEventVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2304712710301061634L;
	
	// Members
	private String id;
	private String fullUrl;
	private String title;
	private String assetUrl;
	private String body;
	private SeoDataVO seoData = new SeoDataVO();
	private LocationVO location;
	private long startDate;
	private long endDate;
	private Date start;
	private Date end;
	
	/**
	 * 
	 */
	public EmailEventVO() {
		super();
	}

	/**
	 * @param req
	 */
	public EmailEventVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public EmailEventVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * Determines if there are multiple days in the event
	 * @return
	 */
	public boolean isMultipleDays() {
		int numDays = getEndDay() - getStartDay();
		return (numDays != 0);
	}
	
	/**
	 * 
	 * @return
	 */
	public String getStartMonth() {
		if (start == null) start = new Date(startDate);
		SimpleDateFormat ft = new SimpleDateFormat ("MMM");
		return ft.format(start);
	}
	
	/**
	 * Calculates the start day of the event
	 * @return
	 */
	public int getStartDay() {
		if (start == null) start = new Date(startDate);
		Calendar cal = Calendar.getInstance();
		cal.setTime(start);
		
		return cal.get(Calendar.DAY_OF_MONTH);
	}
	
	/**
	 * Calculates the start day of the event
	 * @return
	 */
	public int getEndDay() {
		if (end == null) end = new Date(endDate);
		Calendar cal = Calendar.getInstance();
		cal.setTime(end);
		
		return cal.get(Calendar.DAY_OF_MONTH);
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the fullUrl
	 */
	public String getFullUrl() {
		return fullUrl;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @return the assetUrl
	 */
	public String getAssetUrl() {
		return assetUrl;
	}

	/**
	 * @return the seoData
	 */
	public SeoDataVO getSeoData() {
		return seoData;
	}

	/**
	 * @return the location
	 */
	public LocationVO getLocation() {
		return location;
	}

	/**
	 * @return the startDate
	 */
	public long getStartDate() {
		return startDate;
	}

	/**
	 * @return the endDate
	 */
	public long getEndDate() {
		return endDate;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @param fullUrl the fullUrl to set
	 */
	public void setFullUrl(String fullUrl) {
		this.fullUrl = fullUrl;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @param assetUrl the assetUrl to set
	 */
	public void setAssetUrl(String assetUrl) {
		this.assetUrl = assetUrl;
	}

	/**
	 * @param location the location to set
	 */
	public void setLocation(LocationVO location) {
		this.location = location;
	}

	/**
	 * @param startDate the startDate to set
	 */
	public void setStartDate(long startDate) {
		this.startDate = startDate;
	}

	/**
	 * @param endDate the endDate to set
	 */
	public void setEndDate(long endDate) {
		this.endDate = endDate;
	}

	/**
	 * @return the body
	 */
	public String getBody() {
		return body;
	}

	/**
	 * @param body the body to set
	 */
	public void setBody(String body) {
		this.body = body;
	}

	/**
	 * @param seoData the seoData to set
	 */
	public void setSeoData(SeoDataVO seoData) {
		this.seoData = seoData;
	}

}
