package com.perfectstorm.data.weather.forecast.element;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title:</b> HazardVO.java
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

public class HazardVO extends BeanDataVO {

	private static final long serialVersionUID = -5369165099976669875L;
	
	// Keys for the data map
	public static final String MIXING_HEIGHT = "mixingHeight";
	public static final String HAINES = "hainesIndex";
	public static final String DISPERSION = "dispersionIndex";
	public static final String GRASSLAND_FIRE_DANGER = "grasslandFireDangerIndex";
	public static final String DAVIS_STABILITY = "davisStabilityIndex";
	public static final String ATMOSPHERIC_DISPERSION = "atmosphericDispersionIndex";
	public static final String LOW_VISIBILITY_OCCURENCE_RISK = "lowVisibilityOccurenceRiskIndex";
	public static final String STABILITY = "stabilityIndex";
	public static final String RED_FLAG_THREAT = "redFlagThreatIndex";
	
	// Members
	private double mixingHeight; // distance (ft, m)
	private int hainesIndex; // index - no unit of measure
	private int dispersionIndex; // index - no unit of measure
	private int grasslandFireDangerIndex; // index - no unit of measure
	private int davisStabilityIndex; // index - no unit of measure
	private int atmosphericDispersionIndex; // index - no unit of measure
	private int lowVisibilityOccurenceRiskIndex; // index - no unit of measure
	private int stabilityIndex; // index - no unit of measure
	private int redFlagThreatIndex; // index - no unit of measure
	
	
	public HazardVO() {
	}

	/**
	 * @param req
	 */
	public HazardVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public HazardVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * Gets a map of all values
	 * @return
	 */
	public Map<String, Integer> getDataMap() {
		Map<String, Integer> dataMap = new HashMap<>();
		dataMap.put(MIXING_HEIGHT, (int) Math.round(mixingHeight));
		dataMap.put(HAINES, hainesIndex);
		dataMap.put(DISPERSION, dispersionIndex);
		dataMap.put(GRASSLAND_FIRE_DANGER, grasslandFireDangerIndex);
		dataMap.put(DAVIS_STABILITY, davisStabilityIndex);
		dataMap.put(ATMOSPHERIC_DISPERSION, atmosphericDispersionIndex);
		dataMap.put(LOW_VISIBILITY_OCCURENCE_RISK, lowVisibilityOccurenceRiskIndex);
		dataMap.put(STABILITY, stabilityIndex);
		dataMap.put(RED_FLAG_THREAT, redFlagThreatIndex);
		
		return dataMap;
	}

	/**
	 * @return the mixingHeight
	 */
	public double getMixingHeight() {
		return mixingHeight;
	}

	/**
	 * @param mixingHeight the mixingHeight to set
	 */
	public void setMixingHeight(double mixingHeight) {
		this.mixingHeight = mixingHeight;
	}

	/**
	 * @return the hainesIndex
	 */
	public int getHainesIndex() {
		return hainesIndex;
	}

	/**
	 * @param hainesIndex the hainesIndex to set
	 */
	public void setHainesIndex(int hainesIndex) {
		this.hainesIndex = hainesIndex;
	}

	/**
	 * @return the dispersionIndex
	 */
	public int getDispersionIndex() {
		return dispersionIndex;
	}

	/**
	 * @param dispersionIndex the dispersionIndex to set
	 */
	public void setDispersionIndex(int dispersionIndex) {
		this.dispersionIndex = dispersionIndex;
	}

	/**
	 * @return the grasslandFireDangerIndex
	 */
	public int getGrasslandFireDangerIndex() {
		return grasslandFireDangerIndex;
	}

	/**
	 * @param grasslandFireDangerIndex the grasslandFireDangerIndex to set
	 */
	public void setGrasslandFireDangerIndex(int grasslandFireDangerIndex) {
		this.grasslandFireDangerIndex = grasslandFireDangerIndex;
	}

	/**
	 * @return the davisStabilityIndex
	 */
	public int getDavisStabilityIndex() {
		return davisStabilityIndex;
	}

	/**
	 * @param davisStabilityIndex the davisStabilityIndex to set
	 */
	public void setDavisStabilityIndex(int davisStabilityIndex) {
		this.davisStabilityIndex = davisStabilityIndex;
	}

	/**
	 * @return the atmosphericDispersionIndex
	 */
	public int getAtmosphericDispersionIndex() {
		return atmosphericDispersionIndex;
	}

	/**
	 * @param atmosphericDispersionIndex the atmosphericDispersionIndex to set
	 */
	public void setAtmosphericDispersionIndex(int atmosphericDispersionIndex) {
		this.atmosphericDispersionIndex = atmosphericDispersionIndex;
	}

	/**
	 * @return the lowVisibilityOccurenceRiskIndex
	 */
	public int getLowVisibilityOccurenceRiskIndex() {
		return lowVisibilityOccurenceRiskIndex;
	}

	/**
	 * @param lowVisibilityOccurenceRiskIndex the lowVisibilityOccurenceRiskIndex to set
	 */
	public void setLowVisibilityOccurenceRiskIndex(int lowVisibilityOccurenceRiskIndex) {
		this.lowVisibilityOccurenceRiskIndex = lowVisibilityOccurenceRiskIndex;
	}

	/**
	 * @return the stabilityIndex
	 */
	public int getStabilityIndex() {
		return stabilityIndex;
	}

	/**
	 * @param stabilityIndex the stabilityIndex to set
	 */
	public void setStabilityIndex(int stabilityIndex) {
		this.stabilityIndex = stabilityIndex;
	}

	/**
	 * @return the redFlagThreatIndex
	 */
	public int getRedFlagThreatIndex() {
		return redFlagThreatIndex;
	}

	/**
	 * @param redFlagThreatIndex the redFlagThreatIndex to set
	 */
	public void setRedFlagThreatIndex(int redFlagThreatIndex) {
		this.redFlagThreatIndex = redFlagThreatIndex;
	}

}
