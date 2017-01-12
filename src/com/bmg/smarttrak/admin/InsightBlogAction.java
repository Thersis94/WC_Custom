package com.bmg.smarttrak.admin;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.blog.BlogFacadeAction;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

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
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
	
		log.debug("insite blog action retrieve called " + actionInit.getActionId());
		//TODO catching the page to i can build the public admin widget directly
		super.retrieve(req);
		this.list(req);
		
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		log.debug("insite blog action retrive called " + actionInit.getActionId());
		
		super.list(req);
				
		//TODO get the correct blog id for the insight blog
		
		//TODO change action id to the right blog id
		log.debug("bbbbbbbb action id: " + actionInit.getActionId());
		actionInit.setActionId("eb19b0e489ade9bd7f000101577715c8");
		log.debug("bbbbbbbb action id: " + actionInit.getActionId());
		
		
		//TODO call retrive on BFA with the right id
		BlogFacadeAction bfa = new BlogFacadeAction();
		
		bfa.setDBConnection(dbConn);
		bfa.setActionInit(actionInit);
		bfa.setAttributes(attributes);
		bfa.retrieve(req);
		
		//TODO might need to move data from admin module to public module
		
		log.debug("post super call " + actionInit.getActionId());
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException{
		log.debug("insite blog action update called");
		
		super.update(req);
		
		//TODO get the correct blog id for the insight blog
		
		//TODO change action id to the right blog id
		log.debug("bbbbbbbb action id: " + actionInit.getActionId());
		actionInit.setActionId("eb19b0e489ade9bd7f000101577715c8");
		log.debug("bbbbbbbb action id: " + actionInit.getActionId());
		
		
		//TODO call retrive on BFA with the right id
		BlogFacadeAction bfa = new BlogFacadeAction();
		
		bfa.setDBConnection(dbConn);
		bfa.setActionInit(actionInit);
		bfa.setAttributes(attributes);
		bfa.update(req);
		
		//TODO might need to move data from admin module to public module
		
		log.debug("post super call " + actionInit.getActionId());
	
	
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void delete(SMTServletRequest req) throws ActionException{
		log.debug("insite blog action delete called");
		super.delete(req);
		
		//TODO get the correct blog id for the insight blog
		
		//TODO change action id to the right blog id
		log.debug("bbbbbbbb action id: " + actionInit.getActionId());
		actionInit.setActionId("eb19b0e489ade9bd7f000101577715c8");
		log.debug("bbbbbbbb action id: " + actionInit.getActionId());
		
		
		//TODO call retrive on BFA with the right id
		BlogFacadeAction bfa = new BlogFacadeAction();
		
		bfa.setDBConnection(dbConn);
		bfa.setActionInit(actionInit);
		bfa.setAttributes(attributes);
		bfa.delete(req);
		
		
		
	}
	
	/**
	 * in the admintool parts of the site and pageVOs were missing.  data for use in the 
	 *      admintool is added in this method.
	 * @param page 
	 * @param site 
	 * @param req
	 * @param sb 
	 * @param sb 
	 * @return 
	 */
/*	private void wrapPageSite(SiteVO site, PageVO page, SMTServletRequest req) {
		
		if (site != null && site.getMainEmail() == null)
			site.setMainEmail(AdminConstants.ADMIN_FROM_EMAIL);

		req.setAttribute(Constants.SITE_DATA, site);

		if (page == null) {
			page = new PageVO();
			page.setRequestURI("/" + getAttribute(Constants.CONTEXT_NAME) +  getAttribute(AdminConstants.ADMIN_TOOL_PATH));
			req.setAttribute(Constants.PAGE_DATA, page);
		}
		
	}*/
	
}
