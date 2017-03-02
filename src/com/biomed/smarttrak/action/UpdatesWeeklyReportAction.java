/**
 *
 */
package com.biomed.smarttrak.action;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.vo.UpdatesVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
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

	public UpdatesWeeklyReportAction() {
		super();
	}

	public UpdatesWeeklyReportAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	public void build(ActionRequest req) throws ActionException {

		//Get Params off the request.
		String [] updateIds = req.getParameterValues("updateId");
		String [] orderNos = req.getParameterValues("orderNo");
		String [] visibleFlgs = req.getParameterValues("visibleFlg");

		List<UpdatesVO> updates = new ArrayList<>();

		//Build Update VOs
		for(int i = 0; i < updateIds.length; i++) {
			UpdatesVO u = new UpdatesVO();
			u.setUpdateId(updateIds[i]);
			u.setOrderNo(Convert.formatInteger(orderNos[i]));
			u.setVisibleFlg(Convert.formatInteger(visibleFlgs[i]));
			updates.add(u);
		}

		//Save Changes.
		processUpdates(updates);

		//Build Message.
		StringBuilder msg = new StringBuilder(50);
		msg.append(updates.size()).append(" records saved successfully.");

		//Return Message on request.
		this.putModuleData(msg.toString());
	}

	public void update(ActionRequest req) throws ActionException {
		super.update(req);
	}

	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}

	/**
	 * Batch update Updates that are modified in Weekly Report.
	 * @param updates
	 */
	public void processUpdates(List<UpdatesVO> updates) {
		try(PreparedStatement ps = dbConn.prepareStatement(getUpdatesBatchSql())) {
			for(UpdatesVO u : updates) {
				ps.setInt(1, u.getOrderNo());
				ps.setInt(2, u.getVisibleFlg());
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
		sql.append("biomedgps_update set order_no = ?, visible_flg = ? where update_id = ?");
		return sql.toString();
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {

		String sectionId = req.getParameter("sectionId");

		List<Object> updates = getUpdates(sectionId, req.hasParameter("actionType"));

		putModuleData(updates);
	}

	/**
	 * Retrieve all the updates
	 * @param isAdmin 
	 * @param updateId
	 * @param statusCd
	 * @param typeCd
	 * @param dateRange
	 * @return
	 */
	public List<Object> getUpdates(String sectionId, boolean isAdmin) {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String sql = formatRetrieveQuery(schema, sectionId, isAdmin);

		List<Object> params = new ArrayList<>();
		if (!StringUtil.isEmpty(sectionId)) params.add(sectionId);

		DBProcessor db = new DBProcessor(dbConn, schema);
		List<Object>  updates = db.executeSelect(sql, params, new UpdatesVO());
		log.debug("loaded " + updates.size() + " updates");
		return updates;
	}

	/**
	 * Build the DBProcessor Retrieval Query for Updates.
	 * @param schema
	 * @param sectionId
	 * @param isAdmin 
	 * @return
	 */
	public String formatRetrieveQuery(String schema, String sectionId, boolean isAdmin) {
		StringBuilder sql = new StringBuilder(400);
		sql.append("select a.*, b.section_id, b.update_section_xr_id ");
		sql.append("from ").append(schema).append("biomedgps_update a ");
		sql.append("inner join ").append(schema).append("biomedgps_update_section b ");
		sql.append("on a.update_id=b.update_id where ");

		//If we have a SectionId, filter results by Section.
		if(!StringUtil.isEmpty(sectionId)) {
			sql.append("b.section_id = ? and ");
		}

		//If we are on a public site, filter by visible Updates only.
		if(!isAdmin) {
			sql.append("a.visible_flg = 1 and ");
		}

		//Filter by only results in the current week.
		sql.append("a.create_dt >= cast(date_trunc('week', current_date) as date) - 1 ");
		sql.append("and a.create_dt < cast(date_trunc('week', current_date) as date) + 5 ");
		sql.append("order by a.type_cd, a.order_no, a.create_dt");

		log.debug(sql);
		return sql.toString();
	}
}