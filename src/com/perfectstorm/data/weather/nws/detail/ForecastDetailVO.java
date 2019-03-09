package com.perfectstorm.data.weather.nws.detail;

// JDK 1.8.x
import java.sql.ResultSet;

import com.perfectstorm.data.weather.nws.ElevationVO;
// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: ForecastDetailVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Stores the fetail data for the weather.gov data
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 12, 2019
 * @updates:
 ****************************************************************************/

public class ForecastDetailVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -301780681789441581L;
	
	// Primitive Members
	private String gridId;
	private String gridX;
	private String gridY;
	private String forecastOffice;
	private String validTimes;
	
	// Attributes to be parsed
	private WeatherAttribueVO temperature;
	private WeatherAttribueVO dewpoint;
	private WeatherAttribueVO maxTemperature;
	private WeatherAttribueVO minTemperature;
	private WeatherAttribueVO relativeHumidity;
	private WeatherAttribueVO apparentTemperature;
	private WeatherAttribueVO heatIndex;
	private WeatherAttribueVO windChill;
	private WeatherAttribueVO skyCover;
	private WeatherAttribueVO windDirection;
	private WeatherAttribueVO windSpeed;
	private WeatherAttribueVO windGust;
	private WeatherAttribueVO probabilityOfPrecipitation;
	private WeatherAttribueVO quantitativePrecipitation;
	private WeatherAttribueVO iceAccumulation;
	private WeatherAttribueVO snowfallAmount;
	private WeatherAttribueVO snowLevel;
	private WeatherAttribueVO ceilingHeight;
	private WeatherAttribueVO visibility;
	private WeatherAttribueVO transportWindSpeed;
	private WeatherAttribueVO transportWindDirection;
	private WeatherAttribueVO mixingHeight;
	private WeatherAttribueVO twentyFootWindSpeed;
	private WeatherAttribueVO twentyFootWindDirection;
	private WeatherAttribueVO probabilityOfTropicalStormWinds;
	private WeatherAttribueVO probabilityOfHurricaneWinds;
	private WeatherAttribueVO lightningActivityLevel;
	private WeatherAttribueVO probabilityOfThunder;

	
	// VO Members
	private ElevationVO elevation;

	/**
	 * 
	 */
	public ForecastDetailVO() {
		super();
	}

	/**
	 * @param req
	 */
	public ForecastDetailVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public ForecastDetailVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the gridId
	 */
	public String getGridId() {
		return gridId;
	}

	/**
	 * @return the gridX
	 */
	public String getGridX() {
		return gridX;
	}

	/**
	 * @return the gridY
	 */
	public String getGridY() {
		return gridY;
	}

	/**
	 * @return the forecastOffice
	 */
	public String getForecastOffice() {
		return forecastOffice;
	}

	/**
	 * @return the temperature
	 */
	public WeatherAttribueVO getTemperature() {
		return temperature;
	}

	/**
	 * @return the dewpoint
	 */
	public WeatherAttribueVO getDewpoint() {
		return dewpoint;
	}

	/**
	 * @return the maxTemperature
	 */
	public WeatherAttribueVO getMaxTemperature() {
		return maxTemperature;
	}

	/**
	 * @return the minTemperature
	 */
	public WeatherAttribueVO getMinTemperature() {
		return minTemperature;
	}

	/**
	 * @return the relativeHumidity
	 */
	public WeatherAttribueVO getRelativeHumidity() {
		return relativeHumidity;
	}

	/**
	 * @return the apparentTemperature
	 */
	public WeatherAttribueVO getApparentTemperature() {
		return apparentTemperature;
	}

	/**
	 * @return the heatIndex
	 */
	public WeatherAttribueVO getHeatIndex() {
		return heatIndex;
	}

	/**
	 * @return the windChill
	 */
	public WeatherAttribueVO getWindChill() {
		return windChill;
	}

	/**
	 * @return the skyCover
	 */
	public WeatherAttribueVO getSkyCover() {
		return skyCover;
	}

	/**
	 * @return the windDirection
	 */
	public WeatherAttribueVO getWindDirection() {
		return windDirection;
	}

	/**
	 * @return the windSpeed
	 */
	public WeatherAttribueVO getWindSpeed() {
		return windSpeed;
	}

	/**
	 * @return the windGust
	 */
	public WeatherAttribueVO getWindGust() {
		return windGust;
	}

	/**
	 * @return the probabilityOfPrecipitation
	 */
	public WeatherAttribueVO getProbabilityOfPrecipitation() {
		return probabilityOfPrecipitation;
	}

	/**
	 * @return the quantitativePrecipitation
	 */
	public WeatherAttribueVO getQuantitativePrecipitation() {
		return quantitativePrecipitation;
	}

	/**
	 * @return the iceAccumulation
	 */
	public WeatherAttribueVO getIceAccumulation() {
		return iceAccumulation;
	}

	/**
	 * @return the snowfallAmount
	 */
	public WeatherAttribueVO getSnowfallAmount() {
		return snowfallAmount;
	}

	/**
	 * @return the snowLevel
	 */
	public WeatherAttribueVO getSnowLevel() {
		return snowLevel;
	}

	/**
	 * @return the ceilingHeight
	 */
	public WeatherAttribueVO getCeilingHeight() {
		return ceilingHeight;
	}

	/**
	 * @return the visibility
	 */
	public WeatherAttribueVO getVisibility() {
		return visibility;
	}

	/**
	 * @return the transportWindSpeed
	 */
	public WeatherAttribueVO getTransportWindSpeed() {
		return transportWindSpeed;
	}

	/**
	 * @return the transportWindDirection
	 */
	public WeatherAttribueVO getTransportWindDirection() {
		return transportWindDirection;
	}

	/**
	 * @return the mixingHeight
	 */
	public WeatherAttribueVO getMixingHeight() {
		return mixingHeight;
	}

	/**
	 * @return the twentyFootWindSpeed
	 */
	public WeatherAttribueVO getTwentyFootWindSpeed() {
		return twentyFootWindSpeed;
	}

	/**
	 * @return the twentyFootWindDirection
	 */
	public WeatherAttribueVO getTwentyFootWindDirection() {
		return twentyFootWindDirection;
	}

	/**
	 * @return the probabilityOfTropicalStormWinds
	 */
	public WeatherAttribueVO getProbabilityOfTropicalStormWinds() {
		return probabilityOfTropicalStormWinds;
	}

	/**
	 * @return the probabilityOfHurricaneWinds
	 */
	public WeatherAttribueVO getProbabilityOfHurricaneWinds() {
		return probabilityOfHurricaneWinds;
	}

	/**
	 * @return the elevation
	 */
	public ElevationVO getElevation() {
		return elevation;
	}

	/**
	 * @param gridId the gridId to set
	 */
	public void setGridId(String gridId) {
		this.gridId = gridId;
	}

	/**
	 * @param gridX the gridX to set
	 */
	public void setGridX(String gridX) {
		this.gridX = gridX;
	}

	/**
	 * @param gridY the gridY to set
	 */
	public void setGridY(String gridY) {
		this.gridY = gridY;
	}

	/**
	 * @param forecastOffice the forecastOffice to set
	 */
	public void setForecastOffice(String forecastOffice) {
		this.forecastOffice = forecastOffice;
	}

	/**
	 * @param temperature the temperature to set
	 */
	public void setTemperature(WeatherAttribueVO temperature) {
		this.temperature = temperature;
	}

	/**
	 * @param dewpoint the dewpoint to set
	 */
	public void setDewpoint(WeatherAttribueVO dewpoint) {
		this.dewpoint = dewpoint;
	}

	/**
	 * @param maxTemperature the maxTemperature to set
	 */
	public void setMaxTemperature(WeatherAttribueVO maxTemperature) {
		this.maxTemperature = maxTemperature;
	}

	/**
	 * @param minTemperature the minTemperature to set
	 */
	public void setMinTemperature(WeatherAttribueVO minTemperature) {
		this.minTemperature = minTemperature;
	}

	/**
	 * @param relativeHumidity the relativeHumidity to set
	 */
	public void setRelativeHumidity(WeatherAttribueVO relativeHumidity) {
		this.relativeHumidity = relativeHumidity;
	}

	/**
	 * @param apparentTemperature the apparentTemperature to set
	 */
	public void setApparentTemperature(WeatherAttribueVO apparentTemperature) {
		this.apparentTemperature = apparentTemperature;
	}

	/**
	 * @param heatIndex the heatIndex to set
	 */
	public void setHeatIndex(WeatherAttribueVO heatIndex) {
		this.heatIndex = heatIndex;
	}

	/**
	 * @param windChill the windChill to set
	 */
	public void setWindChill(WeatherAttribueVO windChill) {
		this.windChill = windChill;
	}

	/**
	 * @param skyCover the skyCover to set
	 */
	public void setSkyCover(WeatherAttribueVO skyCover) {
		this.skyCover = skyCover;
	}

	/**
	 * @param windDirection the windDirection to set
	 */
	public void setWindDirection(WeatherAttribueVO windDirection) {
		this.windDirection = windDirection;
	}

	/**
	 * @param windSpeed the windSpeed to set
	 */
	public void setWindSpeed(WeatherAttribueVO windSpeed) {
		this.windSpeed = windSpeed;
	}

	/**
	 * @param windGust the windGust to set
	 */
	public void setWindGust(WeatherAttribueVO windGust) {
		this.windGust = windGust;
	}

	/**
	 * @param probabilityOfPrecipitation the probabilityOfPrecipitation to set
	 */
	public void setProbabilityOfPrecipitation(WeatherAttribueVO probabilityOfPrecipitation) {
		this.probabilityOfPrecipitation = probabilityOfPrecipitation;
	}

	/**
	 * @param quantitativePrecipitation the quantitativePrecipitation to set
	 */
	public void setQuantitativePrecipitation(WeatherAttribueVO quantitativePrecipitation) {
		this.quantitativePrecipitation = quantitativePrecipitation;
	}

	/**
	 * @param iceAccumulation the iceAccumulation to set
	 */
	public void setIceAccumulation(WeatherAttribueVO iceAccumulation) {
		this.iceAccumulation = iceAccumulation;
	}

	/**
	 * @param snowfallAmount the snowfallAmount to set
	 */
	public void setSnowfallAmount(WeatherAttribueVO snowfallAmount) {
		this.snowfallAmount = snowfallAmount;
	}

	/**
	 * @param snowLevel the snowLevel to set
	 */
	public void setSnowLevel(WeatherAttribueVO snowLevel) {
		this.snowLevel = snowLevel;
	}

	/**
	 * @param ceilingHeight the ceilingHeight to set
	 */
	public void setCeilingHeight(WeatherAttribueVO ceilingHeight) {
		this.ceilingHeight = ceilingHeight;
	}

	/**
	 * @param visibility the visibility to set
	 */
	public void setVisibility(WeatherAttribueVO visibility) {
		this.visibility = visibility;
	}

	/**
	 * @param transportWindSpeed the transportWindSpeed to set
	 */
	public void setTransportWindSpeed(WeatherAttribueVO transportWindSpeed) {
		this.transportWindSpeed = transportWindSpeed;
	}

	/**
	 * @param transportWindDirection the transportWindDirection to set
	 */
	public void setTransportWindDirection(WeatherAttribueVO transportWindDirection) {
		this.transportWindDirection = transportWindDirection;
	}

	/**
	 * @param mixingHeight the mixingHeight to set
	 */
	public void setMixingHeight(WeatherAttribueVO mixingHeight) {
		this.mixingHeight = mixingHeight;
	}

	/**
	 * @param twentyFootWindSpeed the twentyFootWindSpeed to set
	 */
	public void setTwentyFootWindSpeed(WeatherAttribueVO twentyFootWindSpeed) {
		this.twentyFootWindSpeed = twentyFootWindSpeed;
	}

	/**
	 * @param twentyFootWindDirection the twentyFootWindDirection to set
	 */
	public void setTwentyFootWindDirection(WeatherAttribueVO twentyFootWindDirection) {
		this.twentyFootWindDirection = twentyFootWindDirection;
	}

	/**
	 * @param probabilityOfTropicalStormWinds the probabilityOfTropicalStormWinds to set
	 */
	public void setProbabilityOfTropicalStormWinds(WeatherAttribueVO probabilityOfTropicalStormWinds) {
		this.probabilityOfTropicalStormWinds = probabilityOfTropicalStormWinds;
	}

	/**
	 * @param probabilityOfHurricaneWinds the probabilityOfHurricaneWinds to set
	 */
	public void setProbabilityOfHurricaneWinds(WeatherAttribueVO probabilityOfHurricaneWinds) {
		this.probabilityOfHurricaneWinds = probabilityOfHurricaneWinds;
	}

	/**
	 * @param elevation the elevation to set
	 */
	public void setElevation(ElevationVO elevation) {
		this.elevation = elevation;
	}

	/**
	 * @return the lightningActivityLevel
	 */
	public WeatherAttribueVO getLightningActivityLevel() {
		return lightningActivityLevel;
	}

	/**
	 * @param lightningActivityLevel the lightningActivityLevel to set
	 */
	public void setLightningActivityLevel(WeatherAttribueVO lightningActivityLevel) {
		this.lightningActivityLevel = lightningActivityLevel;
	}

	/**
	 * @return the probabilityOfThunder
	 */
	public WeatherAttribueVO getProbabilityOfThunder() {
		return probabilityOfThunder;
	}

	/**
	 * @param probabilityOfThunder the probabilityOfThunder to set
	 */
	public void setProbabilityOfThunder(WeatherAttribueVO probabilityOfThunder) {
		this.probabilityOfThunder = probabilityOfThunder;
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
}

