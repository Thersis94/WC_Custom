package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.biomed.smarttrak.util.SmarttrakSolrUtil;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.util.UpdateIndexer;
import com.biomed.smarttrak.vo.UpdateVO;
import com.biomed.smarttrak.vo.UpdateXRVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.DBUtil;
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
public class UpdatesAction extends ManagementAction {
	public static final String UPDATE_ID = "updateId"; //req param
	public static final String SORT = "sort"; //req param
	public static final String ORDER = "order"; //req param
	public static final String START_DATE = "startDt"; //req param
	public static final String END_DATE = "endDt"; //req param
	
	public static final String STATUS_CD = "statusCd"; //req param
	public static final String TYPE_CD = "typeCd"; //req param
	public static final String SEARCH = "search"; //req param
	private static final String SECTION_ID = "filterSectionId[]";

	/**
	 * @deprecated not sure where this is used, possibly JSPs.  Unlikely it belongs here so reference it from it's source location.
	 */
	@Deprecated
	public static final String ROOT_NODE_ID = SectionHierarchyAction.MASTER_ROOT;

	//ChangeLog TypeCd.  Using the key we swap on for actionType in AdminControllerAction so we can get back.
	public static final String UPDATE_TYPE_CD = "updates";


	public enum UpdateType {
		//NOTE: The order of these UpdateType values impacts the compareTo() method.  Impacts UpdatesEditionSorter if you rearrange these.
		// you cannot override the compareTo() method, but an order and "int compare(int i)" method could be added here.
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
			log.debug("count " + count);
		}

		decryptNames(data);

		addProductCompanyData(data);

		if(count > 0) {
			putModuleData(data, count, false);
		} else {
			putModuleData(data);
		}

