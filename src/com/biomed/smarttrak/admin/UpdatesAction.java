package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.util.SmarttrakSolrUtil;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.util.UpdateIndexer;
import com.biomed.smarttrak.vo.UpdateVO;
import com.biomed.smarttrak.vo.UpdateXRVO;
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
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.util.solr.SolrActionUtil;

/****************************************************************************
 * <b>Title</b>: UpdatesAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Action for managing Update Notifications.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * @author Billy Larsen
 * @version 1.0
 * @since Feb 14, 2017
 ****************************************************************************/
public class UpdatesAction extends AuthorAction {
	public static final String UPDATE_ID = "updateId"; //req param
	public static final String SORT = "sort"; //req param
	public static final String ORDER = "order"; //req param
	public static final String DATE_RANGE = "dateRange"; //req param
	public static final String STATUS_CD = "statusCd"; //req param
	public static final String TYPE_CD = "typeCd"; //req param
	public static final String SEARCH = "search"; //req param
	public static final String ROOT_NODE_ID = MASTER_ROOT;
	public static final int INIT_DISPLAY_LIMIT = 15; //initial display limit

	//ChangeLog TypeCd.  Using the key we swap on for actionType in AdminControllerAction so we can get back.
	public static final String UPDATE_TYPE_CD = "updates";
	
	public enum UpdateType {
		MARKET(12, "Market"),
		REVENUES(15, "Revenues"),
		NEW_PRODUCTS(17, "New Products"),
		DEALS_FINANCING(20, "Deals/Financing"),
		CLINICAL_REGULATORY(30, "Clinical/Regulatory"),
		PATENTS(35, "Patents"),
		REIMBURSEMENT(37, "Reimbursement"),
		ANNOUNCEMENTS(38, "Announcements"),
		STUDIES(40, "Studies");

		private int val;
		private String text;

		UpdateType(int val, String text) {
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

	// Maps the table field name to the db field name for sorting purposes
	private Map<String, String> sortMapper;

	public UpdatesAction() {
		super();
		buildSortMapper();
	}

	public UpdatesAction(ActionInitVO actionInit) {
		super(actionInit);
		buildSortMapper();
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		//loadData gets passed on the ajax call.  If we're not loading data simply go to view to render the bootstrap 
		//table into the view (which will come back for the data).
		if (!req.hasParameter("loadData") && !req.hasParameter("loadhistory") && !req.hasParameter(UPDATE_ID) ) return;
		int count = 0;

		List<Object> data;
		if(req.hasParameter("loadHistory")) {
			data = getHistory(req.getParameter("historyId"));
		} else {

			//Get the Filtered Updates according to Request.
			data = getFilteredUpdates(req);

			// Get the count
			count = getUpdateCount(req, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		}

		decryptNames(data);

		if(count > 0) {
			putModuleData(data, count, false);
		} else {
			putModuleData(data);
		}
		
		//when an add/edit form, load list of BiomedGPS Staff for the "Author" drop-down
		if (req.getParameter("updateId") != null)
			loadAuthors(req);
	}

	/**
	 * Helper method that returns list of Updates filtered by ActionRequest
	 * parameters.
	 * @param req
	 * @return
	 */
	private List<Object> getFilteredUpdates(ActionRequest req) {
		//Get Relevant Params off Request.
		int start = Convert.formatInteger(req.getParameter("offset"),0);
		int rpp = Convert.formatInteger(req.getParameter("limit"),10);
		if (rpp == 0) {//this is initial page load, set default for display listing  
			rpp = INIT_DISPLAY_LIMIT;
		}
		Map<String, String> reqParams = getReqParams(req);
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);

		String sql = formatRetrieveQuery(reqParams, schema, req.hasParameter("loadData"));

		List<Object> params = new ArrayList<>();
		if (!StringUtil.isEmpty(reqParams.get(UPDATE_ID))) params.add(reqParams.get(UPDATE_ID));
		if (!StringUtil.isEmpty(reqParams.get(STATUS_CD))) params.add(reqParams.get(STATUS_CD));
		if (!StringUtil.isEmpty(reqParams.get(TYPE_CD))) params.add(Convert.formatInteger((String)reqParams.get(TYPE_CD)));
		if (!StringUtil.isEmpty(reqParams.get(SEARCH))) params.add("%" + reqParams.get(SEARCH).toLowerCase() + "%");
		params.add(rpp);
		params.add(start);

		DBProcessor db = new DBProcessor(dbConn, schema);
		return db.executeSelect(sql, params, new UpdateVO());
	}

	/**
	 * Helper method that extracts Params off the Request.
	 * @param req
	 * @return
	 */
	private Map<String, String> getReqParams(ActionRequest req) {
		Map<String, String> reqParams = new HashMap<>();

		reqParams.put(UPDATE_ID, req.hasParameter(UPDATE_ID) ? req.getParameter(UPDATE_ID) : null);
		reqParams.put(SORT, StringUtil.checkVal(sortMapper.get(req.getParameter(SORT)), "publish_dt"));
		reqParams.put(ORDER, StringUtil.checkVal(req.getParameter(ORDER), "asc"));
		reqParams.put(SEARCH, StringUtil.checkVal(req.getParameter(SEARCH)).toUpperCase());
		reqParams.put(STATUS_CD, req.getParameter(STATUS_CD));
		reqParams.put(TYPE_CD, req.getParameter(TYPE_CD));
		reqParams.put(DATE_RANGE, req.getParameter(DATE_RANGE));
		return reqParams;
	}

	/**
	* Retrieve all the updates
	* @param updateId
	* @param statusCd
	* @param typeCd
	* @param dateRange
	* @return
	*/
	public List<Object> getAllUpdates(String updateId) {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String sql = formatRetrieveAllQuery(schema, updateId);

		List<Object> params = new ArrayList<>();
		if(!StringUtil.isEmpty(updateId)) params.add(updateId);

		DBProcessor db = new DBProcessor(dbConn, schema);
		List<Object>  updates = db.executeSelect(sql, params, new UpdateVO());
		log.debug("loaded " + updates.size() + " updates");
		return updates;
	}

	/**
	 * Generates Query for getting all records/single record.  Used by Solr Indexer.
	 * @param updateId
	 * @param schema
	 * @return
	 */
	public static String formatRetrieveAllQuery(String schema, String updateId) {
		StringBuilder sql = new StringBuilder(400);
		sql.append("select a.*, p.first_nm, p.last_nm, b.section_id, b.update_section_xr_id ");
		sql.append("from ").append(schema).append("biomedgps_update a ");
		sql.append("inner join profile p on a.creator_profile_id=p.profile_id ");
		sql.append("left outer join ").append(schema).append("biomedgps_update_section b ");
		sql.append("on a.update_id=b.update_id ");
		if(!StringUtil.isEmpty(updateId)) {
			sql.append("where a.update_id = ? ");
		}
		sql.append("order by a.create_dt");

		log.debug(sql);
		return sql.toString();
	}

	/**
	 * Retrieves total number of update records in db.
	 * @param req
	 * @param schema
	 * @return
	 */
	protected int getUpdateCount(ActionRequest req, String schema) {
		int count = 0;
		String search = StringUtil.checkVal(req.getParameter("search")).toUpperCase();

		StringBuilder sql = new StringBuilder(100);
		sql.append("select count(*) from ").append(schema).append("biomedgps_update ");
		if (search.length() > 0) sql.append("where upper(title_txt) like ? ");

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			if (search.length() > 0) {
				ps.setString(1, "%" + search + "%");
			}

			ResultSet rs = ps.executeQuery();
			rs.next();
			count = rs.getInt(1);
		} catch (SQLException e) {
			log.error(e);
		}
		return count;
	}

