package com.depuysynthesinst.assg;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title</b>: AssignmentsFacadeAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jun 27, 2015
 ****************************************************************************/
public class AssignmentsFacadeAction extends SimpleActionAdapter {

	public AssignmentsFacadeAction() {
	}

	/**
	 * @param arg0
	 */
	public AssignmentsFacadeAction(ActionInitVO arg0) {
		super(arg0);
	}

	public void retrieve(SMTServletRequest req) throws ActionException {
		
	}
	
	public void build(SMTServletRequest req) throws ActionException {
		
	}
	
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
}