package com.perfectstorm.data;

// JDK 1.8.x
import java.sql.ResultSet;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: TourDeviceVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value Object for the weather devices on a tour
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 4, 2019
 * @updates:
 ****************************************************************************/
@Table(name="ps_weather_device")
public class TourDeviceVO extends WeatherDeviceVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1078133238271341507L;

	// Members
	private String tourDeviceId;
	private String venueTourId;
	private String ipAddress;
	
	/**
	 * 
	 */
	public TourDeviceVO() {
		super();
	}

	/**
	 * @param req
	 */
	public TourDeviceVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public TourDeviceVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the tourDeviceId
	 */
	@Column(name="tour_device_id", isPrimaryKey=true)
	public String getTourDeviceId() {
		return tourDeviceId;
	}

	/**
	 * @return the venueTourId
	 */
	@Column(name="venue_tour_id")
	public String getVenueTourId() {
		return venueTourId;
	}

	/**
	 * @return the ipAddress
	 */
	@Column(name="ip_address_txt")
	public String getIpAddress() {
		return ipAddress;
	}

	/**
	 * @param tourDeviceId the tourDeviceId to set
	 */
	public void setTourDeviceId(String tourDeviceId) {
		this.tourDeviceId = tourDeviceId;
	}

	/**
	 * @param venueTourId the venueTourId to set
	 */
	public void setVenueTourId(String venueTourId) {
		this.venueTourId = venueTourId;
	}

	/**
	 * @param ipAddress the ipAddress to set
	 */
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

}

