/**
 *
 */
package com.biomed.smarttrak.action;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.util.UpdateIndexer;
import com.biomed.smarttrak.vo.UpdateVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBActionAdapter;
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
public class UpdatesWeeklyReportAction extends SBActionAdapter {
	public static final String TIME_RANGE_DAILY = "daily";
	public static final String TIME_RANGE_WEEKLY = "weekly";
	public static final String EMAIL_UPDATES = "emailUpdates";
	
	public UpdatesWeeklyReportAction() {
		super();
	}

	public UpdatesWeeklyReportAction(ActionInitVO actionInit) {
		super(actionInit);
	}

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

	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}

	/**
	 * Batch update Updates that are modified in Weekly Report.
	 * @param updates
	 */
	public void processUpdates(List<UpdateVO> updates) {
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

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		//if reloadList is present, this is an ajax call. Flag for view.
		if(req.hasParameter("reloadList"))
			req.setAttribute("reload", req.getParameter("reloadList"));
				
		List<Object> updates = getUpdates(req);
		
		putModuleData(updates);
	
	}

	/**
	 * Retrieve all the updates. Can be filtered by admin, sections, and/or time range
	 * @param sectionIds
	 * @param req
	 * @return
	 */
	public List<Object> getUpdates(ActionRequest req) {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		//declare sectionId, time range and boolean flag
		String[] sectionIds = req.getParameterValues("sectionId");
		
		//fetch the appropriate query to be executed
		String sql = formatRetrieveQuery(schema, sectionIds, req);
		log.debug("Updates query to execute: " +sql);
		List<Object> params = new ArrayList<>();
		if(sectionIds != null && sectionIds.length > 0 && !("All").equalsIgnoreCase(sectionIds[0])){
			for (String section : sectionIds) {
				params.add(section);
			}			
		}
		
		DBProcessor db = new DBProcessor(dbConn, schema);
		List<Object>  updates = db.executeSelect(sql, params, new UpdateVO());
		log.debug("loaded " + updates.size() + " updates");
		return updates;
	}

	/**
	 * Build the DBProcessor Retrieval Query for Updates.
	 * @param schema
	 * @param sectionIds
	 * @param req
	 * @return
	 */
	public String formatRetrieveQuery(String schema, String[] sectionIds, ActionRequest req) {
		//grab values from request
		boolean isAdmin = req.hasParameter("actionType");
		String timeRangeCd = req.getParameter("timeRangeCd");
		boolean emailUpdates = req.hasParameter(EMAIL_UPDATES);
		
		StringBuilder sql = new StringBuilder(400);
		sql.append("select a.*, b.section_id, b.update_section_xr_id ");
		sql.append("from ").append(schema).append("biomedgps_update a ");
		sql.append("inner join ").append(schema).append("biomedgps_update_section b ");
		sql.append("on a.update_id=b.update_id where ");

		//If we have SectionId(s), filter results by Sections.	
		if(sectionIds != null && sectionIds.length > 0 
				&& !("All").equalsIgnoreCase(sectionIds[0])){//account for 'All' option
			sql.append("b.section_id in (");
			for (int i = 0; i < sectionIds.length; i++) {
				if(i != 0){
					sql.append(", "); 
				}
				sql.append("?");
			}
			sql.append(") and ");		
		}

		//If we are on a public site, filter by Email Updates only.
		if(!isAdmin) {
			sql.append("a.email_flg = 1 and ");
		}

		//Filter results by the past day or within in the current week.
		if(timeRangeCd != null && timeRangeCd.equalsIgnoreCase(TIME_RANGE_DAILY)){
			sql.append("a.publish_dt >= date_trunc('day', current_timestamp) - interval '1' day ");
			sql.append("and a.publish_dt < date_trunc('day', current_timestamp) ");
			sql.append("and a.status_cd in ('R','N') ");
		}else if(emailUpdates){//updates for email use previous week range
			sql.append("a.publish_dt >= cast(date_trunc('week', current_date) as date) - 7 ");
			sql.append("and a.publish_dt < cast(date_trunc('week', current_date) as date) - 1 ");
			sql.append("and a.status_cd in ('R','N') ");
		}else{//default to weekly(these are the updates that are under review(/manage) based from create dt)
			sql.append("a.create_dt >= cast(date_trunc('week', current_date) as date) - 1 ");
			sql.append("and a.create_dt < cast(date_trunc('week', current_date) as date) + 5 ");
			sql.append("and a.status_cd = 'N' ");
		}
		sql.append("order by a.type_cd, a.order_no, a.publish_dt desc");
		
		return sql.toString();
	}

}