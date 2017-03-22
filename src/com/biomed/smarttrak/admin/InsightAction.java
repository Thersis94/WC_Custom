package com.biomed.smarttrak.admin;
//java 8
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//WC_Custom
import com.biomed.smarttrak.util.BiomedInsightIndexer;
import com.biomed.smarttrak.util.SmarttrakSolrUtil;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.InsightVO;
import com.biomed.smarttrak.vo.InsightVO.InsightStatusCd;
import com.biomed.smarttrak.vo.InsightXRVO;
//SMT baselibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.user.HumanNameIntfc;
import com.siliconmtn.util.user.NameComparator;
//WebCrescendo
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
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
public class InsightAction extends AbstractTreeAction {
	protected static final String INSIGHT_ID = "insightId"; //req param
	public static final String ROOT_NODE_ID = AbstractTreeAction.MASTER_ROOT;
	private Map<String, String> sortMapper;
	
	protected enum Fields {
		INSIGHT_ID, STATUS_CD, TYPE_CD, DATE_RANGE, START, RPP, SORT, ORDER,
		SEARCH, ID_BYPASS;
	}
	
	public InsightAction() {
		super();
		sortMapper = new HashMap<>();
		sortMapper.put("titleTxt", "title_txt");
		sortMapper.put("Published", "publish_dt");
		sortMapper.put("insightType", "type_cd");
		sortMapper.put("featuredFlg", "featured_flg");
		sortMapper.put("orderNo", "order_no");
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
		
		if (req.hasParameter("loadData") || req.hasParameter(INSIGHT_ID) ) {
			loadAuthors(req);
			loadInsightsData(req);
		}
	}

	/**
	 * loads the insight data 
	 * @param req
	 */
	private void loadInsightsData(ActionRequest req) {
		log.debug("loaded data");
		
		EnumMap<Fields, String> insightParamsMap = new EnumMap<Fields,String>(Fields.class);
		
		//loadData gets passed on the ajax call.  If we're not loading data simply go to view to render the bootstrap 
		//table into the view (which will come back for the data).
		if (req.hasParameter(INSIGHT_ID)) insightParamsMap.put(Fields.INSIGHT_ID, req.getParameter(INSIGHT_ID) );
		if (req.hasParameter("statusCd")) insightParamsMap.put(Fields.STATUS_CD, req.getParameter("statusCd"));
		if (req.hasParameter("typeCd")) insightParamsMap.put(Fields.TYPE_CD, req.getParameter("typeCd"));
		if (req.hasParameter("dateRange")) insightParamsMap.put(Fields.DATE_RANGE, req.getParameter("dateRange"));
		insightParamsMap.put(Fields.START, req.getParameter("offset", "0"));
		insightParamsMap.put(Fields.RPP, req.getParameter("limit","10"));
		insightParamsMap.put(Fields.SORT, StringUtil.checkVal(sortMapper.get(req.getParameter("sort")), "publish_dt"));
		insightParamsMap.put(Fields.ORDER, StringUtil.checkVal(req.getParameter("order"), "desc"));
		insightParamsMap.put(Fields.SEARCH, StringUtil.checkVal(req.getParameter("search")).toUpperCase());
		insightParamsMap.put(Fields.ID_BYPASS, "false");
		
		List<Object> insights;
		
		insights = getInsights(insightParamsMap);
		
		decryptNames(insights);

		putModuleData(insights);
		
		
	}

