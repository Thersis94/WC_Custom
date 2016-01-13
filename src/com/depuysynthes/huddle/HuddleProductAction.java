package com.depuysynthes.huddle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.apache.solr.common.SolrDocument;

import com.depuysynthes.action.MediaBinAssetVO;
import com.depuysynthes.lucene.MediaBinSolrIndex;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.action.search.SolrActionIndexVO;
import com.smt.sitebuilder.action.search.SolrActionVO;
import com.smt.sitebuilder.action.search.SolrQueryProcessor;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;

/****************************************************************************
* <b>Title</b>: HuddleProductAction.java <p/>
* <b>Project</b>: WebCrescendo <p/>
* <b>Description: Wrapper for the solr search that translates all cookies and
* request parameters into solr usable values.</b> 
* <p/>
* <b>Copyright:</b> Copyright (c) 2016<p/>
* <b>Company:</b> Silicon Mountain Technologies<p/>
* @author Eric Damschroder
* @version 1.0
* @since Jan 11, 2016<p/>
****************************************************************************/

public class HuddleProductAction extends SimpleActionAdapter {
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		if (req.hasParameter("reqParam_1") && mod.getDisplayColumn().equals(page.getDefaultColumn())) {
			detailSearch(req);
		} else {
			req.setParameter("reqParam_1", "", true);
			listSearch(req, mod.getDisplayColumn().equals(page.getDefaultColumn()));
		}
	}
	
	
	/**
	 * Build a product vo from information retrieved from solr.
	 * @param req
	 * @throws ActionException
	 */
	private void detailSearch(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		actionInit.setActionId((String)mod.getAttribute(SBModuleVO.ATTRIBUTE_1));
		SolrAction sa = new SolrAction(actionInit);
		sa.setAttributes(attributes);
		sa.setDBConnection(dbConn);
		sa.retrieve(req);
	
		//get the response object back from SolrAction
		mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		SolrResponseVO resp = (SolrResponseVO) mod.getActionData();
		if (resp == null || resp.getTotalResponses() == 0) throw new ActionException("No product found with documentId: " + req.getParameter("reqParam_1"));
		ProductVO p = new ProductVO();
		SolrDocument doc = resp.getResultDocuments().get(0);
		p.setProductId((String) doc.getFieldValue(SearchDocumentHandler.DOCUMENT_ID));
		p.setTitle((String) doc.getFieldValue(SearchDocumentHandler.TITLE));
		p.setDescText((String) doc.getFieldValue(SearchDocumentHandler.SUMMARY));
		p.addProdAttribute(HuddleUtils.PROD_ATTR_CLINICAL, doc.getFieldValues(HuddleUtils.PROD_ATTR_CLINICAL));
		p.addProdAttribute(HuddleUtils.PROD_ATTR_IMAGE, doc.getFieldValues(HuddleUtils.PROD_ATTR_IMAGE));
		p.addProdAttribute(HuddleUtils.PROD_ATTR_VALUE, doc.getFieldValues(HuddleUtils.PROD_ATTR_VALUE));
		p.addProdAttribute(HuddleUtils.PROD_ATTR_COMPETITION, doc.getFieldValues(HuddleUtils.PROD_ATTR_COMPETITION));
		p.addProdAttribute(HuddleUtils.PROD_ATTR_SELLING_TIPS, doc.getFieldValues(HuddleUtils.PROD_ATTR_SELLING_TIPS));
		p.addProdAttribute(HuddleUtils.SOLR_OPCO_FIELD, doc.getFieldValue(HuddleUtils.SOLR_OPCO_FIELD));
		
		Collection<Object> mediabin = doc.getFieldValues(HuddleUtils.PROD_ATTR_SYSTEM);
		// If there are no mediabin items the method can exit here
		if (mediabin == null) {
			super.putModuleData(p);
			return;
		} else {
			buildMediabin(p, req, mediabin);
		}
	}


	/**
	 * Build a list of mediabin assets for the product
	 * @param p
	 * @param req
	 * @param mediabin
	 */
	private void buildMediabin(ProductVO p, SMTServletRequest req, Collection<Object> mediabin) {
		SiteVO siteData = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		SolrResponseVO resp = getSolrDocs(siteData.getOrganizationId(), mediabin);
		
		List<MediaBinAssetVO> assets = new ArrayList<>();
		for (SolrDocument d : resp.getResultDocuments()) {
			MediaBinAssetVO asset = new MediaBinAssetVO();
			asset.setActionUrl((String) d.getFieldValue(SearchDocumentHandler.DOCUMENT_URL));
			asset.setActionId((String) d.getFieldValue(SearchDocumentHandler.DOCUMENT_ID));
			asset.setActionName((String) d.getFieldValue(SearchDocumentHandler.TITLE));
			asset.setAssetType((String) d.getFieldValue(MediaBinSolrIndex.MediaBinField.AssetType.getField()));
			asset.setFileSizeNo(Convert.formatInteger(d.getFieldValue(SearchDocumentHandler.FILE_SIZE).toString()));
			asset.setFileNm((String) d.getFieldValue(SearchDocumentHandler.FILE_NAME));
			assets.add(asset);
		}
		p.addProdAttribute(HuddleUtils.PROD_ATTR_SYSTEM, assets);
		
		super.putModuleData(p);
	}


	/**
	 * Get all the mediabin solr documents associated with this product
	 * @param organizationId
	 * @param mediabin
	 * @return
	 */
	private SolrResponseVO getSolrDocs(String organizationId, Collection<Object> mediabin) {
		StringBuilder f = new StringBuilder();
		for (Object o : mediabin) {
			if (f.length() > 0) f.append(" OR ").append(SearchDocumentHandler.DOCUMENT_ID).append(":");
			f.append(o);
		}
	
		SolrQueryProcessor sqp = new SolrQueryProcessor(attributes, (String) attributes.get(Constants.SOLR_COLLECTION_NAME));
		SolrActionVO qData = new SolrActionVO();
		qData.setNumberResponses(200);
		qData.setStartLocation(0);
		qData.setRoleLevel(0);
		qData.setOrganizationId(organizationId);
		qData.addIndexType(new SolrActionIndexVO("", MediaBinSolrIndex.INDEX_TYPE));
		Map<String, String> filter = new HashMap<>();
		filter.put(SearchDocumentHandler.DOCUMENT_ID, f.toString());
		qData.setFilterQueries(filter);
		return sqp.processQuery(qData);
	}


	/**
	 * Get the list of products from solr that fit the supplied filters
	 * @param req
	 * @param mainCol
	 * @throws ActionException
	 */
	private void listSearch(SMTServletRequest req, boolean mainCol) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		req.setParameter("fmid", mod.getPageModuleId());
		
		String solrActionId = StringUtil.checkVal(mod.getAttribute(SBModuleVO.ATTRIBUTE_1));
		actionInit.setActionId(solrActionId);
		
		// Only add filters if this is the main portlet on the page.
		if (mainCol) {
			if (req.hasParameter("category") && !req.hasParameter("fq")) {
				req.setParameter("fq", "hierarchy:" + req.getParameter("category"));
			}
			
			if (req.hasParameter("specialty")) {
				req.setParameter("fq", "opco_ss:" + req.getParameter("specialty"));
			}
			
			req.setParameter("rpp", req.getCookie(HuddleUtils.RPP_COOKIE).getValue());
			
			Cookie sort = req.getCookie(HuddleUtils.SORT_COOKIE);
			
			if (sort == null) {
				// Default to normal sort
			} else if ("recentlyAdded".equals(sort.getValue())) {
				req.setParameter("fieldSort", "updateDate", true);
				req.setParameter("sortDirection", "desc", true);
			} else if ("titleZA".equals(sort.getValue())) {
				req.setParameter("fieldSort", "title_sort", true);
				req.setParameter("sortDirection", "desc", true);
			} else if ("titleAZ".equals(sort.getValue())) {
				req.setParameter("fieldSort", "title_sort", true);
				req.setParameter("sortDirection", "asc", true);
			}
		}
		
		SMTActionInterface sai = new SolrAction(actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		sai.retrieve(req);
	}
}
