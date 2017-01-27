package com.biomed.smarttrak.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.biomed.smarttrak.action.NoteAction;
import com.biomed.smarttrak.vo.NoteVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
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
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {

		log.debug("************************************************* start notes test");
		
		NoteAction na = new NoteAction();
		na.setDBConnection(dbConn);
		na.setAttributes(attributes);
		
		
		List<String> testIds = new ArrayList<>();
		testIds.add("ryan");
		testIds.add("rogelio");
		testIds.add("Loa");
		
		Map<String, List<NoteVO>> testList1 = na.getCompanyNotes("8080", testIds, testIds, "2792");
		
		for( Entry<String, List<NoteVO>> entry : testList1.entrySet() ){
			log.debug("key: " + entry.getKey() );
			log.debug("value: " + entry.getValue())
		}
		
		//req.setParameter("NOTE_ID",);
		//new note test
		req.setParameter("USER_ID","8080");
		//req.setParameter("TEAM_ID",);
		req.setParameter("COMPANY_ID","2792");
		//req.setParameter("COMPANY_ATTRIBUTE_ID",);
		//req.setParameter("PRODUCT_ID",);
		//req.setParameter("PRODUCT_ATTRIBUTE_ID",);
		//req.setParameter("MARKET_ID",);
		//req.setParameter("MARKET_ATTRIBUTE_ID",);
		req.setParameter("NOTE_NM","testing the insert method");
		req.setParameter("NOTE_TXT","this is a new note added to the req object for testing");
		req.setParameter("FILE_PATH_TXT","/nope/not/a/real/path");

		na.build(req);
		
		na.getCompanyNotes("8080", null, null, "2792");
		
		log.debug("************************************************ end notes test");
		
	/*	log.debug("insite blog action retrieve called " + actionInit.getActionId());
		//TODO catching the page to i can build the public admin widget directly
		super.retrieve(req);
		//TODO is this a good way to tell i am not in the admintool?
		if(!Convert.formatBoolean(req.getParameter("manMod"))){
		this.list(req);
		}*/
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
	/*	log.debug("insite blog action list called " + actionInit.getActionId());
		
		log.debug(" action id: " + actionInit.getActionId());
		actionInit.setActionId("eb19b0e489ade9bd7f000101577715c8");
		log.debug(" action id: " + actionInit.getActionId());
		
		if(Convert.formatBoolean(req.getParameter("manmod"))){
			
			super.list(req);
		
		}else {
			super.retrieve(req);
		}
		
		log.debug("post super call " + actionInit.getActionId());*/
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException{
		log.debug("insite blog action update called");
		
		super.update(req);
		
		// get the correct blog id for the insight blog
		
		// change action id to the right blog id
		log.debug(" action id: " + actionInit.getActionId());
		actionInit.setActionId("eb19b0e489ade9bd7f000101577715c8");
		log.debug(" action id: " + actionInit.getActionId());
		
		// might need to move data from admin module to public module
		
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
		
		// get the correct blog id for the insight blog
		
		// change action id to the right blog id
		log.debug(" action id: " + actionInit.getActionId());
		actionInit.setActionId("eb19b0e489ade9bd7f000101577715c8");
		log.debug(" action id: " + actionInit.getActionId());
		

	}

	
}
