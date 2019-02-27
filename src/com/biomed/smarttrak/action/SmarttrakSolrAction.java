package com.biomed.smarttrak.action;

import com.biomed.smarttrak.action.AdminControllerAction.Section;
import com.biomed.smarttrak.security.SmarttrakRoleVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.action.search.SolrActionVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> SmarttrakSolrAction.java<br/>
 * <b>Description:</b> Overrides the superclass to vary which ACL is used depending on where we are on the website.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2017<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Aug 28, 2017
 ****************************************************************************/
public class SmarttrakSolrAction extends SolrAction {

	public static final String SECTION = "aclSection";

	public static final Section BROWSE_SECTION = Section.PRODUCT; //placeholder for our default selection.  This will likely get removed in time.

	public SmarttrakSolrAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public SmarttrakSolrAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}

	
	/*
	 * overrides the superclass method to make a decision about WHICH role ACL we should use for the given query.
	 * Fall-back through a series of settings - 1) explicitly set 2) by Section URL 3) Default to Browse.
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.search.SolrAction#includeRoleACL(com.siliconmtn.action.ActionRequest, com.smt.sitebuilder.action.search.SolrActionVO)
	 */
	@Override
	public void includeRoleACL(ActionRequest req, SolrActionVO data) {
		Object wcRoles = req.getSession().getAttribute(Constants.ROLE_DATA);
		if (wcRoles == null || !(wcRoles instanceof SmarttrakRoleVO)) {
			super.includeRoleACL(req, data);
			return;
		}

		SmarttrakRoleVO roles = (SmarttrakRoleVO) wcRoles;
		Section sec = determineSection(req);
		log.debug("using ACL from sec: " + sec);
		data.setRoleLevel(roles.getRoleLevel());
		data.setRoleACL(roles.getAccessControlList(sec));
	}


	/**
	 * systemically determine which Section to use - either by override, URL, or default entry
	 * @param req
	 * @return
	 */
	public static Section determineSection(ActionRequest req) {
		Section sec;
		//try developer override - set this parameter from invoking actions
		if (req.getAttribute(SECTION) != null) {
			sec = castSection(StringUtil.checkVal(req.getAttribute(SECTION)));
			if (sec != null) 
				return sec;
		}
		//try to get from page alias
		sec = getFromUrl(req);
		if (sec != null) 
			return sec;

		//return a default - The website's Browse ACL ("Prof" on Account permissions page)
		return BROWSE_SECTION;
	}


	/**
	 * iterates the Section enum to determine which Section this webpage represents.
	 * @param req
	 * @return
	 */
	private static Section getFromUrl(ActionRequest req) {
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		String path = req.hasParameter(SECTION) ? req.getParameter(SECTION) + "/" : page.getFullPath() + "/";
		log.debug("pageAlias = " + path);

		//iterate the sections - stop when a section's pageUrl matches the page we're on.
		for (Section sec : Section.values()) {
			if (sec.getPageURL().equals(path)) {
				log.debug("matched " + sec + " for url " + path);
				return sec;
			}
		}
		return null;
	}


	/**
	 * casts a String to a Section - gracefully returns null if no match. 
	 * @param parameter
	 * @return
	 */
	private static Section castSection(String secStr) {
		try {
			return Section.valueOf(secStr);
		} catch (Exception e) {
			return null;
		}
	}
}