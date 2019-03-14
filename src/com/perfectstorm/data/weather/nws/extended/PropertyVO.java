package com.perfectstorm.data.weather.nws.extended;

import java.util.ArrayList;
import java.util.List;

import com.perfectstorm.data.weather.nws.ElevationVO;
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: PropertyVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Properties of an extended daily forecast.
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Feb 11, 2019
 * @updates:
 ****************************************************************************/

public class PropertyVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3820209197543816373L;
	private String units;
	private String forecastGenerator;
	private String generatedAt;
	private String updateTime;
	private String validTimes;
	private ElevationVO elevation;
	private List<PeriodVO> periods =  new ArrayList<>(14);

	/**
	 * @return the units
	 */
	public String getUnits() {
		return units;
	}

	/**
	 * @param units the units to set
	 */
	public void setUnits(String units) {
		this.units = units;
	}

	/**
	 * @return the forecastGenerator
	 */
	public String getForecastGenerator() {
		return forecastGenerator;
	}

	/**
	 * @param forecastGenerator the forecastGenerator to set
	 */
	public void setForecastGenerator(String forecastGenerator) {
		this.forecastGenerator = forecastGenerator;
	}

	/**
	 * @return the generatedAt
	 */
	public String getGeneratedAt() {
		return generatedAt;
	}

	/**
	 * @param generatedAt the generatedAt to set
	 */
	public void setGeneratedAt(String generatedAt) {
		this.generatedAt = generatedAt;
	}

	/**
	 * @return the updateTime
	 */
	public String getUpdateTime() {
		return updateTime;
	}

	/**
	 * @param updateTime the updateTime to set
	 */
	public void setUpdateTime(String updateTime) {
		this.updateTime = updateTime;
	}

	/**
	 * @return the validTimes
	 */
	public String getValidTimes() {
		return validTimes;
	}

	/**
	 * @param validTimes the validTimes to set
	 */
	public void setValidTimes(String validTimes) {
		this.validTimes = validTimes;
	}

	/**
	 * @return the elevation
	 */
	public ElevationVO getElevation() {
		return elevation;
	}

	/**
	 * @param elevation the elevation to set
	 */
	public void setElevation(ElevationVO elevation) {
		this.elevation = elevation;
	}

	/**
	 * @return the periods
	 */
	public List<PeriodVO> getPeriods() {
		return periods;
	}

	/**
	 * @param periods the periods to set
	 */
	public void setPeriods(List<PeriodVO> periods) {
		this.periods = periods;
	}

	/**
	 * 
	 */
	public PropertyVO() {
		super();
	}
}

