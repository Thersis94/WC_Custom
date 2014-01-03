package com.ansmed.sb.locator;

import com.smt.sitebuilder.action.SBActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.common.SiteBuilderUtil;

/****************************************************************************
 * <b>Title</b>: SurgeonLocatorAction.java<p/>
 * <b>Description: </b> Manages the 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Apr 26, 2007
 ****************************************************************************/
public class SurgeonLocatorAction extends SBActionAdapter {
    SiteBuilderUtil util = new SiteBuilderUtil();
    
	/**
	 * 
	 */
	public SurgeonLocatorAction() {
		
	}

	/**
	 * @param arg0
	 */
	public SurgeonLocatorAction(ActionInitVO arg0) {
		super(arg0);
		
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
        super.retrieve(req);
	}


}
