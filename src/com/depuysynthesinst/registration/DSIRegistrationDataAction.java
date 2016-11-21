package com.depuysynthesinst.registration;

import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.registration.RegistrationDataAction;

/****************************************************************************
 * <b>Title</b>: DSIRegistrationDataAction.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> extends the wc registration report and via overide allows 
 * for a custom excel sheet to be returned.  
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Nov 21, 2016<p/>
 * @updates:
 ****************************************************************************/
public class DSIRegistrationDataAction extends RegistrationDataAction {

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.registration.RegistrationDataAction#buildReport(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	protected AbstractSBReportVO buildReport(SMTServletRequest req) {
			return new DSIRegistrationDataActionVO();
	}
	
}
