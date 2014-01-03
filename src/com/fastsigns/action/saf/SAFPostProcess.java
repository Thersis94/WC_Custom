package com.fastsigns.action.saf;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: SAFPostProcess.java<p/>
 * <b>Description: </b>
 * 	This class is responsible for sending Request a Quote and Send A File 
 * 	information through to the keystone servers for logging.  Two calls are made,
 * 	1.  Login to the system
 * 	2.  Send RAQ/SAF Information through.   
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Billy Larsen
 * @version 1.0
 * @since Aug 13, 2012
 ****************************************************************************/
public class SAFPostProcess extends SBActionAdapter {
	
	private static final String SCRIPT_PATH = "/index.php";	//Keystone Path
	private SAFConfig safConfig;							//Config containing form fields	
	private static final String USER_NAME = "smtes";		//Keystone UserName
	private static final String USER_PASSWORD = "simple";	//Keystone Password
	private static final String FIELD_PREFIX = "con_";		//Prefix for
	
	/**
	 * This method builds the urls necessarry to complete the SAF Post Process.
	 * First we add the necessary cookies and send the login request to Keystone 
	 * servers.  On success we send the SAF data to Keystone.    
	 */
	public void build(SMTServletRequest req) throws ActionException {
		String keystoneUrl = (String)getAttribute("keystoneBaseUrl") + SCRIPT_PATH;
		
		//Retrieve the Config with all the keys and build our query string.
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		safConfig = SAFConfig.getInstance(site.getCountryCode());
		String strObj = buildObject(req);
		log.debug(strObj);
		
		//Set up the variables we're gonna use.
		String servUrl = keystoneUrl + "?" + strObj;
		String loginUrl = keystoneUrl + "?module=authentication&action=login&return=%2Findex.php";
		String logPost = "&username=" + USER_NAME + "&password=" + USER_PASSWORD;
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		conn.setFollowRedirects(false);
		
		//10 second timeout
		conn.setConnectionTimeout(10000);
		try {
			
			//Retrieve and store the cookie
			Cookie c = req.getCookie(Constants.JSESSIONID);
			if (c != null)
				conn.addCookie(c.getName(), c.getValue());

			//Send the initial login
			log.debug("Logging into: " + loginUrl);
			byte [] logSend = conn.retrieveDataViaPost(loginUrl, logPost);
			log.debug(new String(logSend));
			
			//Send the follow up to store the data
			log.debug("Sending request to: " + servUrl);
			byte [] retSend = conn.retrieveDataViaPost(servUrl, "");
			log.debug(new String(retSend));

		} catch (IOException e) {
			log.warn("Could not connect to Keystone Database", e);
		}
	}
	/**
	 * Build the query string by iterating over a map of values.  
	 * Replace the spaces with url encoded spaces so as to not break system.
	 * @param req
	 * @return
	 */
	private String buildObject(SMTServletRequest req){
		
		StringBuilder sb = new StringBuilder();
		sb.append("module=API&action=newProspect&");
		Map<String, String> vals = getVals();
		
		//Loop over the parameters and build the query String
		for(String key : vals.keySet()){
			
			//Was throwing errors.  We need to check if param exists before replace.
			String reqVal = req.getParameter(key);
			if(reqVal != null)
				sb.append(vals.get(key) + "=" + reqVal.replace(" ", "%20") + "&");
		}
		
		//Special formatting was required for State/Country, Name and Sales Rep.
		sb.append(getStateAndCountry(req));
		sb.append(getName(req));
		sb.append(getSalesRep(req));
		return sb.toString();
	}

	/**
	 * Map holding all the key mappings for Webcrescendo System to Keystone System
	 * @return
	 */
	private Map<String, String> getVals() {
		Map<String, String> vals = new HashMap<String, String>();
		vals.put("dealerLocationId", "franchiseId");
		vals.put(FIELD_PREFIX + safConfig.getCompanyId(), "company");
		vals.put("pfl_ADDRESS_TXT", "address1");
		vals.put("pfl_ADDRESS2_TXT", "address2");
		vals.put("pfl_CITY_NM", "city");
		vals.put("pfl_ZIP_CD", "zip");
		vals.put("pfl_MAIN_PHONE_TXT", "phone");
		vals.put(FIELD_PREFIX + safConfig.getFaxId(), "fax");
		vals.put("pfl_EMAIL_ADDRESS_TXT", "email");
		vals.put(FIELD_PREFIX + safConfig.getSignTypeId(), "signType");
		vals.put(FIELD_PREFIX + safConfig.getRequestedCompletionDateId(), "requestedCompletionDate");
		vals.put(FIELD_PREFIX + safConfig.getSignQuantityId(), "signQuantity");
		vals.put(FIELD_PREFIX + safConfig.getDesiredHeightId(), "desiredHeight");
		vals.put(FIELD_PREFIX + safConfig.getDesiredWidthId(), "desiredWidth");
		vals.put(FIELD_PREFIX + safConfig.getProjectDescriptionId(), "projectDescription");
		return vals;
	}
	
	/**
	 * Splits the Country_State value coming in
	 * @param req
	 * @return
	 */
	private String getStateAndCountry(SMTServletRequest req){
		//Was throwing errors.  We need to check if state exists before replace.
		String stateStr = req.getParameter("state");
		
		if(stateStr == null)
			return "";
		String [] sc = stateStr.split("_");
		String sCStr = "country=" + sc[0];
		sCStr += "&state=" + sc[1];
		
		return sCStr;
	}
	
	/**
	 * Splits first and last name.
	 * @param req
	 * @return
	 */
	private String getName(SMTServletRequest req) {
		UserDataVO user = new UserDataVO();
		user.setName(req.getParameter("pfl_combinedName"));
		
		//First part goes in firstName
		String pStr = "&firstName=" + user.getFirstName();
		pStr += "&lastName=" + user.getLastName();
		
		user = null;
		return pStr;
	}
	
	/**
	 * This method attempts to see if we have a sales rep.  If we do we check for
	 * a name.  If the name exists we add it to the qs.
	 * @param req
	 * @return
	 */
	private String getSalesRep(SMTServletRequest req){
		String salesStr = "&hasSalesRep=";
		int pc = Convert.formatInteger(req.getParameter("priorContact"));
		if(pc > 0){
			
			//Would throw errors.  We need to check if sales Rep exists before replace.
			String salesRep = req.getParameter(FIELD_PREFIX + safConfig.getSalesContactId());
			if(salesRep != null){
				salesStr += pc;
				salesStr += "&salesRepName=" + salesRep.replace(" ", "%20");
			}
		} else
			salesStr +=pc;
		
		return salesStr;
			
	}
}
