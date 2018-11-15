package com.wsla.data.ticket;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.gis.GeocodeLocation;
import com.wsla.data.provider.ProviderLocationVO;

/****************************************************************************
 * <b>Title</b>: TicketAssignmentVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object for the assignments on a ticket
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 17, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_ticket_assignment")
public class TicketAssignmentVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -868583252567619404L;
	
	/**
	 * Options for the type of Assignment
	 */
	public enum TypeCode {
		CALLER, CAS, OEM, RETAILER, WATCHER;
	}
	
	/**
	 * Definition of who owns the product
	 */
	public enum ProductOwner {
		END_USER, OEM, RETAILER, COURIER, WSLA;
	}
	
	// Member Variables
	private String ticketAssignmentId;
	private String locationId;
	private String userId;
	private String ticketId;
	private int ownerFlag;
	private TypeCode typeCode;
	private Date createDate;
	private Date updateDate;
	
	// Bean Sub-Elements
	private ProviderLocationVO location;
	private UserVO user;

	/**
	 * 
	 */
	public TicketAssignmentVO() {
		super();
	}

	/**
	 * @param req
	 */
	public TicketAssignmentVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public TicketAssignmentVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * Returns the location regardless of whether the assignment is a
	 * user or a provider location.
	 * 
	 * @return
	 */
	public GeocodeLocation getAssignmentLocation() {
		if (TypeCode.CALLER.equals(typeCode)) {
			if (user == null || user.getProfile() == null) {
				return null;
			} else {
				return user.getProfile().getLocation();
			}
		} else {
			return location;
		}
	}
	
	/**
	 * Returns a name regardless of whether the assignment is a
	 * user or a provider location.
	 * 
	 * @return
	 */
	public String getAssignmentName() {
		if (TypeCode.CALLER.equals(typeCode)) {
			if (user == null) {
				return null;
			} else {
				return user.getFirstName() + ' ' + user.getLastName();
			}
		} else {
			if(location == null) {
				return null;
			}else {
				return location.getLocationName();
			}
			
		}
	}
	
	/**
	 * @return the ticketAssignmentId
	 */
	@Column(name="ticket_assg_id", isPrimaryKey=true)
	public String getTicketAssignmentId() {
		return ticketAssignmentId;
	}

	/**
	 * @return the locationId
	 */
	@Column(name="location_id")
	public String getLocationId() {
		return locationId;
	}

	/**
	 * @return the userId
	 */
	@Column(name="user_id")
	public String getUserId() {
		return userId;
	}

	/**
	 * @return the ticketId
	 */
	@Column(name="ticket_id")
	public String getTicketId() {
		return ticketId;
	}

	/**
	 * @return the ownerFlag
	 */
	@Column(name="owner_flg")
	public int getOwnerFlag() {
		return ownerFlag;
	}

	/**
	 * @return the actionableFlag
	 */
	@Column(name="assg_type_cd")
	public TypeCode getTypeCode() {
		return typeCode;
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
	 * @return the location
	 */
	public ProviderLocationVO getLocation() {
		return location;
	}

	/**
	 * @return the user
	 */
	public UserVO getUser() {
		return user;
	}

	/**
	 * @param ticketAssignmentId the ticketAssigmentId to set
	 */
	public void setTicketAssignmentId(String ticketAssignmentId) {
		this.ticketAssignmentId = ticketAssignmentId;
	}

	/**
	 * @param locationId the locationId to set
	 */
	public void setLocationId(String locationId) {
		this.locationId = locationId;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * @param ticketId the ticketId to set
	 */
	public void setTicketId(String ticketId) {
		this.ticketId = ticketId;
	}

	/**
	 * @param ownerFlag the ownerFlag to set
	 */
	public void setOwnerFlag(int ownerFlag) {
		this.ownerFlag = ownerFlag;
	}

	/**
	 * @param actionableFlag the actionableFlag to set
	 */
	public void setTypeCode(TypeCode typeCode) {
		this.typeCode = typeCode;
	}

	/**
	 * 
	 * @param createDate
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}
	
	/**
	 * @param location the location to set
	 */
	@BeanSubElement
	public void setLocation(ProviderLocationVO location) {
		this.location = location;
	}

	/**
	 * @param user the user to set
	 */
	@BeanSubElement
	public void setUser(UserVO user) {
		this.user = user;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

}

