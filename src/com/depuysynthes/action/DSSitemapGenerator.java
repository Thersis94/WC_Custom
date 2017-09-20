package com.depuysynthes.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.commerce.catalog.ProductCategoryVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.security.UserRoleVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.menu.MenuObj;
import com.smt.sitebuilder.action.menu.SitemapGenerator;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;


/*****************************************************************************
<p><b>Title</b>: DSSitemapGenerator</p>
<p>Extends the WC core sitemap loader with DS-specific features.</p>
@author James McKain
@version 1.0
@since Sep 19, 2017
Code Updates
 ***************************************************************************/
public class DSSitemapGenerator extends SitemapGenerator {

	private static final String HCP_PREFIX = "/hcp/";
	protected String qsPath;


	public DSSitemapGenerator(Map<String, Object> attributes, SMTDBConnection dbConn) {
		super(attributes, dbConn);
		qsPath = (String) attributes.get(Constants.QS_PATH);
	}


	/**
	 * overload the Template Method Pattern of the superclass to add some custom DePuy pages to the sitemap.
	 */
	@Override
	public List<Node> loadCustomLowPriPages(ActionRequest req, SiteVO site, UserRoleVO role) {
		List<Node> pages = null;

		// Retrieve the custom Products & Procedures for DS & DS-EMEA (only)
		if ("DPY_SYN".equals(site.getOrganizationId())) {
			pages = loadDSProducts(req, "DS_PRODUCTS", "/products/" + qsPath);
			pages.addAll(loadDSProducts(req, "DS_PROCEDURES", "/procedures/" + qsPath));
		} else if ("DPY_SYN_EMEA".equals(site.getOrganizationId())) {
			pages = loadDSProducts(req, "DS_PRODUCTS_EMEA", "/products/" + qsPath);
			pages.addAll(loadDSProducts(req, "DS_PROCEDURES_EMEA", "/procedures/" + qsPath));
		} else {
			pages = super.loadCustomLowPriPages(req, site, role);
		}

		return pages;
	}


	/**
	 * loads all the Product & Procedure URLs from the DS product catalogs.
	 * @param site
	 * @param req
	 * @return
	 */
	protected List<Node> loadDSProducts(ActionRequest req, String catalogId, String sitePg) {
		List<Node> data = new ArrayList<>();
		ProductCatalogUtil pc = new ProductCatalogUtil();
		pc.setDBConnection(dbConn);

		try {
			Tree prodTree = pc.loadCatalog(catalogId, null, false, req);
			List<String> completed = new ArrayList<>();
			String divisionUrl = null;
			String countryNodeId = null;

			for (Node n : prodTree.preorderList()) {
				ProductCategoryVO vo = (ProductCategoryVO) n.getUserObject();

				//top level categories define our countries
				if (n.getParentId() == null) {
					countryNodeId = n.getNodeId();
					continue; //these are not products (to index)
				} else if (n.getParentId().equals(countryNodeId)) {
					divisionUrl = vo.getUrlAlias();
					continue; //these are child categories, not products (to index)
				} else if (StringUtil.isEmpty(vo.getUrlAlias()) || completed.contains(divisionUrl + vo.getUrlAlias())) {
					continue;
				}

				MenuObj mo = new MenuObj();
				mo.setFullPath(HCP_PREFIX + divisionUrl + sitePg + vo.getUrlAlias());
				mo.setLastModified(vo.getLastUpdate());
				mo.setFileExtension("");
				mo.setContextName(contextPath);

				n.setUserObject(mo);
				data.add(n);
				completed.add(divisionUrl + vo.getUrlAlias());
			}
		} catch(Exception e) {
			log.error("Unable to get Products", e);
		}

		return data;
	}
}
