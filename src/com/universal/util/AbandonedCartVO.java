package com.universal.util;

import java.util.Date;

import com.siliconmtn.commerce.ShoppingCartVO;

/****************************************************************************
 * <b>Title: </b>AbandonedCartVO.java <p/>
 * <b>Project: </b>WC_Custom <p/>
 * <b>Description: </b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2014<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0<p/>
 * @since Sep 3, 2014<p/>
 *<b>Changes: </b>
 * Sep 3, 2014: David Bargerhuff: Created class.
 ****************************************************************************/
public class AbandonedCartVO {
	
	private ShoppingCartVO cart = null;
	private String objectId = null;
	private String profileId = null;
	private String sourceId = null;
	private Date createDate = null;
	
	/**
	 * 
	 */
	public AbandonedCartVO() {
	}

	/**
	 * @return the cart
	 */
	public ShoppingCartVO getCart() {
		return cart;
	}

	/**
	 * @param cart the cart to set
	 */
	public void setCart(ShoppingCartVO cart) {
		this.cart = cart;
	}

	/**
	 * @return the objectId
	 */
	public String getObjectId() {
		return objectId;
	}

	/**
	 * @param objectId the objectId to set
	 */
	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}

	/**
	 * @return the profileId
	 */
	public String getProfileId() {
		return profileId;
	}

	/**
	 * @param profileId the profileId to set
	 */
	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	/**
	 * @return the sourceId
	 */
	public String getSourceId() {
		return sourceId;
	}

	/**
	 * @param sourceId the sourceId to set
	 */
	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * @return the createDate
	 */
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
