package com.depuysynthes.srt.util;

import org.apache.commons.lang.StringEscapeUtils;

import com.depuysynthes.srt.vo.SRTRosterVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

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

	public static final String SRT_ORG_ID = "DPY_SYN";

	//TODO Update with actual Values when decided.  Potentially pull from config.
	public static final String PUBLIC_SITE_ID = "DPY_SYN_HUDDLE_2";
	public static final String REGISTRATION_GRP_ID = "18d2a87d9daef5dfc0a8023743a91557";
	public static final String HOMEPAGE_REGISTER_FIELD_ID = null;
	public static final String ADMIN_PATH = "/manage";
	public enum SrtPage {MASTER_RECORD("/master-record"), PROJECT("/projects"), REQUEST("/request-form");
		private String urlPath;
		private SrtPage(String urlPath) {
			this.urlPath = urlPath;
		}
		public String getUrlPath() {return urlPath;}
	}
	public enum SrtAdmin {MILESTONE("/milestones");
		private String urlPath;
		private SrtAdmin(String urlPath) {
			this.urlPath = StringUtil.join(ADMIN_PATH, urlPath);
		}
		public String getUrlPath() {return urlPath;}
	}
	private SRTUtil() {
		//Hide Default Constructor.
	}

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

	/**
	 * Attempt to load the Users OpCo off the Session Object.
	 * @param req
	 * @return
	 */
	public static String getOpCO(ActionRequest req) {
		SRTRosterVO r = getRoster(req);
		if(r != null) {
			return r.getOpCoId();
		}
		return null;
	}

	/**
	 * Decrypt given username.  May be empty, just first or first and last
	 * name.
	 * @param qualityEngineerNm
	 * @param se
	 * @return
	 * @throws EncryptionException
	 */
	public static String decryptName(String name, StringEncrypter se) throws EncryptionException {
		String [] firstLast = StringUtil.checkVal(name).split(" ");
		if(firstLast == null || firstLast.length == 0) {
			return "";
		} else if(firstLast.length == 1) {
			return StringUtil.checkVal(se.decrypt(firstLast[0])).trim();
		} else {
			return StringUtil.join(StringUtil.checkVal(se.decrypt(firstLast[0])).trim(), " ", StringUtil.checkVal(se.decrypt(firstLast[1])).trim());
		}
	}

	/**
	 * Retrieve the Roster Records off the Request.
	 * @param req
	 * @return
	 */
	public static SRTRosterVO getRoster(ActionRequest req) {
		return (SRTRosterVO)req.getSession().getAttribute(Constants.USER_DATA);

	}
}