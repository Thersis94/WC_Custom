package com.depuysynthesinst;

import java.util.HashSet;
import java.util.Set;

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
 * <b>Description: Wraps WC's core Registration portlet with additional business rules and logic.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jun 8, 2015
 ****************************************************************************/
public class RegistrationAction extends SimpleActionAdapter {

	private Set<String> specialProfs = null;

	public RegistrationAction() {
		super();
		populateSpecialProfs();
	}

	/**
	 * @param arg0
	 */
	public RegistrationAction(ActionInitVO arg0) {
		super(arg0);
		populateSpecialProfs();
	}

	private void populateSpecialProfs() {
		specialProfs = new HashSet<>();
		specialProfs.add("RESIDENT");
		specialProfs.add("FELLOW");
		specialProfs.add("CHIEF");
		specialProfs.add("DIRECTOR");
	}

	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	public void update(SMTServletRequest req) throws ActionException {
		String[] regAction = req.getParameter("attrib1Text").split("~");
		//need to capture the actionId as well as the actionGroupId for submitting/retrieving Registration on the front end
		if (regAction != null && regAction.length == 2) {
			req.setParameter("attrib1Text", regAction[0]); //actionId
			req.setParameter("attrib2Text", regAction[1]); //actionGroupId
		}
		super.update(req);
	}
	

	/**
	 * Invokes WC Registration's retrieve method.  No further logic is needed.
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		mod.setActionId(actionInit.getActionId());
		mod.setActionGroupId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_2));
		setAttribute(Constants.MODULE_DATA, mod);

		SMTActionInterface reg = new RegistrationFacadeAction(actionInit);
		reg.setDBConnection(dbConn);
		reg.setAttributes(getAttributes());
		reg.retrieve(req);
		reg = null;
		
		//TODO
		//if page = 3 and this is a new registration, probe to see if the user is eligible for migration
		//all users getting page 3 are TTLMS eligible; decision is made in the View of which modal comes next.
		if ("3".equals(req.getParameter("page"))) {
			
		}
		
	}


	/**
	 * extend Registration's build method with added logic for:
	 * determining if we need to display pages 3 & 4 of registration
	 */
	public void build(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		mod.setActionId(actionInit.getActionId());
		mod.setActionGroupId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_2));
		setAttribute(Constants.MODULE_DATA, mod);
		
		//TODO
		if (req.hasParameter("revokeDirector")) {

			return;
		}

		SMTActionInterface reg = new RegistrationFacadeAction(actionInit);
		reg.setDBConnection(dbConn);
		reg.setAttributes(getAttributes());
		reg.build(req);
		reg = null;
		
		//TODO
		//if this is an edit, call the LMS after each modal save.  Otherwise only call after #4.
		
		
	}
}
