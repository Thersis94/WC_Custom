package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.util.BiomedInsightIndexer;
import com.biomed.smarttrak.util.SmarttrakSolrUtil;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.InsightVO;
import com.biomed.smarttrak.vo.InsightVO.InsightStatusCd;
import com.biomed.smarttrak.vo.InsightXRVO;
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
	protected static final String INSIGHT_ID = "insightsId"; //req param
	public static final String ROOT_NODE_ID = AbstractTreeAction.MASTER_ROOT;

	public InsightAction() {
		super();
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
		
		if (req.hasParameter("loadAuthorList")) {
			loadAuthors(req);
		}
		
		if (req.hasParameter("loadData") || req.hasParameter(INSIGHT_ID) ) {
			loadInsightsData(req);
		}
	}

	/**
	 * loads the insight data 
	 * @param req
	 */
	private void loadInsightsData(ActionRequest req) {
		log.debug("loaded data");
		
		//loadData gets passed on the ajax call.  If we're not loading data simply go to view to render the bootstrap 
		//table into the view (which will come back for the data).
		String insightId = req.hasParameter(INSIGHT_ID) ? req.getParameter(INSIGHT_ID) : null;
		String statusCd = req.getParameter("statusCd");
		String typeCd = req.getParameter("typeCd");
		String dateRange = req.getParameter("dateRange");
		List<Object> insights = getInsights(insightId, statusCd, typeCd, dateRange);

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

		//if we are looking for only managers then place them on the mod data so the 
		// table can find them.  if not they will be on the req attributes where other 
		// forms are looking for them
		if(!req.hasParameter("loadData")){
			log.debug(" placed on req for model");
			putModuleData(req.getAttribute("managers"));
		}
		
	}

	/**
	 * used to pull back a list of insights based on the codes and types.  
	 * @param insightId
	 * @param statusCd
	 * @param typeCd
	 * @param dateRange
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Object> getInsights(String insightId, String statusCd, String typeCd, String dateRange) {

		String schema = (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA);
		String sql = formatRetrieveQuery(insightId, statusCd, typeCd, dateRange, schema);

		List<Object> params = new ArrayList<>();
		if (!StringUtil.isEmpty(insightId)) params.add(insightId);
		if (!StringUtil.isEmpty(statusCd)) params.add(statusCd);
		if (!StringUtil.isEmpty(typeCd)) params.add(Convert.formatInteger(typeCd));

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
	 * @return
	 */
	public static String formatRetrieveQuery(String insightId, String statusCd, String typeCd, String dateRange, String schema) {
		StringBuilder sql = new StringBuilder(400);
		sql.append("select a.*, p.first_nm, p.last_nm, p.profile_img, b.section_id ");
		sql.append("from ").append(schema).append("biomedgps_insight a ");
		sql.append("inner join profile p on a.creator_profile_id=p.profile_id ");
		sql.append("left outer join ").append(schema).append("biomedgps_insight_section b ");
		sql.append("on a.insight_id=b.insight_id where 1=1 ");
		if (!StringUtil.isEmpty(insightId)) sql.append("and a.insight_id=? ");
		if (!StringUtil.isEmpty(statusCd)) sql.append("and a.status_cd=? ");
		if (!StringUtil.isEmpty(typeCd)) sql.append("and a.type_cd=? ");
		if (!StringUtil.isEmpty(dateRange)) {
			if("1".equals(dateRange)) {
				sql.append("and a.create_Dt > CURRENT_DATE - INTERVAL '6 months' ");
			} else if ("2".equals(dateRange)) {
				sql.append("and a.create_Dt < CURRENT_DATE - INTERVAL '6 months' ");
			}
		}

		sql.append("order by a.create_dt");

		log.debug(sql);
		return sql.toString();
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