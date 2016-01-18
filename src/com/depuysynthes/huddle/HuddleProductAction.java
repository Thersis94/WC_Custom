package com.depuysynthes.huddle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.Cookie;

import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.common.SolrDocument;

import com.depuysynthes.action.MediaBinAssetVO;
import com.depuysynthes.lucene.MediaBinSolrIndex;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.commerce.catalog.ProductAttributeContainer;
import com.siliconmtn.commerce.catalog.ProductAttributeVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserRoleVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.action.search.SolrActionVO;
import com.smt.sitebuilder.action.search.SolrFieldVO;
import com.smt.sitebuilder.action.search.SolrFieldVO.FieldType;
import com.smt.sitebuilder.action.search.SolrQueryProcessor;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.action.search.SolrFieldVO.BooleanType;
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
	
	public HuddleProductAction() {
		super();
	}

	public HuddleProductAction(ActionInitVO arg0) {
		super(arg0);
	}

	@Override
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}

	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String param1 = req.getParameter("reqParam_1");
		
		if (param1 != null && !"".equals(param1) && mod.getDisplayColumn().equals(page.getDefaultColumn())) {
			detailSearch(req);
			
		} else {
			req.setParameter("reqParam_1", "", true);
			listSearch(req, mod.getDisplayColumn().equals(page.getDefaultColumn()));

			//put reqParam_1 back on the request, it wasn't meant for us
			if (param1 != null)
				req.setParameter("reqParam_1", param1, true);
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
		if (resp == null || resp.getTotalResponses() == 0) 
			throw new ActionException("No product found with documentId: " + req.getParameter("reqParam_1"));
		
		ProductVO p = new ProductVO();
		SolrDocument doc = resp.getResultDocuments().get(0);
		p.setProductId((String) doc.getFieldValue(SearchDocumentHandler.DOCUMENT_ID));
		p.setTitle((String) doc.getFieldValue(SearchDocumentHandler.TITLE));
		p.setDescText((String) doc.getFieldValue(SearchDocumentHandler.SUMMARY));
		
		ProductAttributeContainer pac = new ProductAttributeContainer();
		List<Node> images = new ArrayList<>();
		
		// Loop through all items on the document looking for any prefixed with attribute types
		for (String key : doc.getFieldValuesMap().keySet()) {
			
			if (key == null) continue;
			if (key.startsWith(HuddleUtils.PROD_ATTR_IMG_PREFIX)) {
				// keys starting with the image prefix are added to the ProductAttributeContainer
				// and are used to create the product's image gallery.
				for (Object o : doc.getFieldValues(key)) {
					if (o == null) continue;
					ProductAttributeVO attr = new ProductAttributeVO();
					attr.setAttributeName(key.replace(HuddleUtils.PROD_ATTR_IMG_PREFIX, ""));
					attr.setValueText(o.toString());
					attr.setAttributeType(HuddleUtils.PROD_ATTR_IMG_TYPE);
					Node n = new Node(key, null);
					n.setUserObject(attr);
					images.add(n);
				}
			} else if (key.startsWith(HuddleUtils.PROD_ATTR_MB_PREFIX)) {
				// Keys starting with the mediabin prefix need to be sent out
				// to solr in order to get all the documents with those ids
				p.addProdAttribute(key.replace(HuddleUtils.PROD_ATTR_MB_PREFIX, "").replace("_ss", ""), buildMediabin(req, doc.getFieldValues(key)));
			}
		}
		pac.addAll(images);
		p.setAttributes(pac);
		super.putModuleData(p);
	}


	/**
	 * Build a list of mediabin assets for the product
	 * @param p
	 * @param req
	 * @param mediabin
	 * @return 
	 */
	private List<MediaBinAssetVO> buildMediabin(SMTServletRequest req, Collection<Object> mediaBinAssets) {
		SiteVO siteData = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		UserRoleVO role = (UserRoleVO)req.getSession().getAttribute(Constants.ROLE_DATA);
		SolrResponseVO resp = getMBSolrDocs(siteData.getOrganizationId(), mediaBinAssets, role.getRoleLevel());

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
		
		return assets;
	}


	/**
	 * Get all the mediabin solr documents associated with this product
	 * @param organizationId
	 * @param mediabin
	 * @return
	 */
	private SolrResponseVO getMBSolrDocs(String organizationId, Collection<Object> mediabin, int roleLevel) {
		SolrQueryProcessor sqp = new SolrQueryProcessor(attributes, (String) attributes.get(Constants.SOLR_COLLECTION_NAME));
		SolrActionVO qData = new SolrActionVO();
		for (Object o : mediabin) {
			SolrFieldVO field = new SolrFieldVO();
			field.setBooleanType(BooleanType.OR);
			field.setFieldType(FieldType.SEARCH);
			field.setFieldCode(SearchDocumentHandler.DOCUMENT_ID);
			field.setValue((String) o);
			qData.addSolrField(field);
		}

		qData.setNumberResponses(mediabin.size());
		qData.setStartLocation(0);
		qData.setRoleLevel(roleLevel);
		qData.setOrganizationId(organizationId);
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
			if (req.getCookie(HuddleUtils.RPP_COOKIE) != null)
				req.setParameter("rpp", req.getCookie(HuddleUtils.RPP_COOKIE).getValue());

			Cookie sortCook = req.getCookie(HuddleUtils.SORT_COOKIE);
			String sort = (sortCook != null) ? sortCook.getValue() : "titleAZ";

			// Called on the category page of the site.
			// Turns the category parameter into a hierarchy fq
			if (req.hasParameter("category") && !req.hasParameter("fq")) {
				String category = req.getParameter("category");
				req.setParameter("fq", SearchDocumentHandler.HIERARCHY + ":" + StringUtil.capitalizePhrase(category));
			}

			// Called on the specialty page of the site.
			// Turns the speciality parameter into an opco fq. 
			if (req.hasParameter("specialty")) {
				req.setParameter("fq", HuddleUtils.SOLR_OPCO_FIELD + ":" + req.getParameter("specialty"));
			}

			// Called on the speciality home pages of the site.
			// Uses the last section of the request uri to determine the 
			// speciality of the page that the portlet is on and make an opco fq
			if (!req.hasParameter("fq")) {
				String uri = req.getRequestURI().substring(req.getRequestURI().lastIndexOf("/")+1).replace('-', ' ');
				req.setParameter("fq", HuddleUtils.SOLR_OPCO_FIELD + ":" + StringUtil.capitalizePhrase(uri));
				// This scenario ignores the user's sort preference to show new products 
				// on the home page.  Sort by recentlyAdded first
				sort = "recentlyAdded";
			}


			if ("recentlyAdded".equals(sort)) {
				req.setParameter("fieldSort", SearchDocumentHandler.UPDATE_DATE, true);
				req.setParameter("sortDirection", ORDER.desc.toString(), true);
			} else if ("titleZA".equals(sort)) {
				req.setParameter("fieldSort", SearchDocumentHandler.TITLE_SORT, true);
				req.setParameter("sortDirection", ORDER.desc.toString(), true);
			} else if ("titleAZ".equals(sort)) {
				req.setParameter("fieldSort", SearchDocumentHandler.TITLE_SORT, true);
				req.setParameter("sortDirection", ORDER.asc.toString(), true);
			}
		}

		SMTActionInterface sai = new SolrAction(actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		sai.retrieve(req);
	}
}
