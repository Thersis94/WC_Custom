package com.ansmed.sb.sales;

// SMT Base Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;

// SB Libs
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: SalesRepFacadeAction.java</p>
 <p>Description: <b/>Manages the sales rep info for ANS Medical</p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 1.0
 @since Aug 28, 2007
 Last Updated:
 ***************************************************************************/

public class SalesRepFacadeAction extends SimpleActionAdapter {

    public static final String ANS_AREA_MESSAGE = "ansAreaMessage";
    
	/**
	 * 
	 */
	public SalesRepFacadeAction() {
        super();
	}

	/**
	 * @param actionInit
	 */
	public SalesRepFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		SMTActionInterface sbac = null;
		if (Convert.formatBoolean(req.getParameter("repUpdate"))) {
			log.debug("Updating Sales Rep Info");
	        sbac = new SalesRepAction(actionInit);
	        sbac.setAttributes(this.attributes);
	        sbac.setDBConnection(dbConn);
	        sbac.update(req);
		} else if (Convert.formatBoolean(req.getParameter("assignAll"))) {
			log.debug("Reassigning all reps");
	        sbac = new AssignRepAction(actionInit);
	        sbac.setAttributes(this.attributes);
	        sbac.setDBConnection(dbConn);
	        sbac.update(req);
		} else if (Convert.formatBoolean(req.getParameter("deleteRep"))) {
			log.debug("Deleting individual rep");
	        sbac = new SalesRepAction(actionInit);
	        sbac.setAttributes(this.attributes);
	        sbac.setDBConnection(dbConn);
	        sbac.delete(req);
		}

		// Build the redir url
		StringBuffer url = new StringBuffer();
		url.append(req.getRequestURI()).append("?msg=").append(req.getAttribute(ANS_AREA_MESSAGE));
		url.append("&page=").append(req.getParameter("page"));
		url.append("&order=").append(req.getParameter("order"));
		if (Convert.formatBoolean(req.getParameter("assignAll"))) {
			url.append("&assignRep=true");
			url.append("&salesRepId=").append(req.getParameter("origSalesRepId"));
			url.append("&repName=").append(req.getParameter("repName"));
			url.append("&regionId=").append(req.getParameter("regionId"));
		}
		// redirect back to the list of areas
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
	      super.retrieve(req);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("Getting Sales Rep Info");
		
		SMTActionInterface sbac = null;
		if (Convert.formatBoolean(req.getParameter("assignRep"))) {
			sbac = new AssignRepAction(actionInit);
		} else {
	        sbac = new SalesRepAction(actionInit);
		}
		
        sbac.setAttributes(this.attributes);
        sbac.setDBConnection(dbConn);
        sbac.retrieve(req);
	}
}
