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
	
	// Member Variables
	private String ticketAssigmentId;
	private String locationId;
	private String userId;
	private String ticketId;
	private int ownerFlag;
	private int actionableFlag;
	private Date createDate;
	
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
	 * @return the ticketAssigmentId
	 */
	@Column(name="ticket_assig_id", isPrimaryKey=true)
	public String getTicketAssigmentId() {
		return ticketAssigmentId;
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
	@Column(name="actionable_flg")
	public int getActionableFlag() {
		return actionableFlag;
	}
	
	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
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
	 * @param ticketAssigmentId the ticketAssigmentId to set
	 */
	public void setTicketAssigmentId(String ticketAssigmentId) {
		this.ticketAssigmentId = ticketAssigmentId;
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
	public void setActionableFlag(int actionableFlag) {
		this.actionableFlag = actionableFlag;
	}

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

}