		//when an add/edit form, load list of BiomedGPS Staff for the "Author" drop-down
		if (req.hasParameter(UPDATE_ID))
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
		int rpp = Convert.formatInteger(req.getParameter("limit"), 10);
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);

		String sql = formatRetrieveQuery(req, schema, req.hasParameter("loadData"), false);

		log.debug(" sql: " + sql);
		
		List<Object> params = new ArrayList<>();
		if (req.hasParameter(UPDATE_ID)) params.add(req.getParameter(UPDATE_ID));
		if (req.hasParameter(STATUS_CD)) params.add(req.getParameter(STATUS_CD));
		if (req.hasParameter(TYPE_CD)) params.add(Convert.formatInteger(req.getParameter(TYPE_CD)));
		if (req.hasParameter(SEARCH)) {
			String searchData = "%" + StringUtil.checkVal(req.getParameter(SEARCH)).toLowerCase() + "%";
			params.add(searchData);
			params.add(searchData);
		}
		//check if dates were passed before adding to params list
		if(req.hasParameter(START_DATE)) 
			params.add(Convert.formatDate(req.getParameter(START_DATE)));
		if(req.hasParameter(END_DATE)) 
			params.add(Convert.formatDate(req.getParameter(END_DATE)));
			
		String[] sectionIds = req.hasParameter(SECTION_ID) ? req.getParameterValues(SECTION_ID) : null;
			
		if (sectionIds != null) { //restrict to certain sections only
			for (String s : getSectionFamily(sectionIds))
				params.add(s);
		}
		params.add(rpp);
		params.add(start);

		DBProcessor db = new DBProcessor(dbConn, schema);
		return db.executeSelect(sql, params, new UpdateVO());
	}


	/**
	 * processes the section ides supplied and gets childern or granchildren as needed to mimic the data returned by the 
	 * front public side search
	 * @param sectionIds
	 * @return
	 */
	private Set<String> getSectionFamily(String[] sectionIds) {
		if (sectionIds == null) return new HashSet<>();
		//set up a set to hold all the ids
		Set<String> set = new HashSet<>();
		
		// load the section hierarchy Tree from superclass
		Tree t = loadDefaultTree();

		// Generate the Node Paths using Node Names.
		t.buildNodePaths(t.getRootNode(), SearchDocumentHandler.HIERARCHY_DELIMITER, true);
		
		//process ids according to their depth level
		for (String s : sectionIds){
			int depth = t.findNode(s).getDepthLevel();
			//if the id is already in the set its a child or grand child and we don't need to process it again
			if (set.contains(s)) continue;
			
			if (depth < 3) {
				set.add(s);
				processTwoNodeLayers(set, t, s);
			} else if (depth == 3){
				set.add(s);
				processOneNodeLayer(set, t, s);
			}else {
				set.add(s);
			}
		}

		return set;
	}

	/**
	 * adds the ids children from the tree to the set
	 * @param set
	 * @param t
	 * @param s
	 */
	private void processOneNodeLayer(Set<String> set, Tree t, String s) {
		for (Node n : t.findNode(s).getChildren()){
			set.add(n.getNodeId());
			}
	}

	/**
	 * adds the ids children and grand children to the set.
	 * @param set
	 * @param t
	 */
	private void processTwoNodeLayers(Set<String> set, Tree t, String s) {
		for (Node n : t.findNode(s).getChildren()){
			set.add(n.getNodeId());
			for (Node g : n.getChildren()){
				set.add(g.getNodeId());
			}
		}
	}

	/**
	 * Retrieve all the updates - called by the Solr OOB indexer
	 * @param updateId
	 * @param statusCd
	 * @param typeCd
	 * @param dateRange
	 * @return
	 */
	public List<Object> getAllUpdates(String updateId) {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String sql = formatRetrieveAllQuery(schema, updateId);

		log.debug(sql);
		List<Object> params = new ArrayList<>();
		if (!StringUtil.isEmpty(updateId)) params.add(updateId);

		DBProcessor db = new DBProcessor(dbConn, schema);
		List<Object>  updates = db.executeSelect(sql, params, new UpdateVO());
		log.debug("loaded " + updates.size() + " updates");
		return updates;
	}

	/**
	 * takes the current list of updates and adds the company information to any product updates
	 * @param updates
	 *TODO change query to "in (1,2,3)" instead of looping outside the query - Zoho SC-232
	 */
	private void addProductCompanyData(List<Object> updates) {
		log.debug("adding company short name and id ");
		for (Object ob : updates) {
			//loops all the updates and see if they have a product id
			UpdateVO vo = (UpdateVO) ob;

			if (StringUtil.isEmpty(vo.getProductId())) continue;

			StringBuilder sb = new StringBuilder(161);
			sb.append("select c.company_id, c.short_nm_txt from ").append(customDbSchema).append("biomedgps_product p ");
			sb.append(INNER_JOIN).append(customDbSchema).append("biomedgps_company c on p.company_id = c.company_id ");
			sb.append("where p.product_id=?");

			try (PreparedStatement ps = dbConn.prepareStatement(sb.toString())) {
				ps.setString(1, vo.getProductId());
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					vo.setCompanyId(rs.getString(1));
					vo.setCompanyNm(rs.getString(2));
				}
			} catch(SQLException sqle) {
				log.error("could not confirm security by id ", sqle);
			}
		}
	}

	/**
	 * Generates Query for getting all records/single record.  Used by Solr Indexer.
	 * Note: This query closely compliments what we use in UpdatesScheduledAction (for a real-time load of Updates)
	 * @param updateId
	 * @param schema
	 * @return
	 */
	protected String formatRetrieveAllQuery(String schema, String updateId) {
		StringBuilder sql = new StringBuilder(400);
		sql.append("select up.update_id, up.title_txt, up.message_txt, up.publish_dt, up.type_cd, us.update_section_xr_id, us.section_id, ");
		sql.append("c.short_nm_txt as company_nm, prod.short_nm as product_nm, up.order_no, ");
		sql.append("coalesce(up.product_id,prod.product_id) as product_id, coalesce(up.company_id, c.company_id) as company_id, ");
		sql.append("m.short_nm as market_nm, coalesce(up.market_id, m.market_id) as market_id, ");
		sql.append("'").append(getAttribute(Constants.QS_PATH)).append("' as qs_path, up.create_dt "); //need to pass this through for building URLs
		sql.append("from ").append(schema).append("biomedgps_update up ");
		sql.append("inner join profile p on up.creator_profile_id=p.profile_id ");
		sql.append(LEFT_OUTER_JOIN).append(schema).append("biomedgps_update_section us on up.update_id=us.update_id ");
		sql.append(LEFT_OUTER_JOIN).append(schema).append("biomedgps_product prod on up.product_id=prod.product_id ");
		sql.append(LEFT_OUTER_JOIN).append(schema).append("biomedgps_company c on (up.company_id is not null and up.company_id=c.company_id) or (up.product_id is not null and prod.company_id=c.company_id) "); //join from the update, or from the product.
		sql.append(LEFT_OUTER_JOIN).append(schema).append("biomedgps_market m on up.market_id=m.market_id ");
		if (!StringUtil.isEmpty(updateId))
			sql.append("where up.update_id = ? ");

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
		String sql = formatRetrieveQuery(req, schema, true, true);
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			setStatementValues(ps, req);

			ResultSet rs = ps.executeQuery();
			if (rs.next()) 
				return rs.getInt(1);

		} catch (SQLException e) {
			log.error(e);
		}
		return 0;
	}

	/**
	 * Helper method to set the values to the PreparedStatement
	 * @param ps
	 * @param req
	 * @throws SQLException
	 */
	private void setStatementValues(PreparedStatement ps, ActionRequest req) throws SQLException{
		int i = 0;
		
		if (req.hasParameter(UPDATE_ID)) ps.setString(++i, req.getParameter(UPDATE_ID));
		if (req.hasParameter(STATUS_CD)) ps.setString(++i, req.getParameter(STATUS_CD));
		if (req.hasParameter(TYPE_CD)) ps.setInt(++i, Convert.formatInteger(req.getParameter(TYPE_CD)));
		if (req.hasParameter(SEARCH)) {
			String searchData = "%" + StringUtil.checkVal(req.getParameter(SEARCH)).toLowerCase() + "%";
			ps.setString(++i, searchData);
			ps.setString(++i, searchData);
		}
		if(req.hasParameter(START_DATE))
			ps.setDate(++i, Convert.formatSQLDate(req.getParameter(START_DATE)));
		if(req.hasParameter(END_DATE))
			ps.setDate(++i, Convert.formatSQLDate(req.getParameter(END_DATE)));
		
		String[] sectionIds = req.hasParameter(SECTION_ID) ? req.getParameterValues(SECTION_ID) : null;
		if (sectionIds != null) { //restrict to certain sections only
			for (String s : getSectionFamily(sectionIds))
				ps.setString(++i, s);
		}
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
	public String formatRetrieveQuery(ActionRequest req, String schema, boolean isList, boolean isCount) {
		StringBuilder sql = new StringBuilder(800);
		sql.append(buildSelectClause(isList, isCount));
		sql.append(buildFromClause(isList, schema));
		sql.append(buildWhereClause(req));

		if (!isCount) {
			sql.append("order by ").append(StringUtil.checkVal(sortMapper.get(req.getParameter(SORT)), "publish_dt"));
			sql.append(" ").append(StringUtil.checkVal(req.getParameter(ORDER), "asc"));
			sql.append(", create_dt desc limit ? offset ? ");
		}

		log.debug(" sql "+sql);
		return sql.toString();
	}

	/**
	 * Build the select clause based on whether this is 
	 * a list select and whether it is a count select.
	 * @param isList
	 * @param isCount
	 * @return
	 */
	private String buildSelectClause(boolean isList, boolean isCount) {
		StringBuilder sql = new StringBuilder(150);
		sql.append("select ");
		if (isCount) {
			sql.append("count(distinct a.update_id) ");
		} else {
			sql.append("distinct a.*, p.first_nm, p.last_nm, ");
			if (isList) {
				sql.append("s.wc_sync_id ");
			} else {
				sql.append("m.market_nm, c.company_nm, pr.product_nm, b.section_id, b.update_section_xr_id ");
			}
		}
		return sql.toString();
	}


	/**
	 * Build the from clause based on whether it is a list request or not.
	 * @param isList
	 * @param schema
	 * @return
	 */
	private String buildFromClause(boolean isList, String schema) {
		StringBuilder sql = new StringBuilder(300);
		sql.append("from ").append(schema).append("biomedgps_update a ");
		sql.append("inner join profile p on a.creator_profile_id=p.profile_id ");
		if (isList) {
			sql.append(LEFT_OUTER_JOIN).append("(select wc_sync_id, wc_key_id, row_number() ");
			sql.append("over (partition by wc_key_id order by create_dt) as rn from wc_sync) s ");
			sql.append("on s.wc_key_id=a.update_id and s.rn=1 ");
			sql.append(LEFT_OUTER_JOIN).append(schema).append("biomedgps_update_type ut on ut.TYPE_CD=a.TYPE_CD ");
			sql.append(LEFT_OUTER_JOIN).append(schema).append("biomedgps_update_section b ");
			sql.append("on a.update_id=b.update_id ");
		} else {
			sql.append(LEFT_OUTER_JOIN).append(schema).append("biomedgps_update_section b ");
			sql.append("on a.update_id=b.update_id ");
			sql.append(LEFT_OUTER_JOIN).append(schema).append("biomedgps_market m ");
			sql.append("on a.market_id=m.market_id ");
			sql.append(LEFT_OUTER_JOIN).append(schema).append("biomedgps_company c ");
			sql.append("on a.company_id=c.company_id ");
			sql.append(LEFT_OUTER_JOIN).append(schema).append("biomedgps_product pr ");
			sql.append("on a.product_id=pr.product_id ");
		}
		return sql.toString();
	}


	/**
	 * Build the where clause from the request parameters
	 * @param reqParams
	 * @return
	 */
	private String buildWhereClause(ActionRequest req) {
		StringBuilder sql = new StringBuilder(200);
		sql.append("where 1=1 ");
		if (req.hasParameter(UPDATE_ID)) sql.append("and a.update_id=? ");
		if (req.hasParameter(STATUS_CD)) sql.append("and a.status_cd=? ");
		if (req.hasParameter(TYPE_CD)) sql.append("and a.type_cd=? ");
		if (req.hasParameter(SEARCH)) {
			sql.append("and (lower(a.title_txt) like ? ");
			sql.append("or lower(a.message_txt) like ? ) ");
		}
		//check if dates were passed before appending to query
		if(req.hasParameter(START_DATE)) sql.append("and a.create_dt >= ? ");
		if(req.hasParameter(END_DATE)) sql.append("and a.create_dt <= ? ");

		String[] sectionIds = req.hasParameter(SECTION_ID) ? req.getParameterValues(SECTION_ID) : null;
		if (sectionIds != null && sectionIds.length > 0) { //restrict to certain sections only
			sql.append("and b.section_id in (");
			DBUtil.preparedStatmentQuestion(getSectionFamily(sectionIds).size(), sql);
			sql.append(") ");
		}
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
		return loadDefaultTree();
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		if (Convert.formatBoolean(req.getParameter("markReviewed"))) {
			markReviewed(req.getParameter(UPDATE_ID));
		} else {
			saveRecord(req, false);
		}
	}


	/**
	 * Change the supplied update's status code to Reviewed
	 * @param updateId
	 * @throws ActionException
	 */
	private void markReviewed(String updateId) throws ActionException {
		StringBuilder sql = new StringBuilder(100);
		sql.append("UPDATE ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_UPDATE set status_cd = 'R' where UPDATE_ID = ?");

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, updateId);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new ActionException(e);
		}
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

		// The form used to send this update can come from either the updates tool or 
		// we need the updates review tool. If it has come from the review tool 
		// the end redirect to send the user back there instead of the updates tool
		if (Convert.formatBoolean(req.getParameter("reviewUpdate")))
			req.setAttribute(Constants.REDIRECT_URL, "?actionType=uwr");

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
		sortMapper.put("typeNm", "type_nm");
		sortMapper.put("statusNm", "status_cd");
	}
}