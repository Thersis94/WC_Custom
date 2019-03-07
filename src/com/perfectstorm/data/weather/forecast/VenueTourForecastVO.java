package com.perfectstorm.data.weather.forecast;

import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title:</b> VenueTourForecastVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> 
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Mar 6 2019
 * @updates:
 ****************************************************************************/

@Table(name="ps_venue_tour_forecast")
public class VenueTourForecastVO extends BeanDataVO {

	private static final long serialVersionUID = -9103580992936124002L;

	// Members
	private String venueTourForecastId;
	private String venueTourId;
	private String forecastText;
	private Date createDate;

	public VenueTourForecastVO() {
		super();
	}

	/**
	 * @param req
	 */
	public VenueTourForecastVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public VenueTourForecastVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * @return the venueTourForecastId
	 */
	@Column(name="venue_tour_forecast_id", isPrimaryKey=true)
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
	 * @return the venueTourId
	 */
	@Column(name="venue_tour_id")
	public String getVenueTourId() {
		return venueTourId;
	}

	/**
	 * @param venueTourId the venueTourId to set
	 */
	public void setVenueTourId(String venueTourId) {
		this.venueTourId = venueTourId;
	}

	/**
	 * @return the forecastText
	 */
	@Column(name="forecast_txt")
	public String getForecastText() {
		return forecastText;
	}

	/**
	 * @param forecastText the forecastText to set
	 */
	public void setForecastText(String forecastText) {
		this.forecastText = forecastText;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

}
