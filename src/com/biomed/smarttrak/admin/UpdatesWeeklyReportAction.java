package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.action.UpdatesEditionAction;
import com.biomed.smarttrak.util.UpdateIndexer;
import com.biomed.smarttrak.vo.UpdateVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Tree;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: UpdatesWeeklyReportAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Action manages Weekly Update Report that gets sent out
 * in emails.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Mar 1, 2017
 ****************************************************************************/
public class UpdatesWeeklyReportAction extends UpdatesEditionAction {
	public static final String TIME_RANGE_DAILY = "daily";
	public static final String TIME_RANGE_WEEKLY = "weekly";

	public UpdatesWeeklyReportAction() {
		super();
	}

	public UpdatesWeeklyReportAction(ActionInitVO actionInit) {
		super(actionInit);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {

		//Get Params off the request.
		String [] updateIds = req.getParameterValues("updateId");
		String [] orderNos = req.getParameterValues("orderNo");
		String [] emailFlgs = req.getParameterValues("emailFlg");

		List<UpdateVO> updates = new ArrayList<>();

		//Build Update VOs
		for(int i = 0; i < updateIds.length; i++) {
			UpdateVO u = new UpdateVO();
			u.setUpdateId(updateIds[i]);
			u.setOrderNo(Convert.formatInteger(orderNos[i]));
			u.setEmailFlg(Convert.formatInteger(emailFlgs[i]));
			updates.add(u);
		}

		//Save Changes.
		processUpdates(updates);

		//Update Solr with Changes.
		updateSolr(updates);

		//Build Message.
		StringBuilder msg = new StringBuilder(50);
		msg.append(updates.size()).append(" records saved successfully.");

		//Return Message on request.
		this.putModuleData(msg.toString());
	}


	/**
	 * Helper method ensures that Order Number is submitted to Solr.
	 * @param updates
	 */
	protected void updateSolr(List<UpdateVO> updates) {
		UpdateIndexer ui = UpdateIndexer.makeInstance(attributes);
		ui.setDBConnection(getDBConnection());
		for(UpdateVO u : updates) {
			ui.addSingleItem(u.getUpdateId());
		}
	}


	/**
	 * Batch update Updates that are modified in Weekly Report.
	 * @param updates
	 */
	protected void processUpdates(List<UpdateVO> updates) {
		try(PreparedStatement ps = dbConn.prepareStatement(getUpdatesBatchSql())) {
			for(UpdateVO u : updates) {
				ps.setInt(1, u.getOrderNo());
				ps.setInt(2, u.getEmailFlg());
				ps.setString(3, u.getUpdateId());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException e) {
			log.error("Error Saving Weekly Updates Report", e);
		}
	}


	/**
	 * Build the Batch Query for updating Updates.
	 * @return
	 */
	private String getUpdatesBatchSql() {
		StringBuilder sql = new StringBuilder(200);
		sql.append("update ").append((String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_update set order_no = ?, email_flg = ? where update_id = ?");
		return sql.toString();
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		//if reloadList is present, this is an ajax call. Flag for view.
		if (req.hasParameter("reloadList"))
			req.setAttribute("reload", req.getParameter("reloadList"));

		super.retrieve(req);
	}


	/*
	 * (non-Javadoc)
	 * @see com.biomed.smarttrak.action.UpdatesEditionAction#fetchUpdates(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected List<UpdateVO> fetchUpdates(ActionRequest req) throws ActionException {
		UpdatesWeeklyReportDataLoader loader = new UpdatesWeeklyReportDataLoader();
		loader.setDBConnection(getDBConnection());
		loader.setAttributes(getAttributes());
		loader.retrieve(req);
		ModuleVO mod = (ModuleVO) loader.getAttribute(Constants.MODULE_DATA);
		return (List<UpdateVO>) mod.getActionData();
	}


	/**
	 * overwritten by subclass (embed action) - puts the data into a container useful to the View.
	 * @param t
	 * @param updates
	 *TODO Billy - remove this method, the superclass will put a Tree into ModuleVO already
	 */
	@Override
	protected void packageDataForDisplay(Tree t, List<UpdateVO> updates) {
		putModuleData(updates, updates.size(), false);
	}
}