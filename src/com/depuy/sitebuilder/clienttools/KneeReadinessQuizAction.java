package com.depuy.sitebuilder.clienttools;

// JDK 5.0 
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.http.session.SMTSession;

//SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;

// SB Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.SiteBuilderUtil;

/****************************************************************************
 * <b>Title</b>: PrinterFriendlyAction.java<p/>
 * <b>Description:Provides the printer friendly formatting for the requested 
 * page</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2005<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 1.0
 * @since Jul 7, 2005
 ****************************************************************************/

public class KneeReadinessQuizAction extends SBActionAdapter {
    SiteBuilderUtil util = null;
    
    /**
     * 
     */
    public KneeReadinessQuizAction() {
        super();
        util = new SiteBuilderUtil();
    }

    /**
     * @param arg0
     */
    public KneeReadinessQuizAction(ActionInitVO arg0) {
        super(arg0);
        util = new SiteBuilderUtil();
    }


	/* (non-Javadoc)
	 * @see com.siliconmtn.action.ActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
    @Override
	public void build(ActionRequest req) throws ActionException {
		// empty by design
	}
    
    /* (non-Javadoc)
     * @see com.siliconmtn.action.ActionController#list(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
    public void list(ActionRequest req) throws ActionException {
		// empty by design
    }


	/* (non-Javadoc)
	 * @see com.siliconmtn.action.ActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public void retrieve(ActionRequest req) throws ActionException {
		String page = StringUtil.checkVal(req.getParameter("page"));
		SMTSession ses = req.getSession();
    	
		//store all request values into the session for the results page display
		Map<String,String> vals = (Map<String,String>) ses.getAttribute("kneeReadinessQuizMap");
		if (vals == null || "".equals(page)) vals = new HashMap<>(100);  //if no page assume they started over
		Enumeration iter = req.getParameterNames();
		String paramName;
		String paramValue;
		try {
			while (iter.hasMoreElements()) {
				paramName = StringUtil.checkVal(iter.nextElement());
				paramValue = req.getParameter(paramName);
				vals.put(paramName, paramValue);
			}
		} catch (Exception e) {
			log.error("Error Parsing Request Values", e);
		}
    	
		ses.setAttribute("kneeReadinessQuizMap", vals);
		log.debug("passed page was " + page);
	}

}
