package com.depuysynthes.huddle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.solr.common.SolrDocument;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.search.SolrActionVO;
import com.smt.sitebuilder.action.search.SolrFieldVO;
import com.smt.sitebuilder.action.search.SolrQueryProcessor;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.action.search.SolrFieldVO.BooleanType;
import com.smt.sitebuilder.action.search.SolrFieldVO.FieldType;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>: SpecialtyProductsAction.java<p/>
 * <b>Description: Allows the admin to pick and choose which products appear on the
 * Specialty homepage.  There's support in SolrAction to do the same thing dynamically 
 * using a solr query.  Both actions use the same View (JSP).</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Feb 29, 2016
 ****************************************************************************/
public class SpecialtyProductsAction extends SimpleActionAdapter {

	public SpecialtyProductsAction() {
	}

	public SpecialtyProductsAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
		ModuleVO mod = (ModuleVO) getAttribute(AdminConstants.ADMIN_MODULE_DATA);
		
		//preserve values on form reloads.
		if (req.hasParameter("setSpecialty")) {
			mod.setActionId(req.getParameter("sbActionId"));
			mod.setActionName(req.getParameter("name"));
			mod.setActionDesc(req.getParameter("desc"));
			mod.setAttribute(SBModuleVO.ATTRIBUTE_1, req.getParameter("setSpecialty"));
			log.debug("MOD=" + mod);
			super.putModuleData(mod, 0, true);
		}
		
		SBModuleVO sbMod = (SBModuleVO) mod.getActionData();
		if (sbMod != null && sbMod.getAttribute(SBModuleVO.ATTRIBUTE_1) != null) {
			req.setAttribute("productMap", loadProducts((String)sbMod.getAttribute(SBModuleVO.ATTRIBUTE_1)));
			req.setAttribute("selProdsArr", StringUtil.checkVal(sbMod.getIntroText()).split(","));
		}
		
	}
	
	
	/**
	 * returns a map of the products presented in the dropdown menus.
	 * Done here in the action instead of the View (JSTL) so we can reuse the list
	 * for both columns without having to iterated an RS twice.
	 * @param categoryCd
	 * @return
	 */
	private Map<String, String> loadProducts(String categoryCd) {
		Map<String, String> data = new LinkedHashMap<>(200);
		StringBuilder sql = new StringBuilder(300);
		sql.append("select a.PRODUCT_ID, a.PRODUCT_NM from PRODUCT a ");
		sql.append("inner join PRODUCT_CATEGORY_XR b on a.PRODUCT_ID=b.PRODUCT_ID and b.PRODUCT_CATEGORY_CD=? ");
		sql.append("where (len(a.product_group_id)=0 or a.product_group_id is null) ");
		sql.append("and PRODUCT_CATALOG_ID=? ");
		sql.append("order by PRODUCT_NM");
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, categoryCd);
			ps.setString(2, HuddleUtils.CATALOG_ID);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				data.put(rs.getString(1), rs.getString(2));
			
		} catch (SQLException sqle) {
			log.error("could not load products", sqle);
		}
		
		return data;
	}

	
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		if (mod.getIntroText() == null || mod.getIntroText().length() == 0) return; //no products to load
		
		//create a map of the search results in the order we want them to display on the website.
		Map<String, SolrDocument> products = new LinkedHashMap<>();
		for (String s : mod.getIntroText().split(",")) {
			if (s == null || s.length() == 0) continue;
			products.put(s.trim(), null);
		}
		
		//load the solr assets for each of the products we need.
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		super.putModuleData(loadSolrAssets(role, products));
	}
	
	
	/**
	 * loads the solr documents (products) for the records we need to display
	 * @param favs
	 * @return
	 */
	protected Map<String, SolrDocument> loadSolrAssets(SBUserRole role, Map<String, SolrDocument> products) {
		SolrActionVO qData = new SolrActionVO();
		qData.setOrganizationId(role.getOrganizationId());
		qData.setRoleLevel(role.getRoleLevel());
		qData.setNumberResponses(products.size()); //all
		
		//add the documentId's of the favorites to the query
		for (String prodId : products.keySet()) {
			log.debug("adding productId: " + prodId);
			SolrFieldVO field = new SolrFieldVO();
			field.setBooleanType(BooleanType.OR);
			field.setFieldType(FieldType.SEARCH);
			field.setFieldCode(SearchDocumentHandler.DOCUMENT_ID);
			field.setValue(prodId);
			qData.addSolrField(field);
		}
		
		SolrQueryProcessor sqp = new SolrQueryProcessor(getAttributes());
		SolrResponseVO solrResp =  sqp.processQuery(qData);
		for (SolrDocument sd : solrResp.getResultDocuments())
			products.put("" + sd.get(SearchDocumentHandler.DOCUMENT_ID), sd);
		
		return products;
	}
}