/**
 *
 */
package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.admin.user.HumanNameIntfc;
import com.biomed.smarttrak.admin.user.NameComparator;
import com.biomed.smarttrak.vo.UpdatesVO;
import com.biomed.smarttrak.vo.UpdatesXRVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
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
public class UpdatesAction extends SBActionAdapter {
	protected static final String UPDATE_ID = "updateId"; //req param

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
		List<Object> updates = getUpdates(updateId);

		decryptNames(updates);

		putModuleData(updates);
	}

	public List<Object> getUpdates(String updateId) {

		String schema = (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA);
		String sql = formatRetrieveQuery(updateId, schema);

		List<Object> params = new ArrayList<>();
		if (updateId != null) params.add(updateId);

		DBProcessor db = new DBProcessor(dbConn, schema);
		List<Object>  updates = db.executeSelect(sql, params, new UpdatesVO());
		log.debug("loaded " + updates.size() + " updates");
		return updates;
	}
	/**
	 * Formats the account retrieval query.
	 * @return
	 */
	public static String formatRetrieveQuery(String updateId, String schema) {
		StringBuilder sql = new StringBuilder(400);
		sql.append("select a.*, p.first_nm, p.last_nm, b.section_id ");
		sql.append("from ").append(schema).append("biomedgps_update a ");
		sql.append("inner join profile p on a.creator_profile_id=p.profile_id ");
		sql.append("left outer join ").append(schema).append("biomedgps_update_section b ");
		sql.append("on a.update_id=b.update_id ");
		if (updateId != null) sql.append("where a.update_id=? ");
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
		UpdatesVO u = new UpdatesVO(req);
		try {
			if (isDelete) {
				db.delete(u);
			} else {
				db.save(u);

				if(StringUtil.isEmpty(u.getUpdateId())) {
					u.setUpdateId(db.getGeneratedPKId());
					for(UpdatesXRVO uxr : u.getUpdateSections()) {
						uxr.setUpdateId(u.getUpdateId());
					}
				}
				//Save Update Sections.
				saveSections(u);

				//Add to Solr if published
				if("R".equals(u.getStatusCd())) {
					try(SolrActionUtil sau = new SolrActionUtil(getAttributes())) {
						sau.addDocument(u);
					} catch (Exception e) {
						log.error("Error Saving to Solr.", e);
					}
					log.debug("added document to solr");
				}
			}
		} catch (InvalidDataException | DatabaseException e) {
			throw new ActionException(e);
		}
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
	private void deleteSections(String updateId) throws ActionException {
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
}