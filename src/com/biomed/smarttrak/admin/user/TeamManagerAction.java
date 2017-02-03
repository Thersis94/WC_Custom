package com.biomed.smarttrak.admin.user;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;

// WebCrescendo libs
import com.smt.sitebuilder.action.SBActionAdapter;

/*****************************************************************************
 <p><b>Title</b>: TeamManagerAction.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Feb 1, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class TeamManagerAction extends SBActionAdapter {

	/**
	* Constructor
	*/
	public TeamManagerAction() {
		super();
	}
	
	/**
	 * Constructor
	 */
	public TeamManagerAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		// admin: retrieve team(s)
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		// admin: update team(s)
	}
	
}
