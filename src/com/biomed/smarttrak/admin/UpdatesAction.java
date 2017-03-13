package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.util.SmarttrakSolrUtil;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.util.UpdateIndexer;
import com.biomed.smarttrak.vo.UpdatesVO;
import com.biomed.smarttrak.vo.UpdatesXRVO;
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
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Feb 14, 2017
 ****************************************************************************/
public class UpdatesAction extends AbstractTreeAction {
	public static final String UPDATE_ID = "updateId"; //req param
	public static final String ROOT_NODE_ID = MASTER_ROOT;

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

	public UpdatesAction() {
		super();
	}
	public UpdatesAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	public void retrieve(ActionRequest req) throws ActionException {
		//loadData gets passed on the ajax call.  If we're not loading data simply go to view to render the bootstrap 
		//table into the view (which will come back for the data).
		if (!req.hasParameter("loadData") && !req.hasParameter("loadhistory") && !req.hasParameter(UPDATE_ID) ) return;
		String updateId = req.hasParameter(UPDATE_ID) ? req.getParameter(UPDATE_ID) : null;

		List<Object> data;
		if(req.hasParameter("loadHistory")) {
			data = getHistory(req.getParameter("historyId"));
		} else {
			String statusCd = req.getParameter("statusCd");
			String typeCd = req.getParameter("typeCd");
			String dateRange = req.getParameter("dateRange");
			data = getUpdates(updateId, statusCd, typeCd, dateRange);
		}

		decryptNames(data);

		putModuleData(data);
	}


	/**
	 * Retrieve list of Updates containing historical Revisions.
	 * @param parameter
	 * @return
	 */
	protected List<Object> getHistory(String updateId) {
		String sql = formatHistoryRetrieveQuery(updateId);

		List<Object> params = new ArrayList<>();
		if (!StringUtil.isEmpty(updateId)) params.add(updateId);

		DBProcessor db = new DBProcessor(dbConn);
		List<Object>  updates = db.executeSelect(sql, params, new UpdatesVO());
		log.debug("loaded " + updates.size() + " updates");
		return updates;
	}

	/**
	 * Build History Sql Retrieval against ChangeLog Table.
	 * @param updateId
	 * @param schema
	 * @return
	 */
	private String formatHistoryRetrieveQuery(String updateId) {
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
	 * Retrieve all the updates
	 * @param updateId
	 * @param statusCd
	 * @param typeCd
	 * @param dateRange
	 * @return
	 */
	public List<Object> getUpdates(String updateId, String statusCd, String typeCd, String dateRange) {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String sql = formatRetrieveQuery(updateId, statusCd, typeCd, dateRange, schema);

		List<Object> params = new ArrayList<>();
		if (!StringUtil.isEmpty(updateId)) params.add(updateId);
		if (!StringUtil.isEmpty(statusCd)) params.add(statusCd);
		if (!StringUtil.isEmpty(typeCd)) params.add(Convert.formatInteger(typeCd));

		DBProcessor db = new DBProcessor(dbConn, schema);
		List<Object>  updates = db.executeSelect(sql, params, new UpdatesVO());
		log.debug("loaded " + updates.size() + " updates");
		return updates;
	}


	/**
	 * Formats the account retrieval query.
	 * @return
	 */
	public static String formatRetrieveQuery(String updateId, String statusCd, String typeCd, String dateRange, String schema) {
		StringBuilder sql = new StringBuilder(400);
		sql.append("select a.*, p.first_nm, p.last_nm, b.section_id, b.update_section_xr_id ");
		sql.append("from ").append(schema).append("biomedgps_update a ");
		sql.append("inner join profile p on a.creator_profile_id=p.profile_id ");
		sql.append("left outer join ").append(schema).append("biomedgps_update_section b ");
		sql.append("on a.update_id=b.update_id where 1=1 ");
		if (!StringUtil.isEmpty(updateId)) sql.append("and a.update_id=? ");
		if (!StringUtil.isEmpty(statusCd)) sql.append("and a.status_cd=? ");
		if (!StringUtil.isEmpty(typeCd)) sql.append("and a.type_cd=? ");
		if (!StringUtil.isEmpty(dateRange)) {
			if ("1".equals(dateRange)) {
				sql.append("and a.create_dt > CURRENT_DATE - INTERVAL '6 months' ");
			} else if ("2".equals(dateRange)) {
				sql.append("and a.create_dt < CURRENT_DATE - INTERVAL '6 months' ");
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
		UpdatesVO u = new UpdatesVO(req);

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
	protected void fixPkids(UpdatesVO u, String generatedPKId) {
		//Set the UpdateId on UpdatesXRVOs
		if (StringUtil.isEmpty(u.getUpdateId())) {
			//Ensure proper UpdateId and Publish Dt are set.
			u.setUpdateId(generatedPKId);

			for (UpdatesXRVO uxr : u.getUpdateSections())
				uxr.setUpdateId(u.getUpdateId());
		}
	}


	/**
	 * Removes an Updates Record from Solr.
	 * @param u
	 */
	protected void deleteFromSolr(UpdatesVO u) {
		try (SolrActionUtil sau = new SmarttrakSolrUtil(getAttributes())) {
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
	protected void writeToSolr(UpdatesVO u) {
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
	protected void saveSections(UpdatesVO u) throws ActionException, InvalidDataException, DatabaseException {
		//Delete old Update Section XRs
		deleteSections(u.getUpdateId());

		//Save new Sections.
		DBProcessor db = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		for(UpdatesXRVO uxr : u.getUpdateSections())
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


	@Override
	public String getCacheKey() {
		return null;
	}
}