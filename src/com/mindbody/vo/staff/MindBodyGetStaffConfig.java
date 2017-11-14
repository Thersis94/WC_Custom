package com.mindbody.vo.staff;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.mindbody.MindBodyStaffApi.StaffDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;
import com.mindbodyonline.clients.api._0_5_1.ArrayOfInt;
import com.mindbodyonline.clients.api._0_5_1.StaffCredentials;
import com.mindbodyonline.clients.api._0_5_1.StaffFilter;

/****************************************************************************
 * <b>Title:</b> MindBodyGetStaffConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages GetStaff Endpoint unique Configuration.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 11, 2017
 ****************************************************************************/
public class MindBodyGetStaffConfig extends MindBodyStaffConfig {

	private StaffCredentials staffCredentials;
	private Integer sessionTypeId;
	private Date startDateTime;
	private Integer locationId;
	private List<StaffFilter.Enum> filters;

	/**
	 * @param type
	 * @param sourceName
	 * @param sourceKey
	 * @param siteIds
	 */
	public MindBodyGetStaffConfig(MindBodyCredentialVO source, MindBodyCredentialVO user) {
		super(StaffDocumentType.GET_STAFF, source, user);
		this.filters = new ArrayList<>();
	}

	public MindBodyGetStaffConfig(MindBodyCredentialVO source, MindBodyCredentialVO user, boolean addStaffLocationField) {
		this(source, user);

		if(addStaffLocationField) {
			addField("Staff.Locations");
		}
	}

	/**
	 * @return the staffCredentials
	 */
	public StaffCredentials getStaffCredentials() {
		return staffCredentials;
	}
	/**
	 * @return the sessionTypeId
	 */
	public Integer getSessionTypeId() {
		return sessionTypeId;
	}
	/**
	 * @return the startDateTime
	 */
	public Date getStartDateTime() {
		return startDateTime;
	}
	/**
	 * @return the locationId
	 */
	public Integer getLocationId() {
		return locationId;
	}

	/**
	 * @param staffCredentials the staffCredentials to set.
	 */
	public void setStaffCredentials(StaffCredentials staffCredentials) {
		this.staffCredentials = staffCredentials;
	}

	public void setStaffCredentials(String userName, String password, List<Integer> siteIds) {
		StaffCredentials sc = StaffCredentials.Factory.newInstance();

		sc.setUsername(userName);
		sc.setPassword(password);
		ArrayOfInt arrInt = ArrayOfInt.Factory.newInstance();

		for(Integer i : siteIds) {
			arrInt.addInt(i);
		}

		sc.setSiteIDs(arrInt);

		staffCredentials = sc;
	}
	/**
	 * @param sessionTypeId the sessionTypeId to set.
	 */
	public void setSessionTypeId(Integer sessionTypeId) {
		this.sessionTypeId = sessionTypeId;
	}
	/**
	 * @param startDateTime the startDateTime to set.
	 */
	public void setStartDateTime(Date startDateTime) {
		this.startDateTime = startDateTime;
	}
	/**
	 * @param locationId the locationId to set.
	 */
	public void setLocationId(Integer locationId) {
		this.locationId = locationId;
	}

	@Override
	public boolean isValid() {
		return super.isValid() && staffCredentials != null;
	}

	/**
	 * @return the filters
	 */
	public List<StaffFilter.Enum> getFilters() {
		return filters;
	}

	public void addFilters(StaffFilter.Enum filter) {
		filters.add(filter);
	}
	/**
	 * @param filters the filters to set.
	 */
	public void setFilters(List<StaffFilter.Enum> filters) {
		this.filters = filters;
	}
}