package com.mts.subscriber.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs 3.5
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: SubscriptionUserVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Value Object for the subscription user for publications
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 19, 2019
 * @updates:
 ****************************************************************************/
@Table(name="mts_subscription_publication_xr")
public class SubscriptionUserVO extends BeanDataVO {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6995611353303042397L;
	
	// Members
	private String subscriptionPublicationId;
	private String publicationId;
	private String userId;
	private int trialFlag;
	private Date createDate;
	private Date expirationDate;
	
	// Helpers
	private String publicationName;

	/**
	 * 
	 */
	public SubscriptionUserVO() {
		super();
	}

	/**
	 * @param req
	 */
	public SubscriptionUserVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public SubscriptionUserVO(ResultSet rs) {
		super(rs);
	}

	
	public boolean isValid() {
		return !(StringUtil.isEmpty(userId) && StringUtil.isEmpty(publicationId));
	}
	
	/**
	 * @return the subscriptionPublicationId
	 */
	@Column(name="subscription_publication_id", isPrimaryKey=true)
	public String getSubscriptionPublicationId() {
		return subscriptionPublicationId;
	}

	/**
	 * @return the publicationId
	 */
	@Column(name="publication_id")
	public String getPublicationId() {
		return publicationId;
	}

	/**
	 * @return the userId
	 */
	@Column(name="user_id")
	public String getUserId() {
		return userId;
	}
	
	/**
	 * Expiration date for a given users publication
	 * @return
	 */
	@Column(name="expiration_dt")
	public Date getExpirationDate() {
		return expirationDate;
	}

	/**
	 * @return the publicationName
	 */
	@Column(name="publication_nm", isReadOnly=true)
	public String getPublicationName() {
		return publicationName;
	}

	/**
	 * @return the trialFlag
	 */
	@Column(name="trial_flg")
	public int getTrialFlag() {
		return trialFlag;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param subscriptionPublicationId the subscriptionPublicationId to set
	 */
	public void setSubscriptionPublicationId(String subscriptionPublicationId) {
		this.subscriptionPublicationId = subscriptionPublicationId;
	}

	/**
	 * @param publicationId the publicationId to set
	 */
	public void setPublicationId(String publicationId) {
		this.publicationId = publicationId;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * Expiration date for a given users publication
	 * @param expirationDate
	 */
	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}

	
	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @param publicationName the publicationName to set
	 */
	public void setPublicationName(String publicationName) {
		this.publicationName = publicationName;
	}

	/**
	 * @param trialFlag the trialFlag to set
	 */
	public void setTrialFlag(int trialFlag) {
		this.trialFlag = trialFlag;
	}

}
