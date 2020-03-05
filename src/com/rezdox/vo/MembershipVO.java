package com.rezdox.vo;

//Java 8
import java.io.Serializable;
import java.util.Date;

// SMTBaseLibs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;

/*****************************************************************************
 <p><b>Title</b>: MembershipVO.java</p>
 <p><b>Description: </b>Value object that encapsulates a RezDox membership.</p>
 <p> 
 <p>Copyright: (c) 2018 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Tim Johnson
 @version 1.0
 @since Jan 26, 2018
 <b>Changes:</b> 
 ***************************************************************************/
@Table(name="REZDOX_MEMBERSHIP")
public class MembershipVO implements Serializable  {
	private static final long serialVersionUID = -2721552856523040208L;

	private String membershipId;
	private String membershipName;
	private String descriptionText;
	private Group group;
	private int statusFlag;
	private double costNo;
	private int quantityNo;
	private int orderNo;
	private int newMemberDefaultFlag;
	private String paypalButtonId;
	private Date createDate;
	private Date updateDate;

	public enum Group {
		HO("Residence"), BU("Business"), ALL("All");

		private String label;
		
		private Group(String lbl) {
			this.label = lbl;
		}
		
		public String getLabel() { return label; }
	}

	public MembershipVO() {
		super();
	}

	/**
	 * @param req
	 */
	public MembershipVO(ActionRequest req) {
		this();
		setData(req);
	}
	
	/**
	 * Sets data from the request
	 * 
	 * @param req
	 */
	public void setData(ActionRequest req) {
		setMembershipId(req.getParameter("membershipId"));
		setMembershipName(req.getParameter("membershipName"));
		setDescriptionText(req.getParameter("descriptionText"));
		setGroupCode(req.getParameter("groupCode"));
		setStatusFlag(Convert.formatInteger(req.getParameter("statusFlag")));
		setCostNo(Convert.formatDouble(req.getParameter("costNo")));
		setQuantityNo(Convert.formatInteger(req.getParameter("quantityNo")));
		setOrderNo(Convert.formatInteger(req.getParameter("orderNo")));
		setNewMemberDefaultFlag(Convert.formatInteger(req.getParameter("newMemberDefaultFlag")));
		setPaypalButtonId(req.getParameter("paypalButtonId"));
	}

	/**
	 * @return the membershipId
	 */
	@Column(name="membership_id", isPrimaryKey=true)
	public String getMembershipId() {
		return membershipId;
	}

	/**
	 * @param membershipId the membershipId to set
	 */
	public void setMembershipId(String membershipId) {
		this.membershipId = membershipId;
	}

	/**
	 * @return the membershipName
	 */
	@Column(name="membership_nm")
	public String getMembershipName() {
		return membershipName;
	}

	/**
	 * @param membershipName the membershipName to set
	 */
	public void setMembershipName(String membershipName) {
		this.membershipName = membershipName;
	}

	/**
	 * @return the descriptionText
	 */
	@Column(name="description_txt")
	public String getDescriptionText() {
		return descriptionText;
	}

	/**
	 * @param descriptionText the descriptionText to set
	 */
	public void setDescriptionText(String descriptionText) {
		this.descriptionText = descriptionText;
	}

	/**
	 * @return the group
	 */
	public Group getGroup() {
		return group;
	}

	/**
	 * @param group the group to set
	 */
	public void setGroup(Group group) {
		this.group = group;
	}

	/**
	 * @return the group code
	 */
	@Column(name="group_cd")
	public String getGroupCode() {
		return group == null ? null : group.name();
	}

	/**
	 * @param groupCode the group code to set
	 */
	public void setGroupCode(String groupCode) {
		setGroup(Group.valueOf(groupCode));
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
	 * @return the costNo
	 */
	@Column(name="cost_no")
	public double getCostNo() {
		return costNo;
	}

	/**
	 * @param costNo the costNo to set
	 */
	public void setCostNo(double costNo) {
		this.costNo = costNo;
	}

	/**
	 * @return the quantityNo
	 */
	@Column(name="qty_no")
	public int getQuantityNo() {
		return quantityNo;
	}

	/**
	 * @param quantityNo the quantityNo to set
	 */
	public void setQuantityNo(int quantityNo) {
		this.quantityNo = quantityNo;
	}

	/**
	 * @return the newMemberDefaultFlag
	 */
	@Column(name="new_mbr_dflt_flg")
	public int getNewMemberDefaultFlag() {
		return newMemberDefaultFlag;
	}

	/**
	 * @param newMemberDefaultFlag the newMemberDefaultFlag to set
	 */
	public void setNewMemberDefaultFlag(int newMemberDefaultFlag) {
		this.newMemberDefaultFlag = newMemberDefaultFlag;
	}

	/**
	 * @return the paypalButtonId
	 */
	@Column(name="paypal_button_id")
	public String getPaypalButtonId() {
		return paypalButtonId;
	}

	/**
	 * @param paypalButtonId the paypalButtonId to set
	 */
	public void setPaypalButtonId(String paypalButtonId) {
		this.paypalButtonId = paypalButtonId;
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

	@Column(name="order_no")
	public int getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}
}
