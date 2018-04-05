package com.rezdox.vo;

//Java 8
import java.io.Serializable;
import java.util.Date;

// SMTBaseLibs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.user.HumanNameIntfc;

/*****************************************************************************
 <p><b>Title</b>: MemberVO.java</p>
 <p><b>Description: </b>Value object that encapsulates a RezDox user.</p>
 <p> 
 <p>Copyright: (c) 2018 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Tim Johnson
 @version 1.0
 @since Jan 18, 2018
 <b>Changes:</b> 
 ***************************************************************************/
@Table(name="REZDOX_MEMBER")
public class MemberVO extends UserDataVO implements HumanNameIntfc, Serializable {
	private static final long serialVersionUID = 6973805787915145277L;

	private String memberId;
	private String registerSubmittalId;
	private int statusFlg;
	private int privacyFlg;
	private String profilePicPath;
	private Date createDate;

	/**
	 * RezDox privacy flags.
	 */
	public enum Privacy {
		PUBLIC(0, "Public"),
		CONNECTIONS(1, "Connections Only"),
		PRIVATE(2, "Private");

		private int cd;
		private String label;

		private Privacy(int cd, String lbl) {
			this.cd = cd;
			this.label = lbl;
		}

		public int getCode() { return cd; }
		public String getLabel() { return label; }
	}

	public enum Status {
		ACTIVE(1, "Active"),
		INACTIVE(0, "Inactive");

		private int cd;
		private String label;

		private Status(int cd, String lbl) {
			this.cd = cd;
			this.label = lbl;
		}

		public int getCode() { return cd; }
		public String getLabel() { return label; }
	}

	public MemberVO() {
		super();
	}

	/**
	 * @param req
	 */
	public MemberVO(ActionRequest req) {
		this();
		setData(req);
	}

	/**
	 * Sets data from the request
	 * 
	 * @param req
	 */
	@Override
	public void setData(ActionRequest req) {
		super.setData(req);
		setMemberId(req.getParameter("memberId"));
		setRegisterSubmittalId(req.getParameter("registerSubmittalId"));
		setStatusFlg(Convert.formatInteger(req.getParameter("statusFlg")));
		setPrivacyFlg(Convert.formatInteger(req.getParameter("privacyFlg")));
		setProfilePicPath(req.getParameter("profilePicPath"));
	}

	/**
	 * @return the memberId
	 */
	@Column(name="member_id", isPrimaryKey=true)
	public String getMemberId() {
		return memberId;
	}

	/**
	 * @param memberId the memberId to set
	 */
	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}

	/**
	 * @return the registerSubmittalId
	 */
	@Column(name="register_submittal_id")
	public String getRegisterSubmittalId() {
		return registerSubmittalId;
	}

	/**
	 * @param registerSubmittalId the registerSubmittalId to set
	 */
	public void setRegisterSubmittalId(String registerSubmittalId) {
		this.registerSubmittalId = registerSubmittalId;
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
	@Override
	@Column(name="update_dt", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	@Override
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	@Column(name="status_flg")
	public int getStatusFlg() {
		return statusFlg;
	}

	public void setStatusFlg(int status) {
		this.statusFlg = status;
	}

	public String getStatusName() {
		for (Status s : Status.values()) {
			if (s.getCode() == getStatusFlg())
				return s.getLabel();
		}
		return "";
	}

	@Column(name="privacy_flg")
	public int getPrivacyFlg() {
		return privacyFlg;
	}

	public void setPrivacyFlg(int privacyFlg) {
		this.privacyFlg = privacyFlg;
	}

	public String getPrivacyName() {
		for (Privacy p : Privacy.values()) {
			if (p.getCode() == getPrivacyFlg())
				return p.getLabel();
		}
		return "";
	}

	/**
	 * @return the profilePicPath
	 */
	@Column(name="profile_pic_pth")
	public String getProfilePicPath() {
		return profilePicPath;
	}

	/**
	 * @param profilePicPath the profilePicPath to set
	 */
	public void setProfilePicPath(String profilePicPath) {
		this.profilePicPath = profilePicPath;
	}

	/**
	 * Override for db processor to add profile_id to the member record
	 */
	@Override
	@Column(name="profile_id")
	public String getProfileId() {
		return super.getProfileId();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.UserDataVO#getFirstName()
	 */
	@Override
	@Column(name="first_nm")
	public String getFirstName() {
		return super.getFirstName();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.UserDataVO#getLastName()
	 */
	@Override
	@Column(name="last_nm")
	public String getLastName() {
		return super.getLastName();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.UserDataVO#getEmailAddress()
	 */
	@Override
	@Column(name="email_address_txt")
	public String getEmailAddress() {
		return super.getEmailAddress();
	}


	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		if (!super.equals(o)) return false;
		MemberVO vo = (MemberVO)o;
		return StringUtil.checkVal(vo.getMemberId()).equals(getMemberId());
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return super.hashCode() + StringUtil.checkVal(getMemberId()).hashCode();
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return StringUtil.getToString(this);
	}
}
