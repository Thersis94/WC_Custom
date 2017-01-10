package com.depuy.sitebuilder.clienttools;

// SMT Base Libs 2.0

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;

// SB Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: MapFacadeAction.java</p>
 <p>Since there are 2 actions associated to an Event (groups and entries)
 this class determines the appropriate action and forwards the request to 
 that action.  The retrieve method for the action is implemented in this 
 class</p>
 <p>Copyright: Copyright (c) 2000 - 2005 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 2.0
 @since Mar 19, 2006
 Code Updates
 James Camire, Mar 19, 2006 - Creating Initial Class File
 ***************************************************************************/

public class ClientToolsFacadeAction extends SimpleActionAdapter {
	
	
    /**
     * 
     */
    public ClientToolsFacadeAction() {
        super();
    }

    /**
     * @param arg0
     */
    public ClientToolsFacadeAction(ActionInitVO arg0) {
        super(arg0);
    }

    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
     */
    @Override
    public void retrieve(ActionRequest req) throws ActionException {
    	SMTActionInterface eg = null;
    	String type = StringUtil.checkVal(req.getParameter("type"));

		ModuleVO modVO = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		log.debug("vo=" + modVO);
		type = StringUtil.checkVal(modVO.getAttribute(ModuleVO.ATTRIBUTE_1));
		log.debug("type=" + type);
		
		log.debug("name=" + modVO.getActionName());
    	
    	if (type.equals("kneeReadinessQuiz")) {
    		eg = new KneeReadinessQuizAction(this.actionInit);
    	} else if (type.equals("kneePainAssessment")) {
    		eg = new KneePainAssessmentAction(this.actionInit);
    	}
    	
    	if (eg != null) {
    		log.debug("starting action");
	    	eg.setAttributes(this.attributes);
	    	eg.retrieve(req);
	    	log.debug("action completed");
    	}
    }

    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#list(com.siliconmtn.http.SMTServletRequest)
     */
    @Override
    public void list(ActionRequest req) throws ActionException {
    	log.info("Starting client tools - list");
        super.retrieve(req);
    }
}
