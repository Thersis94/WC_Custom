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
	private Date createDate;

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
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

}
