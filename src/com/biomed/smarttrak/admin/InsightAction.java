package com.biomed.smarttrak.admin;

//java 8
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.action.AdminControllerAction;
//WC_Custom
import com.biomed.smarttrak.util.BiomedChangeLogUtil;
import com.biomed.smarttrak.util.BiomedInsightIndexer;
import com.biomed.smarttrak.util.SmarttrakSolrUtil;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.InsightVO;
import com.biomed.smarttrak.vo.InsightXRVO;
import com.biomed.smarttrak.vo.UserVO;
//SMT baselibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.http.CookieUtil;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.filter.fileupload.ProfileDocumentFileManagerStructureImpl;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.user.HumanNameIntfc;
import com.siliconmtn.util.user.NameComparator;
import com.smt.sitebuilder.action.file.transfer.ProfileDocumentAction;
import com.smt.sitebuilder.changelog.ChangeLogVO;
//WebCrescendo
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.solr.SolrActionUtil;

/****************************************************************************
 * <b>Title</b>: InsightAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Action for managing insights.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Ryan Riker
 * @version 1.0
 * @since Feb 14, 2017
 ****************************************************************************/
public class InsightAction extends ManagementAction {
	protected static final String INSIGHT_ID = "insightId"; //req param
	public static final String TITLE_BYPASS = "titleBypass"; //req param
	
	/**
	 * @deprecated not sure where this is used, possibly JSPs.  Unlikely it belongs here so reference it from it's source location.
	 */
	@Deprecated
	public static final String ROOT_NODE_ID = AbstractTreeAction.MASTER_ROOT;
	
	public static final String INSIGHTS_DIRECTORY_PATH = "/featuredImage";
	private Map<String, String> sortMapper;

	protected enum Fields {
		INSIGHT_ID, STATUS_CD, TYPE_CD, DATE_RANGE, START, RPP, SORT, ORDER,
		SEARCH, ID_BYPASS, TITLE_BYPASS, CREATOR_PROFILE_ID, FEATURED_FLG, 
		START_DATE, END_DATE, HIERACHIES, SECTION_BYPASS;
	}


	public InsightAction() {
		super();
		sortMapper = new HashMap<>();
		sortMapper.put("titleTxt", "title_txt");
		sortMapper.put("Published", "publish_dt");
		sortMapper.put("insightType", "type_cd");
		sortMapper.put("featuredFlg", "featured_flg");
		sortMapper.put("orderNo", "order_no");
		sortMapper.put("sectionFlg", "section_flg");
	}

	public InsightAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("insight retrieve called");

		if (Convert.formatBoolean(req.getParameter("preview")) || req.getBooleanParameter("loadSolrInsights")) {
			loadPreview(req);
			return;
		}
		if(req.getBooleanParameter("editorsDesk")) {
			retrieveSyncId(req);
			return;
		}
		
		ModuleVO modVo = (ModuleVO) getAttribute(Constants.MODULE_DATA);

		if (req.hasParameter("loadData") || req.hasParameter(INSIGHT_ID) ) {
			req.setParameter(TITLE_BYPASS, "true");
			if (req.hasParameter(INSIGHT_ID)){
				loadAuthors(req);
			}
			loadInsightsData(req);
		}

