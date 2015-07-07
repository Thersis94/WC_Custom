/**
 * 
 */
package com.fastsigns.action.franchise;

import com.fastsigns.action.franchise.centerpage.FranchiseLocationInfoAction;
import com.fastsigns.action.franchise.vo.FranchiseBlogVO;
import com.fastsigns.action.franchise.vo.FranchiseVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.blog.BlogAction;
import com.smt.sitebuilder.action.blog.BlogFacadeAction;
import com.smt.sitebuilder.action.blog.BlogGroupVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>FranchiseBlogAction.java<p/>
 * <b>Description: Fetches franchise data to be used with the core blog </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Erik Wingo
 * @since Jun 30, 2015
 * <b>Changes: </b>
 ****************************************************************************/
public class FranchiseBlogAction extends SBActionAdapter {
	
	/**
	 * Default Constructor
	 */
	public FranchiseBlogAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public FranchiseBlogAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException{
		super.retrieve(req);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException{
		req.setParameter(SB_ACTION_ID, actionInit.getActionId());
		super.retrieve(req);
		
		ModuleVO mod = (ModuleVO) this.getAttribute(Constants.MODULE_DATA);
		SBModuleVO data = (SBModuleVO) mod.getActionData();
		
		FranchiseBlogVO vo = new FranchiseBlogVO();
		//get franchise location data
		this.getFranchise(req, vo);
		
		String gId = (String)data.getAttribute(SBModuleVO.ATTRIBUTE_1); 
		boolean isPreview = Convert.formatBoolean(req.getAttribute(Constants.PAGE_PREVIEW), false);
		//get the group of blogs to display (blogGroupId stored in attrib1Text)
		BlogAction ba = new BlogAction(actionInit);
		ba.setAttributes(attributes);
		ba.setDBConnection(dbConn);
		this.getBlogGroup(req,vo, ba.getBlogActionId(gId, isPreview));
		
		this.putModuleData(vo);
	}
	
	/**
	 * Retrieves the location data for a franchise
	 * @param req
	 * @param vo
	 */
	private void getFranchise(SMTServletRequest req, FranchiseBlogVO vo){
		//get franchise info for the current site
		String franId = StringUtil.checkVal(CenterPageAction.getFranchiseId(req),null);
		//skip lookup if we can't identify the franchise
		if (franId == null){
			log.error("Cannot get franchise info: Missing franchiseId.");
			return;
		}
		
		//grab the franchise's location data
		FranchiseLocationInfoAction fla = new FranchiseLocationInfoAction(actionInit);
		fla.setAttributes(attributes);
		fla.setDBConnection(dbConn);
		
		FranchiseVO fran = fla.getLocationInfo(franId, false);
		vo.setFranchise(fran);
	}
	
	/**
	 * Retrieves the blogs from the parent corp
	 * @param req
	 * @param vo
	 * @param blogId
	 * @throws ActionException
	 */
	private void getBlogGroup(SMTServletRequest req, FranchiseBlogVO vo, String blogId) throws ActionException{
		String aId = StringUtil.checkVal(blogId, null);
		if (aId == null) {
			throw new ActionException("Missing blog Id");
		}
		
		//actionId changed to match corporate blog's actionId
		actionInit.setActionId(aId);
		//get blogs
		BlogFacadeAction bfa = new BlogFacadeAction(actionInit);
		bfa.setDBConnection(dbConn);
		bfa.setAttributes(attributes);
		bfa.retrieve(req);
		
		//add blogs to vo
		ModuleVO mod = (ModuleVO)bfa.getAttribute(Constants.MODULE_DATA);
		vo.setData((BlogGroupVO) mod.getActionData());
	}
}
