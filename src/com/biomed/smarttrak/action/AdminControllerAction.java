/**
 *
 */
package com.biomed.smarttrak.action;

import com.biomed.smarttrak.action.gap.GapFacadeAction;
import com.biomed.smarttrak.admin.ContentHierarchyAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: BioMedAjaxAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> BioMed Ajax Action that will facade the proper call for
 * each Action.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author raptor
 * @version 1.0
 * @since Jan 13, 2017
 ****************************************************************************/
public class AdminControllerAction extends SimpleActionAdapter {

	public AdminControllerAction() {
		super();
	}

	public AdminControllerAction(ActionInitVO arg0) {
		super(arg0);
	}

	@Override
	public void list(SMTServletRequest req) throws ActionException {
		//pass to superclass for portlet registration (WC admintool)
		//this method is not called from the front-end UI
		super.retrieve(req);
	}

	@Override
	public void build(SMTServletRequest req) throws ActionException {
		String cPage = StringUtil.checkVal(req.getParameter("cPage"));
		String msg = (String) attributes.get(AdminConstants.KEY_SUCCESS_MESSAGE);

		try {
			loadAction(cPage).build(req);
		} catch (ActionException ae) {
			log.error("could not forward requested Action.", ae.getCause());
			msg = (String) attributes.get(AdminConstants.KEY_ERROR_MESSAGE);
		}

		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		StringBuilder url = new StringBuilder(200);
		url.append(page.getFullPath()).append("?msg=").append(msg);
		url.append("&cPage=").append(cPage);

		sbUtil.manualRedirect(req, url.toString());
	}

	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		String cPage = StringUtil.checkVal(req.getParameter("cPage"));
		loadAction(cPage).retrieve(req);
	}


	/**
	 * Based on passed cPage, instantiate the appropriate class and return.
	 * @param cPage
	 * @return
	 * @throws ActionException
	 */
	private SMTActionInterface loadAction(String cPage) throws ActionException {
		SMTActionInterface action = null;
		switch (StringUtil.checkVal(cPage)) {
			case "gapAnalysis":
				action = new GapFacadeAction();
				break;
			case "contentHierarchy":
				action = new ContentHierarchyAction();
				break;
		}

		action.setDBConnection(dbConn);
		action.setAttributes(getAttributes());
		return action;
	}
}
