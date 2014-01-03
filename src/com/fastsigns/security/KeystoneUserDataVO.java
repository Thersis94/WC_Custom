package com.fastsigns.security;

import com.siliconmtn.security.UserDataVO;



/****************************************************************************
 * <b>Title</b>: KeystoneUserDataVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Feb 21, 2013
 ****************************************************************************/
public class KeystoneUserDataVO extends UserDataVO {

	private static final long serialVersionUID = -4612573217872694536L;
	
	private String accountId = null;
	private String webId = null;  //this is our correlation to the Franchise object.
	private boolean allowPurchaseOrders = false;

	public KeystoneUserDataVO() {
	}
	
	public String getAccountId() {
		return accountId;
	}
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}
	public String getUserLoginId() {
		return super.getAuthenticationId();
	}
	public void setUserLoginId(String userLoginId) {
		super.setAuthenticationId(userLoginId);
	}
	public String getUserId() {
		return super.getProfileId();
	}
	public void setUserId(String userId) {
		super.setProfileId(userId);
	}
	public String getWebId() {
		return webId;
	}
	public void setWebId(String webId) {
		this.webId = webId;
	}

	public boolean isAllowPurchaseOrders() {
		return allowPurchaseOrders;
	}

	public void setAllowPurchaseOrders(boolean allowPurchaseOrders) {
		this.allowPurchaseOrders = allowPurchaseOrders;
	}
	

}
