package com.perfectstorm.data.weather.forecast.element;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title:</b> WindVO.java
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

public class WindVO extends BeanDataVO {

	private static final long serialVersionUID = -156342374154881806L;
	
	// Keys for the data map
	private static final String WIND_DIRECTION = "direction";
	private static final String WIND_SPEED = "speed";
	private static final String SPEED_MAX = "speedMax";
	private static final String GUST_SPEED = "gustSpeed";
	private static final String TRANSPORT_DIRECTION = "transportDirection";
	private static final String TRANSPORT_SPEED = "transportSpeed";
	private static final String TWENTY_FOOT_DIRECTION = "twentyFootDirection";
	private static final String TWENTY_FOOT_SPEED = "twentyFootSpeed";
	private static final String TROPICAL_STORM_WIND_PROBABILITY = "tropicalStormWindProbability";
	private static final String HURRICANE_STORM_WIND_PROBABILITY = "hurricaneStormWindProbability";
	
	// Members
	private int direction; // angle (degrees)
	private double speed; // knots, mph, kmph, m/s
	private double speedMax; // knots, mph, kmph, m/s
	private double gustSpeed; // knots, mph, kmph, m/s
	private int transportDirection; // angle (degrees)
	private double transportSpeed; // knots, mph, kmph, m/s
	private int twentyFootDirection; // angle (degrees)
	private double twentyFootSpeed; // knots, mph, kmph, m/s
	private int tropicalStormWindProbability; // percent
	private int hurricaneStormWindProbability; // percent
	private Map<Integer, Boolean> windPotential; // potential by speed (mph)
	private Map<Integer, Boolean> windGustPotential; // potential by speed (mph)
	
	
	public WindVO() {
	}

	/**
	 * @param req
	 */
	public WindVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public WindVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * Gets a map of all values
	 * @return
	 */
	public Map<String, Integer> getDataMap() {
		Map<String, Integer> dataMap = new HashMap<>();
		dataMap.put(WIND_DIRECTION, direction);
		dataMap.put(WIND_SPEED, (int) Math.round(speed));
		dataMap.put(SPEED_MAX, (int) Math.round(speedMax));
		dataMap.put(GUST_SPEED, (int) Math.round(gustSpeed));
		dataMap.put(TRANSPORT_DIRECTION, transportDirection);
		dataMap.put(TRANSPORT_SPEED, (int) Math.round(transportSpeed));
		dataMap.put(TWENTY_FOOT_DIRECTION, twentyFootDirection);
		dataMap.put(TWENTY_FOOT_SPEED, (int) Math.round(twentyFootSpeed));
		dataMap.put(TROPICAL_STORM_WIND_PROBABILITY, tropicalStormWindProbability);
		dataMap.put(HURRICANE_STORM_WIND_PROBABILITY, hurricaneStormWindProbability);
		
		return dataMap;
	}

	/**
	 * @return the direction
	 */
	public int getDirection() {
		return direction;
	}

	/**
	 * @param direction the direction to set
	 */
	public void setDirection(int direction) {
		this.direction = direction;
	}

	/**
	 * @return the speed
	 */
	public double getSpeed() {
		return speed;
	}

	/**
	 * @param speed the speed to set
	 */
	public void setSpeed(double speed) {
		this.speed = speed;
	}

	/**
	 * @return the gustSpeed
	 */
	public double getGustSpeed() {
		return gustSpeed;
	}

	/**
	 * @param gustSpeed the gustSpeed to set
	 */
	public void setGustSpeed(double gustSpeed) {
		this.gustSpeed = gustSpeed;
	}

	/**
	 * @return the transportDirection
	 */
	public int getTransportDirection() {
		return transportDirection;
	}

	/**
	 * @param transportDirection the transportDirection to set
	 */
	public void setTransportDirection(int transportDirection) {
		this.transportDirection = transportDirection;
	}

	/**
	 * @return the transportSpeed
	 */
	public double getTransportSpeed() {
		return transportSpeed;
	}

	/**
	 * @param transportSpeed the transportSpeed to set
	 */
	public void setTransportSpeed(double transportSpeed) {
		this.transportSpeed = transportSpeed;
	}

	/**
	 * @return the twentyFootDirection
	 */
	public int getTwentyFootDirection() {
		return twentyFootDirection;
	}

	/**
	 * @param twentyFootDirection the twentyFootDirection to set
	 */
	public void setTwentyFootDirection(int twentyFootDirection) {
		this.twentyFootDirection = twentyFootDirection;
	}

	/**
	 * @return the twentyFootSpeed
	 */
	public double getTwentyFootSpeed() {
		return twentyFootSpeed;
	}

	/**
	 * @param twentyFootSpeed the twentyFootSpeed to set
	 */
	public void setTwentyFootSpeed(double twentyFootSpeed) {
		this.twentyFootSpeed = twentyFootSpeed;
	}

	/**
	 * @return the tropicalStormWindProbability
	 */
	public int getTropicalStormWindProbability() {
		return tropicalStormWindProbability;
	}

	/**
	 * @param tropicalStormWindProbability the tropicalStormWindProbability to set
	 */
	public void setTropicalStormWindProbability(int tropicalStormWindProbability) {
		this.tropicalStormWindProbability = tropicalStormWindProbability;
	}

	/**
	 * @return the hurricaneStormWindProbability
	 */
	public int getHurricaneStormWindProbability() {
		return hurricaneStormWindProbability;
	}

	/**
	 * @param hurricaneStormWindProbability the hurricaneStormWindProbability to set
	 */
	public void setHurricaneStormWindProbability(int hurricaneStormWindProbability) {
		this.hurricaneStormWindProbability = hurricaneStormWindProbability;
	}

	/**
	 * @return the windPotential
	 */
	public Map<Integer, Boolean> getWindPotential() {
		return windPotential;
	}

	/**
	 * @param windPotential the windPotential to set
	 */
	public void setWindPotential(Map<Integer, Boolean> windPotential) {
		this.windPotential = windPotential;
	}

	/**
	 * @return the windGustPotential
	 */
	public Map<Integer, Boolean> getWindGustPotential() {
		return windGustPotential;
	}

	/**
	 * @param windGustPotential the windGustPotential to set
	 */
	public void setWindGustPotential(Map<Integer, Boolean> windGustPotential) {
		this.windGustPotential = windGustPotential;
	}

	/**
	 * @return the speedMax
	 */
	public double getSpeedMax() {
		return speedMax;
	}

	/**
	 * @param speedMax the speedMax to set
	 */
	public void setSpeedMax(double speedMax) {
		this.speedMax = speedMax;
	}

}
