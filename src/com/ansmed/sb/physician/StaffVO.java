package com.ansmed.sb.physician;

import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.AbstractSiteBuilderVO;

/*****************************************************************************
 <p><b>Title</b>: StaffVO.java</p>
 <p>Description: <b/></p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 1.0
 @since Sep 22, 2007
 Last Updated:
 ***************************************************************************/

public class StaffVO extends AbstractSiteBuilderVO {
	private static final long serialVersionUID = 1l;
	private Integer staffTypeId = null;
	private String staffTypeName = null;
	private String surgeonId = null;
	private String memberName = null;
	private String emailAddress = null;
	private String phoneNumber = null;
	private String comments = null;
	
	/**
	 * 
	 */
	public StaffVO() {
		
	}
	
	/**
	 * Initializes the VO to the params provided in the row object
	 * @param rs
	 */
	public StaffVO(ResultSet rs) {
		super();
		setData(rs);
	}
	
	/**
	 * Initializes the VO to the params provided in the request object
	 * @param rs
	 */
	public StaffVO(SMTServletRequest req) {
		super();
		setData(req);
	}
	
	/**
	 * Sets the VO to the params provided in the row object
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		actionId = db.getStringVal("staff_id", rs);
		staffTypeName = db.getStringVal("type_nm", rs);
		surgeonId = db.getStringVal("surgeon_id", rs);
		memberName = db.getStringVal("member_nm", rs);
		emailAddress = db.getStringVal("email_txt", rs);
		phoneNumber = db.getStringVal("phone_no", rs);
		comments = db.getStringVal("comments_txt", rs);
		staffTypeId = db.getIntegerVal("staff_type_id", rs);
	}
	
	/**
	 * Sets the VO to the params provided in the request object
	 * @param req
	 */
	public void setData(SMTServletRequest req) {
		StringEncoder se = new StringEncoder();
		actionId = req.getParameter("staffId");
		staffTypeName = req.getParameter("staffTypeName");
		surgeonId = req.getParameter("surgeonId");
		memberName = se.decodeValue(req.getParameter("memberName"));
		emailAddress = req.getParameter("emailAddress");
		phoneNumber = req.getParameter("phoneNumber");
		comments = se.decodeValue(req.getParameter("comments"));
		staffTypeId = Convert.formatInteger(req.getParameter("staffTypeId"));
	}

	/**
	 * @return the staffTypeId
	 */
	public Integer getStaffTypeId() {
		return staffTypeId;
	}

	/**
	 * @param staffTypeId the staffTypeId to set
	 */
	public void setStaffTypeId(Integer staffTypeId) {
		this.staffTypeId = staffTypeId;
	}

	/**
	 * @return the staffTypeName
	 */
	public String getStaffTypeName() {
		return staffTypeName;
	}

	/**
	 * @param staffTypeName the staffTypeName to set
	 */
	public void setStaffTypeName(String staffTypeName) {
		this.staffTypeName = staffTypeName;
	}

	/**
	 * @return the surgeonId
	 */
	public String getSurgeonId() {
		return surgeonId;
	}

	/**
	 * @param surgeonId the surgeonId to set
	 */
	public void setSurgeonId(String surgeonId) {
		this.surgeonId = surgeonId;
	}

	/**
	 * @return the memberName
	 */
	public String getMemberName() {
		return memberName;
	}

	/**
	 * @param memberName the memberName to set
	 */
	public void setMemberName(String memberName) {
		this.memberName = memberName;
	}

	/**
	 * @return the emailAddress
	 */
	public String getEmailAddress() {
		return emailAddress;
	}

	/**
	 * @param emailAddress the emailAddress to set
	 */
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	/**
	 * @return the phoneNumber
	 */
	public String getPhoneNumber() {
		return phoneNumber;
	}

	/**
	 * @param phoneNumber the phoneNumber to set
	 */
	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	/**
	 * @return the comments
	 */
	public String getComments() {
		return comments;
	}

	/**
	 * @param comments the comments to set
	 */
	public void setComments(String comments) {
		this.comments = comments;
	}

}
