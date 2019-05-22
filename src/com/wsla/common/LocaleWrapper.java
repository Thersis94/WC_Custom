package com.wsla.common;

// JDK 1.8.x
import java.util.Locale;

import com.siliconmtn.action.ActionRequest;
// SMT Base Libs
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: LocaleWrapper.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Convenience class to take a language_Country locale
 * and get a Locale Object 
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Nov 16, 2018
 * @updates:
 ****************************************************************************/

public class LocaleWrapper extends BeanDataVO {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1141735696743427694L;
	private Locale locale;
	
	/**
	 * Builds a Locale from the combine language/country
	 * @param strLocale
	 */
	public LocaleWrapper(String strLocale) {
		if (strLocale == null || strLocale.indexOf('_') == -1) return;
		
		String[] vals = strLocale.split("_");
		if (vals.length == 1) {
			//if there is no country get the base locale for the language
			locale = new Locale(vals[0]);
		}else {
			locale = new Locale(vals[0], vals[1]);
		}
 	}
	
	/**
	 * Takes the enum and coverts to a Locale
	 * @param wslaLocale
	 */
	public LocaleWrapper(WSLALocales wslaLocale) {
		this(wslaLocale.name());
	}
	
	/**
	 * Gets the user data form the session and sets the locale
	 * @param req
	 */
	public LocaleWrapper(ActionRequest req) {
		UserDataVO profile = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
		UserVO user = (UserVO)profile.getUserExtendedInfo();
		locale = user.getUserLocale();
	}
	
	/**
	 * Returns the locale
	 * @return
	 */
	public Locale getLocale() {
		return locale;
	}
}

