package com.rezdox.vo;

//Java 8
import java.io.Serializable;
import java.util.Date;

// SMTBaseLibs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;

/*****************************************************************************
 <p><b>Title</b>: SubscriptionVO.java</p>
 <p><b>Description: </b>Value object that encapsulates a RezDox member's subscription.</p>
 <p> 
 <p>Copyright: (c) 2018 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Tim Johnson
 @version 1.0
 @since Jan 26, 2018
 <b>Changes:</b> 
 ***************************************************************************/

@Table(name="REZDOX_SUBSCRIPTION")
public class SubscriptionVO implements Serializable {
	private static final long serialVersionUID = -4531854612880049798L;

	private String subscriptionId;
	private PaymentTypeVO paymentType;
	private MemberVO member;
	private MembershipVO membership;
	private PromotionVO promotion;
	private double costNo;
	private double discountNo;
	private int quantityNo;
	private String transactionCd;
	private Date createDate;
	private Date updateDate;

	public SubscriptionVO() {
		setPaymentType(new PaymentTypeVO());
		setMember(new MemberVO());
		setMembership(new MembershipVO());
		setPromotion(new PromotionVO());
	}

	public SubscriptionVO(ActionRequest req) {
		setSubscriptionId(req.getParameter("subscriptionId"));
		setPaymentType(new PaymentTypeVO(req));
		setMember(new MemberVO(req));
		setMembership(new MembershipVO(req));
		setPromotion(new PromotionVO(req));
		setCostNo(Convert.formatDouble(req.getParameter("costNo")));
		setDiscountNo(Convert.formatDouble(req.getParameter("discountNo")));
		setTransactionCd(req.getParameter("transactionCd"));
	}
	
	/**
	 * @return the subscriptionId
	 */
	@Column(name="subscription_id", isPrimaryKey=true)
	public String getSubscriptionId() {
		return subscriptionId;
	}

	/**
	 * @param subscriptionId the subscriptionId to set
	 */
	public void setSubscriptionId(String subscriptionId) {
		this.subscriptionId = subscriptionId;
	}

	/**
	 * @return the paymentType
	 */
	public PaymentTypeVO getPaymentType() {
		return paymentType;
	}

	/**
	 * @param paymentType the paymentType to set
	 */
	@BeanSubElement
	public void setPaymentType(PaymentTypeVO paymentType) {
		this.paymentType = paymentType;
	}

	@Column(name="payment_type_id")
	public String paymentTypeId() {
		return paymentType.getPaymentTypeId();
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

	@Column(name="member_id")
	public String getMemberId() {
		return member.getMemberId();
	}

	/**
	 * @return the membership
	 */
	public MembershipVO getMembership() {
		return membership;
	}

	/**
	 * @param membership the membership to set
	 */
	@BeanSubElement
	public void setMembership(MembershipVO membership) {
		this.membership = membership;
	}

	@Column(name="membership_id")
	public String getMembershipId() {
		return membership.getMembershipId();
	}

	/**
	 * @return the promotion
	 */
	public PromotionVO getPromotion() {
		return promotion;
	}

	/**
	 * @param promotion the promotion to set
	 */
	@BeanSubElement
	public void setPromotion(PromotionVO promotion) {
		this.promotion = promotion;
	}

	@Column(name="promotion_id")
	public String getPromotionId() {
		return promotion.getPromotionId();
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
	 * @return the discountNo
	 */
	@Column(name="discount_no")
	public double getDiscountNo() {
		return discountNo;
	}

	/**
	 * @param discountNo the discountNo to set
	 */
	public void setDiscountNo(double discountNo) {
		this.discountNo = discountNo;
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
	 * @return the transactionCd
	 */
	@Column(name="transaction_cd")
	public String getTransactionCd() {
		return transactionCd;
	}

	/**
	 * @param transactionCd the transactionCd to set
	 */
	public void setTransactionCd(String transactionCd) {
		this.transactionCd = transactionCd;
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
