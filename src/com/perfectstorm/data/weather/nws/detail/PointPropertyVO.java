package com.perfectstorm.data.weather.nws.detail;

import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: PointPropertyVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Metadata properties of a weather location point. 
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Feb 14, 2019
 * @updates:
 ****************************************************************************/

public class PointPropertyVO extends BeanDataVO {

	private static final long serialVersionUID = 6401784057543340407L;

	private int gridX;
	private int gridY;
	private String forecast;
	private String forecastHourly;
	private String forecastGridData;
	private String radarStation;
	private String cwa;

	public PointPropertyVO() {
		super();
	}

	/**
	 * @return the gridX
	 */
	public int getGridX() {
		return gridX;
	}

	/**
	 * @param gridX the gridX to set
	 */
	public void setGridX(int gridX) {
		this.gridX = gridX;
	}

	/**
	 * @return the gridY
	 */
	public int getGridY() {
		return gridY;
	}

	/**
	 * @param gridY the gridY to set
	 */
	public void setGridY(int gridY) {
		this.gridY = gridY;
	}

	/**
	 * @return the forecast
	 */
	public String getForecast() {
		return forecast;
	}

	/**
	 * @param forecast the forecast to set
	 */
	public void setForecast(String forecast) {
		this.forecast = forecast;
	}

	/**
	 * @return the forecastHourly
	 */
	public String getForecastHourly() {
		return forecastHourly;
	}

	/**
	 * @param forecastHourly the forecastHourly to set
	 */
	public void setForecastHourly(String forecastHourly) {
		this.forecastHourly = forecastHourly;
	}

	/**
	 * @return the forecastGridData
	 */
	public String getForecastGridData() {
		return forecastGridData;
	}

	/**
	 * @param forecastGridData the forecastGridData to set
	 */
	public void setForecastGridData(String forecastGridData) {
		this.forecastGridData = forecastGridData;
	}

	/**
	 * @return the radarStation
	 */
	public String getRadarStation() {
		return radarStation;
	}

	/**
	 * @param radarStation the radarStation to set
	 */
	public void setRadarStation(String radarStation) {
		this.radarStation = radarStation;
	}

	/**
	 * @return the cwa
	 */
	public String getCwa() {
		return cwa;
	}

	/**
	 * @param cwa the cwa to set
	 */
	public void setCwa(String cwa) {
		this.cwa = cwa;
	}
}

