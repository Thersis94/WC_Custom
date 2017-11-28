package com.mindbody.action;

import com.mindbody.util.ClientApiUtil;
import com.mindbody.util.ClientApiUtil.MBUserStatus;
import com.mindbody.vo.MindBodyResponseVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> MindBodyRegistrationPostProcessorAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Registration Post Processor.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 17, 2017
 ****************************************************************************/
public class MindBodyRegistrationPostProcessorAction extends SBActionAdapter {

	/**
	 * 
	 */
	public MindBodyRegistrationPostProcessorAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public MindBodyRegistrationPostProcessorAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	@Override
	public void build(ActionRequest req) throws ActionException {

		//Get New/Updated user VO.
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		ClientApiUtil util = new ClientApiUtil(site.getSiteConfig());

		//Check for User in MindBody System.
		MBUserStatus stat = util.getClientStatus(user);

		if(!MBUserStatus.INVALID_CREDENTIALS.equals(stat)) {
			MindBodyResponseVO resp = util.addOrUpdateClients(user, null);
			if(!resp.isValid()) {
				log.error("Error Creating MindBody Record: " + resp.getMessage());
			}
		}
	}
}