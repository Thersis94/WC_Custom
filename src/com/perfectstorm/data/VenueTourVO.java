package com.perfectstorm.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

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
	public String getRadarCode() {
		return radarCode;
	}

	/**
	 * @param radarCode the radarCode to set
	 */
	public void setRadarCode(String radarCode) {
		this.radarCode = radarCode;
	}
}

