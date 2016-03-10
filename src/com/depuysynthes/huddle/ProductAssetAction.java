package com.depuysynthes.huddle;

import org.apache.solr.common.SolrDocument;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserRoleVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.search.SolrActionVO;
import com.smt.sitebuilder.action.search.SolrFieldVO;
import com.smt.sitebuilder.action.search.SolrQueryProcessor;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.action.search.SolrFieldVO.BooleanType;
import com.smt.sitebuilder.action.search.SolrFieldVO.FieldType;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;

/****************************************************************************
 * <b>Title</b>: ProductAssetAction.java<p/>
 * <b>Description: Wraps the display of a CMS/MediaBin asset with a secondary 
 * lookup for the first product tied to this asset; so we can print the product name 
 * and link at the top of the page.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 18, 2016
 ****************************************************************************/
public class ProductAssetAction extends SimpleActionAdapter {

	public ProductAssetAction() {
		super();
	}

	public ProductAssetAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		UserRoleVO role = (UserRoleVO)req.getSession().getAttribute(Constants.ROLE_DATA);
		String documentId = StringUtil.checkVal(req.getParameter(SMTServletRequest.PARAMETER_KEY + "1"));
		
		SolrDocument resp = querySolr(mod.getOrganizationId(), documentId, role.getRoleLevel(), false, null);
		req.setAttribute("assetSolrDoc", resp);
		
		//don't bother with the product lookup if the asset is not there.
		if (resp != null) {
			//look at the referring page; if it was a product let's try and find that one instead of the 'first available'.
			String referer = StringUtil.checkVal(req.getHeader("Referer"));
			log.debug("referer=" + referer);
			String productAlias = null;
			int idx = referer.indexOf(HuddleUtils.PRODUCT_PG_ALIAS + getAttribute(Constants.QS_PATH));
			if (idx > -1) {
				productAlias = referer.substring(idx+(HuddleUtils.PRODUCT_PG_ALIAS + getAttribute(Constants.QS_PATH)).length());
			}
			req.setAttribute("productSolrDoc", querySolr(mod.getOrganizationId(), documentId, role.getRoleLevel(), true, productAlias));
			//overwrite the browser title
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			page.setTitleName(resp.getFieldValue(SearchDocumentHandler.TITLE).toString());
		}
	}
	

	
	/**
	 * Queries Solr in support of 1 of 2 scenarios; 
	 * 1) The mediabin or CMS asset in question
	 * 2) The first product affiliated to this mediabin/CMS asset
	 * @param organizationId
	 * @param documentId
	 * @param roleLevel
	 * @param isProduct
	 * @return
	 */
	private SolrDocument querySolr(String organizationId, String documentId, int roleLevel, boolean isProduct, String productAlias) {
		SolrQueryProcessor sqp = new SolrQueryProcessor(getAttributes(), getAttribute(Constants.SOLR_COLLECTION_NAME).toString());
		SolrActionVO qData = new SolrActionVO();
		
		if (!isProduct) { //query for mediabin or CMS asset by documentId (solr primary key)
			SolrFieldVO field = new SolrFieldVO();
			field.setBooleanType(BooleanType.AND);
			field.setFieldType(FieldType.SEARCH);
			field.setFieldCode(SearchDocumentHandler.DOCUMENT_ID);
			field.setValue(documentId);
			qData.addSolrField(field);
			
		} else if (productAlias != null && productAlias.length() > 0) {
			log.debug("searching productAlias: " + productAlias);
			//query product using documentUrl provided
			SolrFieldVO field = new SolrFieldVO();
			field.setBooleanType(BooleanType.AND);
			field.setFieldType(FieldType.SEARCH);
			field.setFieldCode(SearchDocumentHandler.DOCUMENT_URL);
			field.setValue(productAlias);
			qData.addSolrField(field);
			
		} else {
			//for product queries with no documentUrl passed, we need to search for documentId across product attribute fields
			//this needs to use an eDisMax query
			qData.setSearchData(documentId);
			
			//define the eDisMax fields we want to search across
			for (String fieldNm : HuddleUtils.getProductAttributeSolrFields(dbConn, organizationId)) {
				SolrFieldVO field = new SolrFieldVO();
				field.setFieldType(FieldType.BOOST);
				field.setFieldCode(fieldNm + "_ss"); //the _ss here gets appended by SolrActionUtil via annotations when we index.  That's why it's not coming out of HuddleUtils
				qData.addSolrField(field);
			}
			
			//add a filter constraint for indexType
			SolrFieldVO field = new SolrFieldVO();
			field.setBooleanType(BooleanType.AND);
			field.setFieldType(FieldType.FILTER);
			field.setFieldCode(SearchDocumentHandler.INDEX_TYPE);
			field.setValue(HuddleUtils.IndexType.PRODUCT.toString());
			qData.addSolrField(field);
		}

		qData.setNumberResponses(1);
		qData.setStartLocation(0);
		qData.setRoleLevel(roleLevel);
		qData.setOrganizationId(organizationId);
		SolrResponseVO resp = sqp.processQuery(qData);
		
		if (resp != null && resp.getResultDocuments().size() > 0) {
			return resp.getResultDocuments().get(0);
		} else {
			return null;
		}
	}
}