		setupAttributes(modVo);
		setAttribute(Constants.MODULE_DATA, modVo);
	}

	/**
	 * Call out to the public side insight action in 
	 * order to simulate a standard page view.
	 * @throws ActionException 
	 */
	private void loadPreview(ActionRequest req) throws ActionException {
		setAttribute(Constants.PAGE_PREVIEW, true);
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		mod.setAttribute(ModuleVO.ATTRIBUTE_1, mod.getActionUrl());
		ActionInterface ai = new com.biomed.smarttrak.action.InsightAction();
		ai.setActionInit(actionInit);
		ai.setDBConnection(dbConn);
		ai.setAttributes(attributes);
		
		ai.retrieve(req);
	}

	/**
	 * retrieves the wc sync id for the insight and sets onto request
	 * @param req
	 */
	private void retrieveSyncId(ActionRequest req) {
		if(StringUtil.isEmpty(req.getParameter(INSIGHT_ID))) return;
		
		//perform lookup
		List<Object> changeLogs = new BiomedChangeLogUtil(dbConn, attributes).loadRecords(req.getParameter(INSIGHT_ID), null, null, true);
		String currentSyncId = ((ChangeLogVO) changeLogs.get(0)).getWcSyncId();
		
		// set current sync id on request
		putModuleData(currentSyncId);
	}

	/**
	 * sets a file upload token to the class
	 * @param modVo
	 */
	public void setupAttributes(ModuleVO modVo) {
		String fileToken = null;
		try {
			fileToken = ProfileDocumentFileManagerStructureImpl.makeDocToken((String)getAttribute(Constants.ENCRYPT_KEY));
		} catch (EncryptionException e) {
			log.error("could not generate fileToken", e);
		}
		modVo.setAttribute("filePrefix", INSIGHTS_DIRECTORY_PATH);
		modVo.setAttribute(ProfileDocumentFileManagerStructureImpl.DOC_TOKEN, fileToken );
		log.debug("set doc token: " + fileToken);
	}

	/**
	 * loads the insight data 
	 * @param req
	 */
	private void loadInsightsData(ActionRequest req) {
		log.debug("loaded data");

		EnumMap<Fields, String> insightParamsMap = new EnumMap<>(Fields.class);

		//loadData gets passed on the ajax call.  If we're not loading data simply go to view to render the bootstrap 
		//table into the view (which will come back for the data).
		if (req.hasParameter(INSIGHT_ID)) insightParamsMap.put(Fields.INSIGHT_ID, req.getParameter(INSIGHT_ID) );
		if (req.hasParameter("statusCd")) insightParamsMap.put(Fields.STATUS_CD, req.getParameter("statusCd"));
		if (req.hasParameter(TITLE_BYPASS)) insightParamsMap.put(Fields.TITLE_BYPASS, req.getParameter(TITLE_BYPASS));
		if (req.hasParameter("authorId")) insightParamsMap.put(Fields.CREATOR_PROFILE_ID, req.getParameter("authorId"));
		if (req.hasParameter("featuredFlg")) insightParamsMap.put(Fields.FEATURED_FLG, req.getParameter("featuredFlg"));
		
		insightParamsMap.put(Fields.START, req.getParameter("offset", "0"));
		if (!Convert.formatBoolean(req.getParameter("retrieveAll")))
			insightParamsMap.put(Fields.RPP, req.getParameter("limit","10"));
		insightParamsMap.put(Fields.SORT, StringUtil.checkVal(sortMapper.get(req.getParameter("sort")), "publish_dt"));
		insightParamsMap.put(Fields.ORDER, StringUtil.checkVal(req.getParameter("order"), "desc"));
		insightParamsMap.put(Fields.SEARCH, StringUtil.checkVal(req.getParameter("search")).toUpperCase());
		insightParamsMap.put(Fields.ID_BYPASS, "false");
		String typeCd = CookieUtil.getValue("insightType", req.getCookies());
		if (!StringUtil.isEmpty(typeCd))
			insightParamsMap.put(Fields.TYPE_CD, typeCd);
		String startDt = CookieUtil.getValue("insightStartDt", req.getCookies());
		if(!StringUtil.isEmpty(startDt))
			insightParamsMap.put(Fields.START_DATE, startDt);
		String endDt = CookieUtil.getValue("insightEndDt", req.getCookies());
		if(!StringUtil.isEmpty(endDt))
			insightParamsMap.put(Fields.END_DATE, endDt);
		String hierarchies = CookieUtil.getValue("insightMarkets", req.getCookies());
		if (!StringUtil.isEmpty(hierarchies)) 
			insightParamsMap.put(Fields.HIERACHIES, hierarchies);
		if (Convert.formatBoolean(req.getParameter("sectionBypass"))) 
			insightParamsMap.put(Fields.SECTION_BYPASS, "true");

		List<InsightVO> insights = getInsights(insightParamsMap);
		decryptNames(insights);
		Long count = getCount(insightParamsMap);

		log.debug(" total count is: " + count);
		putModuleData(insights, count.intValue(), false);
	}

	/**
	 * @param insightParamsMap
	 * @return
	 */
	private long getCount(EnumMap<Fields, String> insightParamsMap) {
		String sql = formatCountQuery(insightParamsMap, customDbSchema);

		//remove the sort and order fields so the total rows count is accurate
		insightParamsMap.remove(Fields.RPP);
		insightParamsMap.remove(Fields.START);
		insightParamsMap.remove(Fields.SORT);
		insightParamsMap.remove(Fields.ORDER);

		List<Object> params = loadSqlParams(insightParamsMap);
		DBProcessor db = new DBProcessor(dbConn, customDbSchema);
		List<InsightVO> insights = db.executeSelect(sql, params, new InsightVO());

		if (!insights.isEmpty()) {
			InsightVO ivo = insights.get(0);
			return ivo.getCountNumber();
		} else {
			return 0;
		}
	}

	/**
	 * @param insightParamsMap
	 * @param schema
	 * @return
	 */
	private String formatCountQuery(EnumMap<Fields, String> insightParamsMap, String schema) {
		StringBuilder sql = new StringBuilder(400);
		generateSelectCountQuery(sql, schema);
		generateJoinSectionOfQuery(sql, schema, insightParamsMap);
		generateWhereClauseOfQuery(sql, insightParamsMap );
		return sql.toString();
	}

	/**
	 * @param sql
	 * @param schema
	 * @param insightParamsMap
	 */
	private void generateSelectCountQuery(StringBuilder sql, String schema) {
		sql.append("select count(*) as count_no from ").append(schema).append("biomedgps_insight a ");
	}

	/**
	 * used to pull back a list of insights based on the codes and types. sets a id bypass to true
	 * and will return all the insight data for each insight in the list, if you do not require 
	 * text fields
	 * @param insightId
	 * @param statusCd
	 * @param typeCd
	 * @param dateRange
	 * @return
	 */
	public List<InsightVO> getInsights(String insightId, String statusCd, String typeCd, String dateRange) {
		EnumMap<Fields, String> insightParamsMap = new EnumMap<>(Fields.class);
		if (!StringUtil.isEmpty(insightId)) insightParamsMap.put(Fields.INSIGHT_ID, insightId );
		if (!StringUtil.isEmpty(statusCd)) insightParamsMap.put(Fields.STATUS_CD, statusCd);
		if (!StringUtil.isEmpty(typeCd)) insightParamsMap.put(Fields.TYPE_CD, typeCd);
		if (!StringUtil.isEmpty(dateRange)) insightParamsMap.put(Fields.DATE_RANGE, dateRange);
		insightParamsMap.put(Fields.ID_BYPASS, "false");

		insightParamsMap.put(Fields.SORT, StringUtil.checkVal("publish_dt"));
		insightParamsMap.put(Fields.ORDER, StringUtil.checkVal( "desc"));

		return getInsights (insightParamsMap);
	}
	
	/**
	 * Load all supplied insights for solr.
	 * @param insightIds
	 * @return
	 */
	public List<InsightVO> loadForSolr(String ...insightIds) {
		EnumMap<Fields, String> insightParamsMap = new EnumMap<>(Fields.class);
		insightParamsMap.put(Fields.ID_BYPASS, "true");

		String sql = formatSolrRetrieveQuery(insightIds.length, customDbSchema, insightParamsMap);
		List<Object> params = new ArrayList<>();
		for (String id : insightIds) params.add(id);
		return getFromDatabase(params, sql, false);
	}

	/**
	 * used to pull back a list of insights based on the codes and types. sets a id bypass to true
	 * and will return all the insight data for each insight in the list, if you do not require 
	 * text fields
	 * @param insightId
	 * @param statusCd
	 * @param typeCd
	 * @param dateRange
	 * @return
	 */
	public List<InsightVO> getInsights(String insightId, String statusCd, String typeCd, String dateRange, boolean idBypass) {
		EnumMap<Fields, String> insightParamsMap = new EnumMap<>(Fields.class);
		if (!StringUtil.isEmpty(insightId)) insightParamsMap.put(Fields.INSIGHT_ID, insightId );
		if (!StringUtil.isEmpty(statusCd)) insightParamsMap.put(Fields.STATUS_CD, statusCd);
		if (!StringUtil.isEmpty(typeCd)) insightParamsMap.put(Fields.TYPE_CD, typeCd);
		if (!StringUtil.isEmpty(dateRange)) insightParamsMap.put(Fields.DATE_RANGE, dateRange);
		insightParamsMap.put(Fields.ID_BYPASS, StringUtil.checkVal(idBypass));

		insightParamsMap.put(Fields.SORT, StringUtil.checkVal("publish_dt"));
		insightParamsMap.put(Fields.ORDER, StringUtil.checkVal("desc"));

		return getInsights(insightParamsMap);
	}

	/**
	 *  used to pull back a list of insights based on the codes and types.
	 * @param insightId
	 * @param statusCd
	 * @param typeCd
	 * @param dateRange
	 * @return
	 */
	public List<InsightVO> getInsights(Map<Fields, String> insightParamsMap) {
		
		boolean isTitleBypass = insightParamsMap.containsKey(Fields.TITLE_BYPASS) && Convert.formatBoolean(insightParamsMap.get(Fields.TITLE_BYPASS));

		String sql = formatRetrieveQuery(insightParamsMap, customDbSchema);
		List<Object> params = loadSqlParams(insightParamsMap);
		List<InsightVO> insights = getFromDatabase(params, sql, isTitleBypass);

		new NameComparator().decryptNames(insights, (String)getAttribute(Constants.ENCRYPT_KEY));
		return insights;
	}


	/**
	 * Get insights from the database
	 * @param params
	 * @param sql
	 * @param isTitleBypass
	 * @return
	 */
	private List<InsightVO> getFromDatabase(List<Object> params, String sql, boolean isTitleBypass) {
		Map<String, String> authorTitles = new HashMap<>();
		DBProcessor db = new DBProcessor(dbConn, customDbSchema);

		//Load Insights
		List<InsightVO>  insights = db.executeSelect(sql, params, new InsightVO());

		//Decrypt Insights
		decryptNames(insights);

		//Load Titles if necessary.
		if (!isTitleBypass && insights.size() == 1){
			//Load Single Author if not a multi-retrieve.
			authorTitles = loadAuthorTitles(insights.get(0).getCreatorProfileId());
		} else if (!isTitleBypass){
			//Load All authors.
			authorTitles = loadAuthorTitles(null);
		}

		//Match and load other Insight Data.
		for (InsightVO vo : insights) {
			vo.setQsPath((String)getAttribute(Constants.QS_PATH));
			if(!isTitleBypass && authorTitles.containsKey(vo.getCreatorProfileId())) {
				vo.setCreatorTitle(authorTitles.get(vo.getCreatorProfileId()));
			}
			
			if (!"P".equals(vo.getStatusCd())) {
				vo.addRole(AdminControllerAction.STAFF_ROLE_LEVEL);
			} else {
				vo.addRole(AdminControllerAction.DEFAULT_ROLE_LEVEL); //any logged in ST user can see this.
			}

			ProfileDocumentAction pda = new ProfileDocumentAction();
			pda.setAttributes(attributes);
			pda.setDBConnection(dbConn);
			pda.setActionInit(actionInit);

			vo.setProfileDocuments(pda.getDocumentByFeatureId(vo.getInsightId()));
			log.debug(" doc size " + vo.getProfileDocuments().size());
		}
		return insights;
	}

	/**
	 * @param insightParamsMap 
	 * @return
	 */
	private List<Object> loadSqlParams(Map<Fields, String> insightParamsMap) {
		List<Object> params = new ArrayList<>();
		if (insightParamsMap.containsKey(Fields.INSIGHT_ID)) params.add(insightParamsMap.get(Fields.INSIGHT_ID));
		if (insightParamsMap.containsKey(Fields.STATUS_CD)) params.add(insightParamsMap.get(Fields.STATUS_CD));
		if (insightParamsMap.containsKey(Fields.TYPE_CD)) params.add(Convert.formatInteger(insightParamsMap.get(Fields.TYPE_CD)));
		if (!StringUtil.isEmpty(insightParamsMap.get(Fields.SEARCH)))
			params.add(StringUtil.checkVal("%"+insightParamsMap.get(Fields.SEARCH)+"%"));
		if (insightParamsMap.containsKey(Fields.CREATOR_PROFILE_ID)) 
			params.add(insightParamsMap.get(Fields.CREATOR_PROFILE_ID));
		if (insightParamsMap.containsKey(Fields.FEATURED_FLG)) params.add(Convert.formatInteger(insightParamsMap.get(Fields.FEATURED_FLG)));
		if (insightParamsMap.containsKey(Fields.START_DATE)) {
			Date startDt = Convert.formatDate(Convert.DATE_TIME_SLASH_PATTERN, insightParamsMap.get(Fields.START_DATE));
			params.add(Convert.formatTimestamp(startDt));
		}
		if (insightParamsMap.containsKey(Fields.END_DATE)) {
			//format the end date to account for the entire day
			Date endDt = Convert.formatDate(Convert.DATE_TIME_SLASH_PATTERN, insightParamsMap.get(Fields.END_DATE));
			params.add(Convert.formatTimestamp(getFullDateTime(endDt)));
		}
		if (insightParamsMap.containsKey(Fields.HIERACHIES)) {
			String hierarchies = insightParamsMap.get(Fields.HIERACHIES);
			for (String val : hierarchies.split(",")) {
				params.add(val);
			}
		}
		if (insightParamsMap.containsKey(Fields.RPP) && Convert.formatInteger(insightParamsMap.get(Fields.RPP)) > 0 && insightParamsMap.containsKey(Fields.START)) {
			params.add(Convert.formatInteger(insightParamsMap.get(Fields.RPP)));
			params.add(Convert.formatInteger(insightParamsMap.get(Fields.START)));
		}

		return params;
	}

	/**
	 * Formats the account retrieval query.
	 * @param schema 
	 * @return
	 */
	private static String formatRetrieveQuery(Map<Fields, String> insightParamsMap, String schema) {

		StringBuilder sql = new StringBuilder(400);

		generateSelectSectionOfQuery(sql, schema, insightParamsMap);

		generateJoinSectionOfQuery(sql, schema, insightParamsMap);

		generateWhereClauseOfQuery(sql, insightParamsMap );

		generatePaginationClauseOfQuery(sql, insightParamsMap);

		log.debug(sql);
		return sql.toString();
	}

	/**
	 * Formats the account retrieval query.
	 * @param schema 
	 * @return
	 */
	private static String formatSolrRetrieveQuery(int numIds, String schema, Map<Fields, String> insightParamsMap) {
		StringBuilder sql = new StringBuilder(400);

		generateSelectSectionOfQuery(sql, schema, insightParamsMap);

		generateJoinSectionOfQuery(sql, schema, insightParamsMap);

		generateSolrWhereClauseOfQuery(sql, numIds );

		log.debug(sql);
		return sql.toString();
	}

	/**
	 * generates the order by and limit and offset for the query used for bootstrap table
	 * @param sql
	 * @param insightParamsMap
	 */
	private static void generatePaginationClauseOfQuery(StringBuilder sql,
			Map<Fields, String> insightParamsMap) {

		sql.append("order by ").append(insightParamsMap.get(Fields.SORT)).append(" ").append(insightParamsMap.get(Fields.ORDER));
		// Ties should be broken by publish date
		sql.append(", publish_dt desc");

		if (insightParamsMap.containsKey(Fields.RPP) && Convert.formatInteger(insightParamsMap.get(Fields.RPP)) > 0 && insightParamsMap.containsKey(Fields.START)){
			sql.append(" limit ? offset ? ");
		}
	}

	/**
	 * generates the where clause of the query based on supplied params
	 * @param sql
	 * @param insightId
	 * @param statusCd
	 * @param typeCd
	 * @param dateRange
	 * @param idByPass 
	 */
	private static void generateWhereClauseOfQuery(StringBuilder sql, Map<Fields, String> insightParamsMap) {
		sql.append("where 1=1 ");

		if (!StringUtil.isEmpty(insightParamsMap.get(Fields.INSIGHT_ID))) 
			sql.append("and a.insight_id=? ");

		if (!StringUtil.isEmpty(insightParamsMap.get(Fields.STATUS_CD))){
			sql.append("and a.status_cd=? ");
		}else if(StringUtil.isEmpty(insightParamsMap.get(Fields.INSIGHT_ID))){
			sql.append("and a.status_cd != 'D' ");
		}

		if ( !StringUtil.isEmpty(insightParamsMap.get(Fields.TYPE_CD)))
			sql.append("and a.type_cd=? ");

		if (!StringUtil.isEmpty(insightParamsMap.get(Fields.START_DATE))) 
				sql.append("and a.publish_dt >= ? ");
		if (!StringUtil.isEmpty(insightParamsMap.get(Fields.END_DATE)))
				sql.append("and a.publish_dt < ? ");

		if (!StringUtil.isEmpty(insightParamsMap.get(Fields.SEARCH)))
			sql.append("and upper(title_txt) like ? "); 

		if (!StringUtil.isEmpty(insightParamsMap.get(Fields.CREATOR_PROFILE_ID)))
			sql.append("and a.creator_profile_id=? ");
		
		if (!StringUtil.isEmpty(insightParamsMap.get(Fields.FEATURED_FLG)))
			sql.append("and a.featured_flg=? ");
		
		if (!StringUtil.isEmpty(insightParamsMap.get(Fields.HIERACHIES))) {
			String [] hierarchies = insightParamsMap.get(Fields.HIERACHIES).split(",");
			sql.append("and b.section_id in(");
			DBUtil.preparedStatmentQuestion(hierarchies.length, sql);
			sql.append(") "); 
		}
		
	}

	/**
	 * Build the where clause specific to the solr's needs.
	 * @param sql
	 * @param numIds
	 */
	private static void generateSolrWhereClauseOfQuery(StringBuilder sql, int numIds) {
		if (numIds > 0) {
			sql.append("where ");
			sql.append("a.insight_id in (");
			DBUtil.preparedStatmentQuestion(numIds, sql);
			sql.append(") ");
		}
	}

	/**
	 * updates string builder with the join section of the query based on supplied params
	 * @param sql
	 * @param schema 
	 * @param insightId
	 * @param schema
	 * @param idByPass
	 */
	private static void generateJoinSectionOfQuery(StringBuilder sql, String schema, Map<Fields, String> insightParamsMap) {
		sql.append("inner join profile p on a.creator_profile_id=p.profile_id ");
		if (!StringUtil.isEmpty(insightParamsMap.get(Fields.INSIGHT_ID)) || Convert.formatBoolean(insightParamsMap.get(Fields.ID_BYPASS)) 
				|| !StringUtil.isEmpty(insightParamsMap.get(Fields.HIERACHIES))|| !StringUtil.isEmpty(insightParamsMap.get(Fields.SECTION_BYPASS))){
			sql.append("left outer join ").append(schema).append("biomedgps_insight_section b ");
			sql.append("on a.insight_id=b.insight_id ");
		}
	}

	/**
	 * updates the string builder with the select section of the query based on supplied params
	 * @param sql
	 * @param schema 
	 * @param idByPass 
	 * @param schema 
	 * @param insightId 
	 */
	private static void generateSelectSectionOfQuery(StringBuilder sql, String schema, Map<Fields, String> insightParamsMap) {
		sql.append("select ");
		if (!StringUtil.isEmpty(insightParamsMap.get(Fields.INSIGHT_ID)) || Convert.formatBoolean(insightParamsMap.get(Fields.ID_BYPASS))){
			sql.append("a.* ");
		}else{
			sql.append("a.insight_id,a.status_cd, a.type_cd, a.publish_dt, a.title_txt, a.featured_flg, a.slider_flg, a.section_flg, a.order_no ");
		}

		sql.append(", p.first_nm, p.last_nm, p.profile_img ");

		if (!StringUtil.isEmpty(insightParamsMap.get(Fields.INSIGHT_ID)) || Convert.formatBoolean(insightParamsMap.get(Fields.ID_BYPASS))
				|| !StringUtil.isEmpty(insightParamsMap.get(Fields.SECTION_BYPASS)))
			sql.append(", b.section_id ");

		sql.append("from ").append(schema).append("biomedgps_insight a ");
	}

	/**
	 * loop and de-crypt owner names, which came from the profile table
	 * @param accounts
	 */
	protected void decryptNames(List<? extends HumanNameIntfc> data) {
		new NameComparator().decryptNames(data, (String)getAttribute(Constants.ENCRYPT_KEY));
	}

	/**
	 * loads a list of profileId|Names for the BiomedGPS Staff role level - these are their Account Managers
	 * @param req
	 * @throws ActionException
	 */
	protected void loadSections(ActionRequest req) throws ActionException {
		SectionHierarchyAction cha = new SectionHierarchyAction(this.actionInit);
		cha.setDBConnection(dbConn);
		cha.setAttributes(getAttributes());
		cha.retrieve(req);

		req.setAttribute("sections", ((ModuleVO)getAttribute(Constants.MODULE_DATA)).getActionData());	
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("insights action build called");
		saveRecord(req, false);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		log.debug("delete called");
		saveRecord(req, true);
	}

	/**
	 * reusable internal method for invoking DBProcessor
	 * @param req
	 * @param isDelete
	 * @throws ActionException
	 */
	protected void saveRecord(ActionRequest req, boolean isDelete) throws ActionException {
		DBProcessor db = new DBProcessor(dbConn, (String)customDbSchema);
		InsightVO ivo = new InsightVO(req);
		populateAuthorData(req, ivo);

		if (isDelete) {
			/*
			 * Insights rely on status to determine deleting status.  They
			 * are deleted from Solr, however the database record is simple
			 * flagged InstightStatusCd.D
			 */
			log.debug("deleting " + ivo);
			ivo.setStatusCd(InsightVO.InsightStatusCd.D.name());
			updateStatus(db, ivo);
		} else {
			if (req.hasParameter("listSave")) {
				updateFeatureOrder(ivo, db);
				//fill the vo up with the rest of the data so there is something to push to solr
				ivo = loadInsight(ivo);
			} else {
				saveInsight(db, ivo, StringUtil.isEmpty(req.getParameter(INSIGHT_ID)));
				saveProfileDoc(req, ivo);
			}
		}
		writeToSolr(ivo);
		req.setParameter(INSIGHT_ID, ivo.getInsightId());
	}

	/**
	 * if there is a profile doc on the insight saves it
	 * @param ivo 
	 * @param req 
	 */
	private void saveProfileDoc(ActionRequest req, InsightVO ivo) {

		if (StringUtil.isEmpty(ivo.getFeaturedImageTxt())) return;

		SMTSession ses = req.getSession();
		UserVO user = (UserVO) ses.getAttribute(Constants.USER_DATA);
		log.debug("user id = " + user.getUserId());
		ivo.setUserId(user.getUserId());

		processProfileDocumentCreation(ivo, req, user.getProfileId());

	}

	/**
	 * this method will make and save a profile document entry for the new insight.
	 * @param vo
	 * @param req
	 * @param profileId 
	 */
	protected void processProfileDocumentCreation(InsightVO vo, ActionRequest req, String profileId) {
		log.debug("process profile document creation called ");
		ProfileDocumentAction pda = new ProfileDocumentAction();
		pda.setAttributes(attributes);
		pda.setDBConnection(dbConn);
		pda.setActionInit(actionInit);

		String orgId = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getOrganizationId();

		req.setParameter("profileId", profileId);
		req.setParameter("featureId", vo.getInsightId());
		req.setParameter("organizationId", orgId);
		req.setParameter("actionId", actionInit.getActionId());

		try {
			//deletes all records and files related to this featured id
			pda.deleteByFeaturedId(vo.getInsightId(), orgId);
			//adds the new record and file
			pda.build(req);
		} catch (ActionException e) {
			log.error("error occured during profile document generation " , e);
		}
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
		if(authors != null && !authors.isEmpty()) {
			ivo.setCreatorTitle(authors.get(0).getTitle());
		}
	}

	/**
	 * fills an insight vo by id if possible
	 * @param ivo
	 * @return 
	 */
	private InsightVO loadInsight(InsightVO ivo) {
		List<InsightVO> insights = getInsights(ivo.getInsightId(), null, null, null);
		if (!insights.isEmpty()) 
			return insights.get(0);

		return ivo;
	}

	/**
	 * Save an InsightsVO to solr.
	 * @param u
	 */
	protected void writeToSolr(InsightVO ivo) {
		BiomedInsightIndexer indexer = BiomedInsightIndexer.makeInstance(getAttributes());
		indexer.setDBConnection(dbConn);
		indexer.indexItems(ivo.getInsightId());
	}

	/**
	 * Removes an Updates Record from Solr.
	 * @param u
	 */
	protected void deleteFromSolr(InsightVO ivo) {
		try (SolrActionUtil sau = new SmarttrakSolrUtil(getAttributes())) {
			sau.removeDocument(ivo.getDocumentId());
		} catch (Exception e) {
			log.error("Error Deleting from Solr.", e);
		}
		log.debug("removed document from solr");
	}

	/**
	 * uses db util to do a full update or insert on the passed vo
	 * @param u 
	 * @param db 
	 * @param isInsert
	 * @throws ActionException 
	 * 
	 */
	private void saveInsight(DBProcessor db, InsightVO ivo, boolean isInsert) throws ActionException {
		try {
			db.save(ivo);
			setInsightIdOnInsert(ivo, isInsert);
			saveSections(ivo);

		} catch (Exception e) {
			throw new ActionException(e);
		}
	}

	private void updateStatus(DBProcessor db, InsightVO ivo) throws ActionException {
		log.debug("updating status code on insight");
		StringBuilder sql = new StringBuilder(50);
		sql.append("update ").append(customDbSchema).append("biomedgps_insight ");
		sql.append("set status_cd = ? ");
		sql.append("where insight_id = ? ");

		log.debug(" sql " + sql.toString() +"|"+ ivo.getStatusCd() +"|"+ivo.getInsightId());

		List<String> fields = new ArrayList<>();
		fields.add("status_cd");
		fields.add("insight_id");

		try {
			db.executeSqlUpdate(sql.toString(), ivo, fields);
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}

	/**
	 * does a quick update issued from the list view.
	 * @param db 
	 * @param u
	 * @throws ActionException 
	 */
	private void updateFeatureOrder(InsightVO ivo, DBProcessor db) throws ActionException {
		log.debug("update featured ordered no");
		StringBuilder sb = new StringBuilder(50);
		sb.append("update ").append(customDbSchema).append("biomedgps_insight ");
		sb.append("set featured_flg= ?, order_no = ? ");
		sb.append("where insight_id = ? ");

		log.debug(" sql " + sb.toString() +"|"+ StringUtil.checkVal(ivo.getFeaturedFlg()) +"|"+StringUtil.checkVal(ivo.getOrderNo()+"|"+ivo.getInsightId()));

		List<String> fields = new ArrayList<>();
		fields.add("featured_flg");
		fields.add("order_no");
		fields.add("insight_id");

		try {
			int x = db.executeSqlUpdate(sb.toString(), ivo, fields);
			log.debug("rows updated " + x );
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}


	/**
	 * sets the new insight vo on insert
	 * @param u 
	 * @param isInsert
	 */
	private void setInsightIdOnInsert(InsightVO ivo, boolean isInsert) {
		if (!isInsert) return; //nothing to do if this is not an insert
		
		for(InsightXRVO uxr : ivo.getInsightSections()) {
			uxr.setInsightId(ivo.getInsightId());
		}
		
	}


	/**
	 * Delete old Insight Sections and save new ones.
	 * @param u
	 * @throws ActionException
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	protected void saveSections(InsightVO ivo) throws Exception {
		//Delete old Insight Section XRs
		deleteSections(ivo.getInsightId());

		//Save new Sections.
		DBProcessor db = new DBProcessor(dbConn, (String)customDbSchema);
		for(InsightXRVO uxr : ivo.getInsightSections()) {
			db.save(uxr);
		}
	}

	/**
	 * Delete old Insight Section XRs 
	 * @param insightId
	 * @throws ActionException 
	 */
	protected void deleteSections(String insightId) throws ActionException {
		StringBuilder sql = new StringBuilder(100);
		sql.append("delete from ").append(customDbSchema);
		sql.append("biomedgps_insight_section where insight_id = ?");

		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, insightId);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}
	
	/**
	 * Returns a Date object that includes complete date and time for the 
	 * entire date passed(just before next day). 
	 * @param initialDate
	 * @return
	 */
	private Date getFullDateTime(Date initialDate) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(initialDate);
		cal.add(Calendar.DATE, 1);
		cal.add(Calendar.SECOND, -1);
		return cal.getTime(); 
	}

	/**
	 * Load the Section Tree so that Hierarchies can be generated.
	 * @param req
	 * @throws ActionException
	 * @deprecated - call the loadDefaultTree method directly.
	 */
	@Deprecated
	public SmarttrakTree loadSections() {
		return super.loadDefaultTree();
	}
}