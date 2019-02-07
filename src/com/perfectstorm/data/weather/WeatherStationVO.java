package com.perfectstorm.data.weather;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: WeatherStationVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value Object for the list of weather stations
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 6, 2019
 * @updates:
 ****************************************************************************/
@Table(name="ps_weather_station")
public class WeatherStationVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2911478488363254820L;
	
	// Members
	private String weatherStationId;
	private String timezoneId;
	private String stationCode;
	private String stationName;
	private String city;
	private String state;
	private String country;
	private int elevation;
	private int activeFlag;
	private double latitude;
	private double longitude;
	private Date createDate;
	private Date updateDate;
	
	/**
	 * 
	 */
	public WeatherStationVO() {
		super();
	}

	/**
	 * @param req
	 */
	public WeatherStationVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public WeatherStationVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the weatherStationId
	 */
	@Column(name="weather_station_id", isPrimaryKey=true)
	public String getWeatherStationId() {
		return weatherStationId;
	}

	/**
	 * @return the timezoneId
	 */
	@Column(name="timezone_id")
	public String getTimezoneId() {
		return timezoneId;
	}

	/**
	 * @return the stationCode
	 */
	@Column(name="station_cd")
	public String getStationCode() {
		return stationCode;
	}

	/**
	 * @return the stationName
	 */
	@Column(name="station_nm")
	public String getStationName() {
		return stationName;
	}

	/**
	 * @return the city
	 */
	@Column(name="city_nm")
	public String getCity() {
		return city;
	}

	/**
	 * @return the state
	 */
	@Column(name="state_cd")
	public String getState() {
		return state;
	}

	/**
	 * @return the country
	 */
	@Column(name="country_cd")
	public String getCountry() {
		return country;
	}

	/**
	 * @return the elevation
	 */
	@Column(name="elevation_no")
	public int getElevation() {
		return elevation;
	}

	/**
	 * @return the activeFlag
	 */
	@Column(name="active_flg")
	public int getActiveFlag() {
		return activeFlag;
	}

	/**
	 * @return the latitude
	 */
	@Column(name="latitude_no")
	public double getLatitude() {
		return latitude;
	}

	/**
	 * @return the longitude
	 */
	@Column(name="longitude_no")
	public double getLongitude() {
		return longitude;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly=true, isAutoGen=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @param weatherStationId the weatherStationId to set
	 */
	public void setWeatherStationId(String weatherStationId) {
		this.weatherStationId = weatherStationId;
	}

	/**
	 * @param timezoneId the timezoneId to set
	 */
	public void setTimezoneId(String timezoneId) {
		this.timezoneId = timezoneId;
	}

	/**
	 * @param stationCode the stationCode to set
	 */
	public void setStationCode(String stationCode) {
		this.stationCode = stationCode;
	}

	/**
	 * @param stationName the stationName to set
	 */
	public void setStationName(String stationName) {
		this.stationName = stationName;
	}

	/**
	 * @param city the city to set
	 */
	public void setCity(String city) {
		this.city = city;
	}

	/**
	 * @param state the state to set
	 */
	public void setState(String state) {
		this.state = state;
	}

	/**
	 * @param country the country to set
	 */
	public void setCountry(String country) {
		this.country = country;
	}

	/**
	 * @param elevation the elevation to set
	 */
	public void setElevation(int elevation) {
		this.elevation = elevation;
	}

	/**
	 * @param activeFlag the activeFlag to set
	 */
	public void setActiveFlag(int activeFlag) {
		this.activeFlag = activeFlag;
	}

	/**
	 * @param latitude the latitude to set
	 */
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	/**
	 * @param longitude the longitude to set
	 */
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

}

