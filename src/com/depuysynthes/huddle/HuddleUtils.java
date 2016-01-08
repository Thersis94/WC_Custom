package com.depuysynthes.huddle;

import java.util.Arrays;

/****************************************************************************
 * <b>Title</b>: HuddleConstants.java<p/>
 * <b>Description: Constants for DS Huddle.  Commonly database pkIds that will never change.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 29, 2015
 ****************************************************************************/
public class HuddleUtils {

	/** registration (user account) fields **/
	public static final String WWID_REGISTER_FIELD_ID = "HUDDLE_WWID";
	public static final String HOMEPAGE_REGISTER_FIELD_ID = "c0a80241f4bfdb229fce1431e31a1cfe";
	public static final String COMM_PREFS_REGISTER_FIELD_ID = "HUDDLE_COMM_CHANNEL";
	public static final String CELL_PHONE_REGISTER_FIELD_ID = "7f000001517b18842a834a598cdeafa"; //from WC core
	
	// wwid attribute (name) stored in UserDataVO during SSO login.
	public static final String WWID = "wwid";
	
	//session constants
	public static final String MY_HOMEPAGE = "huddleMyHomepage";
	
	//cookies
	public static final String SORT_COOKIE = "huddleSort";
	public static final String RPP_COOKIE = "huddleRpp";
	public static final int DEFAULT_RPP_INT = 12;
	public static final String DEFAULT_RPP = "" + DEFAULT_RPP_INT; //set as String, the same way we'd get it from the Browser/Cookie

	
	/**
	 * the number of days to subtract from an event to designate when Registration opens
	 */
	public static final int EVENT_REGISTRATION_OPENS = -90;
	
	
	
	/**
	 *  Per Bradley / TDS - 12.28.2015
	 *  o   SPINE 000942, pre-select 
	 *  o   CMF (001221), do NOT pre-select a value
	 *  o   CODMAN 000945, pre-select 
	 *  o   ETHICON 001220 OR 001510, pre-select 
	 *  o   JOINT RECON 000940, pre-select 
	 *  o   POWER TOOLS (001221), ), do NOT pre-select a value
	 *  o   SPORTS MED 001225, pre-select 
	 *  o   TRAUMA (001221), ), do NOT pre-select a value
	 *  
	 *  These are used on the registration page, to pre-select which homepage 
	 *  option is recommended for the user (based on their SSO value).
	 *  
	 *  A blank pageAlias means we won't pre-select during initial registration
	 */
	public enum SSOBusinessUnit {
		Spine("SPINE","spine", "000942"),
		Cmf("CMF","","001221"),
		Codman("CODMAN NEURO","codman-neuro","000945"),
		Ethicon("ETHICON","ethicon","001220","001510"),
		JointRecon("JOINT RECONSTRUCTION","joint-reconstruction","000940"),
		PowerTools("POWER TOOLS", "", "001221"),
		SportsMed("SPORTS MEDICINE","sports-medicine","001225"),
		Trauma("TRAUMA", "", "001221");

		private String name;
		private String pageAlias;
		private String[] businessUnits;
		SSOBusinessUnit(String name, String pageAlias, String... businessUnits) {
			this.name = name;
			this.pageAlias = pageAlias;
			this.businessUnits = businessUnits;
		}
		public String getName() { return name;	}
		public String getPageAlias() { return pageAlias; }
		public String[] getBusinessUnits() { return businessUnits; }
	}
	
	
	public static String getBusUnitNm(String mrcCode) {
		SSOBusinessUnit bu = getSSOBusinessUnit(mrcCode);
		return (bu != null) ? bu.getName() : null;
	}
	
	public static SSOBusinessUnit getSSOBusinessUnit(String mrcCode) {
		for (SSOBusinessUnit vo : SSOBusinessUnit.values()) {
			if (Arrays.asList(vo.getBusinessUnits()).contains(mrcCode))
				return vo;
		}
		return null;
	}
}
