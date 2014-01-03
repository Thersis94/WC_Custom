package com.fastsigns.action;

import java.util.Map;

import com.fastsigns.security.FsFranchiseRoleAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: RegistrationPostProcessor.java<p/>
 * <b>Description: Performs post-registration processing for Fastsigns registration
 * submittals.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Jan 04, 2011
 ****************************************************************************/
public class RegistrationPostProcessor extends SBActionAdapter {
	
	public RegistrationPostProcessor(ActionInitVO actionInit) {
		super(actionInit);
	}

	public RegistrationPostProcessor() {
	}

	/* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
	@SuppressWarnings("unchecked")
    public void build(SMTServletRequest req) throws ActionException {	
		//call the franchise role xr managing action
		SMTActionInterface sai = new FsFranchiseRoleAction(this.actionInit);
		sai.setAttributes(attributes);
		sai.setAttribute("profileId", req.getParameter("profileId"));
		sai.setDBConnection(dbConn);
		sai.build(req);	
		
		// get user data from session, set the user attributes map franchise map from the action
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		user.setAttributes((Map<String, Object>)req.getAttribute(FsFranchiseRoleAction.FRANCHISE_MAP));
		req.removeAttribute(FsFranchiseRoleAction.FRANCHISE_MAP);
	}
	
	/* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		this.build(req);
	}
}
