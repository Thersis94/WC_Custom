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

	public ContentReportAction() {
		super();
	}

	public ContentReportAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		AbstractSBReportVO report = new ContentReportVO();
		report.setData(getSolrDocuments(req.getParameter("organizationId")));
		report.setFileName("Huddle Content Report.xls");
		req.setAttribute(Constants.BINARY_DOCUMENT, report);
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, true);
	}


	/**
	 * Get all the products from solr
	 * @return
	 */
	protected List<SolrDocument> getSolrDocuments(String organizationId) {
		SolrQueryProcessor sqp = new SolrQueryProcessor(getAttributes());
		SolrActionVO qData = new SolrActionVO();
		qData.setFieldSort(SearchDocumentHandler.TITLE_LCASE);
		qData.setSortDirection(ORDER.asc);
		qData.setNumberResponses(5000);
		qData.setRoleLevel(SecurityController.PUBLIC_REGISTERED_LEVEL);
		qData.setOrganizationId(organizationId);

		SolrFieldVO field = new SolrFieldVO();
		field.setBooleanType(BooleanType.AND);
		field.setFieldType(FieldType.SEARCH);
		field.setFieldCode(SearchDocumentHandler.INDEX_TYPE);
		field.setValue(HuddleUtils.IndexType.PRODUCT.toString());
		qData.addSolrField(field);
		SolrResponseVO resp = sqp.processQuery(qData);

		// If there are still products in solr go back to get whatever is left
		if (resp.getTotalResponses() <= resp.getResultDocuments().size())
			return resp.getResultDocuments();

		//compile a new list - we need to go back to Solr for more results
		// Keep iterating until all the products have been retrieved.
		List<SolrDocument> docs = new ArrayList<>(resp.getResultDocuments());
		do {
			qData.setStartLocation(docs.size());
			resp = sqp.processQuery(qData);
			docs.addAll(resp.getResultDocuments());
		} while (resp.getTotalResponses() > docs.size());

		return docs;
	}
}