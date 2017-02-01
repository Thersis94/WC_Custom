package com.depuysynthes.huddle;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.common.SolrDocument;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.search.SolrActionVO;
import com.smt.sitebuilder.action.search.SolrFieldVO;
import com.smt.sitebuilder.action.search.SolrQueryProcessor;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.action.search.SolrFieldVO.BooleanType;
import com.smt.sitebuilder.action.search.SolrFieldVO.FieldType;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <b>Title</b>: ContentReportAction.java<p/>
 * <b>Description: Get all products from solr and place them into
 * a report vo.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Jan 25, 2016
 ****************************************************************************/
public class ContentReportAction extends SBActionAdapter {
	
	private final int MAX_SOLR_DOCUMENTS = 5000;
	private final String reportName = "Huddle_Content_Report.xlsx";
	
	public ContentReportAction() {
		
	}
	
	public ContentReportAction(ActionInitVO actionInit){
		this.actionInit = actionInit;
	}
	
	public void build(ActionRequest req) throws ActionException {
		AbstractSBReportVO report = new ContentReportVO();
		report.setData(getSolrDocuments(req.getParameter("organizationId")));
		report.setFileName(reportName);
		req.setAttribute(Constants.BINARY_DOCUMENT, report);
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, true);
	}

	
	/**
	 * Get all the products from solr
	 * @return
	 */
	private List<SolrDocument> getSolrDocuments(String organizationId) {
		
		SolrQueryProcessor sqp = new SolrQueryProcessor(getAttributes(), getAttribute(Constants.SOLR_COLLECTION_NAME).toString());
		SolrActionVO qData = new SolrActionVO();

		SolrFieldVO field = new SolrFieldVO();
		field.setBooleanType(BooleanType.AND);
		field.setFieldType(FieldType.SEARCH);
		field.setFieldCode(SearchDocumentHandler.INDEX_TYPE);
		field.setValue(HuddleUtils.IndexType.PRODUCT.toString());
		qData.addSolrField(field);
		
		qData.setFieldSort(SearchDocumentHandler.TITLE_LCASE);
		qData.setSortDirection(ORDER.desc);

		qData.setNumberResponses(MAX_SOLR_DOCUMENTS);
		qData.setStartLocation(0);
		qData.setRoleLevel(SecurityController.PUBLIC_REGISTERED_LEVEL);
		qData.setRoleLevel(SecurityController.PUBLIC_ROLE_LEVEL);
		qData.setOrganizationId(organizationId);
		SolrResponseVO resp = sqp.processQuery(qData);
		
		// If there are still products in solr go back to get whatever is left
		if (resp.getTotalResponses() > resp.getResultDocuments().size()) {
			List<SolrDocument> docs = new ArrayList<>();
			docs.addAll(resp.getResultDocuments());
			int start = 1;
			
			// Keep iterating over pages until all products have been retrieved.
			while (resp.getTotalResponses() > docs.size()) {
				qData.setStartLocation(start++);
				resp = sqp.processQuery(qData);
				docs.addAll(resp.getResultDocuments());
			}
			return docs;
		} else {
			return resp.getResultDocuments();
		}
	}
}
