package com.ram.action.customer;

//SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;

// WebCrescendo 2.0
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title: </b>CustomerFacadeAction.java <p/>
 * <b>Project: </b>WC_Custom <p/>
 * <b>Description: </b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2014<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0<p/>
 * @since May 19, 2014<p/>
 *<b>Changes: </b>
 * May 19, 2014: David Bargerhuff: Created class.
 ****************************************************************************/
public class CustomerFacadeAction extends SBActionAdapter {
	
	/**
	 * 
	 */
	public CustomerFacadeAction() {
		super(new ActionInitVO());
	}

	/**
	 * @param actionInit
	 */
	public CustomerFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("CustomerFacadeAction retrieve...");
		boolean searchSubmitted = Convert.formatBoolean(req.getParameter("searchSubmitted"));
		if (searchSubmitted) {
			// perform search
			performSearch(req);
		} else {
			SMTActionInterface sai = null;
			boolean ft = Convert.formatBoolean(req.getParameter("facadeType"));
			if (ft) {
				// CustomerLocationAction
				sai = new CustomerLocationAction(actionInit);
				sai.setAttributes(attributes);
				sai.setDBConnection(dbConn);
				sai.retrieve(req);
				
			} else {
				// CustomerAction
				sai = new CustomerAction(actionInit);
				sai.setAttributes(attributes);
				sai.setDBConnection(dbConn);
				sai.retrieve(req);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("CustomerFacadeAction build...");
		SMTActionInterface sai = null;
		boolean ft = Convert.formatBoolean(req.getParameter("facadeType"));
		if (ft) {
			// CustomerLocationAction update
			sai = new CustomerLocationAction(actionInit);
			sai.setAttributes(attributes);
			sai.setDBConnection(dbConn);
			sai.build(req);
		} else {
			// CustomerAction update
			sai = new CustomerAction(actionInit);
			sai.setAttributes(attributes);
			sai.setDBConnection(dbConn);
			sai.build(req);
		}
	}
	
	/**
	 * Performs search against customer table(s)
	 * @param req
	 * @throws ActionException 
	 */
	private void performSearch(SMTServletRequest req) throws ActionException {
		log.debug("CustomerFacadeAction performSearch...");
		SMTActionInterface sai = new CustomerSearchAction(actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		sai.retrieve(req);
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
        super.retrieve(req);
	}

}
