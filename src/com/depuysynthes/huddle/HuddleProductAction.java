package com.depuysynthes.huddle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.SolrDocument;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserRoleVO;
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
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String param1 = StringUtil.checkVal(req.getParameter(ActionRequest.PARAMETER_KEY + "1"), null);

		if (param1 != null) {
			//load the details view for a single product
			detailSearch(req);
		} else {
			listSearch(req, mod.getDisplayColumn().equals(page.getDefaultColumn()));
		}
	}


	/**
	 * Build a product vo from information retrieved from solr.
	 * @param req
	 * @throws ActionException
	 */
	private void detailSearch(ActionRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		UserRoleVO role = (UserRoleVO)req.getSession().getAttribute(Constants.ROLE_DATA);
		
		//run a search for the PRODUCT using productAlias (documentUrl)
		req.setAttribute("searchField", SearchDocumentHandler.DOCUMENT_URL);
		actionInit.setActionId((String)mod.getAttribute(SBModuleVO.ATTRIBUTE_1));
		SolrAction sa = new SolrAction(actionInit);
		sa.setAttributes(attributes);
		sa.setDBConnection(dbConn);
		sa.retrieve(req);

		//verify the response object back from SolrAction
		mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		SolrResponseVO resp = (SolrResponseVO) mod.getActionData();
		if (resp == null || resp.getTotalResponses() == 0) {
			mod.setErrorCondition(true);
			mod.setActionData(null);
			setAttribute(Constants.MODULE_DATA, mod);
			return;
		}

		//make a ProductVO from the solr data
		HuddleProductVO p = new HuddleProductVO();
		SolrDocument doc = resp.getResultDocuments().get(0);
		SolrBusinessRules sd = new SolrBusinessRules();
		sd.setSolrDocument(doc);
		p.setProductId((String) doc.getFieldValue(SearchDocumentHandler.DOCUMENT_ID));
		p.setProductUrl((String) doc.getFieldValue(SearchDocumentHandler.DOCUMENT_URL));
		p.setTitle((String) doc.getFieldValue(SearchDocumentHandler.TITLE));
		p.setDescText((String) doc.getFieldValue(SearchDocumentHandler.SUMMARY));
		p.setCatalogId(sd.getFamilyName());
		
		//overwrite the browser title
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		page.setTitleName(p.getTitle());
				
		addProductAttributes(p, doc, req);

		//get the product contacts as well.
		//skip is passed from EmailFriendAction, where we don't need contacts
		if (!req.hasParameter("skipContacts"))
			addProductContacts(p, site.getOrganizationId(), role.getRoleLevel());
		
		super.putModuleData(p);
	}
	
	
	/**
	 * parse the solr document's attributes to determine which are product attibutes.
	 * do another Solr query if need, to load supporting information for the attributes.
	 * @param p
	 * @param doc
	 * @param req
	 */
	private void addProductAttributes(HuddleProductVO p, SolrDocument doc, ActionRequest req) {
		Map<String, Collection<Object>> subqueryList = new HashMap<>();
		
		// Loop through all items on the document looking for any prefixed with attribute types
		for (String key : doc.getFieldNames()) {
			log.debug("found product attribute: " + key);
			
			//verify it's one of our custom product attributes; we don't want all of Solr's junk
			if (!key.startsWith(HuddleUtils.PROD_ATTR_PREFIX)) continue;
			
			String name = HuddleUtils.makeProdAttrNmFromSolrNm(key);
			String type = HuddleUtils.makeProdAttrTypeFromSolrNm(key).toUpperCase();
			Integer order = HuddleUtils.makeProdAttrOrderFromSolrNm(key);
			log.debug(name + " | " + type + " | " + order);
			
			switch (type) {
				case HuddleUtils.PROD_ATTR_IMG_TYPE:
					List<String> images = new ArrayList<>();
					// keys starting with the image prefix are added to the ProductAttributeContainer
					// and are used to create the product's image gallery.
					for (Object o : doc.getFieldValues(key)) {
						if (o == null) continue;
						images.add(o.toString());
					}
					p.setImages(images);
					break;
				case HuddleUtils.PROD_ATTR_MB_TYPE:
					// Keys starting with the mediabin prefix need to be sent out
					// to solr in order to get all the documents with those ids
					subqueryList.put(name, doc.getFieldValues(key));
					p.addResource(order, name, null); //this will populate the sortMap for later so we know the order# of this attribute
					break;
				default:
					p.addResource(order, name, doc.getFieldValues(key));
			}
			log.debug("saved attribute " + name);
		}
		
		//now query for the dependent records (typically mediabin or CMS),  
		// then marry them back to the attribute they belong to
		if (subqueryList.size() > 0)
			runAssetLookup(p, subqueryList, req);
		
	}
	
	
	/**
	 * sends a suplimental Solr query for assets tied to this product
	 */
	private void runAssetLookup(HuddleProductVO p, Map<String, Collection<Object>> subqueryList, ActionRequest req) {
		//build a list of documentIds we need to lookup
		Map<String, SolrDocument> solrDocs = new HashMap<>();
		for (String name : subqueryList.keySet()) {
			for (Object o : subqueryList.get(name))
				solrDocs.put(o.toString(), null);
		}
		
		//query solr once, for all the attributes, then turn it into a Map for each lookup
		List<SolrDocument> solrResults = this.loadAttributeAssets(req, solrDocs.keySet());
		for (SolrDocument sd : solrResults)
			solrDocs.put(sd.getFieldValue(SearchDocumentHandler.DOCUMENT_ID).toString(), sd);
		
		// perform the marriages
		for (String name : subqueryList.keySet()) {
			Collection<Object> myDocs = new ArrayList<>(50);
			//loop the attributes objects, find each in the solr results, and add it to myDocs
			for (Object o : subqueryList.get(name)) {
				SolrDocument sd = solrDocs.get(o.toString());
				if (sd != null) myDocs.add(sd);
			}
			//put myDocs into this attribute's record (replaces the list of documentIds), then move on to the next
			log.debug("set " + myDocs.size() + " docs into " + name);
			p.addProdAttribute(name, myDocs);
		}
	}

	
	/**
	 * lookup the product contacts, which is another Solr query
	 * @param product
	 */
	private void addProductContacts(HuddleProductVO product, String orgId, int roleLevel) {
		log.debug("loading product contacts");
		SolrQueryProcessor sqp = new SolrQueryProcessor(getAttributes(), getAttribute(Constants.SOLR_COLLECTION_NAME).toString());
		SolrActionVO qData = new SolrActionVO();
		qData.setNumberResponses(15); //arbitrarty, should never be this high but we don't know
		qData.setStartLocation(0);
		qData.setRoleLevel(roleLevel);
		qData.setOrganizationId(orgId);
		
		//bind by product name
		SolrFieldVO field = new SolrFieldVO();
		field.setBooleanType(BooleanType.AND);
		field.setFieldType(FieldType.SEARCH);
		field.setFieldCode(SearchDocumentHandler.TITLE_LCASE);
		field.setValue(product.getTitle().toLowerCase());
		qData.addSolrField(field);
		
		//also bind by index_type
		SolrFieldVO field2 = new SolrFieldVO();
		field2.setBooleanType(BooleanType.AND);
		field2.setFieldType(FieldType.SEARCH);
		field2.setFieldCode(SearchDocumentHandler.INDEX_TYPE);
		field2.setValue(HuddleUtils.IndexType.HUDDLE_PRODUCT_CONTACT.toString());
		qData.addSolrField(field2);
		
		List<ProductContactVO> contacts = new ArrayList<>(15); //same count as above
		SolrResponseVO solrResp =  sqp.processQuery(qData);
		for (SolrDocument sd : solrResp.getResultDocuments())
			contacts.add(new ProductContactVO(sd));

		Collections.sort(contacts); //leverages the Comparable Interface in the VOs
		product.setContacts(contacts);
		log.debug("added " + product.getContactCnt() + " contacts to " + product.getTitle().toLowerCase());
	}
	

	/**
	 * loads a list of assets for the product - we return SolrDocuments here so we can apply the SolrBusinessRules object in the view (consistent w/site)
	 * @param p
	 * @param req
	 * @param mediabin
	 * @return 
	 */
	private List<SolrDocument> loadAttributeAssets(ActionRequest req, Set<String> mediabin) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		UserRoleVO role = (UserRoleVO)req.getSession().getAttribute(Constants.ROLE_DATA);
		SolrQueryProcessor sqp = new SolrQueryProcessor(getAttributes(), getAttribute(Constants.SOLR_COLLECTION_NAME).toString());
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
		qData.setRoleLevel(role.getRoleLevel());
		qData.setOrganizationId(site.getOrganizationId());
		SolrResponseVO resp = sqp.processQuery(qData);
		return resp.getResultDocuments();
	}


	/**
	 * Get the list of products from solr that fit the supplied filters
	 * @param req
	 * @param mainCol
	 * @throws ActionException
	 */
	private void listSearch(ActionRequest req, boolean mainCol) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String solrActionId = StringUtil.checkVal(mod.getAttribute(SBModuleVO.ATTRIBUTE_1));
		actionInit.setActionId(solrActionId);

		// Only add filters if this is the main portlet on the page.
		req.setParameter("fmid", mod.getPageModuleId());
		prepareFilterQueries(req);

		SMTActionInterface sai = new SolrAction(actionInit);
		sai.setAttributes(getAttributes());
		sai.setDBConnection(dbConn);
		sai.retrieve(req);

		req.setParameter("fmid", "");
	}

	
	/**
	 * Prepare the filter queries for solr
	 */
	private void prepareFilterQueries(ActionRequest req) {
		// honor category and specialty pre-filters coming off the section homepages
		if (req.hasParameter("category")) {
			//String cat = StringUtil.capitalizePhrase(req.getParameter("category"), 0, " -");
			String cat = req.getParameter("category"); //above cleanup done in view
			req.setParameter("fq", SearchDocumentHandler.HIERARCHY + ":" + cat);
		} else if (req.hasParameter("specialty")) {
			req.setParameter("fq", HuddleUtils.SOLR_OPCO_FIELD + ":" + req.getParameter("specialty"));
		}
		
		HuddleUtils.determineSortParameters(req);

		//called from the codman homepage ancilary view; the data is loaded via ajax, 
		//so we don't need any responses, just the facets.
		if (req.hasParameter("nr")) 
			req.setParameter("rpp", "0");
		
	}
}