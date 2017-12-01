package com.perkville;

import com.google.api.client.auth.oauth2.Credential;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> PerkvilleRewardsAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages retrieving Perkville Perks.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Dec 1, 2017
 ****************************************************************************/
public class PerkvilleRewardsAction extends PerkvilleAction {

	/**
	 * 
	 */
	public PerkvilleRewardsAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public PerkvilleRewardsAction(ActionInitVO init) {
		super(init);
	}

	/**
	 * Load Perkville Perks.
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		SiteVO site = ((SiteVO)req.getAttribute(Constants.SITE_DATA));
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);

		PerkvilleApi api = new PerkvilleApi(dbConn, site, user, null);
		Credential c = api.getToken().getToken();

		//If user has a token, load perks. 
		if(c != null) {

			//Ensure user Auth is set correctly.
			setUserAuth(req, c);

			//Get Perkville Perks
			putModuleData(api.getPerks());
		}
	}
}