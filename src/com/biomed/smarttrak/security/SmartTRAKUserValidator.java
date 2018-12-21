package com.biomed.smarttrak.security;

import com.biomed.smarttrak.vo.UserVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
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
	private static final String PAGE_QS = "?msg=User%20Profile%20Incomplete.%20Please%20Complete%20User%20Profile.";
	private static final String INIT_DEST = "initialDestination";
	

	@Override
	public String prepareRedirect(SMTServletRequest req, boolean isValid) {
		String destUrl = "";
		
		if (isValid) {
			return StringUtil.checkVal(req.getSession().getAttribute(INIT_DEST));
		} else {
			if (StringUtil.isEmpty((String) req.getSession().getAttribute(INIT_DEST))) {
				req.getSession().setAttribute(INIT_DEST, StringUtil.checkVal(req.getRequestURI()).replace(req.getContextPath(), "") + req.getCompleteQueryString());
			}
			destUrl = isValidRequest(req)? "" : ACCOUNT_PAGE + PAGE_QS;
		}
		
		return destUrl;
	}
	
	
	public boolean isValidUser(UserDataVO user) {
		UserVO smarttrakUser = (UserVO) user;
		if (StringUtil.isEmpty(smarttrakUser.getFirstName())) return false;
		if (StringUtil.isEmpty(smarttrakUser.getLastName())) return false;
		if (StringUtil.isEmpty(smarttrakUser.getEmailAddress())) return false;
		if (StringUtil.isEmpty(smarttrakUser.getMainPhone())) return false;
		if (StringUtil.isEmpty(smarttrakUser.getTitle())) return false;
		return smarttrakUser.getDivisions() != null;
	}
	

	@Override
	public boolean isValidRequest(SMTServletRequest req) {
		return ACCOUNT_PAGE.equals(StringUtil.checkVal(req.getRequestURI()).replace(req.getContextPath(), ""));
	}

}
