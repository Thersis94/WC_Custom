package com.rezdox.vo;

//Java 8
import java.io.Serializable;
import java.util.Date;

// SMTBaseLibs
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/*****************************************************************************
 <p><b>Title</b>: PromotionXRVO.java</p>
 <p><b>Description: </b>Value object that encapsulates RezDox membership promotion xr.</p>
 <p> 
 <p>Copyright: (c) 2018 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Tim Johnson
 @version 1.0
 @since Jan 30, 2018
 <b>Changes:</b> 
 ***************************************************************************/

@Table(name="REZDOX_MEMBERSHIP_PROMOTION")
public class PromotionXRVO implements Serializable  {
	private static final long serialVersionUID = 4102928230731183489L;

	private String membershipPromotionId;
	private String promotionId;
	private String membershipId;
	private Date createDate;

	public PromotionXRVO() {}
	
	/**
	 * Sets the required fields
	 * 
	 * @param promotionId
	 * @param membershipId
	 */
	public PromotionXRVO(String promotionId, String membershipId) {
		this.promotionId = promotionId;
		this.membershipId = membershipId;
	}

	/**
	 * @return the membershipPromotionId
	 */
	@Column(name="membership_promotion_id", isPrimaryKey=true)
	public String getMembershipPromotionId() {
		return membershipPromotionId;
	}

	/**
	 * @param membershipPromotionId the membershipPromotionId to set
	 */
	public void setMembershipPromotionId(String membershipPromotionId) {
		this.membershipPromotionId = membershipPromotionId;
	}

	/**
	 * @return the promotionId
	 */
	@Column(name="promotion_id")
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
	 * @return the membershipId
	 */
	@Column(name="membership_id")
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

}
