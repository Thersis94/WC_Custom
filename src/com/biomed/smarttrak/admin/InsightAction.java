package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.admin.user.HumanNameIntfc;
import com.biomed.smarttrak.admin.user.NameComparator;
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
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
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
public class InsightAction extends SBActionAdapter {
	protected static final String INSIGHT_ID = "insightId"; //req param
	public static final String ROOT_NODE_ID = "MASTER_ROOT";

	public enum InsightCategory {
		EXTREMITIES(12, "Extremities"),
		TOTAL_JOINT(15, "Total Joint"),
		TRAUMA(17, "Trauma"),
		EU_TRAUMA(20, "EU Trauma"),
		SPINE(30, "Spine"),
		ORTHOBIO(35, "Ortho-Bio"),
		SOFT_TISSUE(37, "Soft Tissue"),
		ADV_WOUND_CARE(38, "Adv. Wound Care"),
		EU_ADV_WOUND_CARE(40, "EU Adv. Wound Care"),
		SURGICAL_MATRICIES(45, "Surgical Matricies"),
		INF_PREV(50, "Inf Prev"),
		GLUES_AND_SEALANTS(55, "Glues & Sealants"),
		WND_MGMT_STD_OF_CARE(60, "Wnd Mgmt - Std of Care"),
		REGEN_MED(65, "Regen Med"),
		NEUROVASCULAR(70, "Neurovascular"),
		NEUROMODULATION(75, "Neuromodulation");

		private int val;
		private String text;

		InsightCategory(int val, String text) {
			this.val = val;
			this.text = text;
		}

		public int getVal() {
			return this.val;
		}
		public String getText() {
			return this.text;
		}
	}
	
	public enum InsightSection {
		ALL_COMMENTARY(1, "All Commentary"),
		PERSPECTIVE(10, "Perspective"),
		MARKET_RECAP(20, "Market Recap"),
		MARKET_OUTLOOK(25, "Market Outlook"),
		STARTUP_SPOTLIGHT(30, "Start-up Spotlight");

		private int val;
		private String text;

		InsightSection(int val, String text) {
			this.val = val;
			this.text = text;
		}

		public int getVal() {
			return this.val;
		}
		public String getText() {
			return this.text;
		}
	}
	
	

	public InsightAction() {
		super();
	}

	public InsightAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	public void retrieve(ActionRequest req) throws ActionException {
		//TODO whole class copied directly from updates will need altered for insights
		
		//loadData gets passed on the ajax call.  If we're not loading data simply go to view to render the bootstrap 
		//table into the view (which will come back for the data).
		if (!req.hasParameter("loadData") && !req.hasParameter(INSIGHT_ID) ) return;

		String insightId = req.hasParameter(INSIGHT_ID) ? req.getParameter(INSIGHT_ID) : null;
		String statusCd = req.getParameter("statusCd");
		String typeCd = req.getParameter("typeCd");
		String dateRange = req.getParameter("dateRange");
		List<Object> insights = getInsights(insightId, statusCd, typeCd, dateRange);

		decryptNames(insights);

		putModuleData(insights);
	}

	public List<Object> getInsights(String insightId, String statusCd, String typeCd, String dateRange) {

		String schema = (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA);
		String sql = formatRetrieveQuery(insightId, statusCd, typeCd, dateRange, schema);

		List<Object> params = new ArrayList<>();
		if (!StringUtil.isEmpty(insightId)) params.add(insightId);
		if (!StringUtil.isEmpty(statusCd)) params.add(statusCd);
		if (!StringUtil.isEmpty(typeCd)) params.add(Convert.formatInteger(typeCd));

		DBProcessor db = new DBProcessor(dbConn, schema);
		List<Object>  insights = db.executeSelect(sql, params, new InsightVO());
		log.debug("loaded " + insights.size() + " insights");
		return insights;
	}

	/**
	 * Formats the account retrieval query.
	 * @return
	 */
	public static String formatRetrieveQuery(String insightId, String statusCd, String typeCd, String dateRange, String schema) {
		StringBuilder sql = new StringBuilder(400);
		sql.append("select a.*, p.first_nm, p.last_nm, b.section_id ");
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
	 * loop and decrypt owner names, which came from the profile table
	 * @param accounts
	 */
	@SuppressWarnings("unchecked")
	protected void decryptNames(List<Object> data) {
		new NameComparator().decryptNames((List<? extends HumanNameIntfc>)data, (String)getAttribute(Constants.ENCRYPT_KEY));
	}

	/**
	 * loads a list of profileId|Names for the BiomedGPS Staff role level - these are their Account Managers
	 * @param req
	 * @throws ActionException
	 */
	protected void loadSections(ActionRequest req, String schema) throws ActionException {
		ContentHierarchyAction cha = new ContentHierarchyAction(this.actionInit);
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
				db.delete(u);
			} else {
				db.save(u);

				if(StringUtil.isEmpty(u.getInsightId())) {
					u.setInsightId(db.getGeneratedPKId());
					for(InsightXRVO uxr : u.getInsightSections()) {
						uxr.setInsightId(u.getInsightId());
					}
				}
				//Save Insight Sections.
				saveSections(u);

				//Add to Solr if published
				if(InsightStatusCd.R.toString().equals(u.getStatusCd())) {
					saveToSolr(u);
				}
			}
		} catch (InvalidDataException | DatabaseException e) {
			throw new ActionException(e);
		}
	}

	/**
	 * Save an InsightVO to solr.
	 * @param u
	 */
	protected void saveToSolr(InsightVO u) {
		try(SolrActionUtil sau = new SolrActionUtil(getAttributes())) {
			sau.addDocument(u);
		} catch (Exception e) {
			log.error("Error Saving to Solr.", e);
		}
		log.debug("added document to solr");
	}

	/**
	 * Delete old Insight Sections and save new ones.
	 * @param u
	 * @throws ActionException
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	protected void saveSections(InsightVO u) throws ActionException, InvalidDataException, DatabaseException {

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
}