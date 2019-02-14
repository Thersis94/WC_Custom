package com.perfectstorm.data.weather.forecast.element;

import java.sql.ResultSet;

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