	/**
	 * Retrieve list of Updates containing historical Revisions.
	 * @param parameter
	 * @return
	 */
	protected List<Object> getHistory(String updateId) {
		String sql = formatHistoryRetrieveQuery();

		List<Object> params = new ArrayList<>();
		if (!StringUtil.isEmpty(updateId)) params.add(updateId);

		DBProcessor db = new DBProcessor(dbConn);
		List<Object>  updates = db.executeSelect(sql, params, new UpdateVO());
		log.debug("loaded " + updates.size() + " updates");
		return updates;
	}

	/**
	 * Build History Sql Retrieval against ChangeLog Table.
	 * @param schema
	 * @return
	 */
	private String formatHistoryRetrieveQuery() {
		StringBuilder sql = new StringBuilder(400);
		sql.append("select b.wc_sync_id as update_id, a.diff_txt as message_txt, ");
		sql.append("a.create_dt as publish_dt, c.first_nm, c.last_nm from change_log a ");
		sql.append("inner join wc_sync b on a.wc_sync_id = b.wc_sync_id ");
		sql.append("inner join profile c on b.admin_profile_id = c.profile_id ");
		sql.append("where b.wc_key_id = ? order by a.create_dt");

		log.debug(sql);
		return sql.toString();
	}


	/**
	 * Formats the Update retrieval query.
	 * @return
	 */
	public static String formatRetrieveQuery(Map<String, String> reqParams, String schema, boolean isList) {
		StringBuilder sql = new StringBuilder(800);
		sql.append("select a.*, p.first_nm, p.last_nm, ");

		if(isList) {
			sql.append("s.wc_sync_id ");
		} else {
			sql.append("m.market_nm, c.company_nm, pr.product_nm, b.section_id, b.update_section_xr_id ");
		}

		sql.append("from ").append(schema).append("biomedgps_update a ");
		sql.append("inner join profile p on a.creator_profile_id=p.profile_id ");
		if(isList) {
			sql.append("left outer join (select wc_sync_id, wc_key_id, row_number() ");
			sql.append("over (partition by wc_key_id order by create_dt) as rn from wc_sync) s ");
			sql.append("on s.wc_key_id = a.update_id and s.rn = 1 ");
		} else {
			sql.append("left outer join ").append(schema).append("biomedgps_update_section b ");
			sql.append("on a.update_id=b.update_id ");
			sql.append("left outer join ").append(schema).append("biomedgps_market m ");
			sql.append("on a.market_id=m.market_id ");
			sql.append("left outer join ").append(schema).append("biomedgps_company c ");
			sql.append("on a.company_id=c.company_id ");
			sql.append("left outer join ").append(schema).append("biomedgps_product pr ");
			sql.append("on a.product_id=pr.product_id ");
		}
		sql.append("where 1=1 ");
		if (!StringUtil.isEmpty(reqParams.get(UPDATE_ID))) sql.append("and a.update_id=? ");
		if (!StringUtil.isEmpty(reqParams.get(STATUS_CD))) sql.append("and a.status_cd=? ");
		if (!StringUtil.isEmpty(reqParams.get(TYPE_CD))) sql.append("and a.type_cd=? ");
		if (!StringUtil.isEmpty(reqParams.get(SEARCH))) sql.append("and lower(a.title_txt) like ? ");
		String dateRange = reqParams.get(DATE_RANGE);
		if (!StringUtil.isEmpty(dateRange)) {
			if ("1".equals(dateRange)) {
				sql.append("and a.create_dt > CURRENT_DATE - INTERVAL '6 months' ");
			} else if ("2".equals(dateRange)) {
				sql.append("and a.create_dt < CURRENT_DATE - INTERVAL '6 months' ");
			}
		}
		sql.append("order by ").append(reqParams.get(SORT)).append(" ").append(reqParams.get(ORDER));
		sql.append(" limit ? offset ? ");

		log.debug(sql);
		return sql.toString();
	}


