package com.perfectstorm.data.weather;

import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.util.Convert;


/****************************************************************************
 * <b>Title</b>: SunTimeVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> This VO will hold two date objects representing approximate 
 * sun rise and sun set
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Feb 15, 2019
 * @updates:
 ****************************************************************************/
public class SunTimeVO extends BeanDataVO{ 
	
	private static final long serialVersionUID = -53123328524258786L;
	private Date sunsetDate;
	private Date sunriseDate;
	private double longitudeNumber;
	private double latitudeNumber;
	private double negitiveNum = Convert.formatDouble("-100.0");
	private String timeZoneName;
	private Date SourceDate;

	/**
	 * 
	 */
	public SunTimeVO() {
		super();
	}

	/**
	 * 
	 * @param sunSetDate
	 * @param sunRiseDate
	 */
	public SunTimeVO(Date sunSetDate, Date sunRiseDate ) {
		super();
		setSunriseDate(sunRiseDate);
		setSunsetDate(sunSetDate);
	}
	/**
	 * 
	 * @param req
	 */
	public SunTimeVO(ActionRequest req) {
		super(req);
	}
	
	/**
	 * @return the sunSiteDate
	 */
	public Date getSunSetDate() {
		return sunsetDate;
	}

	/**
	 * @param sunSiteDate the sunSiteDate to set
	 */
	public void setSunsetDate(Date sunSetDate) {
		this.sunsetDate = sunSetDate;
	}

	/**
	 * @return the sunRiseDate
	 */
	public Date getSunriseDate() {
		return sunriseDate;
	}

	/**
	 * @param sunRiseDate the sunRiseDate to set
	 */
	public void setSunriseDate(Date sunriseDate) {
		this.sunriseDate = sunriseDate;
	}

	/**
	 * @return the longitudeNumber
	 */
	public double getLongitudeNumber() {
		return longitudeNumber;
	}

	/**
	 * @param longitudeNumber the longitudeNumber to set
	 */
	public void setLongitudeNumber(double longitudeNumber) {
		this.longitudeNumber = longitudeNumber;
	}

	/**
	 * @return the latitudeNumber
	 */
	public double getLatitudeNumber() {
		return latitudeNumber;
	}

	/**
	 * @param latitudeNumber the latitudeNumber to set
	 */
	public void setLatitudeNumber(double latitudeNumber) {
		this.latitudeNumber = latitudeNumber;
	}

	/**
	 * @return the timeZoneName
	 */
	public String getTimeZoneName() {
		return timeZoneName;
	}

	/**
	 * @param timeZoneName the timeZoneName to set
	 */
	public void setTimeZoneName(String timeZoneName) {
		this.timeZoneName = timeZoneName;
	}

	/**
	 * @return the sourceDate
	 */
	public Date getSourceDate() {
		return SourceDate;
	}

	/**
	 * @param sourceDate the sourceDate to set
	 */
	public void setSourceDate(Date sourceDate) {
		SourceDate = sourceDate;
	}

}
