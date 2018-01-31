package com.rezdox.vo;

//Java 8
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

// SMTBaseLibs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;

/*****************************************************************************
 <p><b>Title</b>: PromotionVO.java</p>
 <p><b>Description: </b>Value object that encapsulates a RezDox membership promotion.</p>
 <p> 
 <p>Copyright: (c) 2018 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Tim Johnson
 @version 1.0
 @since Jan 26, 2018
 <b>Changes:</b> 
 ***************************************************************************/

@Table(name="REZDOX_PROMOTION")
public class PromotionVO implements Serializable  {
	private static final long serialVersionUID = 4652720888234349056L;

	private String promotionId;
	private String promotionName;
	private String promotionCode;
	private String descriptionText;
	private String termsText;
	private double discountPctNo;
	private int statusFlag;
	private Date startDate;
	private Date expireDate;
	private Date createDate;
	private Date updateDate;
	
	// XR Data
	private Map<String, MembershipVO> memberships;
	
	public PromotionVO() {
		memberships = new HashMap<>();
	}

	/**
	 * @param req
	 */
	public PromotionVO(ActionRequest req) {
		this();
		setData(req);
	}
	
	/**
	 * Sets data from the request
	 * 
	 * @param req
	 */
	public void setData(ActionRequest req) {
		setPromotionId(req.getParameter("promotionId"));
		setPromotionName(req.getParameter("promotionName"));
		setPromotionCode(req.getParameter("promotionCode"));
		setDescriptionText(req.getParameter("descriptionText"));
		setTermsText(req.getParameter("termsText"));
		setDiscountPctNo(Convert.formatDouble(req.getParameter("discountPctNo")));
		setStatusFlag(Convert.formatInteger(req.getParameter("statusFlag")));
		setStartDate(Convert.parseDateUnknownPattern(req.getParameter("startDate")));
		setExpireDate(Convert.parseDateUnknownPattern(req.getParameter("expireDate")));
	}

	/**
	 * @return the promotionId
	 */
	@Column(name="promotion_id", isPrimaryKey=true)
	public String getPromotionId() {
		return promotionId;
	}

	/**
	 * @param promotionId the promotionId to set
	 */
	public void setPromotionId(String promotionId) {
		this.promotionId = promotionId;
	}

	/**
	 * @return the promotionName
	 */
	@Column(name="promotion_nm")
	public String getPromotionName() {
		return promotionName;
	}

	/**
	 * @param promotionName the promotionName to set
	 */
	public void setPromotionName(String promotionName) {
		this.promotionName = promotionName;
	}

	/**
	 * @return the promotionCode
	 */
	@Column(name="promotion_cd")
	public String getPromotionCode() {
		return promotionCode;
	}

	/**
	 * @param promotionCode the promotionCode to set
	 */
	public void setPromotionCode(String promotionCode) {
		this.promotionCode = promotionCode;
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
	 * @return the termsText
	 */
	@Column(name="terms_txt")
	public String getTermsText() {
		return termsText;
	}

	/**
	 * @param termsText the termsText to set
	 */
	public void setTermsText(String termsText) {
		this.termsText = termsText;
	}

	/**
	 * @return the discountPctNo
	 */
	@Column(name="discount_pct_no")
	public double getDiscountPctNo() {
		return discountPctNo;
	}

	/**
	 * @param discountPctNo the discountPctNo to set
	 */
	public void setDiscountPctNo(double discountPctNo) {
		this.discountPctNo = discountPctNo;
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
	 * @return the startDate
	 */
	@Column(name="start_dt")
	public Date getStartDate() {
		return startDate;
	}

	/**
	 * @param startDate the startDate to set
	 */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/**
	 * @return the expireDate
	 */
	@Column(name="expire_dt")
	public Date getExpireDate() {
		return expireDate;
	}

	/**
	 * @param expireDate the expireDate to set
	 */
	public void setExpireDate(Date expireDate) {
		this.expireDate = expireDate;
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

	/**
	 * @return the memberships
	 */
	public Map<String, MembershipVO> getMemberships() {
		return memberships;
	}

	/**
	 * @param memberships the memberships to set
	 */
	public void setMemberships(Map<String, MembershipVO> memberships) {
		this.memberships = memberships;
	}

	/**
	 * @param membership the membership to add
	 */
	@BeanSubElement
	public void addMembership(MembershipVO membership) {
		memberships.put(membership.getMembershipId(), membership);
	}
}