	/**
	 * loop and decrypt owner names, which came from the profile table
	 * @param accounts
	 */
	@SuppressWarnings("unchecked")
	protected void decryptNames(List<Object> data) {
		new NameComparator().decryptNames((List<? extends HumanNameIntfc>)(List<?>)data, (String)getAttribute(Constants.ENCRYPT_KEY));
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
		UpdateVO u = new UpdateVO(req);

		try {
			if (isDelete) {
				u.setUpdateId(req.getParameter("pkId"));

				//Load the Record before deletion.
				db.getByPrimaryKey(u);

				//Delete the Record.
				db.delete(u);

				//Delete from Solr.
				deleteFromSolr(u);
			} else {
				db.save(u);

				fixPkids(u, db.getGeneratedPKId());

				//Save Update Sections.
				saveSections(u);

				//Save the Update Document to Solr
				writeToSolr(u);
			}

			req.setParameter(UPDATE_ID, u.getUpdateId());
		} catch (InvalidDataException | DatabaseException e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Manages updating given UpdatesVO with generated PKID and updates sections
	 * to match.
	 * @param u
	 * @param generatedPKId
	 */
	protected void fixPkids(UpdateVO u, String generatedPKId) {
		//Set the UpdateId on UpdatesXRVOs
		if (StringUtil.isEmpty(u.getUpdateId())) {
			//Ensure proper UpdateId and Publish Dt are set.
			u.setUpdateId(generatedPKId);

			for (UpdateXRVO uxr : u.getUpdateSections())
				uxr.setUpdateId(u.getUpdateId());
		}
	}


	/**
	 * Removes an Updates Record from Solr.
	 * @param u
	 */
	protected void deleteFromSolr(UpdateVO u) {
		try (SolrActionUtil sau = new SmarttrakSolrUtil(getAttributes(), false)) {
			sau.removeDocument(u.getUpdateId());
		} catch (Exception e) {
			log.error("Error Deleting from Solr.", e);
		}
		log.debug("removed document from solr");
	}


	/**
	 * Save an UpdatesVO to solr.
	 * @param u
	 */
	protected void writeToSolr(UpdateVO u) {
		UpdateIndexer idx = UpdateIndexer.makeInstance(getAttributes());
		idx.setDBConnection(dbConn);
		idx.addSingleItem(u.getUpdateId());
	}


	/**
	 * Delete old Update Sections and save new ones.
	 * @param u
	 * @throws ActionException
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	protected void saveSections(UpdateVO u) throws ActionException, InvalidDataException, DatabaseException {
		//Delete old Update Section XRs
		deleteSections(u.getUpdateId());

		//Save new Sections.
		DBProcessor db = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		for(UpdateXRVO uxr : u.getUpdateSections())
			db.save(uxr);
	}


	/**
	 * Delete old Update Section XRs 
	 * @param updateId
	 * @throws ActionException 
	 */
	protected void deleteSections(String updateId) throws ActionException {
		StringBuilder sql = new StringBuilder(100);
		sql.append("delete from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_update_section where update_id = ?");

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, updateId);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}

	/**
	 * Convert Bootstrap Table Column Names to db names. 
	 * @return
	 */
	private void buildSortMapper() {
		sortMapper = new HashMap<>();
		sortMapper.put("titleTxt", "title_txt");
		sortMapper.put("publishDt", "publish_dt");
		sortMapper.put("typeNm", "type_cd");
		sortMapper.put("statusNm", "status_cd");
	}
}