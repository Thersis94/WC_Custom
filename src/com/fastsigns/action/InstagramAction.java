/**
 * 
 */
package com.fastsigns.action;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title</b>:InstagramAction.java<p/>
 * <b>Description: Displays an image from an Instagram post.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Erik Wingo
 * @version 1.0
 * @since Jan 8, 2015
 ****************************************************************************/
public class InstagramAction extends SimpleActionAdapter {

	/**
	 * Default Constructor
	 */
	public InstagramAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public InstagramAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException{
		//Id used by the adapter was null, so setting it here before calling super
		req.setParameter(InstagramAction.SB_ACTION_ID, actionInit.getActionId());
		super.retrieve(req);
	}
}
