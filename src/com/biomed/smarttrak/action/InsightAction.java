package com.biomed.smarttrak.action;
//java 8
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
//solrcore jar
import org.apache.solr.client.solrj.SolrQuery.ORDER;
//WC customs
import com.biomed.smarttrak.admin.AbstractTreeAction;
import com.biomed.smarttrak.security.SecurityController;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.InsightVO;
//SMT Baselibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.parser.DirectoryParser;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.user.HumanNameIntfc;
import com.siliconmtn.util.user.NameComparator;
//WebCrescendo
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.action.search.SolrFieldVO.FieldType;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.util.solr.SolrActionUtil;

/****************************************************************************
 * <b>Title</b>: InsightAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Public Insights Action that talks to Solr.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Ryan Riker
 * @version 1.0
 * @since Feb 16, 2017
 ****************************************************************************/
public class InsightAction extends AbstractTreeAction {
	private static final String REQ_PARAM_1 = DirectoryParser.PARAMETER_PREFIX + "1";

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		
		//setting pmid for solr action check
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		req.setParameter("pmid", mod.getPageModuleId());
		
		//making a new solr action
		SolrAction sa = new SolrAction(actionInit);
		sa.setDBConnection(dbConn);
		sa.setAttributes(attributes);
		
		if(req.hasParameter(REQ_PARAM_1)){
			
			InsightVO vo = getInsightById(StringUtil.checkVal(req.getParameter(REQ_PARAM_1)));

			if (vo == null ){
				PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
				sbUtil.manualRedirect(req, page.getFullPath());
				return;
			}
			
			//after the vo is build set the hierarchies and check authorization
			vo.configureSolrHierarchies(loadSections());
			SecurityController.getInstance(req).isUserAuthorized(vo, req);
			
			overrideSolrRequest(sa, vo, req);
			
			//move the sol response mod from the mod data to some other area
			transposeModData(mod, vo);
			
		}else{
			//transform some incoming reqParams to where Solr expects to see them
			transposeRequest(req);

			sa.retrieve(req);
		}
	}
	/**
	 * takes and moves the solr data out to attributes so the vo can sit in mod data
	 * where the view will be looking for it.
	 * @param vo 
	 * @param mod 
	 * 
	 */
	private void transposeModData(ModuleVO mod, InsightVO vo) {
		SolrResponseVO solVo = (SolrResponseVO)mod.getActionData();
		mod.setAttribute("solarRes",solVo );
		
		//place insight vo data on req.
		putModuleData(vo);
		
	}
	/**
	 * over rides values on the request object so we can use it to pull back 5 recent 
	 * insights of the same type. 
	 * @param vo 
	 * @param sa 
	 * @param req
	 * @throws ActionException 
	 */
	private void overrideSolrRequest(SolrAction sa, InsightVO vo, ActionRequest req) throws ActionException {
		//use the set up the custom query to get back top five of the same type.  
		req.setParameter("rpp", "5");
		req.setParameter("fieldSort", SearchDocumentHandler.UPDATE_DATE, true);
		req.setParameter("sortDirection", ORDER.desc.toString(), true);

		String[] fqs = new String[0];
		List<String> data = new ArrayList<>(Arrays.asList(fqs));
		data.add(SearchDocumentHandler.MODULE_TYPE + ":" + vo.getTypeCd());
		req.setParameter("fq", data.toArray(new String[data.size()]), true);
		
		//have to temp remove the req param so it doesn't get picked up as an document id request
		req.setParameter(REQ_PARAM_1, "");
		sa.retrieve(req);
		req.setParameter(REQ_PARAM_1, vo.getInsightId());
		
	}
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		log.debug("insights list called");
		super.retrieve(req);
	}
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		log.debug("insights update called");
		super.update(req);
	}
	/**
	 * transpose incoming request parameters into values Solr understands, so they get executed for us.
	 * @param req
	 * @throws ActionException
	 */
	protected void transposeRequest(ActionRequest req) throws ActionException {
		//get the filter queries already on the request.  Add ours to the stack, and put the String[] back on the request for Solr
		String[] fqs = req.getParameterValues("fq");
		if (fqs == null) fqs = new String[0];
		List<String> data = new ArrayList<>(Arrays.asList(fqs));

		//Add Sections Check.  Append a filter query for each section requested
		if (req.hasParameter("hierarchyId")) {
			for (String s : req.getParameterValues("hierarchyId")){
				log.debug(" hierarchyId" + s);
				data.add(SearchDocumentHandler.HIERARCHY + ":" + s);
			}
		}

		//Get a Date Range String.
		String dates = SolrActionUtil.makeRangeQuery(FieldType.DATE, req.getParameter("startDt"), req.getParameter("endDt"));
		if (!StringUtil.isEmpty(dates))
			data.add(SearchDocumentHandler.UPDATE_DATE + ":" + dates);

		//Add a ModuleType filter if typeId was passed
		if (req.hasParameter("typeId"))
			data.add(SearchDocumentHandler.MODULE_TYPE + ":" + req.getParameter("typeId"));

		//put the new list of filter queries back on the request
		req.setParameter("fq", data.toArray(new String[data.size()]), true);
	}
	
	
	/**
	 * look up for one insight searched for by insight id
	 * @param checkVal
	 * @return 
	 */
	@SuppressWarnings("unchecked")
	protected InsightVO getInsightById(String insightId) {
		log.debug("start get insight by id");
		
		String schema = (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA);
		
		StringBuilder sb = new StringBuilder(50);
		sb.append("select a.*, p.first_nm, p.last_nm, b.section_id ");
		sb.append("from ").append(schema).append("biomedgps_insight a ");
		sb.append("inner join profile p on a.creator_profile_id=p.profile_id ");
		sb.append("left outer join ").append(schema).append("biomedgps_insight_section b on a.insight_id=b.insight_id ");
		sb.append("where a.insight_id = ? ");
		
		log.debug("sql: " + sb.toString() + "|" + insightId);
		
		List<Object> params = new ArrayList<>();
		params.add(insightId);
		
		DBProcessor db = new DBProcessor(dbConn, schema);
		List<Object> insight = db.executeSelect(sb.toString(), params, new InsightVO());
		log.debug("loaded " + insight.size() + " insight");
		
		for (Object vo : insight){
			InsightVO ivo = (InsightVO)vo;
			ivo.setQsPath((String)getAttribute(Constants.QS_PATH));
		}
		
		new NameComparator().decryptNames((List<? extends HumanNameIntfc>) (List<?>) insight,
				(String) getAttribute(Constants.ENCRYPT_KEY));
		
		if (!insight.isEmpty()) {
			return (InsightVO) insight.get(0);
		}
		return null;
	}

	/**
	 * Load the Section Tree so that Hierarchies can be generated.
	 * @param req
	 * @throws ActionException
	 */
	public SmarttrakTree loadSections() {
		//load the section hierarchy Tree from superclass
		SmarttrakTree t = loadDefaultTree();

		//Generate the Node Paths using Node Names.
		t.buildNodePaths(t.getRootNode(), SearchDocumentHandler.HIERARCHY_DELIMITER, true);
		return t;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.biomed.smarttrak.admin.AbstractTreeAction#getCacheKey()
	 */
	@Override
	public String getCacheKey() {
		return null;
	}
}
