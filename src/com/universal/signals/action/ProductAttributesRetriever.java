package com.universal.signals.action;

// Java 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;

// WebCrescendo 2.0
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title: </b>ProductAttributesRetriever.java <p/>
 * <b>Project: </b>WC_Custom <p/>
 * <b>Description: </b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2014<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0<p/>
 * @since Dec 30, 2014<p/>
 *<b>Changes: </b>
 * Dec 30, 2014: David Bargerhuff: Created class.
 ****************************************************************************/
public class ProductAttributesRetriever extends SimpleActionAdapter {
	
	public ProductAttributesRetriever() {
		super();
	}
	
	public ProductAttributesRetriever(ActionInitVO ai) {
		super(ai);
	}
	
	/**
	 * Retrieves the product attribute hierarchy based on the product ID
	 * and the parent (product attribute) ID passed on the request.  Returns
	 * a List of ProductAttributeVOs representing the attributes in the hierarchy.
	 */
	public void retrieve(ActionRequest req) {
		String productId = StringUtil.checkVal(req.getParameter("productId"));
		String parentId = StringUtil.checkVal(req.getParameter("parentId"));
		String childLevel = StringUtil.checkVal(req.getParameter("childLevel"));
		String productPrefix = parseProductPrefix(req);
		//log.debug("productId|parentId|childLevel: " + "|" + productPrefix + productId + "|" + parentId + "|" + childLevel);
		StringBuilder sb = new StringBuilder();
		sb.append("select PRODUCT_ATTRIBUTE_ID, ATTRIB1_TXT ");
		sb.append("from PRODUCT_ATTRIBUTE_XR where PRODUCT_ID = ? ");
		sb.append("and PARENT_ID = ? and ATTRIB2_TXT = ? order by ORDER_NO");
		//log.debug("ProductAttributesRetriever SQL: " + sb.toString());
		PreparedStatement ps = null;
		Map<String,String> pmAttrs = new LinkedHashMap<>();
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, productPrefix + productId);
			ps.setString(2, parentId);
			ps.setString(3, childLevel);
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				pmAttrs.put(rs.getString("PRODUCT_ATTRIBUTE_ID"), formatChildAttribute(rs.getString("ATTRIB1_TXT")));
			}
						
		} catch (SQLException sqle) {
			pmAttrs.put("", "---");
			log.error("Error retrieving product attributes for parent ID: " + parentId + ", ", sqle);
		} finally {
			DBUtil.close(ps);
		}

		// place data on module map
		ModuleVO mod = new ModuleVO();
		mod.setActionData(pmAttrs);
		mod.setDataSize(pmAttrs.size());
		this.setAttribute(Constants.MODULE_DATA, mod);
	}
	
	/**
	 * Returns the site ID suffixed with an underscore.  This is pre-pended to the
	 * product ID to be used when querying for product attributes.
	 * @param req
	 * @return
	 */
	private String parseProductPrefix(ActionRequest req) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		return site.getSiteId() + "_";
	}
	
	/**
	 * Formats the raw attribute value passed in and returns a String that has
	 * had certain characters/character sequences replaced.  This matches the 
	 * replacement functions used in the JSTL views.
	 * @param rawVal
	 * @return
	 */
	private String formatChildAttribute(String rawVal) {
		String newVal = StringUtil.checkVal(rawVal);
		newVal = newVal.replace("@", "");
		newVal = newVal.replace("\"\"", "''");
		newVal = newVal.replace("\"", "");
		return newVal;
	}

}
