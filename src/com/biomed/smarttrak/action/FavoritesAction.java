package com.biomed.smarttrak.action;

import com.siliconmtn.action.ActionException;
// SMTBaseLibs
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
// WebCrescendo libs
import com.smt.sitebuilder.action.SBActionAdapter;

/*****************************************************************************
 <p><b>Title</b>: FavoritesAction.java</p>
 <p><b>Description: </b>Retrieves user's 'favorites'.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Feb 16, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class FavoritesAction extends SBActionAdapter {
	
	/**
	 * Constructor
	 */
	public FavoritesAction() {
		super();
	}
	
	/**
	 * Constructor
	 */
	public FavoritesAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		// TODO Auto-generated method stub
		super.retrieve(req);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		// TODO Auto-generated method stub
		super.build(req);
	}
	
	
}
