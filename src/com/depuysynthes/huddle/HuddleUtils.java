package com.depuysynthes.huddle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.servlet.http.Cookie;

import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.response.FacetField;

import com.siliconmtn.commerce.catalog.ProductAttributeVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.search.SearchDocumentHandler;

/****************************************************************************
 * <b>Title</b>: HuddleUtils.java<p/>
 * <b>Description: Utility methods and constants for DS Huddle.  
 * Commonly database pkIds and Solr constants that will rarely change.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 29, 2015
 ****************************************************************************/
public class HuddleUtils {
	
	/** page aliases **/
	public static final String PRODUCT_PG_ALIAS = "/product/"; //used in ProductAssetAction for referer inspection
	public static final String ASSET_PG_ALIAS = "/asset/"; //used in SolrBusinessRules & EmailAFriend
	public static final String MEDIABIN_REDIR_URL = "/json?amid=MEDIA_BIN_AJAX&mbid="; //used in EmailAFriend
	
	/** Catalog Constants **/
	public static final String productCategoryCd = "HUDDLE_CATEGORY";
	public static final String productCatalogId = "DS_HUDDLE";
	
	/** Solr field names of product attributes **/
	public static final String PROD_ATTR_PREFIX = "huddle_";

	//product attribute types - come from the PRODUCT_ATTRIBUTE_TYPE database table
	public static final String PROD_ATTR_IMG_TYPE = "IMAGE";
	public static final String PROD_ATTR_HTML_TYPE = "HTML";
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
	public static final String GRID_VIEW_COOKIE = "huddleGridView";

	//product catalog constants
	public static final String CATALOG_ID = "DS_HUDDLE";
	public static final String SPEC_PRODCAT_ID = "HUDDLE_SPECIALTY";
	public static final String CAT_PRODCAT_ID = "HUDDLE_CATEGORY";
	
	//solr fields
	public static final String SOLR_OPCO_FIELD = "opco_ss";
	public static final String SOLR_IMAGE_FIELD = "huddle_1|image|images_ss";
	private static String[] solrProdAttributeFields = null;
	/**
	 * leverage a method to evaluate whether we've already obtained the product attributes
	 * and stored them statically, or if we need to query the DB and load them up.
	 * @param dbConn
	 * @param orgId
	 * @return
	 */
	public static final String[] getProductAttributeSolrFields(SMTDBConnection dbConn, String orgId) {
		//leverage a static variable so we can store this fairly stable piece of data in memory and not have to query the DB every time
		if (solrProdAttributeFields == null) {
			Set<String> data = new HashSet<>();
			for (ProductAttributeVO vo: loadProductAttributes(dbConn, orgId))
				data.add(makeSolrNmFromProdAttrNm(vo.getDisplayOrderNo(), vo.getAttributeName(), vo.getAttributeType()));
			
			solrProdAttributeFields = data.toArray(new String[data.size()]);
		}
			
		
		return solrProdAttributeFields;
	}
	
	
	/**
	 * loads the sort order of the attributes since we could not get this accurately from the ProductAttributeController
	 */
	public static List<ProductAttributeVO> loadProductAttributes(SMTDBConnection dbConn, String orgId) {
		List<ProductAttributeVO> data = new ArrayList<>();
		String sql = "select attribute_id, attribute_nm, type_nm, display_order_no " +
				"from PRODUCT_ATTRIBUTE where organization_id=? and active_flg=1 and (attribute_group_id is null or len(attribute_group_id)=0)";
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, orgId);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				data.add(new ProductAttributeVO(rs));
		} catch (SQLException sqle) { }
		
		return data;
	}
	

	//solrize (for indexing)
	public static String makeSolrNmFromProdAttrNm(int order, String nm, String type) {
		return (PROD_ATTR_PREFIX + order + "|" +type +"|" + StringUtil.replace(nm, " ", "_")).toLowerCase();
	}
	//desolrize (for display)
	public static String makeProdAttrNmFromSolrNm(String nm) {
		nm = nm.toLowerCase();
		if (nm.endsWith("_ss")) 
			nm = nm.substring(0, nm.lastIndexOf("_ss")); //prune-off Solr's suffix
		
		nm = nm.substring(nm.lastIndexOf("|")+1);
		return StringUtil.replace(nm,"_"," ");
	}
	public static String makeProdAttrTypeFromSolrNm(String nm) {
		nm = nm.substring(nm.indexOf("|")+1); //removes the prefix and the <order>|
		return nm.substring(0, nm.indexOf("|"));
	}
	public static Integer makeProdAttrOrderFromSolrNm(String nm) {
		nm = nm.substring(0, nm.indexOf("|")+1); //removes the prefix and the <order>|
		return Convert.formatInteger(StringUtil.replace(nm,PROD_ATTR_PREFIX,""));
	}
	
	
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
	public enum IndexType {
		COURSE_CAL("Courses & Events"),
		HUDDLE_BLOG("News"),
		HUDDLE_CONSULTANTS("Consultants"),
		HUDDLE_PRODUCT_CONTACT("Product Contacts"),
		MEDIA_BIN("Documents"),
		PRODUCT("Products"),
		CMS_QUICKSTREAM("Documents"),
		FORM("Forms"),
		// Module Types used in the same way as index Types
		DOCUMENT("Documents"),
		EVENT("Courses & Events"),
		BLOG("News"),
		FORM_BUILDER("Forms");
		
		private String name;
		IndexType(String name) { this.name = name; }
		public String getName() { return name; }
		
		/**
		 * return a type without throwing an exception
		 * @param t
		 * @return
		 */
		public static IndexType quietValueOf(String t) {
			try {
				return IndexType.valueOf(t);
			} catch (Exception e) {
				return null;
			}
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
	
	
	/**
	 * ties specifically to Site Search.  We facet on moduleType, which are not the display values 
	 * used on the website.  This method substitutes in the proper display values
	 * and returns a reordered alphabetical list.
	 * @param solrResp
	 * @return
	 */
	public static Collection<GenericVO> facetModuleType(Collection<FacetField.Count> solrResp) {
		if (solrResp == null) return null;
		Map<String, GenericVO> records = new TreeMap<>();
		for (FacetField.Count c : solrResp) {
			if (c.getCount() == 0) continue;
			String key = IndexType.quietValueOf(c.getName()).getName(); //quietValueOf will not throw exceptions
			if (key == null || key.length() == 0) continue;
			records.put(key, new GenericVO(key, c));
		}
		return records.values();
	}
}
