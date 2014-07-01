package com.telovations.action;

import java.io.IOException;


import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: WebServiceAction.java <p/>
 * <b>Project</b>: WC_MISC <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Jul 15, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class WebServiceAction extends SBActionAdapter {
	//public static final String BASE_URL = "http://xsp.telovations.net/com.broadsoft.xsi-actions/v1.0/user/";
	public static final String BASE_URL = "http://xsplab.telovations.net/com.broadsoft.xsi-actions/v2.0/user/";
	
	/**
	 * 
	 */
	public WebServiceAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public WebServiceAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		int wsType = Convert.formatInteger(mod.getAttribute(ModuleVO.ATTRIBUTE_1) + "");
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		if (user == null) return;
		
		String url = BASE_URL + user.getProfileId() + "/";
		
		switch (wsType) {
			case 1:
				String fieldType = StringUtil.checkVal(req.getParameter("fieldType"));
				String searchVal = StringUtil.checkVal(req.getParameter("searchVal"));
				url += "directories/Group?results=10&start=";
				url += Convert.formatInteger(req.getParameter("start"), 1);
				
				if (fieldType.length() > 0 && searchVal.length() > 0) 
					url += "&" + fieldType + "=" + searchVal;
				
				break;
			case 2:
				url += "";
				break;
			case 3:
				url += "profile";
				break;
			case 4:
				String type = StringUtil.checkVal(req.getParameter("callType"));
				if (type.length() > 0) url += "directories/EnhancedCallLogs/" + type + "?results=10";
				else return;
				
				break;
			default:
				return;	
		}
		
		String xml = null;
		String msg = null;
		try {
			xml = this.loadWebService(user.getProfileId(), user.getPassword(), url);
		} catch (Exception e) {
			log.error("Error retrieving webservice: " + url, e);
			msg = "Unable to retrieve Web Servie Information";
		}
		
		this.putModuleData(xml, 0, false, msg);
	}
	
	
	
	
	/**
	 * 
	 * @param user
	 * @param pass
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public String loadWebService(String user, String pass, String url) throws IOException {
		log.debug("Calling the web service at: " + url + "|" + user + "|" + pass);
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		conn.setFollowRedirects(false);
		byte[] data = conn.basicAuthLogin(user, pass, url, null);
		String xml = null;
		if (data != null) xml = new String(data);
		
		// Parse out the Namessapce due to the JSTL Bug
		if (xml != null)
			xml = xml.replace("xmlns=\"http://schema.broadsoft.com/xsi\"", "");
		
		return xml;
	}
 }
