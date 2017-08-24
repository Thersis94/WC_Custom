package com.biomed.smarttrak.admin;

import com.biomed.smarttrak.action.FeaturedInsightAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: FeaturedInsightsAction.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> Creates an instance of the public featured insights action
 * from information set on the AdminControllerAction in order to return a preview
 * of what the homepage of the public side looks like.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @since Aug 9, 2017<p/>
 * @updates:
 ****************************************************************************/

public class FeaturedInsightsAction extends SBActionAdapter {

	public FeaturedInsightsAction() {
		super();
	}

	public FeaturedInsightsAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		// Pass along the id of the solr search widget used for featured insights
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		mod.setAttribute(ModuleVO.ATTRIBUTE_1, mod.getActionUrl());
		
		ActionInterface ai = new FeaturedInsightAction();
		ai.setActionInit(actionInit);
		ai.setAttributes(attributes);
		ai.setDBConnection(dbConn);
		ai.retrieve(req);
	}
}
