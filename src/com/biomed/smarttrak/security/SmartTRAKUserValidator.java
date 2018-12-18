package com.biomed.smarttrak.security;

import com.biomed.smarttrak.vo.UserVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.UserValidatorInterface;


/****************************************************************************
 * <b>Title</b>:SmartTRAKUserValidator.java<p/>
 * <b>Description: Ensure that the use has thier names, email, title, division, and phone
 * set and force them to remain on the register page until those fields are filled out. </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2018<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @since Dec 18, 2018
 * @updates 
 * 
 ****************************************************************************/

public class SmartTRAKUserValidator implements UserValidatorInterface {
	private static final String ACCOUNT_PAGE = "/my-account";
	private static final String PAGE_QS = "?msg=User%20Profile%20Icomplete.%20Please%20Complete%20User%20Profile.";
	private static final String SITE_LOCK = "siteLock";
	private static final String INIT_DEST = "initialDestination";

	@Override
	public boolean validateUser(SMTServletRequest req) {
		if (Convert.formatBoolean(req.getSession().getAttribute(SITE_LOCK)) &&
				ACCOUNT_PAGE.equals(StringUtil.checkVal(req.getRequestURI()).replace(req.getContextPath(), ""))) return true;
		
		return isValidUser(req);
	}

	@Override
	public void userApproved(SMTServletRequest req) {
		req.getSession().removeAttribute(SITE_LOCK);
		String initDest = (String)req.getSession().getAttribute(INIT_DEST);
		if (!StringUtil.isEmpty(initDest)
				&& isValidUser(req)) {
			req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
			req.setAttribute(Constants.REDIRECT_URL, initDest);
			req.getSession().removeAttribute(INIT_DEST);
		}
	}

	
	/**
	 * Check to see if the supplied user has all fields set properly
	 * @param req
	 * @return
	 */
	private boolean isValidUser(SMTServletRequest req) {
		UserVO smarttrakUser = (UserVO) req.getSession().getAttribute(Constants.USER_DATA);
		if (smarttrakUser == null) return true;
		if (StringUtil.isEmpty(smarttrakUser.getFirstName())) return false;
		if (StringUtil.isEmpty(smarttrakUser.getLastName())) return false;
		if (StringUtil.isEmpty(smarttrakUser.getEmailAddress())) return false;
		if (StringUtil.isEmpty(smarttrakUser.getMainPhone())) return false;
		if (StringUtil.isEmpty(smarttrakUser.getTitle())) return false;
		if (smarttrakUser.getDivisions() == null)return false;
		
		return true;
	}

	@Override
	public void userRejected(SMTServletRequest req) {
		req.getSession().setAttribute(SITE_LOCK, true);
		if (StringUtil.isEmpty((String) req.getSession().getAttribute(INIT_DEST)))
			req.getSession().setAttribute(INIT_DEST, StringUtil.checkVal(req.getRequestURI()).replace(req.getContextPath(), "") + req.getCompleteQueryString());
		
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, ACCOUNT_PAGE + PAGE_QS);
	}

}
