package com.perfectstorm.data.weather.forecast.element;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title:</b> PrecipitationVO.java
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

public class PrecipitationVO extends BeanDataVO {

	private static final long serialVersionUID = -2347374507354154012L;
	
	// Keys for the data map
	public static final String RELATIVE_HUMIDITY = "relativeHumidity";
	public static final String PRECIP_PROBABILITY = "probability";
	public static final String PRECIP_QUANTITY = "quantity";
	public static final String ICE_ACCUMULATION = "iceAccumulation";
	public static final String SNOW_FALL = "snowFall";
	public static final String SNOW_LEVEL = "snowLevel";
	
	// Members
	private int relativeHumidity; // percent
	private int probability; // percent
	private double quantity; // depth (in, mm)
	private double iceAccumulation; // depth (in, mm)
	private double snowFall; // depth (in, mm)
	private double snowLevel; // depth (ft, m)
	
	
	public PrecipitationVO() {
	}

	/**
	 * @param req
	 */
	public PrecipitationVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public PrecipitationVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * Gets a map of all values
	 * @return
	 */
	public Map<String, Integer> getDataMap() {
		Map<String, Integer> dataMap = new HashMap<>();
		dataMap.put(RELATIVE_HUMIDITY, relativeHumidity);
		dataMap.put(PRECIP_PROBABILITY, probability);
		dataMap.put(PRECIP_QUANTITY, (int) Math.round(quantity));
		dataMap.put(ICE_ACCUMULATION, (int) Math.round(iceAccumulation));
		dataMap.put(SNOW_FALL, (int) Math.round(snowFall));
		dataMap.put(SNOW_LEVEL, (int) Math.round(snowLevel));
		
		return dataMap;
	}

	/**
	 * @return the relativeHumidity
	 */
	public int getRelativeHumidity() {
		return relativeHumidity;
	}

	/**
	 * @param relativeHumidity the relativeHumidity to set
	 */
	public void setRelativeHumidity(int relativeHumidity) {
		this.relativeHumidity = relativeHumidity;
	}

	/**
	 * @return the probability
	 */
	public int getProbability() {
		return probability;
	}

	/**
	 * @param probability the probability to set
	 */
	public void setProbability(int probability) {
		this.probability = probability;
	}

	/**
	 * @return the quantity
	 */
	public double getQuantity() {
		return quantity;
	}

	/**
	 * @param quantity the quantity to set
	 */
	public void setQuantity(double quantity) {
		this.quantity = quantity;
	}

	/**
	 * @return the iceAccumulation
	 */
	public double getIceAccumulation() {
		return iceAccumulation;
	}

	/**
	 * @param iceAccumulation the iceAccumulation to set
	 */
	public void setIceAccumulation(double iceAccumulation) {
		this.iceAccumulation = iceAccumulation;
	}

	/**
	 * @return the snowFall
	 */
	public double getSnowFall() {
		return snowFall;
	}

	/**
	 * @param snowFall the snowFall to set
	 */
	public void setSnowFall(double snowFall) {
		this.snowFall = snowFall;
	}

	/**
	 * @return the snowLevel
	 */
	public double getSnowLevel() {
		return snowLevel;
	}

	/**
	 * @param snowLevel the snowLevel to set
	 */
	public void setSnowLevel(double snowLevel) {
		this.snowLevel = snowLevel;
	}

}
