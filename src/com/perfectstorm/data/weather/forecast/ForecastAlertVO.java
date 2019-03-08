package com.perfectstorm.data.weather.forecast;

import java.sql.ResultSet;
import java.util.Date;

import com.perfectstorm.data.AttributeVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title:</b> ForecastAlertVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Weather Forecast Alerts
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Mar 06 2019
 * @updates:
 ****************************************************************************/

@Table(name="ps_forecast_alert")
public class ForecastAlertVO extends AttributeVO {

	private static final long serialVersionUID = 5589070578147587775L;
	
	private String forecastAlertId;
	private String venueTourForecastId;
	private int value;
	private int newFlag;
	private Date venueDate;
	
	public ForecastAlertVO() {
		super();
	}

	/**
	 * @param req
	 */
	public ForecastAlertVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public ForecastAlertVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the forecastAlertId
	 */
	@Column(name="forecast_alert_id", isPrimaryKey=true)
	public String getForecastAlertId() {
		return forecastAlertId;
	}

	/**
	 * @param forecastAlertId the forecastAlertId to set
	 */
	public void setForecastAlertId(String forecastAlertId) {
		this.forecastAlertId = forecastAlertId;
	}

	/**
	 * @return the venueTourForecastId
	 */
	@Column(name="venue_tour_forecast_id")
	public String getVenueTourForecastId() {
		return venueTourForecastId;
	}

	/**
	 * @param venueTourForecastId the venueTourForecastId to set
	 */
	public void setVenueTourForecastId(String venueTourForecastId) {
		this.venueTourForecastId = venueTourForecastId;
	}

	/**
	 * @return the value
	 */
	@Column(name="value_no")
	public int getValue() {
		return value;
	}

	/**
	 * @param value the value to set
	 */
	public void setValue(int value) {
		this.value = value;
	}

	/**
	 * @return the newFlag
	 */
	@Column(name="new_flg")
	public int getNewFlag() {
		return newFlag;
	}

	/**
	 * @param newFlag the newFlag to set
	 */
	public void setNewFlag(int newFlag) {
		this.newFlag = newFlag;
	}

	/**
	 * @return the venueDate
	 */
	@Column(name="venue_dt")
	public Date getVenueDate() {
		return venueDate;
	}

	/**
	 * @param venueDate the venueDate to set
	 */
	public void setVenueDate(Date venueDate) {
		this.venueDate = venueDate;
	}

}
