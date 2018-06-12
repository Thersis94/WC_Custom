package com.rezdox.vo;

import java.io.Serializable;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/*****************************************************************************
<p><b>Title</b>: BusinessReviewVO.java</p>
<p><b>Description: </b>Value object that encapsulates a RezDox business review.</p>
<p> 
<p>Copyright: (c) 2018 SMT, All Rights Reserved</p>
<p>Company: Silicon Mountain Technologies</p>
@author Tim Johnson
@version 1.0
@since Mar 8, 2018
<b>Changes:</b> 
***************************************************************************/

@Table(name="REZDOX_MEMBER_BUSINESS_REVIEW")
public class BusinessReviewVO extends BeanDataVO implements Serializable {
	private static final long serialVersionUID = -5304473466685432141L;

	private String businessReviewId;
	private String parentId;
	private String groupId;
	private MemberVO member;
	private BusinessVO business;
	private int ratingNo;
	private String reviewText;
	private int moderatedFlag;
	private long replyCount;
	private Date createDate;
	private Date updateDate;

	private String createDateStr;

	public BusinessReviewVO() {
		super();
		member = new MemberVO();
		business = new BusinessVO();
	}
	
	/**
	 * @param req
	 */
	public BusinessReviewVO(ActionRequest req) {
		this();
		populateData(req);
	}

	/**
	 * @return the businessReviewId
	 */
	@Column(name="business_review_id", isPrimaryKey=true)
	public String getBusinessReviewId() {
		return businessReviewId;
	}

	/**
	 * @param businessReviewId the businessReviewId to set
	 */
	public void setBusinessReviewId(String businessReviewId) {
		this.businessReviewId = businessReviewId;
	}

	/**
	 * @return the parentId
	 */
	@Column(name="parent_id")
	public String getParentId() {
		return parentId;
	}

	/**
	 * @param parentId the parentId to set
	 */
	public void setParentId(String parentId) {
		this.parentId = StringUtil.checkVal(parentId, null);
	}

	/**
	 * @return the groupId
	 */
	@Column(name="group_id")
	public String getGroupId() {
		return groupId;
	}

	/**
	 * @param groupId the groupId to set
	 */
	public void setGroupId(String groupId) {
		this.groupId = StringUtil.checkVal(groupId, null);
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
	 * @return the businessId
	 */
	@Column(name="business_id")
	public String getBusinessId() {
		return business.getBusinessId();
	}

	/**
	 * @param businessId the businessId to set
	 */
	public void setBusinessId(String businessId) {
		business.setBusinessId(StringUtil.checkVal(businessId, null));
	}

	/**
	 * @return the business
	 */
	public BusinessVO getBusiness() {
		return business;
	}

	/**
	 * @param business the business to set
	 */
	@BeanSubElement
	public void setBusiness(BusinessVO business) {
		this.business = business;
	}

	/**
	 * @return the ratingNo
	 */
	@Column(name="rating_no")
	public int getRatingNo() {
		return ratingNo;
	}

	/**
	 * @param ratingNo the ratingNo to set
	 */
	public void setRatingNo(int ratingNo) {
		this.ratingNo = ratingNo;
	}

	/**
	 * @return the reviewText
	 */
	@Column(name="review_txt")
	public String getReviewText() {
		return reviewText;
	}

	/**
	 * @param reviewText the reviewText to set
	 */
	public void setReviewText(String reviewText) {
		this.reviewText = reviewText;
	}

	/**
	 * @return the moderatedFlag
	 */
	@Column(name="moderated_flg")
	public int getModeratedFlag() {
		return moderatedFlag;
	}

	/**
	 * @param moderatedFlag the moderatedFlag to set
	 */
	public void setModeratedFlag(int moderatedFlag) {
		this.moderatedFlag = moderatedFlag;
	}

	/**
	 * @return the replyCount
	 */
	@Column(name="reply_count", isReadOnly=true)
	public long getReplyCount() {
		return replyCount;
	}

	/**
	 * @param replyCount the replyCount to set
	 */
	public void setReplyCount(long replyCount) {
		this.replyCount = replyCount;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}
	
	public String getCreateDateStr() {
		return createDateStr;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
		this.createDateStr = Convert.formatDate(getCreateDate(), Convert.DATE_DASH_PATTERN);
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