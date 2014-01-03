package com.depuy.forefront.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import com.depuy.forefront.action.vo.RoutineVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

public class RoutineAction extends SBActionAdapter {
	
	public RoutineAction() {
		super();
	}
	public RoutineAction(ActionInitVO ai) {
		super(ai);
	}
	
	public void delete(SMTServletRequest req) {
	}
	
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("Beginning RoutineAction retrieve");
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String hospitalInstId = StringUtil.checkVal(req.getSession().getAttribute(HospitalInstanceAction.HOSP_INST_ID), null);
		String programId = (String) req.getSession().getAttribute(ProgramAction.PROGRAM_ID);
		String stageId = StringUtil.checkVal(req.getParameter("stageId"), null);
		String routineId = StringUtil.checkVal(req.getParameter("routineId"), null);
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		Map<String, RoutineVO> data = new LinkedHashMap<String, RoutineVO>();
		
		StringBuilder sb = new StringBuilder();
		sb.append("select a.stage_nm, a.stage_id, a.surgery_week_no, b.routine_id, b.header_txt, b.hospital_inst_id from ");
		sb.append(customDb).append("FOREFRONT_STAGE a ");
		sb.append("left outer join ").append(customDb).append("FOREFRONT_ROUTINE b ");
		sb.append("on a.stage_id=b.stage_id ");
		sb.append("where a.program_id=? and (b.hospital_inst_id=? or b.hospital_inst_id is null) ");
		if (routineId != null) {
			sb.append("and b.routine_id=? ");
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
			if (routineId != null) {
				ps.setString(3, routineId); //get the hospital's record (customized at this stage)
			} else if (stageId != null) {
				ps.setString(3, stageId); //get the program's record (default at this stage)
			}
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				//put all the program's records in first (SQL orderBy), then replace them with whichever got customized (via Map uniqueness)
				data.put(rs.getString("stage_id"), new RoutineVO(rs));
			}
			
		} catch (SQLException sqle) {
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		mod.setActionData(new ArrayList<RoutineVO>(data.values()));
		log.debug("Returned "+ data.size() + " Routines");
	}
	
	
	public void build(SMTServletRequest req) throws ActionException {
		req.setValidateInput(Boolean.FALSE);
		log.debug("Beginning Routine update");
		String msg = "Routine Saved Successfully";
		RoutineVO vo = new RoutineVO(req);
		Boolean cloneExercises = Boolean.FALSE;
		
		StringBuilder sb = new StringBuilder();
		if (vo.getRoutineId() != null) {
			sb.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sb.append("FOREFRONT_ROUTINE set stage_id=?, hospital_inst_id=?, header_txt=?, UPDATE_DT=? where routine_id=?");
		} else {
			sb.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sb.append("FOREFRONT_ROUTINE (stage_id, hospital_inst_id, header_txt, create_dt, routine_id) ");
			sb.append("values (?,?,?,?,?)");
			vo.setRoutineId(new UUIDGenerator().getUUID());
			//if this insert is to add a hospital's custom routine, clone the programs Exercises.
			cloneExercises = (vo.getHospitalInstId() != null); 
		}
		log.debug(sb);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, vo.getStageId());
			ps.setString(2, vo.getHospitalInstId());
			ps.setString(3, vo.getHeaderText());
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
			ps.setString(5, vo.getRoutineId());
			ps.executeUpdate();
			
		} catch (SQLException sqle) {
			msg = "A problem occured while saving the routine.";
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		if (cloneExercises && req.hasParameter("origRoutineId")) {
			ExerciseAssocAction ai = new ExerciseAssocAction(actionInit);
			ai.setDBConnection(dbConn);
			ai.setAttributes(attributes);
			ai.cloneRoutine(req.getParameter("origRoutineId"), vo.getRoutineId());
			ai = null;
		}

		req.setValidateInput(Boolean.TRUE);
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		super.sendRedirect(page.getFullPath() + "?actionType=7", msg, req);
	}
}
