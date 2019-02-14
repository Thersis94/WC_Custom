package com.perfectstorm.data.weather.forecast;

import java.sql.ResultSet;
import java.util.Date;

import com.perfectstorm.data.weather.forecast.element.ConditionVO;
import com.perfectstorm.data.weather.forecast.element.HazardVO;
import com.perfectstorm.data.weather.forecast.element.PrecipitationVO;
import com.perfectstorm.data.weather.forecast.element.TemperatureVO;
import com.perfectstorm.data.weather.forecast.element.WaveVO;
import com.perfectstorm.data.weather.forecast.element.WindVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title:</b> ForecastVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> 
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Feb 13 2019
 * @updates:
 ****************************************************************************/

public class ForecastVO extends BeanDataVO {

	private static final long serialVersionUID = 7462880490325606412L;
	
	// Base forecast data
	private Date startDate;
	private Date endDate;
	
	// Forecast elements
	private TemperatureVO temperature;
	private WindVO wind;
	private PrecipitationVO precipitation;
	private ConditionVO condition;
	private HazardVO hazard;
	private WaveVO wave;
	

	public ForecastVO() {
	}

	/**
	 * @param req
	 */
	public ForecastVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public ForecastVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the startDate
	 */
	public Date getStartDate() {
		return startDate;
	}

	/**
	 * @param startDate the startDate to set
	 */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/**
	 * @return the endDate
	 */
	public Date getEndDate() {
		return endDate;
	}

	/**
	 * @param endDate the endDate to set
	 */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	/**
	 * @return the temperature
	 */
	public TemperatureVO getTemperature() {
		return temperature;
	}

	/**
	 * @param temperature the temperature to set
	 */
	public void setTemperature(TemperatureVO temperature) {
		this.temperature = temperature;
	}

	/**
	 * @return the wind
	 */
	public WindVO getWind() {
		return wind;
	}

	/**
	 * @param wind the wind to set
	 */
	public void setWind(WindVO wind) {
		this.wind = wind;
	}

	/**
	 * @return the precipitation
	 */
	public PrecipitationVO getPrecipitation() {
		return precipitation;
	}

	/**
	 * @param precipitation the precipitation to set
	 */
	public void setPrecipitation(PrecipitationVO precipitation) {
		this.precipitation = precipitation;
	}

	/**
	 * @return the condition
	 */
	public ConditionVO getCondition() {
		return condition;
	}

	/**
	 * @param condition the condition to set
	 */
	public void setCondition(ConditionVO condition) {
		this.condition = condition;
	}

	/**
	 * @return the hazard
	 */
	public HazardVO getHazard() {
		return hazard;
	}

	/**
	 * @param hazard the hazard to set
	 */
	public void setHazard(HazardVO hazard) {
		this.hazard = hazard;
	}

	/**
	 * @return the wave
	 */
	public WaveVO getWave() {
		return wave;
	}

	/**
	 * @param wave the wave to set
	 */
	public void setWave(WaveVO wave) {
		this.wave = wave;
	}

}
