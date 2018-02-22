package com.depuysynthes.srt.util;

import org.apache.commons.lang.StringEscapeUtils;

import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> SRTUtil.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Util class to hold SRT Related Helper methods and
 * constants.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 22, 2018
 ****************************************************************************/
public class SRTUtil {

	private SRTUtil() {
		//Hide Default Constructor.
	}

	//TODO Update with actual Values when decided.  Potentially pull from config.
	public static final String SRT_ORG_ID = "DPY_SYN_HUDDLE";
	public static final String PUBLIC_SITE_ID = "DPY_SYN_HUDDLE_2";
	public static final String REGISTRATION_GRP_ID = "18d2a87d9daef5dfc0a8023743a91557";
	public static final String HOMEPAGE_REGISTER_FIELD_ID = null;

	/**
	 * takes the pain out of passing Strings in and out of URLs/forms.  Typically these form values arrive HTML encoded.  
	 * Use encodeURIComponent in your JS to compliment what this is doing server-side (at the client).
	 * @param value
	 * @return
	 */
	public static String urlEncode(String value) {
		if (StringUtil.isEmpty(value)) return ""; //going in a URL, we don't want to return a null
		return StringEncoder.urlEncode(StringEscapeUtils.unescapeHtml(value)).replace("+", "%20");
	}
}