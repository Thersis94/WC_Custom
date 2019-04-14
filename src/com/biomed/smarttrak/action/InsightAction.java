package com.biomed.smarttrak.action;

//java 8
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//solrcore jar
import org.apache.solr.client.solrj.SolrQuery.ORDER;

import com.biomed.smarttrak.action.AdminControllerAction.Section;
//WC customs
import com.biomed.smarttrak.admin.AccountUserAction;
import com.biomed.smarttrak.admin.SectionHierarchyAction;
import com.biomed.smarttrak.security.SecurityController;
import com.biomed.smarttrak.security.SmarttrakRoleVO;
import com.biomed.smarttrak.util.BiomedLinkCheckerUtil;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.InsightVO;
import com.biomed.smarttrak.vo.InsightVO.InsightStatusCd;
import com.biomed.smarttrak.vo.UserVO;
//SMT Baselibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.parser.DirectoryParser;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.user.HumanNameIntfc;
import com.siliconmtn.util.user.NameComparator;
import com.smt.sitebuilder.action.SimpleActionAdapter;
//WebCrescendo
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.action.search.SolrFieldVO.FieldType;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.util.solr.SolrActionUtil;

/****************************************************************************
 * <b>Title</b>: InsightAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description</b>: Public Insights Action that talks to Solr.
 * <b>Copyright</b>: Copyright (c) 2017
 * <b>Company</b>: Silicon Mountain Technologies
 *
 * @author Ryan Riker
 * @version 1.0
 * @since Feb 16, 2017
 ****************************************************************************/
public class InsightAction extends SimpleActionAdapter {
	private static final String REQ_PARAM_1 = DirectoryParser.PARAMETER_PREFIX + "1";


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		// setting pmid for solr action check
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		actionInit.setActionId((String) mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		req.setParameter("pmid", mod.getPageModuleId());

		// making a new solr action
		req.setAttribute(SmarttrakSolrAction.SECTION, Section.INSIGHT);
		SolrAction sa = new SmarttrakSolrAction(actionInit);
		sa.setDBConnection(dbConn);
		sa.setAttributes(attributes);

		if (req.hasParameter(REQ_PARAM_1)) {
			// Public users can get a preview of the insights. Registered users need to confirm permissions.
			PageVO page = (PageVO)req.getAttribute(Constants.PAGE_DATA);
			SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
			SmarttrakRoleVO role = (SmarttrakRoleVO)req.getSession().getAttribute(Constants.ROLE_DATA);
			if (role == null)
				SecurityController.throwAndRedirect(req);

			InsightVO vo = getInsightById(StringUtil.checkVal(req.getParameter(REQ_PARAM_1)));
			if (vo == null) {
				sbUtil.manualRedirect(req, page.getFullPath());
				return;
			}

			populateAuthorData(req, vo);

			// after the vo is build set the hierarchies and check authorization
			vo.configureSolrHierarchies(loadSections());
			SecurityController.getInstance(req).isUserAuthorized(vo, req);
			page.setTitleName(vo.getTitleTxt() + " | " + site.getSiteName());

			overrideSolrRequest(sa, vo, req);
			
			//if this request is for manage tool, update appropriate public site links
			if(Convert.formatBoolean(getAttribute(Constants.PAGE_PREVIEW))) {
				adjustContentLinks(vo, req);
			}
			
			// move the sol response mod from the mod data to some other area
			transposeModData(mod, vo);

		} else {
			// Prevent changes to the fq field from poisoning other solr widgets on the same page.
			String[] fq = null;
			if (req.hasParameter("fq"))req.getParameterValues("fq").clone();
			
			transposeRequest(req);
			sa.retrieve(req);
			mod = (ModuleVO) sa.getAttribute(Constants.MODULE_DATA);
			sortFacets(mod.getActionData(), req);
			
			req.setParameter("fq", fq, true);
		}
	}


	/**
	 * filter the faceted data based on the market sections the user is authorized to view.
	 * leverage UpdatesAction here, which is reusable code that does the same thing.
	 * @param actionData
	 * @return
	 */
	private void sortFacets(Object actionData, ActionRequest req) {
		SolrResponseVO resp = (SolrResponseVO) actionData;
		if (resp == null || resp.getFacets() == null || resp.getFacetByName("hierarchy") == null) return;

		ActionInitVO actionInit = new ActionInitVO(null, null, "SMARTRAK_INSIGHT_HIERARCHY");
		UpdatesAction ia = new UpdatesAction(actionInit);
		ia.setAttributes(getAttributes());
		ia.setDBConnection(getDBConnection());
		List<Node> sections = ia.loadSections(req);
		ia.sortFacets(sections, resp);
	}


	/**
	 * takes and moves the solr data out to attributes so the vo can sit in mod
	 * data where the view will be looking for it.
	 * 
	 * @param vo
	 * @param mod
	 */
	private void transposeModData(ModuleVO mod, InsightVO vo) {
		SolrResponseVO solVo = (SolrResponseVO) mod.getActionData();
		mod.setAttribute("solarRes", solVo);

		// place insight vo data on req.
		putModuleData(vo);
	}


