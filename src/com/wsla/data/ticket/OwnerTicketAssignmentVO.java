package com.wsla.data.ticket;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.wsla.data.provider.ProviderLocationVO;

/****************************************************************************
 * <b>Title</b>: OwnerTicketAssignmentVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> used to map owner data to the correct ticket assignment vo
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Nov 6, 2018
 * @updates:
 ****************************************************************************/
public class OwnerTicketAssignmentVO extends TicketAssignmentVO {

	private static final long serialVersionUID = 3327535846603396947L;

	/**
	 * 
	 */
	public OwnerTicketAssignmentVO() {
	}

	/**
	 * @param req
	 */
	public OwnerTicketAssignmentVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public OwnerTicketAssignmentVO(ResultSet rs) {
		super(rs);
		this.setlocationData(rs);
	}


	/**
	 * @param rs
	 * @throws SQLException 
	 */
	private void setlocationData(ResultSet rs)  {
		ProviderLocationVO loc = getLocation();
		if(loc == null) {
			loc = new ProviderLocationVO();
		}
		try {
			loc.setAddress(rs.getString("own_address_txt"));
			loc.setAddress2(rs.getString("own_address2_txt"));
			loc.setState(rs.getString("own_state_cd"));
			loc.setCity(rs.getString("own_city_nm"));
			loc.setLocationName(rs.getString("own_location_nm"));
			loc.setZipCode(rs.getString("own_zip_cd"));
			loc.setLongitude(rs.getDouble("own_longitude_no"));
			loc.setLatitude(rs.getDouble("own_latitude_no"));
			
		} catch (SQLException e) {
			log.error("could not set data to owner location",e);
		}
		
		setLocation(loc);
	}

	/**
	 * @return the ticketAssignmentId
	 */
	@Override
	@Column(name="own_ticket_assg_id", isPrimaryKey=true, isReadOnly=true)
	public String getTicketAssignmentId() {
		return super.getTicketAssignmentId();
	}

	/**
	 * @return the locationId
	 */
	@Override
	@Column(name="own_location_id", isReadOnly=true)
	public String getLocationId() {
		return super.getLocationId();
	}

	/**
	 * @return the userId
	 */
	@Override
	@Column(name="own_user_id", isReadOnly=true)
	public String getUserId() {
		return super.getUserId();
	}

	/**
	 * @return the ticketId
	 */
	@Override
	@Column(name="own_ticket_id", isReadOnly=true)
	public String getTicketId() {
		return super.getTicketId();
	}

	/**
	 * @return the ownerFlag
	 */
	@Override
	@Column(name="own_owner_flg", isReadOnly=true)
	public int getOwnerFlag() {
		return super.getOwnerFlag();
	}

	/**
	 * @return the actionableFlag
	 */
	@Override
	@Column(name="own_assg_type_cd", isReadOnly=true)
	public TypeCode getTypeCode() {
		return super.getTypeCode();
	}
	
	/**
	 * @return the createDate
	 */
	@Override
	@Column(name="own_create_dt", isReadOnly=true)
	public Date getCreateDate() {
		return super.getCreateDate();
	}

	/**
	 * @return the updateDate
	 */
	@Override
	@Column(name="own_update_dt", isReadOnly=true)
	public Date getUpdateDate() {
		return super.getUpdateDate();
	}
}
