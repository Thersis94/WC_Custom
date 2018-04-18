package com.rezdox.vo;

import java.io.Serializable;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;

/*****************************************************************************
<p><b>Title</b>: InvitationVO.java</p>
<p><b>Description: </b>Value object that encapsulates a RezDox invitation.</p>
<p> 
<p>Copyright: (c) 2018 SMT, All Rights Reserved</p>
<p>Company: Silicon Mountain Technologies</p>
@author Tim Johnson
@version 1.0
@since Mar 28, 2018
<b>Changes:</b> 
***************************************************************************/

@Table(name="REZDOX_INVITATION")
public class InvitationVO extends BeanDataVO implements Serializable {
	private static final long serialVersionUID = -9082322712087270350L;

	private String invitationId;
	private MemberVO member;
	private String emailAddressText;
	private int statusFlag;
	private Date createDate;
	private Date updateDate;

	public enum Status {
		DELETED(0, "Deleted"),
		SENT(1, "Sent"),
		RESENT(2, "Re-Sent"),
		JOINED(3, "Joined");

		private int cd;
		private String label;

		private Status(int cd, String lbl) {
			this.cd = cd;
			this.label = lbl;
		}

		public int getCode() { return cd; }
		public String getLabel() { return label; }
	}

	public InvitationVO() {
		super();
		member = new MemberVO();
	}
	
	/**
	 * @param req
	 */
	public InvitationVO(ActionRequest req) {
		this();
		populateData(req);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return StringUtil.getToString(this);
	}
	
	/**
	 * @return the invitationId
	 */
	@Column(name="invitation_id", isPrimaryKey=true)
	public String getInvitationId() {
		return invitationId;
	}

	/**
	 * @param invitationId the invitationId to set
	 */
	public void setInvitationId(String invitationId) {
		this.invitationId = invitationId;
	}

	/**
	 * @return the memberId
	 */
	@Column(name="member_id")
	public String getMemberId() {
		return member.getMemberId();
	}

	/**
	 * @param memberId the memberId to set
	 */
	public void setMemberId(String memberId) {
		member.setMemberId(memberId);
	}

	/**
	 * @return the member
	 */
	public MemberVO getMember() {
		return member;
	}

	/**
	 * @param member the member to set
	 */
	@BeanSubElement
	public void setMember(MemberVO member) {
		this.member = member;
	}

	/**
	 * @return the emailAddressText
	 */
	@Column(name="email_address_txt")
	public String getEmailAddressText() {
		return emailAddressText;
	}

	/**
	 * @param emailAddressText the emailAddressText to set
	 */
	public void setEmailAddressText(String emailAddressText) {
		this.emailAddressText = emailAddressText;
	}

	/**
	 * @return the statusFlag
	 */
	@Column(name="status_flg")
	public int getStatusFlag() {
		return statusFlag;
	}

	/**
	 * @param statusFlag the statusFlag to set
	 */
	public void setStatusFlag(int statusFlag) {
		this.statusFlag = statusFlag;
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
	 * @return the updateDate
	 */
	@Column(name="update_dt", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}
}