	/**
	 * loads a list of to the request.
	 * @param req
	 * @throws ActionException 
	 */
	private void loadAuthors(ActionRequest req) throws ActionException {
		log.debug("loaded authors");
		
		AccountAction aa = new AccountAction();
		aa.setActionInit(actionInit);
		aa.setAttributes(attributes);
		aa.setDBConnection(dbConn);
		aa.loadManagerList(req, (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA));

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
	public List<Object> getInsights(String insightId, String statusCd, String typeCd, String dateRange) {
		
		EnumMap<Fields, String> insightParamsMap = new EnumMap<Fields,String>(Fields.class);
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
	 * used to pull back a list of insights based on the codes and types. sets a id bypass to true
	 * and will return all the insight data for each insight in the list, if you do not require 
	 * text fields
	 * @param insightId
	 * @param statusCd
	 * @param typeCd
	 * @param dateRange
	 * @return
	 */
	public List<Object> getInsights(String insightId, String statusCd, String typeCd, String dateRange, boolean idBypass) {
		
		EnumMap<Fields, String> insightParamsMap = new EnumMap<Fields,String>(Fields.class);
		if (!StringUtil.isEmpty(insightId)) insightParamsMap.put(Fields.INSIGHT_ID, insightId );
		if (!StringUtil.isEmpty(statusCd)) insightParamsMap.put(Fields.STATUS_CD, statusCd);
		if (!StringUtil.isEmpty(typeCd)) insightParamsMap.put(Fields.TYPE_CD, typeCd);
		if (!StringUtil.isEmpty(dateRange)) insightParamsMap.put(Fields.DATE_RANGE, dateRange);
		insightParamsMap.put(Fields.ID_BYPASS, StringUtil.checkVal(idBypass));
		
		insightParamsMap.put(Fields.SORT, StringUtil.checkVal("publish_dt"));
		insightParamsMap.put(Fields.ORDER, StringUtil.checkVal("desc"));
		
		return getInsights (insightParamsMap);
	}

	/**
	 *  used to pull back a list of insights based on the codes and types.
	 * @param insightId
	 * @param statusCd
	 * @param typeCd
	 * @param dateRange
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Object> getInsights(Map<Fields, String> insightParamsMap) {

		String schema = (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA);
		String sql = formatRetrieveQuery(insightParamsMap, schema);

		List<Object> params = new ArrayList<>();
		
		if (insightParamsMap.containsKey(Fields.INSIGHT_ID)) params.add(insightParamsMap.get(Fields.INSIGHT_ID));
		if (insightParamsMap.containsKey(Fields.STATUS_CD)) params.add(insightParamsMap.get(Fields.STATUS_CD));
		if (insightParamsMap.containsKey(Fields.TYPE_CD)) params.add(Convert.formatInteger(insightParamsMap.get(Fields.TYPE_CD)));

		if (!StringUtil.isEmpty(insightParamsMap.get(Fields.SEARCH)))
			params.add(StringUtil.checkVal("%"+insightParamsMap.get(Fields.SEARCH)+"%"));
			
		if (insightParamsMap.containsKey(Fields.RPP) && insightParamsMap.containsKey(Fields.START)){
			params.add(Convert.formatInteger(insightParamsMap.get(Fields.RPP)));
			params.add(Convert.formatInteger(insightParamsMap.get(Fields.START)));
		}
		
		DBProcessor db = new DBProcessor(dbConn, schema);
		List<Object>  insights = db.executeSelect(sql, params, new InsightVO());

		for (Object ob : insights){
			InsightVO vo = (InsightVO)ob;
			vo.setQsPath((String)getAttribute(Constants.QS_PATH));
		}

		new NameComparator().decryptNames((List<? extends HumanNameIntfc>)(List<?>)insights, (String)getAttribute(Constants.ENCRYPT_KEY));

		return insights;
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
	 * generates the order by and limit and offset for the query used for bootstrap table
	 * @param sql
	 * @param insightParamsMap
	 */
	private static void generatePaginationClauseOfQuery(StringBuilder sql,
			Map<Fields, String> insightParamsMap) {
		
		sql.append("order by ").append(insightParamsMap.get(Fields.SORT)).append(" ").append(insightParamsMap.get(Fields.ORDER));
		
		if (insightParamsMap.containsKey(Fields.RPP) && insightParamsMap.containsKey(Fields.START)){
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
		}else if(Convert.formatBoolean(insightParamsMap.get(Fields.ID_BYPASS))){
			//when solr indexes, index only published insights
			sql.append("and a.status_cd = 'P' ");
		}else {
			sql.append("and a.status_cd != 'D' ");
		}
		
		if ( !StringUtil.isEmpty(insightParamsMap.get(Fields.TYPE_CD)))
			sql.append("and a.type_cd=? ");
		
		if (!StringUtil.isEmpty(insightParamsMap.get(Fields.DATE_RANGE))) {
			if("1".equals(insightParamsMap.get(Fields.DATE_RANGE))) {
				sql.append("and a.create_Dt > CURRENT_DATE - INTERVAL '6 months' ");
			} else if ("2".equals(insightParamsMap.get(Fields.DATE_RANGE))) {
				sql.append("and a.create_Dt < CURRENT_DATE - INTERVAL '6 months' ");
			}
		}

		if (!StringUtil.isEmpty(insightParamsMap.get(Fields.SEARCH)))
			sql.append("and upper(title_txt) like ? "); 
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
		
		if (!StringUtil.isEmpty(insightParamsMap.get(Fields.INSIGHT_ID)) || Convert.formatBoolean(insightParamsMap.get(Fields.ID_BYPASS))){
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
			sql.append("a.insight_id,a.status_cd, a.type_cd, a.publish_dt, a.title_txt, a.featured_flg, a.order_no ");
		}
				
		sql.append(", p.first_nm, p.last_nm, p.profile_img ");
		
		if (!StringUtil.isEmpty(insightParamsMap.get(Fields.INSIGHT_ID)) || Convert.formatBoolean(insightParamsMap.get(Fields.ID_BYPASS)))
			sql.append(", b.section_id ");
		
		sql.append("from ").append(schema).append("biomedgps_insight a ");
	}

	/**
	 * loop and de-crypt owner names, which came from the profile table
	 * @param accounts
	 */
	@SuppressWarnings("unchecked")
	protected void decryptNames(List<Object> data) {
		new NameComparator().decryptNames((List<? extends HumanNameIntfc>)(List<?>)data, (String)getAttribute(Constants.ENCRYPT_KEY));
	}

	/**
	 * loads a list of profileId|Names for the BiomedGPS Staff role level - these are their Account Managers
	 * @param req
	 * @throws ActionException
	 */
	protected void loadSections(ActionRequest req, String schema) throws ActionException {
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
		DBProcessor db = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		InsightVO u = new InsightVO(req);
		try {
			if (isDelete) {
				log.debug("deleting " + u);
				db.delete(u);
				deleteFromSolr(u);
			} else {

				if (req.hasParameter("listSave")){
					updateFeatureOrder(u);
				}else {
					saveInsight(db, u);
				}

				//Add to Solr if published
				if(InsightStatusCd.P.toString().equals(u.getStatusCd())) {
					writeToSolr(u);
				}
			}
			req.setParameter(INSIGHT_ID, u.getInsightId());
		} catch (Exception e) {
			throw new ActionException(e);
		}
	}

	/**
	 * Save an InsightsVO to solr.
	 * @param u
	 */
	protected void writeToSolr(InsightVO u) {
		BiomedInsightIndexer bindx = BiomedInsightIndexer.makeInstance(getAttributes());
		bindx.setDBConnection(dbConn);
		bindx.addSingleItem(u.getInsightId());
	}

	/**
	 * Removes an Updates Record from Solr.
	 * @param u
	 */
	protected void deleteFromSolr(InsightVO i) {
		try (SolrActionUtil sau = new SmarttrakSolrUtil(getAttributes())) {
			sau.removeDocument(i.getInsightId());
		} catch (Exception e) {
			log.error("Error Deleting from Solr.", e);
		}
		log.debug("removed document from solr");
	}

	/**
	 * uses db util to do a full update or insert on the passed vo
	 * @param u 
	 * @param db 
	 * @throws Exception 
	 * 
	 */
	private void saveInsight(DBProcessor db, InsightVO u) throws Exception {
		db.save(u);

		setInsightIdOnInsert(u, db);

		//Save Insight Sections.
		saveSections(u);

	}

	/**
	 * does a quick update issued from the list view.
	 * @param u
	 * @throws ActionException 
	 */
	private void updateFeatureOrder(InsightVO u) throws ActionException {
		log.debug("update featured ordered no");
		StringBuilder sb = new StringBuilder(50);
		sb.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("biomedgps_insight ");
		sb.append("set FEATURED_FLG= ?, ORDER_NO = ? ");
		sb.append("where insight_id = ? ");

		log.debug(" sql " + sb.toString() +"|"+ u.getFeaturedFlg() +"|"+u.getOrderNo()+"|"+u.getInsightId());

		try(PreparedStatement ps = dbConn.prepareStatement(sb.toString())) {
			ps.setInt(1, u.getFeaturedFlg());
			ps.setInt(2, u.getOrderNo());
			ps.setString(3,u.getInsightId());
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}

	/**
	 * sets the new insight vo on insert
	 * @param db 
	 * @param u 
	 */
	private void setInsightIdOnInsert(InsightVO u, DBProcessor db) {

		if(StringUtil.isEmpty(u.getInsightId())) {
			u.setInsightId(db.getGeneratedPKId());
			for(InsightXRVO uxr : u.getInsightSections()) {
				uxr.setInsightId(u.getInsightId());
			}
		}

	}

	/**
	 * Delete old Insight Sections and save new ones.
	 * @param u
	 * @throws ActionException
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	protected void saveSections(InsightVO u) throws Exception {

		//Delete old Insight Section XRs
		deleteSections(u.getInsightId());

		DBProcessor db = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));

		//Save new Sections.
		for(InsightXRVO uxr : u.getInsightSections()) {
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
		sql.append("delete from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_insight_section where insight_id = ?");

		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, insightId);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new ActionException(e);
		}
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

	/* (non-Javadoc)
	 * @see com.biomed.smarttrak.admin.AbstractTreeAction#getCacheKey()
	 */
	@Override
	public String getCacheKey() {
		return null;
	}
}