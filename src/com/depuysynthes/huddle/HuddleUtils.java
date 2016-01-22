package com.depuysynthes.huddle;

import java.util.Arrays;

import javax.servlet.http.Cookie;

import org.apache.solr.client.solrj.SolrQuery.ORDER;

import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.search.SearchDocumentHandler;

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
	
	/** Catalog Constants **/
	public static final String productCategoryCd = "HUDDLE_CATEGORY";
	public static final String productCatalogId = "DS_HUDDLE";
	
	/** Solr field names of product attributes **/
	public static final String PROD_ATTR_IMG_PREFIX = "image_";
	public static final String PROD_ATTR_MB_PREFIX = "mediabin_";

	public static final String PROD_ATTR_IMG_TYPE = "IMAGE";
	public static final String PROD_ATTR_MB_TYPE = "MEDIABIN";
	
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
	public static final String PROD_SHARE_COOKIE = "huddle-share-products";

	//product catalog constants
	public static final String CATALOG_ID = "DS_HUDDLE";
	public static final String SPEC_PRODCAT_ID = "HUDDLE_SPECIALTY";
	public static final String CAT_PRODCAT_ID = "HUDDLE_CATEGORY";
	
	//solr fields
	public static final String SOLR_OPCO_FIELD = "opco_ss";
	public static final String SOLR_SALES_CONSULTANT_IDX_TYPE = "HUDDLE_CONSULTANTS";
	protected static final String SOLR_PROD_CONTACT_IDX_TYPE = "HUDDLE_PRODUCT_CONTACT";
	protected static final String[] SOLR_PROD_ATTR_FIELD_ARR = {
		"mediabin_system_information_ss",
		"mediabin_selling_tips_ss ",
		"mediabin_value_ss",
		"mediabin_competition_ss",
		"mediabin_clinical_ss"
	};
	
	/**
	 * the number of days to subtract from an event to designate when Registration opens
	 */
	public static final int EVENT_REGISTRATION_OPENS = -90;
	
	
	/**
	 * These are used to create reader friendly versions of titles that are
	 * stored in solr in non reader friendly formats.
	 * @author root
	 *
	 */
	public enum filterNameFormat {
		COURSE_CAL("Courses & Events"),
		HUDDLE_BLOG("News"),
		HUDDLE_CONSULTANTS("Consultants"),
		MEDIA_BIN("Documents"),
		PRODUCT("Products");
		
		private String name;
		filterNameFormat(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
	}
	
	
	
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
		Spine("Spine", "DSP","spine", "000942"),
		Cmf("CMF","CMF","","001221"),
		Codman("Codman Neuro","CDM","codman-neuro","000945"),
		Ethicon("Ethicon","ETH","ethicon","001220","001510"),
		JointRecon("Joint Reconstruction","DJT","joint-reconstruction","000940"),
		PowerTools("Power Tools","PWT", "", "001221"),
		SportsMed("Sports Medicine","SPM","sports-medicine","001225"),
		Trauma("Trauma","DJT", "", "001221");

		private String name;
		private String abbr;
		private String pageAlias;
		private String[] businessUnits;
		SSOBusinessUnit(String name, String abbr, String pageAlias, String... businessUnits) {
			this.name = name;
			this.abbr = abbr;
			this.pageAlias = pageAlias;
			this.businessUnits = businessUnits;
		}
		public String getName() { return name;	}
		public String getAbbreviation() { return abbr; }
		public String getPageAlias() { return pageAlias; }
		public String[] getBusinessUnits() { return businessUnits; }
	}
	
	
	public static String getBusUnitNm(String mrcCode) {
		SSOBusinessUnit bu = getSSOBusinessUnit(mrcCode);
		return (bu != null) ? bu.getName() : null;
	}
	
	public static String getBusUnitNmFromAbbr(String abbr) {
		for (SSOBusinessUnit vo : SSOBusinessUnit.values()) {
			if (vo.getAbbreviation().equalsIgnoreCase(abbr))
				return vo.getName();
		}
		return null;
	}
	
	public static SSOBusinessUnit getSSOBusinessUnit(String mrcCode) {
		for (SSOBusinessUnit vo : SSOBusinessUnit.values()) {
			if (Arrays.asList(vo.getBusinessUnits()).contains(mrcCode))
				return vo;
		}
		return null;
	}
	
	
	public static void determineSortParameters(SMTServletRequest req) {
		determineSortParameters(req, "titleAZ");
	}
	

	/**
	 * Get the sort type and rpp from cookies and assign them to the request
	 * @param req
	 */
	public static void determineSortParameters(SMTServletRequest req, String defaultSort) {
		if (req.getCookie(HuddleUtils.RPP_COOKIE) != null)
			req.setParameter("rpp", req.getCookie(HuddleUtils.RPP_COOKIE).getValue());

		Cookie sortCook = req.getCookie(HuddleUtils.SORT_COOKIE);
		String sort = (sortCook != null) ? sortCook.getValue() : defaultSort;
		
		setSearchParameters(req, sort);
	}

	
	/**
	 * Set the sort field and direction.
	 */
	public static void setSearchParameters(SMTServletRequest req, String sort) {
		if ("recentlyAdded".equals(sort)) {
			req.setParameter("fieldSort", SearchDocumentHandler.UPDATE_DATE, true);
			req.setParameter("sortDirection", ORDER.desc.toString(), true);
		} else if ("titleZA".equals(sort)) {
			req.setParameter("fieldSort", SearchDocumentHandler.TITLE_LCASE, true);
			req.setParameter("sortDirection", ORDER.desc.toString(), true);
		} else if ("titleAZ".equals(sort)) {
			req.setParameter("fieldSort", SearchDocumentHandler.TITLE_LCASE, true);
			req.setParameter("sortDirection", ORDER.asc.toString(), true);
		}
	}
}
