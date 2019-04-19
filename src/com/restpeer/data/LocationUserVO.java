package com.restpeer.data;

// JDk 1.8.x
import java.sql.ResultSet;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: LocationUserVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the assignment and roles of a user to a location
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 15, 2019
 * @updates:
 ****************************************************************************/
@Table(name="rp_location_user_xr")
public class LocationUserVO extends RPUserVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9066825926399099692L;

	// Members
	private String locationUserId;
	private String memberLocationId;
	
	/**
	 * 
	 */
	public LocationUserVO() {
		super();
	}

	/**
	 * @param req
	 */
	public LocationUserVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public LocationUserVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the locationUserId
	 */
	@Column(name="location_user_id", isPrimaryKey=true)
	public String getLocationUserId() {
		return locationUserId;
	}

	/**
	 * @return the memberLocationId
	 */
	@Column(name="dealer_location_id")
	public String getMemberLocationId() {
		return memberLocationId;
	}

	/**
	 * @return the roleId
	 */
	@Column(name="role_id")
	@Override
	public String getRoleId() {
		return super.getRoleId();
	}

	/**
	 * @param locationUserId the locationUserId to set
	 */
	public void setLocationUserId(String locationUserId) {
		this.locationUserId = locationUserId;
	}

	/**
	 * @param memberLocationId the memberLocationId to set
	 */
	public void setMemberLocationId(String memberLocationId) {
		this.memberLocationId = memberLocationId;
	}
}

