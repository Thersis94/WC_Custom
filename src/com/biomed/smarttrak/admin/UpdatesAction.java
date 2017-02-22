/**
 *
 */
package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.biomed.smarttrak.admin.user.HumanNameIntfc;
import com.biomed.smarttrak.admin.user.NameComparator;
import com.biomed.smarttrak.vo.UpdatesVO;
import com.biomed.smarttrak.vo.UpdatesXRVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;
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
	protected static final String UPDATE_ID = "updateId"; //req param
	public static final String ROOT_NODE_ID = "MASTER_ROOT";

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
		if (!req.hasParameter("loadData") && !req.hasParameter(UPDATE_ID) ) return;

		String updateId = req.hasParameter(UPDATE_ID) ? req.getParameter(UPDATE_ID) : null;
		String statusCd = req.getParameter("statusCd");
		String typeCd = req.getParameter("typeCd");
		String dateRange = req.getParameter("dateRange");
		List<Object> updates = getUpdates(updateId, statusCd, typeCd, dateRange);

		decryptNames(updates);

		putModuleData(updates);
	}

	public List<Object> getUpdates(String updateId, String statusCd, String typeCd, String dateRange) {

		String schema = (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA);
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
		sql.append("select a.*, p.first_nm, p.last_nm, b.section_id ");
		sql.append("from ").append(schema).append("biomedgps_update a ");
		sql.append("inner join profile p on a.creator_profile_id=p.profile_id ");
		sql.append("left outer join ").append(schema).append("biomedgps_update_section b ");
		sql.append("on a.update_id=b.update_id where 1=1 ");
		if (!StringUtil.isEmpty(updateId)) sql.append("and a.update_id=? ");
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
	 * Load the Section Tree so that Hierarchies can be generated.
	 * @param req
	 * @throws ActionException
	 */
	public Tree loadSections() {
		//load the section hierarchy Tree from superclass
		Tree t = loadDefaultTree();

		//Generate the Node Paths using Node Names.
		t.buildNodePaths(t.getRootNode(), "~", true);
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
				u.setUpdateId("pkId");
				db.delete(u);
				deleteFromSolr(u);
			} else {
				db.save(u);

				//Set the UpdateId on UpdatesXRVOs
				if(StringUtil.isEmpty(u.getUpdateId())) {

					//Ensure proper UpdateId and Publish Dt are set.
					u.setUpdateId(db.getGeneratedPKId());
					u.setPublishDt(new Date());

					for(UpdatesXRVO uxr : u.getUpdateSections()) {
						uxr.setUpdateId(u.getUpdateId());
					}
				}

				//Save Update Sections.
				saveSections(u);

				//Add hierarchies to the Section
				u.setHierarchies(loadSections());

				//Save the Update Document to Solr
				saveToSolr(u);
			}
		} catch (InvalidDataException | DatabaseException e) {
			throw new ActionException(e);
		}
	}

	/**
	 * Removes an Updates Record from Solr.
	 * @param u
	 */
	protected void deleteFromSolr(UpdatesVO u) {
		try(SolrActionUtil sau = new SolrActionUtil(getAttributes())) {
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
	protected void saveToSolr(UpdatesVO u) {
		try(SolrActionUtil sau = new SolrActionUtil(getAttributes())) {
			sau.addDocument(u);
		} catch (Exception e) {
			log.error("Error Saving to Solr.", e);
		}
		log.debug("added document to solr");
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

		DBProcessor db = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));

		//Save new Sections.
		for(UpdatesXRVO uxr : u.getUpdateSections()) {
			db.save(uxr);
		}
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

		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
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