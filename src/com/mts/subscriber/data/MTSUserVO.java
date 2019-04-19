package com.mts.subscriber.data;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.user.UserVO;

/****************************************************************************
 * <b>Title</b>: MTSUserVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the MTS User Data.  
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 2, 2019
 * @updates:
 ****************************************************************************/
@Table(name="mts_user")
public class MTSUserVO extends UserVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8260500460160598111L;
	
	// Members
	private String cv;
	private String imagePath;
	private String title;
	private String companyName;
	private String secondaryUserId;
	private String twitterName;
	private String linkedinName;
	private String facebookName;
	private int activeFlag;
	private int yearsExperience;
	
	// Sub Beans
	private List<SubscriptionUserVO> subscriptions = new ArrayList<>();;	
	
	/**
	 * 
	 */
	public MTSUserVO() {
		super();
	}

	/**
	 * @param req
	 */
	public MTSUserVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public MTSUserVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * Gets the user name concatenated
	 * @return
	 */
	public String getFullName() {
		return StringUtil.checkVal(getFirstName()) + " " + StringUtil.checkVal(getLastName());
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.user.UserVO#getUserId()
	 */
	@Column(name="user_id", isPrimaryKey=true)
	@Override
	public String getUserId() {
		return super.getUserId();
	}
	
	/**
	 * @return the cv
	 */
	@Column(name="cv_desc")
	public String getCv() {
		return cv;
	}

	/**
	 * @return the imagePath
	 */
	@Column(name="img_path")
	public String getImagePath() {
		return imagePath;
	}

	/**
	 * @return the title
	 */
	@Column(name="pro_title_nm")
	public String getTitle() {
		return title;
	}

	/**
	 * @return the companyName
	 */
	@Column(name="company_nm")
	public String getCompanyName() {
		return companyName;
	}

	/**
	 * @return the activeFlag
	 */
	@Column(name="active_flg")
	public int getActiveFlag() {
		return activeFlag;
	}

	/**
	 * @return the secondaryUserId
	 */
	@Column(name="sec_user_id")
	public String getSecondaryUserId() {
		return secondaryUserId;
	}

	/**
	 * @return the twitterName
	 */
	@Column(name="twitter_txt")
	public String getTwitterName() {
		return twitterName;
	}

	/**
	 * @return the linkedinName
	 */
	@Column(name="linkedin_txt")
	public String getLinkedinName() {
		return linkedinName;
	}

	/**
	 * @return the facebookName
	 */
	@Column(name="facebook_txt")
	public String getFacebookName() {
		return facebookName;
	}

	/**
	 * @return the yearsExperience
	 */
	@Column(name="yrs_experience_no")
	public int getYearsExperience() {
		return yearsExperience;
	}

	/**
	 * @param cv the cv to set
	 */
	public void setCv(String cv) {
		this.cv = cv;
	}

	/**
	 * @param imagePath the imagePath to set
	 */
	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @param companyName the companyName to set
	 */
	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	/**
	 * @param activeFlag the activeFlag to set
	 */
	public void setActiveFlag(int activeFlag) {
		this.activeFlag = activeFlag;
	}

	/**
	 * @param secondaryUserId the secondaryUserId to set
	 */
	public void setSecondaryUserId(String secondaryUserId) {
		this.secondaryUserId = secondaryUserId;
	}

	/**
	 * @param twitterName the twitterName to set
	 */
	public void setTwitterName(String twitterName) {
		this.twitterName = twitterName;
	}

	/**
	 * @param linkedinName the linkedinName to set
	 */
	public void setLinkedinName(String linkedinName) {
		this.linkedinName = linkedinName;
	}

	/**
	 * @param facebookName the facebookName to set
	 */
	public void setFacebookName(String facebookName) {
		this.facebookName = facebookName;
	}

	/**
	 * @param yearsExperience the yearsExperience to set
	 */
	public void setYearsExperience(int yearsExperience) {
		this.yearsExperience = yearsExperience;
	}

	/**
	 * @return the subscriptions
	 */
	public List<SubscriptionUserVO> getSubscriptions() {
		return subscriptions;
	}

	/**
	 * @param subscriptions the subscriptions to set
	 */
	public void setSubscriptions(List<SubscriptionUserVO> subscriptions) {
		this.subscriptions = subscriptions;
	}

	/**
	 * 
	 * @param subscription
	 */
	@BeanSubElement
	public void addSubscription(SubscriptionUserVO subscription) {
		if (subscription.isValid())
			this.subscriptions.add(subscription);
	}
}

