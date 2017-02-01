package com.depuy.forefront.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import com.depuy.forefront.action.vo.ActionPlanVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

public class ActionPlanAction extends SBActionAdapter {
	
	public ActionPlanAction() {
		super();
	}
	public ActionPlanAction(ActionInitVO ai) {
		super(ai);
	}
	
	public void delete(ActionRequest req) {
	}
	
	public void retrieve(ActionRequest req) throws ActionException{
		log.debug("Beginning RoutineAction retrieve");
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String hospitalInstId = StringUtil.checkVal(req.getSession().getAttribute(HospitalInstanceAction.HOSP_INST_ID), null);
		String programId = (String) req.getSession().getAttribute(ProgramAction.PROGRAM_ID);
		String stageId = StringUtil.checkVal(req.getParameter("stageId"), null);
		String actionPlanId = StringUtil.checkVal(req.getParameter("actionPlanId"), null);
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		Map<String, ActionPlanVO> data = new LinkedHashMap<String, ActionPlanVO>();
		
		StringBuilder sb = new StringBuilder();
		sb.append("select a.stage_nm, a.stage_id, a.surgery_week_no, b.action_plan_id, b.header_txt, b.hospital_inst_id from ");
		sb.append(customDb).append("FOREFRONT_STAGE a ");
		sb.append("left outer join ").append(customDb).append("FOREFRONT_ACTION_PLAN b ");
		sb.append("on a.stage_id=b.stage_id ");
		sb.append("where a.program_id=? and (b.hospital_inst_id=? or b.hospital_inst_id is null) ");
		if (actionPlanId != null) {
			sb.append("and b.action_plan_id=? ");
		} else if (stageId != null) {
			sb.append("and a.stage_id=? ");
		}
		sb.append("order by a.surgery_week_no, b.hospital_inst_id ");
		log.debug(sb);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, programId);
			ps.setString(2, hospitalInstId);
			if (actionPlanId != null) {
				ps.setString(3, actionPlanId); //get the hospital's record (customized at this stage)
			} else if (stageId != null) {
				ps.setString(3, stageId); //get the program's record (default at this stage)
			}
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				//put all the program's records in first (SQL orderBy), then replace them with whichever got customized (via Map uniqueness)
				data.put(rs.getString("stage_id"), new ActionPlanVO(rs));
			}
			
		} catch (SQLException sqle) {
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		mod.setActionData(new ArrayList<ActionPlanVO>(data.values()));
		log.debug("Returned "+ data.size() + " Action Plans");
		
	}
	
	
	public void build(ActionRequest req) throws ActionException {
		req.setValidateInput(Boolean.FALSE);
		log.debug("Beginning ActionPlan update");
		String msg = "Action Plan Saved Successfully";
		ActionPlanVO vo = new ActionPlanVO(req);
		Boolean cloneItems = Boolean.FALSE;
		
		StringBuilder sb = new StringBuilder();
		if (vo.getActionPlanId() != null) {
			sb.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sb.append("FOREFRONT_ACTION_PLAN set stage_id=?, hospital_inst_id=?, header_txt=?, UPDATE_DT=? where action_plan_id=?");
		} else {
			sb.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sb.append("FOREFRONT_ACTION_PLAN (stage_id, hospital_inst_id, header_txt, create_dt, action_plan_id) ");
			sb.append("values (?,?,?,?,?)");
			vo.setActionPlanId(new UUIDGenerator().getUUID());
			//if this insert is to add a hospital's custom routine, clone the programs Exercises.
			cloneItems = (vo.getHospitalInstId() != null); 
		}
		log.debug(sb);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, vo.getStageId());
			ps.setString(2, vo.getHospitalInstId());
			ps.setString(3, vo.getHeaderText());
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
			ps.setString(5, vo.getActionPlanId());
			ps.executeUpdate();
			
		} catch (SQLException sqle) {
			msg = "A problem occured while saving the Action Plan.";
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		if (cloneItems && req.hasParameter("origActionPlanId")) {
			ActionPlanAssocAction ai = new ActionPlanAssocAction(actionInit);
			ai.setDBConnection(dbConn);
			ai.setAttributes(attributes);
			ai.cloneActionPlan(req.getParameter("origActionPlanId"), vo.getActionPlanId());
			ai = null;
		}
		
		//save Featured & Caregiver Item bindings
		this.saveItemBinding(req.getParameter("featuredItemId"), req.getParameter("caregiverItemId"), vo.getActionPlanId());
		
		req.setValidateInput(Boolean.TRUE);
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		super.sendRedirect(page.getFullPath() + "?actionType=8", msg, req);
	}
	
	private void saveItemBinding(String featuredItemId, String caregiverItemId, String actionPlanId) {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder("delete from ").append(customDb);
		sql.append("FOREFRONT_ACTION_PLAN_XR where action_plan_id=? and ");
		sql.append("(featured_flg=1 or caregiver_flg=1)");
		log.debug(sql);
		
		//delete the existing records
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, actionPlanId);
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("could not delete special item bindings", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		//check if we even need to insert anything
		if (featuredItemId.length() == 0 && caregiverItemId.length() == 0)
			return;
		
		//run the inserts as a batch
		sql = new StringBuilder();
		sql.append("insert into ").append(customDb).append("FOREFRONT_ACTION_PLAN_XR ");
		sql.append("(action_plan_xr_id, action_plan_id, list_item_id, caregiver_flg, ");
		sql.append("featured_flg, create_dt) values (?,?,?,?,?,?)");
		try {
			ps = dbConn.prepareStatement(sql.toString());
			if (featuredItemId.length() > 0) {
				ps.setString(1, new UUIDGenerator().getUUID());
				ps.setString(2, actionPlanId);
				ps.setString(3, featuredItemId);
				ps.setInt(4, 0);
				ps.setInt(5, 1);
				ps.setTimestamp(6, Convert.getCurrentTimestamp());
				ps.addBatch();
			}
			
			if (caregiverItemId.length() > 0) {
				ps.setString(1, new UUIDGenerator().getUUID());
				ps.setString(2, actionPlanId);
				ps.setString(3, caregiverItemId);
				ps.setInt(4, 1);
				ps.setInt(5, 0);
				ps.setTimestamp(6, Convert.getCurrentTimestamp());
				ps.addBatch();
			}
			
			ps.executeBatch();
			
		} catch (SQLException sqle) {
			log.error("could not save special item bindings", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}
}
