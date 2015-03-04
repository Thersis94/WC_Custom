package com.ram.action.customer;

//SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;

import com.siliconmtn.util.StringUtil;
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
	
	//Used to hold type param we look for and the values that are valid.
	public static final String STEP_PARAM = "bType";
	public static enum CUSTOMER_TYPE {customer, location, code}
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
	 * updated to use getAction method.
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("CustomerFacadeAction retrieve...");
		boolean searchSubmitted = Convert.formatBoolean(req.getParameter("searchSubmitted"));
		if (searchSubmitted) {
			// perform search
			performSearch(req);
		} else {
			String bType = StringUtil.checkVal(req.getParameter(STEP_PARAM), CUSTOMER_TYPE.customer.name());
			getAction(bType).retrieve(req);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 * updated to use getAction method
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("CustomerFacadeAction build...");
		String bType = req.getParameter(STEP_PARAM);
		getAction(bType).build(req);
	}
	
	/**
	 * Clean interface that returns the proper action we want pre populated with all the required data.
	 * @param action
	 * @return
	 */
	public SMTActionInterface getAction(String action) {
		SMTActionInterface sai = null;
		switch(CUSTOMER_TYPE.valueOf(action)) {
		case location: 
			sai = new CustomerLocationAction(actionInit);
			break;
		case code:
			sai = new CustomerCodeAction(actionInit);
			break;
		default:
			sai = new CustomerAction(actionInit);
			break;
		}
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		return sai;
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