	/**
	 * over rides values on the request object so we can use it to pull back 5
	 * recent insights of the same type.
	 * 
	 * @param vo
	 * @param sa
	 * @param req
	 * @throws ActionException
	 */
	private void overrideSolrRequest(SolrAction sa, InsightVO vo, ActionRequest req) throws ActionException {
		// use the set up the custom query to get back top five of the same type.
		req.setParameter("rpp", "5");
		req.setParameter("fieldSort", "publish_dt", true);
		req.setParameter("sortDirection", ORDER.desc.toString(), true);

		String[] fqs = new String[0];
		List<String> data = new ArrayList<>(Arrays.asList(fqs));
		data.add(SearchDocumentHandler.MODULE_TYPE + ":" + vo.getTypeCd());

		req.setParameter("fq", data.toArray(new String[data.size()]), true);

		// have to temp remove the req param so it doesn't get picked up as an document id request
		req.setParameter(REQ_PARAM_1, "");
		sa.retrieve(req);
		req.setParameter(REQ_PARAM_1, vo.getInsightId());
	}


	/**
	 * Helper method loads Author Data.  Slightly Different for one case, we can
	 * just load the single profile and Set it.
	 * @param req
	 * @return
	 * @throws ActionException 
	 */
	private void populateAuthorData(ActionRequest req, InsightVO ivo) throws ActionException {
		AccountUserAction aua = new AccountUserAction(this.actionInit);
		aua.setDBConnection(dbConn);
		aua.setAttributes(attributes);
		List<UserVO> authors = aua.loadAccountUsers(req, ivo.getCreatorProfileId());
		if (authors != null && !authors.isEmpty())
			ivo.setCreatorTitle(authors.get(0).getTitle());
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}


	/**
	 * transpose incoming request parameters into values Solr understands, so
	 * they get executed for us.
	 * 
	 * @param req
	 * @throws ActionException
	 */
	protected void transposeRequest(ActionRequest req) {
		// get the filter queries already on the request. Add ours to the stack,
		// and put the String[] back on the request for Solr
		String[] fqs = req.getParameterValues("fq");
		if (fqs == null) {
			fqs = new String[0];
		}
		List<String> data = new ArrayList<>(Arrays.asList(fqs));

		// Add Sections Check. Append a filter query for each section requested
		if (req.hasParameter("hierarchyId")) {
			for (String s : req.getParameterValues("hierarchyId")) {
				log.debug(" hierarchyId" + s);
				data.add(SearchDocumentHandler.HIERARCHY + ":" + s);
			}
		}

		// Get a Date Range String.
		String dates = SolrActionUtil.makeRangeQuery(FieldType.DATE, req.getParameter("startDt"), req.getParameter("endDt"));
		if (!StringUtil.isEmpty(dates)) {
			data.add("publishDate:" + dates);
		}

		// Add a ModuleType filter if typeId was passed
		if (req.hasParameter("typeId")) {
			for (String s : req.getParameterValues("typeId")) {
				data.add(SearchDocumentHandler.MODULE_TYPE + ":" + s);
			}
		}

		data.add(StringUtil.join("status_s:", InsightStatusCd.P.name()));

		// put the new list of filter queries back on the request
		req.setParameter("fq", data.toArray(new String[data.size()]), true);
	}


	/**
	 * look up for one insight searched for by insight id
	 * 
	 * @param checkVal
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected InsightVO getInsightById(String insightId) {
		String schema = (String) getAttributes().get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sb = new StringBuilder(350);
		sb.append("select a.*, p.first_nm, p.last_nm, b.section_id ");
		sb.append("from ").append(schema).append("biomedgps_insight a ");
		sb.append("inner join profile p on a.creator_profile_id=p.profile_id ");
		sb.append("left outer join ").append(schema).append("biomedgps_insight_section b on a.insight_id=b.insight_id ");
		sb.append("where a.insight_id = ? ");
		log.debug("sql: " + sb + "|" + insightId);

		List<Object> params = new ArrayList<>();
		params.add(insightId);

		DBProcessor db = new DBProcessor(dbConn, schema);
		List<Object> insight = db.executeSelect(sb.toString(), params, new InsightVO());
		log.debug("loaded " + insight.size() + " insight");

		if (insight.isEmpty())
			return null;

		for (Object vo : insight) {
			InsightVO ivo = (InsightVO) vo;
			ivo.setQsPath((String) getAttribute(Constants.QS_PATH));
		}

		new NameComparator().decryptNames((List<? extends HumanNameIntfc>) (List<?>) insight, (String) getAttribute(Constants.ENCRYPT_KEY));
		return (InsightVO) insight.get(0);
	}


	/**
	 * Load the Section Tree so that Hierarchies can be generated.
	 * 
	 * @param req
	 * @throws ActionException
	 */
	public SmarttrakTree loadSections() {
		// load the section hierarchy Tree from the hierarchy action
		SectionHierarchyAction sha = new SectionHierarchyAction();
		sha.setAttributes(getAttributes());
		sha.setDBConnection(getDBConnection());
		SmarttrakTree t = sha.loadDefaultTree();

		// Generate the Node Paths using Node Names.
		t.buildNodePaths(t.getRootNode(), SearchDocumentHandler.HIERARCHY_DELIMITER, true);
		return t;
	}
	
	/**
	 * Modifies public links to their corresponding manage tool link
	 * @param insight
	 * @param req
	 */
	protected void adjustContentLinks(InsightVO insight, ActionRequest req) {
		//create link checker util
		SiteVO siteData = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		BiomedLinkCheckerUtil linkUtil = new BiomedLinkCheckerUtil(dbConn, siteData);
		
		//update links for main, abstract, and side content areas
		insight.setContentTxt(linkUtil.modifySiteLinks(insight.getContentTxt()));	
		insight.setAbstractTxt(linkUtil.modifySiteLinks(insight.getAbstractTxt()));	
		insight.setSideContentTxt(linkUtil.modifySiteLinks(insight.getSideContentTxt()));				
	}
}
