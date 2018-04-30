package com.rezdox.vo;

import java.io.Serializable;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: ConnectionVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> A value object that will hold one connections worth of data
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Mar 26, 2018
 * @updates:
 ****************************************************************************/

@Table(name="REZDOX_CONNECTION")
public class ConnectionVO extends BeanDataVO implements Serializable {

	private static final long serialVersionUID = 2364416015206367281L;
	
	private String connectionId;
	private String senderMemberId;
	private String recipientMemberId;
	private String senderBusinessId;
	private String recipientBusinessId;
	private int approvedFlag;
	private Date createDate;
	
	public ConnectionVO() {
		super();
	}
	/**
	 * @param req
	 */
	public ConnectionVO(ActionRequest req) {
		this();
		populateData(req);
	}
	/**
	 * @return the connectionId
	 */
	@Column(name="connection_id", isPrimaryKey=true)
	public String getConnectionId() {
		return connectionId;
	}
	/**
	 * @return the senderMemberId
	 */
	@Column(name="sndr_member_id")
	public String getSenderMemberId() {
		return senderMemberId;
	}
	/**
	 * @return the recipientMemberId
	 */
	@Column(name="rcpt_member_id")
	public String getRecipientMemberId() {
		return recipientMemberId;
	}
	/**
	 * @return the senderBusinessId
	 */
	@Column(name="sndr_business_id")
	public String getSenderBusinessId() {
		return senderBusinessId;
	}
	/**
	 * @return the recipientBusinessId
	 */
	@Column(name="rcpt_business_id")
	public String getRecipientBusinessId() {
		return recipientBusinessId;
	}
	/**
	 * @return the approvedFlag
	 */
	@Column(name="approved_flg")
	public int getApprovedFlag() {
		return approvedFlag;
	}
	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}
	
	
	
	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}
	/**
	 * @param approvedFlag the approvedFlag to set
	 */
	public void setApprovedFlag(int approvedFlag) {
		this.approvedFlag = approvedFlag;
	}
	/**
	 * @param recipientBusinessId the recipientBusinessId to set
	 */
	public void setRecipientBusinessId(String recipientBusinessId) {
		this.recipientBusinessId = recipientBusinessId;
	}
	/**
	 * @param senderBusinessId the senderBusinessId to set
	 */
	public void setSenderBusinessId(String senderBusinessId) {
		this.senderBusinessId = senderBusinessId;
	}
	/**
	 * @param recipientMemberId the recipientMemberId to set
	 */
	public void setRecipientMemberId(String recipientMemberId) {
		this.recipientMemberId = recipientMemberId;
	}
	/**
	 * @param senderMemberId the senderMemberId to set
	 */
	public void setSenderMemberId(String senderMemberId) {
		this.senderMemberId = senderMemberId;
	}
	/**
	 * @param connectionId the connectionId to set
	 */
	public void setConnectionId(String connectionId) {
		this.connectionId = connectionId;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.data.parser.BeanDataVO#toString()
	 */
	@Override
	public String toString() {
		return StringUtil.getToString(this);
	}

}
