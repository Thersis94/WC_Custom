package com.irricurb.action.data.vo;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.Location;
import com.siliconmtn.util.StringUtil;

/********************************************************************
 * <b>Title: </b>ProjectLocationVO.java<br/>
 * <b>Description: </b>Data bean holding the project location information<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author James Camire
 * @version 3.x
 * @since Dec 18, 2017
 * Last Updated: 
 *******************************************************************/
@Table(name="ic_project_location")
public class ProjectLocationVO extends GeocodeLocation {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1430336792758465699L;

	// Member Variables
	private String projectLocationId;
	private String projectId;
	private String name;
	private String networkAddress;
	private int manualFlag;
	
	// Collections
	private List<ProjectZoneVO> zones = new ArrayList<>(32);
	
	/**
	 * 
	 */
	public ProjectLocationVO() {
		super();
	}

	/**
	 * @param fullAddress
	 */
	public ProjectLocationVO(String fullAddress) {
		super(fullAddress);
	}
	
	/**
	 * @param l
	 */
	public ProjectLocationVO(Location l) {
		super(l);
	}
	
	/**
	 * 
	 * @param rs
	 */
	public ProjectLocationVO(ResultSet rs) {
		super();
		populateData(rs);
	}
	
	/**
	 * 
	 * @param rs
	 */
	public ProjectLocationVO(ActionRequest req) {
		super();
		populateData(req);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.gis.GeocodeLocation#toString()
	 */
	@Override
	public String toString() {
		return StringUtil.getToString(this);
	}

	/**
	 * @return the projectLocationId
	 */
	@Column(name="project_location_id",isPrimaryKey=true)
	public String getProjectLocationId() {
		return projectLocationId;
	}

	/**
	 * @return the projectId
	 */
	@Column(name="project_id")
	public String getProjectId() {
		return projectId;
	}

	/**
	 * @return the name
	 */
	@Column(name="location_nm")
	public String getName() {
		return name;
	}

	/**
	 * @return the manualFlag
	 */
	@Column(name="manual_flg")
	public int getManualFlag() {
		return manualFlag;
	}

	/**
	 * @param projectLocationId the projectLocationId to set
	 */
	public void setProjectLocationId(String projectLocationId) {
		this.projectLocationId = projectLocationId;
	}

	/**
	 * @param projectId the projectId to set
	 */
	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param manualFlag the manualFlag to set
	 */
	public void setManualFlag(int manualFlag) {
		this.manualFlag = manualFlag;
	}

	/**
	 * @return the zones
	 */
	public List<ProjectZoneVO> getZones() {
		return zones;
	}

	/**
	 * @param zones  add a zone to the zones set
	 */
	@BeanSubElement
	public void addZone(ProjectZoneVO zone) {
		this.zones.add(zone);
	}
	
	/**
	 * @param zones the zones to set
	 */
	public void setZones(List<ProjectZoneVO> zones) {
		this.zones = zones;
	}

	/**
	 * @return the networkAddress
	 */
	@Column(name="network_address_txt")
	public String getNetworkAddress() {
		return networkAddress;
	}

	/**
	 * @param networkAddress the networkAddress to set
	 */
	public void setNetworkAddress(String networkAddress) {
		this.networkAddress = networkAddress;
	}

}
