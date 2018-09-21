package com.depuysynthes.srt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.solr.common.SolrDocument;

import com.depuysynthes.srt.util.SRTUtil;
import com.depuysynthes.srt.vo.SRTOpCoVO;
import com.depuysynthes.srt.vo.SRTSolrUIVO;
import com.depuysynthes.srt.vo.SRTSolrUIVO.SearchType;
import com.ram.workflow.modules.EmailWFM;
import com.siliconmtn.action.ActionControllerFactoryImpl;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.workflow.WorkflowLookupUtil;
import com.siliconmtn.workflow.data.WorkflowMessageVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.WorkflowSender;
import com.smt.sitebuilder.util.solr.SolrActionUtil;

/****************************************************************************
 * <b>Title:</b> SRTSearchAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> SRT Search Tool
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Mar 27, 2018
 ****************************************************************************/
public class SRTSearchAction extends SimpleActionAdapter {

	public static final String REQ_SEARCH_DATA = "searchData";
	public static final String REQ_BOOTSTRAP_LIMIT = "limit";
	private static final String DATE_RANGE_START_PREFIX = "start_";
	private static final String DATE_RANGE_END_PREFIX = "end_";

	public SRTSearchAction() {
		super();
	}

	public SRTSearchAction(ActionInitVO init) {
		super(init);
	}

	/**
	 * Get the results of the two solr search associate to this widget and put them into a list
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if(req.getBooleanParameter("isExport")) {
			exportResults(req);
		} else if(req.hasParameter(REQ_SEARCH_DATA)){
			searchSolr(req);
		} else if(req.hasParameter("loadUIConfig")){
			SearchType type = EnumUtil.safeValueOf(SearchType.class, req.getParameter("searchType", SearchType.PROJECT.toString()));
			Map<String, SRTSolrUIVO> formData = loadUIData(type, req.getParameter("opCoId"));
			this.putModuleData(formData, formData.size(), false);
		} else {
			this.putModuleData(loadOpCos());
		}
	}

	/**
	 * Load All OpCos that have projects.
	 * @return
	 */
	private List<SRTOpCoVO> loadOpCos() {
		String custom = getCustomSchema();
		StringBuilder sql = new StringBuilder();
		sql.append(DBUtil.SELECT_CLAUSE).append("distinct o.*").append(DBUtil.FROM_CLAUSE);
		sql.append(custom).append("dpy_syn_srt_project p ").append(DBUtil.INNER_JOIN);
		sql.append(custom).append("dpy_syn_srt_op_co o on p.op_co_id = o.op_co_id");

		return new DBProcessor(dbConn, custom).executeSelect(sql.toString(), null, new SRTOpCoVO());
	}

	/**
	 * Loads UI Data For Building the UI Configuration Options.
	 * @param searchType
	 * @return
	 */
	private Map<String, SRTSolrUIVO> loadUIData(SearchType searchType, String opCoId) {
		Map<String, SRTSolrUIVO> formData;

		List<SRTSolrUIVO> data = new DBProcessor(dbConn, getCustomSchema()).executeSelect(loadUiSql(), Arrays.asList(searchType.name()), new SRTSolrUIVO());

		if(!data.isEmpty()) {
			data.stream().forEach(d -> d.setOpCoId(opCoId));
			formData = data.stream().collect(Collectors.toMap(SRTSolrUIVO::getSolrFieldId, Function.identity()));
		} else {
			formData = Collections.emptyMap();
		}

		return formData;
	}

	/**
	 * Builds the UI config retrieval Query.
	 * @return
	 */
	private String loadUiSql() {
		StringBuilder sql = new StringBuilder(200);
		sql.append(DBUtil.SELECT_FROM_STAR);
		sql.append(getCustomSchema()).append("DPY_SYN_SRT_SOLR_UI_CONFIG ");
		sql.append(DBUtil.WHERE_CLAUSE).append("SEARCH_TYPE = ? ");
		sql.append(DBUtil.ORDER_BY).append("LABEL_TXT desc ");
		return sql.toString();
	}

	/**
	 * Helper method that runs search against solr for all DocumentIds that
	 * match loads Data for Projects, then exports data to Excel Sheet.
	 *
	 * @param req
	 * @throws ActionException
	 */
	private void exportResults(ActionRequest req) throws ActionException {
		SolrResponseVO resp;
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);

		prepSolrRequest(req);

		req.setParameter("rpp", Integer.toString(5000));
		req.setParameter("page", Integer.toString(0));

		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		req.setParameter("pmid", mod.getPageModuleId());

