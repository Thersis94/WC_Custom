package com.depuysynthes.huddle;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.registration.RegistrationFacadeAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: RegistrationAction.java<p/>
 * <b>Description: Wraps the stock Registration class with additional support for 
 * email campaign permissions, which are embedded on the Huddle View. </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 30, 2015
 ****************************************************************************/
public class RegistrationAction extends SimpleActionAdapter {

	public RegistrationAction() {
		super();
	}
	
	public RegistrationAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}

	
	/**
	 * load the registration form.  The load a list of Email Campaigns for this Org.
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		mod.setActionGroupId((String) mod.getAttribute(ModuleVO.ATTRIBUTE_1));

		actionInit.setActionGroupId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		SMTActionInterface reg = new RegistrationFacadeAction(actionInit);
		reg.setDBConnection(dbConn);
		reg.setAttributes(getAttributes());
		reg.retrieve(req);
		reg = null;
	}
	
	
	/**
	 * save the registration form.  Then save the email campaign opt-in values.
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		mod.setActionGroupId((String) mod.getAttribute(ModuleVO.ATTRIBUTE_1));

		actionInit.setActionGroupId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		SMTActionInterface reg = new RegistrationFacadeAction(actionInit);
		reg.setDBConnection(dbConn);
		reg.setAttributes(getAttributes());
		reg.build(req);
		reg = null;
		
		//update the homepage set on the user's session, which was set when they logged in (HuddleLoginModule)
		String homepage = req.getParameter("reg_||" + HuddleUtils.HOMEPAGE_REGISTER_FIELD_ID);
		if (homepage != null && homepage.length() > 0)
			req.getSession().setAttribute(HuddleUtils.MY_HOMEPAGE, homepage);
		
		if (req.hasParameter("firstVisit")) { //continue this new user along to their selected homepage
			req.setAttribute(Constants.REDIRECT_URL, "/" + homepage);
		}
	}
}