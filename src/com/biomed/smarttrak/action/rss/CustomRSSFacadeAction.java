package com.biomed.smarttrak.action.rss;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.rss.SyncScheduleAssocAction;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> CustomRSSFacadeAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Action manages Admin calls for Smarttrak Custom RSS Data. 
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 1.0
 * @since May 9, 2017
 ****************************************************************************/
public class CustomRSSFacadeAction extends SBActionAdapter {

	public enum FeedType {FEED, SYNC, GROUP, TYPE, SEGMENT, TERMS, FILTER, CONSOLE, NEWSROOM}
	public static final String FACADE_TYPE = "facadeType";

	public CustomRSSFacadeAction() {
		super();
	}

	public CustomRSSFacadeAction(ActionInitVO init) {
		super(init);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {

		if(req.hasParameter(FACADE_TYPE)) {
			// Determine the request type and forward to the appropriate action
			getAction(FeedType.valueOf(req.getParameter(FACADE_TYPE))).delete(req);
			buildFeedsRedirect(req);
		}
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		if(req.hasParameter(FACADE_TYPE)) {

			// Determine the request type and forward to the appropriate action
			getAction(FeedType.valueOf(req.getParameter(FACADE_TYPE))).build(req);

			buildFeedsRedirect(req);
		}
	}


	/**
	 * Helper method that builds the Feeds Admin Redirect.
	 * @param req
	 */
	private void buildFeedsRedirect(ActionRequest req) {
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		StringBuilder url = new StringBuilder(200);
		url.append(page.getFullPath());
		url.append("?actionType=").append(req.getParameter(AdminControllerAction.ACTION_TYPE));
		url.append("&facadeType=").append(req.getParameter(FACADE_TYPE));
		if(req.hasParameter("isAdmin")) {
			url.append("&isAdmin=true");
		}
		if(req.hasParameter("isConsole")) {
			url.append("&isConsole=true");
		}

		req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if(req.hasParameter(FACADE_TYPE)) {
			// Determine the request type and forward to the appropriate action
			ActionInterface ai = getAction(FeedType.valueOf(req.getParameter(FACADE_TYPE)));
			if(Convert.formatBoolean(req.getParameter("isAdmin")) && !FeedType.CONSOLE.name().equals(req.getParameter(FACADE_TYPE))) {
				ai.list(req);
			} else {
				ai.retrieve(req);
			}
		}
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		if(req.hasParameter(FACADE_TYPE)) {
			// Determine the request type and forward to the appropriate action
			getAction(FeedType.valueOf(req.getParameter(FACADE_TYPE))).list(req);
		}
	}

	/**
	 * Helper method that manages building an Action and populating it with necessary pieces.
	 * @param type
	 * @return
	 */
	public ActionInterface getAction(FeedType type) {
		ActionInterface ai;
		switch(type) {
			case SYNC:
				ai = new SyncScheduleAssocAction(this.actionInit);
				break;
			case GROUP:
				ai = new RSSGroupAction(this.actionInit);
				break;
			case SEGMENT:
				ai = new RSSSegmentAction(this.actionInit);
				break;
			case TERMS:
				ai = new RSSTermsAction(this.actionInit);
				break;
			case TYPE:
				ai = new RSSTypeAction(this.actionInit);
				break;
			case FILTER:
				ai = new RSSFilterAction(this.actionInit);
				break;
			case NEWSROOM:
				ai = new NewsroomAction(this.actionInit);
				break;
			case CONSOLE:
				ai = new NewsroomConsoleAction(this.actionInit);
				break;
			case FEED:
			default:
				ai = new SmarttrakRSSFeedAction(this.actionInit);
				break;
		}
		ai.setAttributes(this.attributes);
		ai.setDBConnection(dbConn);
		return ai;
	}
}