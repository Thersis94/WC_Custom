package com.irricurb.action.data.vo;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/********************************************************************
 * <b>Title: </b>ZoneGeocodeVO.java<br/>
 * <b>Description: </b>Bean holding a point in a polygon for the zone<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author James Camire
 * @version 3.x
 * @since Dec 18, 2017
 * Last Updated: 
 *******************************************************************/
@Table(name="ic_project_zone_geo")
public class ZoneGeocodeVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6750156456055340674L;
	
	// Member Variables
	private String zoneGeoId;
	private String projectZoneId;
	private double latitude;
	private double longitude;
	private Date createDate;
	
	/**
	 * 
	 */
	public ZoneGeocodeVO() {
		super();
	}

	/**
	 * @param req
	 */
	public ZoneGeocodeVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public ZoneGeocodeVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the zoneGeoId
	 */
	@Column(name="project_zone_geo_id", isPrimaryKey=true, isAutoGen=true)
	public String getZoneGeoId() {
		return zoneGeoId;
	}

	/**
	 * @return the projectZoneId
	 */
	@Column(name="project_zone_id")
	public String getProjectZoneId() {
		return projectZoneId;
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
	 * @param zoneGeoId the zoneGeoId to set
	 */
	public void setZoneGeoId(String zoneGeoId) {
		this.zoneGeoId = zoneGeoId;
	}

	/**
	 * @param projectZoneId the projectZoneId to set
	 */
	public void setProjectZoneId(String projectZoneId) {
		this.projectZoneId = projectZoneId;
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

}
