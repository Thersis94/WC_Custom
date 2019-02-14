package com.perfectstorm.data.weather.forecast.element;

import java.sql.ResultSet;
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
	
	private int direction; // angle (degrees)
	private double speed; // knots, mph, kmph, m/s
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

}
