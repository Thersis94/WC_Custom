/**
 *
 */
package com.biomed.smarttrak.admin;

import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.vo.UpdatesVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: UpdatesAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Action for managing Update Notifications.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author raptor
 * @version 1.0
 * @since Feb 14, 2017
 ****************************************************************************/
public class UpdatesAction extends SBActionAdapter {
	protected static final String UPDATE_ID = "updateId"; //req param

	public enum UpdateType {
		Market(12, "Market"),
		Revenues(15, "Revenues"),
		NewProducts(17, "New Products"),
		DealsFinancing(20, "Deals/Financing"),
		ClinicalRegulatory(30, "Clinical/Regulatory"),
		Patents(35, "Patents"),
		Reimbursement(37, "Reimbursement"),
		Announcements(38, "Announcements"),
		Studies(40, "Studies");

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
		if (!req.hasParameter("loadData") && !req.hasParameter(UPDATE_ID)) return;

		String schema = (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA);
		String updateId = req.hasParameter(UPDATE_ID) ? req.getParameter(UPDATE_ID) : null;
		String sql = formatRetrieveQuery(updateId, schema);

		List<Object> params = new ArrayList<>();
		if (updateId != null) params.add(updateId);

		DBProcessor db = new DBProcessor(dbConn, schema);
		List<Object>  updates = db.executeSelect(sql, params, new UpdatesVO());
		log.debug("loaded " + updates.size() + " updates");

		decryptNames(updates);

		if (updateId != null) {
			loadSections(req, schema);
		}

		putModuleData(updates);
	}

	/**
	 * Formats the account retrieval query.
	 * @return
	 */
	protected String formatRetrieveQuery(String updateId, String schema) {
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
	protected void decryptNames(List<Object>  updates) {
		StringEncrypter se;
		try {
			se = new StringEncrypter((String)getAttribute(Constants.ENCRYPT_KEY));
		} catch (EncryptionException e1) {
			return; //cannot use the decrypter, fail fast
		}

		for (Object o : updates) {
			try {
				UpdatesVO u = (UpdatesVO) o;
				u.setFirstNm(se.decrypt(u.getFirstNm()));
				u.setLastNm(se.decrypt(u.getLastNm()));
			} catch (Exception e) {
				//ignoreable
			}
		}
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
		try {
			if (isDelete) {
				db.delete(new UpdatesVO(req));
			} else {
				db.save(new UpdatesVO(req));
			}
		} catch (InvalidDataException | DatabaseException e) {
			throw new ActionException(e);
		}
	}
}
