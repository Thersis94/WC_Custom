package com.biomed.smarttrak.admin;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: insightBlogAction.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> wraps the blog action for the public facing smarttrak tool
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Jan 10, 2017<p/>
 * @updates:
 ****************************************************************************/
public class InsightBlogAction extends SBActionAdapter {
	
	public InsightBlogAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public InsightBlogAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
	
		log.debug("insite blog action retrieve called " + actionInit.getActionId());

	}
	
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		log.debug("insite blog action list called " + actionInit.getActionId());
		String type = StringUtil.checkVal(req.getParameter("type"));
		
		if ("author".equals(type)) {
			type = "team";
		}
		log.debug(" action id: " + actionInit.getActionId());
		actionInit.setActionId("eb19b0e489ade9bd7f000101577715c8");
		req.setParameter(SB_ACTION_ID, "eb19b0e489ade9bd7f000101577715c8");
		req.setAttribute(SB_ACTION_ID, "eb19b0e489ade9bd7f000101577715c8");
		req.setParameter("display", "/custom/biomed/smarttrak/admin/smarttrak_insight/" + type  + ".jsp");

		log.debug(" action id: " + actionInit.getActionId());
		
	
		
		log.debug("post super call " + actionInit.getActionId());
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.ActionRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException{
		log.debug("insite blog action update called");
		
	
		
		// get the correct blog id for the insight blog
		String type = StringUtil.checkVal(req.getParameter("type"));
		
		if ("author".equals(type)) {
			type = "team";
		}
		// change action id to the right blog id
		log.debug(" action id: " + actionInit.getActionId());
		actionInit.setActionId("eb19b0e489ade9bd7f000101577715c8");
		req.setParameter(SB_ACTION_ID, "eb19b0e489ade9bd7f000101577715c8");
		req.setAttribute(SB_ACTION_ID, "eb19b0e489ade9bd7f000101577715c8");
		req.setParameter("display", "/custom/biomed/smarttrak/admin/smarttrak_insight/" + type  + ".jsp");
		log.debug(" action id: " + actionInit.getActionId());
		
		// might need to move data from admin module to public module
		
		log.debug("post super call " + actionInit.getActionId());
	
	
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException{
		log.debug("insite blog action delete called");
		
		String type = StringUtil.checkVal(req.getParameter("type"));
		
		if ("author".equals(type)) {
			type = "team";
		}
		// get the correct blog id for the insight blog
		
		// change action id to the right blog id
		log.debug(" action id: " + actionInit.getActionId());
		actionInit.setActionId("eb19b0e489ade9bd7f000101577715c8");
		req.setParameter(SB_ACTION_ID, "eb19b0e489ade9bd7f000101577715c8");
		req.setAttribute(SB_ACTION_ID, "eb19b0e489ade9bd7f000101577715c8");
		req.setParameter("display", "/custom/biomed/smarttrak/admin/smarttrak_insight/" + type  + ".jsp");

		log.debug(" action id: " + actionInit.getActionId());
		

	}

	
}
