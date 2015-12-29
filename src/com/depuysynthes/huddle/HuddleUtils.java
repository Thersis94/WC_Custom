package com.depuysynthes.huddle;

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
	public static final String HOMEPAGE_REGISTER_FIELD_ID = "HUDDLE_USR_HOMEPAGE";
	
	// wwid attribute (name) stored in UserDataVO during SSO login.
	public static final String WWID = "wwid";

	
	
	
	/**
	 *  Per Bradley / TDS - 12.28.2015
	 *  000942, pre-select SPINE
	 *  000945, pre-select CODMAN
	 *  001220 OR 001510, pre-select ETHICON
	 *  000940, pre-select JOINT RECON
	 *  001225, pre-select SPORTS MED
	 */
	public enum SSOBusinessUnit {
		Spine("Spine","/spine", "000942"),
		Codman("Codman","/codman","000945"),
		Ethicon("Ethicon","/ethicon","001220","001510"),
		JointRecon("Joint Reconstruction","/jointrecon","000940"),
		SportsMed("Sports Medicine","/sportsmed","001225");

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
}
