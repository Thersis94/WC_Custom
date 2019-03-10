package com.perfectstorm.data.weather.forecast;

import java.sql.ResultSet;
import java.util.Date;
import java.util.Map;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title:</b> FullForecastVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> 
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Mar 08 2019
 * @updates:
 ****************************************************************************/

public class FullForecastVO extends BeanDataVO {
	
	private static final long serialVersionUID = 3367679093320618356L;
	
	private Map<String, ForecastVO> fullForecast;
	private Date forecastDate;

	public FullForecastVO() {
		super();
	}
	
	/**
	 * @param req
	 */
	public FullForecastVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public FullForecastVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the fullForecast
	 */
	public Map<String, ForecastVO> getFullForecast() {
		return fullForecast;
	}

	/**
	 * @param fullForecast the fullForecast to set
	 */
	public void setFullForecast(Map<String, ForecastVO> fullForecast) {
		this.fullForecast = fullForecast;
	}

	/**
	 * @return the forecastDate
	 */
	public Date getForecastDate() {
		return forecastDate;
	}

	/**
	 * @param forecastDate the forecastDate to set
	 */
	public void setForecastDate(Date forecastDate) {
		this.forecastDate = forecastDate;
	}

	/**
	 * Helper to get the number of hours since the forecast was last updated
	 * 
	 * @return
	 */
	public long getHoursSinceForecast() {
		return (new Date().getTime() - getForecastDate().getTime()) / 1000 / 3600;
	}
}
