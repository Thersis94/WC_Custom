package com.ansmed.sb.locator;

import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: LocatorFacadeAction.java<p/>
 * <b>Description: </b> Facade to determine which action is being retrieved
 * for the locator functionality.  Specifically, whether the search or 
 * detail view is called.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Apr 27, 2007
 ****************************************************************************/
public class LocatorFacadeAction extends SimpleActionAdapter {
	public static final String ANS_USER_LATITUDE = "ansUserLatitude";
	public static final String ANS_USER_LONGITUDE = "ansUserLongitude";
	public static final String ANS_USER_STATE = "ansUserState";
	public static final String ANS_RESULT_PAGES = "ansResultPages";
	public static final String ANS_DETAIL_MAP = "ansDetailMap";
	
	/**
	 * 
	 */
	public LocatorFacadeAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public LocatorFacadeAction(ActionInitVO arg0) {
		super(arg0);
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		SMTActionInterface sbac = null;
        sbac = new SurgeonLocatorAction(actionInit);
        sbac.setAttributes(this.attributes);
        sbac.setDBConnection(dbConn);
        sbac.list(req);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		SMTActionInterface sbac = null;
        
        if ( StringUtil.checkVal(req.getParameter("surgeonId")).length() == 0) {
        	sbac = new LocatorSearchAction(actionInit);
        } else {
        	sbac = new SurgeonDetailAction(actionInit);
        }
        
        sbac.setAttributes(this.attributes);
        sbac.setDBConnection(dbConn);
        sbac.retrieve(req);

	}
}
