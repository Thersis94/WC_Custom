package com.perfectstorm.data.weather.forecast.element;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title:</b> ConditionVO.java
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

public class ConditionVO extends BeanDataVO {

	private static final long serialVersionUID = 7294631296483099130L;
	
	// Keys for the data map
	public static final String SKY_COVER = "skyCover";
	public static final String CEILING_HEIGHT = "ceilingHeight";
	public static final String VISIBILITY = "visibility";
	public static final String LIGHTNING_ACTIVITY_LEVEL = "lightningActivityLevel";
	public static final String PRESSURE = "pressure";
	public static final String THUNDER_PROBABILITY = "thunderProbability";
	
	// Members
	private int skyCover; // percent
	private double ceilingHeight; // distance (ft, m)
	private double visibility; // distance (mi, m)
	private int lightningActivityLevel; // no unit of measure
	private double pressure; // mbar, mmHg, psi 
	private int thunderProbability; // percent
	
	
	public ConditionVO() {
	}

	/**
	 * @param req
	 */
	public ConditionVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public ConditionVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * Gets a map of all values
	 * @return
	 */
	public Map<String, Integer> getDataMap() {
		Map<String, Integer> dataMap = new HashMap<>();
		dataMap.put(SKY_COVER, skyCover);
		dataMap.put(CEILING_HEIGHT, (int) Math.round(ceilingHeight));
		dataMap.put(VISIBILITY, (int) Math.round(visibility));
		dataMap.put(LIGHTNING_ACTIVITY_LEVEL, lightningActivityLevel);
		dataMap.put(PRESSURE, (int) Math.round(pressure));
		dataMap.put(THUNDER_PROBABILITY, thunderProbability);
		
		return dataMap;
	}

	/**
	 * @return the skyCover
	 */
	public int getSkyCover() {
		return skyCover;
	}

	/**
	 * @param skyCover the skyCover to set
	 */
	public void setSkyCover(int skyCover) {
		this.skyCover = skyCover;
	}

	/**
	 * @return the ceilingHeight
	 */
	public double getCeilingHeight() {
		return ceilingHeight;
	}

	/**
	 * @param ceilingHeight the ceilingHeight to set
	 */
	public void setCeilingHeight(double ceilingHeight) {
		this.ceilingHeight = ceilingHeight;
	}

	/**
	 * @return the visibility
	 */
	public double getVisibility() {
		return visibility;
	}

	/**
	 * @param visibility the visibility to set
	 */
	public void setVisibility(double visibility) {
		this.visibility = visibility;
	}

	/**
	 * @return the lightningActivityLevel
	 */
	public int getLightningActivityLevel() {
		return lightningActivityLevel;
	}

	/**
	 * @param lightningActivityLevel the lightningActivityLevel to set
	 */
	public void setLightningActivityLevel(int lightningActivityLevel) {
		this.lightningActivityLevel = lightningActivityLevel;
	}

	/**
	 * @return the pressure
	 */
	public double getPressure() {
		return pressure;
	}

	/**
	 * @param pressure the pressure to set
	 */
	public void setPressure(double pressure) {
		this.pressure = pressure;
	}

	/**
	 * @return the thunderProbability
	 */
	public int getThunderProbability() {
		return thunderProbability;
	}

	/**
	 * @param thunderProbability the thunderProbability to set
	 */
	public void setThunderProbability(int thunderProbability) {
		this.thunderProbability = thunderProbability;
	}

}
