package com.ansmed.sb.sales;


import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.common.SiteBuilderUtil;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: SalesAreaFacadeAction.java</p>
 <p>Description: <b/>Manages the areas and regions for ANS Medical</p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 1.0
 @since Aug 28, 2007
 Last Updated:
 ***************************************************************************/

public class SalesAreaFacadeAction extends SimpleActionAdapter {
    SiteBuilderUtil util = null;
    public static final String ANS_AREA_MESSAGE = "ansAreaMessage";
    
	/**
	 * 
	 */
	public SalesAreaFacadeAction() {
        super();
        util = new SiteBuilderUtil();
	}

	/**
	 * @param actionInit
	 */
	public SalesAreaFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
        util = new SiteBuilderUtil();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		
		if (Convert.formatBoolean(req.getParameter("editArea"))) {
			ActionInterface sbac = new SalesAreaAction(actionInit);
	        sbac.setAttributes(this.attributes);
	        sbac.setDBConnection(dbConn);
	        sbac.update(req);
		} else if (Convert.formatBoolean(req.getParameter("editRegion"))) {
			ActionInterface sbac = new SalesRegionAction(actionInit);
	        sbac.setAttributes(this.attributes);
	        sbac.setDBConnection(dbConn);
	        sbac.update(req);
		} else if (Convert.formatBoolean(req.getParameter("deleteRegion"))) {
			ActionInterface sbac = new SalesRegionAction(actionInit);
	        sbac.setAttributes(this.attributes);
	        sbac.setDBConnection(dbConn);
	        sbac.delete(req);
		}
		
		// Build the redir url
		String url = req.getRequestURI() + "?msg=" + req.getAttribute(ANS_AREA_MESSAGE);
		
		// redirect back to the list of areas
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url);

	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
	      super.retrieve(req);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("Getting Area Info");
		ActionInterface sbac = new SalesAreaAction(actionInit);
        sbac.setAttributes(this.attributes);
        sbac.setDBConnection(dbConn);
        sbac.retrieve(req);
	}
}
