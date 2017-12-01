package com.perkville;

import com.google.api.client.auth.oauth2.Credential;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title:</b> PerkvilleAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Base Perkville Action that defines basic interaction
 * methods for the API. 
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Dec 1, 2017
 ****************************************************************************/
public class PerkvilleAction extends SimpleActionAdapter {

	/**
	 * 
	 */
	public PerkvilleAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public PerkvilleAction(ActionInitVO init) {
		super(init);
	}

	/**
	 * Set the User AccessToken on the session.
	 * @param req
	 */
	protected void setUserAuth(ActionRequest req, Credential c) {
		//Place the AccessToken in users session.
		req.getSession().setAttribute(PerkvilleApi.ACCESS_TOKEN, c.getAccessToken());
	}
}