		//Load all Project Records for this search.
		List<String> projectIds = new ArrayList<>();
		do {
			resp = getResults(req);
			loadProjectIds(resp, projectIds);
			req.setParameter("page", Integer.toString(resp.getNextPage()));
			log.debug("Loading results for page " + resp.getPage() + " of " + resp.getPageCount());
		}
		while(projectIds.size() < resp.getTotalResponses());

		//Queue Workflow Message for SRT Report.
		WorkflowSender wfs = new WorkflowSender(attributes);
		wfs.sendWorkflow(buildWorkflowMessage(projectIds, req.getParameter("emailAddress"), SRTUtil.getOpCO(req)));
	}

	/**
	 * Builds a WorkflowMessageVO for Enqueing an SRT Report Export for
	 * emailing.
	 * @param projectIds
	 * @param emailAddress
	 * @return
	 */
	private WorkflowMessageVO buildWorkflowMessage(List<String> projectIds, String emailAddress, String opCoId) {
		Map<String, Object> params = new HashMap<>();
		params.put("projectId", projectIds);
		params.put(EmailWFM.DEST_EMAIL_ADDR, emailAddress);
		params.put("opCoId", opCoId);
		WorkflowMessageVO wmv = new WorkflowMessageVO(new WorkflowLookupUtil(dbConn).lookupWorkflowId("REPORT", "SRT_REPORT"));
		wmv.setParameters(params);
		return wmv;
	}

	/**
	 * Gather all the projectIds from the solr response and return list of
	 * projectIds.
	 * @param resp
	 * @return
	 */
	private void loadProjectIds(SolrResponseVO resp, List<String> projectIds) {
		for(SolrDocument d : resp.getResultDocuments()) {
			projectIds.add(StringUtil.checkVal(d.getFieldValue("documentId")));
		}
	}

	/**
	 * Run standard Search against Solr.
	 * @param req
	 * @throws ActionException
	 */
	private void searchSolr(ActionRequest req) throws ActionException {
		String searchData = StringUtil.checkVal(req.getParameter(REQ_SEARCH_DATA));
		SolrResponseVO resp;
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);

		prepSolrRequest(req);

		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		req.setParameter("pmid", mod.getPageModuleId());
		resp = getResults(req);

		putModuleData(resp, (int)resp.getTotalResponses(), false);
		req.setParameter(REQ_SEARCH_DATA, searchData, true);
	}

	/**
	 * Manages Transposing and setting any relevant params on the req before
	 * solr search is called.
	 * @param req
	 */
	public void prepSolrRequest(ActionRequest req) {

		//Clear Query used to get here.
		req.setParameter(REQ_SEARCH_DATA, "");

		//Convert Bootstrap Table Pagination to Solr Pagination
		if(req.hasParameter(REQ_BOOTSTRAP_LIMIT)) {
			req.setParameter("rpp", req.getParameter(REQ_BOOTSTRAP_LIMIT));
			req.setParameter("page", Integer.toString(req.getIntegerParameter("offset") / req.getIntegerParameter(REQ_BOOTSTRAP_LIMIT, 25)));
		}

		//Convert Bootstrap Table Sort to Solr Sort
		if(req.hasParameter("sort")) {
			req.setParameter("fieldSort", req.getParameter("sort"));
			req.setParameter("sortDirection", req.getParameter("order"));
			req.setParameter("allowCustom", Boolean.TRUE.toString());
		}

		//Add OpCo FQ to list of fqs on the request.
		List<String> values = new ArrayList<>();
		if(req.hasParameter("fq"))
			values.addAll(Arrays.asList(req.getParameterValues("fq")));

		for(String paramNm : Collections.list(req.getParameterNames())) {
			if(paramNm.startsWith(DATE_RANGE_START_PREFIX)) {
				String solrFieldName = paramNm.substring(DATE_RANGE_START_PREFIX.length());
				String endFieldName = StringUtil.join(DATE_RANGE_END_PREFIX, solrFieldName);
				String dateTxt = SolrActionUtil.makeSolrDateRange(req.getParameter(paramNm), req.getParameter(endFieldName));
				values.add(StringUtil.join(solrFieldName, ":", dateTxt));
			}
		}

		req.setParameter("fq", values.toArray(new String [values.size()]), true);
	}

	/**
	 * Create a solr action and call retrieve on it to get the search results
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	private SolrResponseVO getResults(ActionRequest req) throws ActionException {
		// Build the solr action
		SolrAction sa = ActionControllerFactoryImpl.loadAction(SolrAction.class, this);
		sa.retrieve(req);

		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		return (SolrResponseVO) mod.getActionData();
	}	
}