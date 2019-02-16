package com.perfectstorm.data.weather.nws;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;
import java.util.TimeZone;

// SMT Base lIbs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: TimeValueVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Stores the tme and value for a weather attribute
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 12, 2019
 * @updates:
 ****************************************************************************/

public class TimeValueVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7453509868896219407L;
	
	// Members
	private String validTime;
	private double value;
	
	// Non-Serialized Values
	private int duration;
	private Date utcDate;
	
	/**
	 * 
	 */
	public TimeValueVO() {
		super();
	}

	/**
	 * @param req
	 */
	public TimeValueVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public TimeValueVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * 
	 */
	public void updateData() {
		if (StringUtil.isEmpty(validTime) || validTime.length()< 20) return;
		
		// Set the UTC as a date and the duration of the Time Value
		utcDate = Convert.formatDate("yyyy-MM-dd'T'HH:mm:ss", validTime.substring(0, 19));
		String[] item = validTime.substring(validTime.indexOf('/') + 1).split("T");
		int p = Convert.formatInteger(item[0].replaceAll("P", "")) * 24;
		int t = item.length > 1 ? Convert.formatInteger(item[1].replaceAll("H", "")) : 0;
		duration = p + t;
	}

	/**
	 * @return the validTime
	 */
	public String getValidTime() {
		return validTime;
	}

	/**
	 * @return the value
	 */
	public double getValue() {
		return value;
	}

	/**
	 * @return the duration
	 */
	public int getDuration() {
		if (! StringUtil.isEmpty(validTime) && utcDate == null) updateData();
		return duration;
	}

	/**
	 * @return the utcDate
	 */
	public Date getUtcDate() {
		if (! StringUtil.isEmpty(validTime) && utcDate == null) updateData();
		return utcDate;
	}

	/**
	 * Converts the utcDate to the local date/time based upon the passed time zone
	 * @param timeZone
	 * @return
	 */
	public Date getDateWithOffset(String timeZone) {
		if (utcDate == null) return null;
		TimeZone tz = TimeZone.getTimeZone(timeZone);
		long offset= tz.getOffset(new Date().getTime()) / 3600000;
		return  new Date(utcDate.getTime() + offset);
	}
	
	/**
	 * @param validTime the validTime to set
	 */
	public void setValidTime(String validTime) {
		this.validTime = validTime;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(double value) {
		this.value = value;
	}
}

