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

/****************************************************************************
 * <b>Title</b>: TicketCommentVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value object holding a comment for a service order
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 15, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_ticket_comment")
public class TicketCommentVO extends BeanDataVO {
	
	public enum ActivityType {
		COMMENT("Ticket Comment"), EMAIL("Email Activity"), PHONE("Phone Call Activity"),
		MISC_ACTIVITY("Misc. Activity");
		
		private String typeName;
		private ActivityType(String typeName) { this.typeName = typeName; }
		public String getTypeName() { return typeName; }
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -5734280620575130296L;
	
	// Members
	private String ticketCommentId;
	private String ticketId;
	private String parentId;
	private String userId;
	private String comment;
	private String recipientName;
	private String activityType;
	private int priorityTicketFlag;
	private int endUserFlag;
	private int wslaReplyFlag;
	private Date createDate;
	
	// Bean Sub-elements
	private UserVO user;

	/**
	 * 
	 */
	public TicketCommentVO() {
		super();
	}

	/**
	 * @param req
	 */
	public TicketCommentVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public TicketCommentVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the ticketCommentId
	 */
	@Column(name="ticket_comment_id", isPrimaryKey=true)
	public String getTicketCommentId() {
		return ticketCommentId;
	}

	/**
	 * @return the ticketId
	 */
	@Column(name="ticket_id")
	public String getTicketId() {
		return ticketId;
	}

	/**
	 * @return the parentId
	 */
	@Column(name="parent_id")
	public String getParentId() {
		return parentId;
	}

	/**
	 * @return the userId
	 */
	@Column(name="user_id")
	public String getUserId() {
		return userId;
	}

	/**
	 * @return the comment
	 */
	@Column(name="comment_txt")
	public String getComment() {
		return comment;
	}

	/**
	 * @return the priorityTicketFlag
	 */
	@Column(name="priority_ticket_flg")
	public int getPriorityTicketFlag() {
		return priorityTicketFlag;
	}

	/**
	 * @return the recipientName
	 */
	@Column(name="recipient_nm")
	public String getRecipientName() {
		return recipientName;
	}

	/**
	 * @return the activityType
	 */
	@Column(name="activity_type_cd")
	public String getActivityType() {
		return activityType;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @return the user
	 */
	public UserVO getUser() {
		return user;
	}

	/**
	 * @param ticketCommentId the ticketCommentId to set
	 */
	public void setTicketCommentId(String ticketCommentId) {
		this.ticketCommentId = ticketCommentId;
	}

	/**
	 * @param ticketId the ticketId to set
	 */
	public void setTicketId(String ticketId) {
		this.ticketId = ticketId;
	}

	/**
	 * @param parentId the parentId to set
	 */
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * @param comment the comment to set
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	/**
	 * @param priorityTicketFlag the priorityTicketFlag to set
	 */
	public void setPriorityTicketFlag(int priorityTicketFlag) {
		this.priorityTicketFlag = priorityTicketFlag;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @param user the user to set
	 */
	@BeanSubElement
	public void setUser(UserVO user) {
		this.user = user;
	}

	/**
	 * @param recipientName the recipientName to set
	 */
	public void setRecipientName(String recipientName) {
		this.recipientName = recipientName;
	}

	/**
	 * @param activityType the activityType to set
	 */
	public void setActivityType(ActivityType activityType) {
		this.activityType = activityType.name();
	}
	
	/**
	 * @param activityType the activityType to set
	 */
	public void setActivityType(String activityType) {
		this.activityType = activityType;
	}

	/**
	 * @return the endUserFlag
	 */
	@Column(name="end_user_flg")
	public int getEndUserFlag() {
		return endUserFlag;
	}

	/**
	 * @return the wslaReplyFlag
	 */
	@Column(name="wsla_reply_flg")
	public int getWslaReplyFlag() {
		return wslaReplyFlag;
	}

	/**
	 * @param endUserFlag the endUserFlag to set
	 */
	public void setEndUserFlag(int endUserFlag) {
		this.endUserFlag = endUserFlag;
	}

	/**
	 * @param wslaReplyFlag the wslaReplyFlag to set
	 */
	public void setWslaReplyFlag(int wslaReplyFlag) {
		this.wslaReplyFlag = wslaReplyFlag;
	}

}

