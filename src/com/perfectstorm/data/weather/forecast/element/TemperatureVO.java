package com.perfectstorm.data.weather.forecast.element;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title:</b> TemperatureVO.java
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

public class TemperatureVO extends BeanDataVO {

	private static final long serialVersionUID = 2399000651829957648L;
	
	// Keys for the data map
	public static final String TEMP = "temperature";
	public static final String DEW_POINT = "dewPoint";
	public static final String MAX_TEMP = "maxTemperature";
	public static final String MIN_TEMP = "minTemperature";
	public static final String APPARENT_TEMP = "apparentTemperature";
	public static final String HEAT_INDEX = "heatIndex";
	public static final String WIND_CHILL = "windChill";
	
	// Members
	private double temperature; // degrees (F, C)
	private double dewPoint; // degrees (F, C)
	private double maxTemperature; // degrees (F, C)
	private double minTemperature; // degrees (F, C)
	private double apparentTemperature; // degrees (F, C)
	private double heatIndex; // degrees (F, C)
	private double windChill; // degrees (F, C)
	private String trend;
	
	
	public TemperatureVO() {
	}

	/**
	 * @param req
	 */
	public TemperatureVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public TemperatureVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * Gets a map of all values
	 * @return
	 */
	public Map<String, Integer> getDataMap() {
		Map<String, Integer> dataMap = new HashMap<>();
		dataMap.put(TEMP, (int) Math.round(temperature));
		dataMap.put(DEW_POINT, (int) Math.round(dewPoint));
		dataMap.put(MAX_TEMP, (int) Math.round(maxTemperature));
		dataMap.put(MIN_TEMP, (int) Math.round(minTemperature));
		dataMap.put(APPARENT_TEMP, (int) Math.round(apparentTemperature));
		dataMap.put(HEAT_INDEX, (int) Math.round(heatIndex));
		dataMap.put(WIND_CHILL, (int) Math.round(windChill));
		
		return dataMap;
	}

	/**
	 * @return the temperature
	 */
	public double getTemperature() {
		return temperature;
	}

	/**
	 * @param temperature the temperature to set
	 */
	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}

	/**
	 * @return the dewPoint
	 */
	public double getDewPoint() {
		return dewPoint;
	}

	/**
	 * @param dewPoint the dewPoint to set
	 */
	public void setDewPoint(double dewPoint) {
		this.dewPoint = dewPoint;
	}

	/**
	 * @return the maxTemperature
	 */
	public double getMaxTemperature() {
		return maxTemperature;
	}

	/**
	 * @param maxTemperature the maxTemperature to set
	 */
	public void setMaxTemperature(double maxTemperature) {
		this.maxTemperature = maxTemperature;
	}

	/**
	 * @return the minTemperature
	 */
	public double getMinTemperature() {
		return minTemperature;
	}

	/**
	 * @param minTemperature the minTemperature to set
	 */
	public void setMinTemperature(double minTemperature) {
		this.minTemperature = minTemperature;
	}

	/**
	 * @return the apparentTemperature
	 */
	public double getApparentTemperature() {
		return apparentTemperature;
	}

	/**
	 * @param apparentTemperature the apparentTemperature to set
	 */
	public void setApparentTemperature(double apparentTemperature) {
		this.apparentTemperature = apparentTemperature;
	}

	/**
	 * @return the heatIndex
	 */
	public double getHeatIndex() {
		return heatIndex;
	}

	/**
	 * @param heatIndex the heatIndex to set
	 */
	public void setHeatIndex(double heatIndex) {
		this.heatIndex = heatIndex;
	}

	/**
	 * @return the windChill
	 */
	public double getWindChill() {
		return windChill;
	}

	/**
	 * @param windChill the windChill to set
	 */
	public void setWindChill(double windChill) {
		this.windChill = windChill;
	}

	/**
	 * @return the trend
	 */
	public String getTrend() {
		return trend;
	}

	/**
	 * @param trend the trend to set
	 */
	public void setTrend(String trend) {
		this.trend = trend;
	}

}
