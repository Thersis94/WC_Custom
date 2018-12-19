package com.biomed.smarttrak.security;

import com.biomed.smarttrak.vo.UserVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.http.WCUtilityFilter;
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
	public String approveUser(SMTServletRequest req) {
		String initDest = (String)req.getSession().getAttribute(INIT_DEST);
		UserVO smarttrakUser = (UserVO) req.getSession().getAttribute(Constants.USER_DATA);
		if (!StringUtil.isEmpty(initDest) && smarttrakUser.isValidProfile()) {
			return initDest;
		}
		return "";
	}
	

	@Override
	public String rejectUser(SMTServletRequest req) {
		if (StringUtil.isEmpty((String) req.getSession().getAttribute(INIT_DEST))) {
			req.getSession().setAttribute(INIT_DEST, StringUtil.checkVal(req.getRequestURI()).replace(req.getContextPath(), "") + req.getCompleteQueryString());
			req.getSession().setAttribute(WCUtilityFilter.APPROVAL_REDIRECT, true);
		}
		return ACCOUNT_PAGE + PAGE_QS;
	}
	

	@Override
	public boolean isValidRequest(SMTServletRequest req) {
		return ACCOUNT_PAGE.equals(StringUtil.checkVal(req.getRequestURI()).replace(req.getContextPath(), ""));
	}

}
