package com.perfectstorm.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.perfectstorm.data.weather.VenueWeatherStationVO;
// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: VenueVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value Object for a Venue location
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 4, 2019
 * @updates:
 ****************************************************************************/
@Table(name="ps_venue")
public class VenueVO extends GeocodeLocation {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3863178709122752267L;
	
	// Members
	private String venueId;
	private String customerId;
	private String timezone;
	private String venueName;
	private String venueDescription;
	private int activeFlag;
	private int manualGeocodeFlag;
	private long numberStation;
	private String observationStationCode;
	private String radarStationCode;
	private String forecastOfficeCode;
	private int forecastGridXNo;
	private int forecastGridYNo;
	private Date createDate;
	private Date updateDate;
	
	// Helpers
	private List<Date> radarTime;
	private String customerName;
	
	// Bean SubElements
	private List<VenueAttributeVO> attributes = new ArrayList<>();
	private List<VenueWeatherStationVO> weatherStations = new ArrayList<>();
	
	/**
	 * 
	 */
	public VenueVO() {
		super();
	}

	/**
	 * @param req
	 */
	public VenueVO(ActionRequest req) {
		this.populateData(req);
		setObservationStationCode(StringUtil.checkVal(getObservationStationCode(), null));
	}

	/**
	 * @param rs
	 */
	public VenueVO(ResultSet rs) {
		this.populateData(rs);
	}

	/**
	 * @return the venueId
	 */
	@Column(name="venue_id", isPrimaryKey=true)
	public String getVenueId() {
		return venueId;
	}

	/**
	 * @return the venueName
	 */
	@Column(name="venue_nm")
	public String getVenueName() {
		return venueName;
	}

	/**
	 * @return the venueDescription
	 */
	@Column(name="venue_desc")
	public String getVenueDescription() {
		return venueDescription;
	}

	/**
	 * @return the activeFlag
	 */
	@Column(name="active_flg")
	public int getActiveFlag() {
		return activeFlag;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @return the manualGeocodeFlag
	 */
	@Column(name="manual_geocode_flg")
	public int getManualGeocodeFlag() {
		return manualGeocodeFlag;
	}

	/**
	 * @return the customerId
	 */
	@Column(name="customer_id")
	public String getCustomerId() {
		return customerId;
	}

	/**
	 * @return the numberStation
	 */
	@Column(name="station_no", isReadOnly=true)
	public long getNumberStation() {
		return numberStation;
	}

	/**
	 * @return the timezone
	 */
	@Column(name="timezone_cd")
	public String getTimezone() {
		return timezone;
	}

	/**
	 * @return the attributes
	 */
	public List<VenueAttributeVO> getAttributes() {
		return attributes;
	}

	/**
	 * @return the weatherStations
	 */
	public List<VenueWeatherStationVO> getWeatherStations() {
		return weatherStations;
	}

	/**
	 * @param venueId the venueId to set
	 */
	public void setVenueId(String venueId) {
		this.venueId = venueId;
	}

	/**
	 * @param venueName the venueName to set
	 */
	public void setVenueName(String venueName) {
		this.venueName = venueName;
	}

	/**
	 * @param venueDescription the venueDescription to set
	 */
	public void setVenueDescription(String venueDescription) {
		this.venueDescription = venueDescription;
	}

	/**
	 * @param activeFlag the activeFlag to set
	 */
	public void setActiveFlag(int activeFlag) {
		this.activeFlag = activeFlag;
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

	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(List<VenueAttributeVO> attributes) {
		this.attributes = attributes;
	}
	
	/**
	 * 
	 * @param attribute
	 */
	@BeanSubElement
	public void addAttribute(VenueAttributeVO attribute) {
		attributes.add(attribute);
	}

	/**
	 * @param manualGeocodeFlag the manualGeocodeFlag to set
	 */
	public void setManualGeocodeFlag(int manualGeocodeFlag) {
		this.manualGeocodeFlag = manualGeocodeFlag;
	}

	/**
	 * @param customerId the customerId to set
	 */
	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	/**
	 * @param weatherStations the weatherStations to set
	 */
	public void setWeatherStations(List<VenueWeatherStationVO> weatherStations) {
		this.weatherStations = weatherStations;
	}
	
	/**
	 * 
	 * @param weatherStation
	 */
	@BeanSubElement
	public void addWeatherStation(VenueWeatherStationVO weatherStation) {
		weatherStations.add(weatherStation);
	}

	/**
	 * @param numberStation the numberStation to set
	 */
	public void setNumberStation(long numberStation) {
		this.numberStation = numberStation;
	}

	/**
	 * @param timezone the timezone to set
	 */
	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	/**
	 * @return the radarTime
	 */
	public List<Date> getRadarTime() {
		return radarTime;
	}

	/**
	 * @param radarTime the radarTime to set
	 */
	public void setRadarTime(List<Date> radarTime) {
		this.radarTime = radarTime;
	}

	/**
	 * @return the observationStationCode
	 */
	@Column(name="observation_station_cd")
	public String getObservationStationCode() {
		return observationStationCode;
	}

	/**
	 * @param observationStationCode the observationStationCode to set
	 */
	public void setObservationStationCode(String observationStationCode) {
		this.observationStationCode = observationStationCode;
	}

	/**
	 * @return the radarStationCode
	 */
	@Column(name="radar_station_cd")
	public String getRadarStationCode() {
		return radarStationCode;
	}

	/**
	 * @param radarStationCode the radarStationCode to set
	 */
	public void setRadarStationCode(String radarStationCode) {
		this.radarStationCode = radarStationCode;
	}

	/**
	 * @return the forecastOfficeCode
	 */
	@Column(name="forecast_office_cd")
	public String getForecastOfficeCode() {
		return forecastOfficeCode;
	}

	/**
	 * @param forecastOfficeCode the forecastOfficeCode to set
	 */
	public void setForecastOfficeCode(String forecastOfficeCode) {
		this.forecastOfficeCode = forecastOfficeCode;
	}

	/**
	 * @return the forecastGridXNo
	 */
	@Column(name="forecast_gridx_no")
	public int getForecastGridXNo() {
		return forecastGridXNo;
	}

	/**
	 * @param forecastGridXNo the forecastGridXNo to set
	 */
	public void setForecastGridXNo(int forecastGridXNo) {
		this.forecastGridXNo = forecastGridXNo;
	}

	/**
	 * @return the forecastGridYNo
	 */
	@Column(name="forecast_gridy_no")
	public int getForecastGridYNo() {
		return forecastGridYNo;
	}

	/**
	 * @param forecastGridYNo the forecastGridYNo to set
	 */
	public void setForecastGridYNo(int forecastGridYNo) {
		this.forecastGridYNo = forecastGridYNo;
	}

	/**
	 * @return the customerName
	 */
	@Column(name="customer_nm", isReadOnly=true)
	public String getCustomerName() {
		return customerName;
	}

	/**
	 * @param customerName the customerName to set
	 */
	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}
}

