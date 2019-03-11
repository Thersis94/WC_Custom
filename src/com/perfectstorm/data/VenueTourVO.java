package com.perfectstorm.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.perfectstorm.data.weather.forecast.ForecastVO;
// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.weather.SunTimeVO;

/****************************************************************************
 * <b>Title</b>: TourVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for a venue assigned to a tour
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 4, 2019
 * @updates:
 ****************************************************************************/
@Table(name="ps_venue_tour_xr")
public class VenueTourVO extends VenueVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7510226690607925688L;
	
	// Members
	private String venueTourId;
	private String tourId;
	private Date eventDate;
	private Date startRetrieve;
	private Date endRetrieve;
	private int orderNumber;
	
	// Helpers
	private String radarTypeCode;
	private String radarCode;
	private String tourName;
	private String tourTypeCode;
	private ForecastVO eventForecast;
	private ForecastVO currentConditions;
	private SunTimeVO eventSunTime;
	private SunTimeVO currentSunTime;
	private String timeUntilEvent;
	
	// Bean SubElements
	private List<TourDeviceVO> devices = new ArrayList<>();
	
	/**
	 * 
	 */
	public VenueTourVO() {
		super();
	}

	/**
	 * @param req
	 */
	public VenueTourVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public VenueTourVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the venueTourId
	 */
	@Column(name="venue_tour_id", isPrimaryKey=true)
	public String getVenueTourId() {
		return venueTourId;
	}

	/**
	 * @return the tourId
	 */
	@Column(name="tour_id")
	public String getTourId() {
		return tourId;
	}

	/**
	 * @return the eventDate
	 */
	@Column(name="event_dt")
	public Date getEventDate() {
		return eventDate;
	}

	/**
	 * @return the startRetrieve
	 */
	@Column(name="start_retrieve_dt")
	public Date getStartRetrieve() {
		return startRetrieve;
	}

	/**
	 * @return the endRetrieve
	 */
	@Column(name="end_retrieve_dt")
	public Date getEndRetrieve() {
		return endRetrieve;
	}

	/**
	 * @return the orderNumber
	 */
	@Column(name="order_no")
	public int getOrderNumber() {
		return orderNumber;
	}

	/**
	 * @return the devices
	 */
	public List<TourDeviceVO> getDevices() {
		return devices;
	}

	/**
	 * @param venueTourId the venueTourId to set
	 */
	public void setVenueTourId(String venueTourId) {
		this.venueTourId = venueTourId;
	}

	/**
	 * @param tourId the tourId to set
	 */
	public void setTourId(String tourId) {
		this.tourId = tourId;
	}

	/**
	 * @param eventDate the eventDate to set
	 */
	public void setEventDate(Date eventDate) {
		this.eventDate = eventDate;
		setTimeUntilEvent();
	}
 
	/**
	 * @param startRetrieve the startRetrieve to set
	 */
	public void setStartRetrieve(Date startRetrieve) {
		this.startRetrieve = startRetrieve;
	}

	/**
	 * @param endRetrieve the endRetrieve to set
	 */
	public void setEndRetrieve(Date endRetrieve) {
		this.endRetrieve = endRetrieve;
	}

	/**
	 * @param orderNumber the orderNumber to set
	 */
	public void setOrderNumber(int orderNumber) {
		this.orderNumber = orderNumber;
	}

	/**
	 * @param devices the devices to set
	 */
	public void setDevices(List<TourDeviceVO> devices) {
		this.devices = devices;
	}

	/**
	 * 
	 * @param device
	 */
	@BeanSubElement
	public void addDevice(TourDeviceVO device) {
		devices.add(device);
	}

	/**
	 * @return the radarTypeCode
	 */
	public String getRadarTypeCode() {
		return radarTypeCode;
	}

	/**
	 * @param radarTypeCode the radarTypeCode to set
	 */
	public void setRadarTypeCode(String radarTypeCode) {
		this.radarTypeCode = radarTypeCode;
	}

	/**
	 * @return the radarCode
	 */
	@Column(name="radar_cd", isReadOnly=true)
	public String getRadarCode() {
		return radarCode;
	}

	/**
	 * @param radarCode the radarCode to set
	 */
	public void setRadarCode(String radarCode) {
		this.radarCode = radarCode;
	}

	/**
	 * @return the tourName
	 */
	@Column(name="tour_nm", isReadOnly=true)
	public String getTourName() {
		return tourName;
	}

	/**
	 * @param tourName the tourName to set
	 */
	public void setTourName(String tourName) {
		this.tourName = tourName;
	}

	/**
	 * @return the tourTypeCode
	 */
	@Column(name="tour_type_cd", isReadOnly=true)
	public String getTourTypeCode() {
		return tourTypeCode;
	}

	/**
	 * @param tourTypeCode the tourTypeCode to set
	 */
	public void setTourTypeCode(String tourTypeCode) {
		this.tourTypeCode = tourTypeCode;
	}

	/**
	 * @return the eventForecast
	 */
	public ForecastVO getEventForecast() {
		return eventForecast;
	}

	/**
	 * @param eventForecast the eventForecast to set
	 */
	public void setEventForecast(ForecastVO eventForecast) {
		this.eventForecast = eventForecast;
	}

	/**
	 * @return the currentConditions
	 */
	public ForecastVO getCurrentConditions() {
		return currentConditions;
	}

	/**
	 * @param currentConditions the currentConditions to set
	 */
	public void setCurrentConditions(ForecastVO currentConditions) {
		this.currentConditions = currentConditions;
	}

	/**
	 * @return the eventSunTime
	 */
	public SunTimeVO getEventSunTime() {
		return eventSunTime;
	}

	/**
	 * @param eventSunTime the eventSunTime to set
	 */
	public void setEventSunTime(SunTimeVO eventSunTime) {
		this.eventSunTime = eventSunTime;
	}

	/**
	 * @return the currentSunTime
	 */
	public SunTimeVO getCurrentSunTime() {
		return currentSunTime;
	}

	/**
	 * @param currentSunTime the currentSunTime to set
	 */
	public void setCurrentSunTime(SunTimeVO currentSunTime) {
		this.currentSunTime = currentSunTime;
	}

	/**
	 * @return the timeUntilEvent
	 */
	public String getTimeUntilEvent() {
		return timeUntilEvent;
	}

	/**
	 * @param timeUntilEvent the timeUntilEvent to set
	 */
	public void setTimeUntilEvent(String timeUntilEvent) {
		this.timeUntilEvent = timeUntilEvent;
	}

	/**
	 * Sets the time until the start of the event using the bean's eventDate value
	 */
	public void setTimeUntilEvent() {
		LocalDateTime eventDateTime = eventDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
		LocalDateTime now = LocalDateTime.now();
		Duration remainingTime = Duration.between(now, eventDateTime);
		
		Long days = remainingTime.toDays();
		String dayString = days + " day" + (days != 1 ? "s" : "");

		Long hours = remainingTime.toHours() - (days * 24);
		String hourString = hours + " hour" + (hours != 1 ? "s" : "");
		
		String separator = days > 0 && hours > 0 ? ", " : "";
		
		timeUntilEvent = (days > 0 ? dayString : "") + separator + (hours > 0 || days == 0 ? hourString : "");
	}
}

