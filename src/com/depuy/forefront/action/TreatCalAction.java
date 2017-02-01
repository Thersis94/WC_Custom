package com.depuy.forefront.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import com.depuy.forefront.action.vo.TreatCalVO;
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

public class TreatCalAction extends SBActionAdapter {
	
	public TreatCalAction() {
		super();
	}
	public TreatCalAction(ActionInitVO ai) {
		super(ai);
	}
	
	public void delete(ActionRequest req) {
	}
	
	public void retrieve(ActionRequest req) throws ActionException{
		log.debug("Beginning TreatCalAction retrieve");
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String hospitalInstId = StringUtil.checkVal(req.getSession().getAttribute(HospitalInstanceAction.HOSP_INST_ID), null);
		String programId = (String) req.getSession().getAttribute(ProgramAction.PROGRAM_ID);
		String stageId = StringUtil.checkVal(req.getParameter("stageId"), null);
		String treatCalId = StringUtil.checkVal(req.getParameter("treatCalId"), null);
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		Map<String, TreatCalVO> data = new LinkedHashMap<String, TreatCalVO>();
		
		StringBuilder sb = new StringBuilder();
		sb.append("select a.stage_nm, a.stage_id, a.surgery_week_no, b.* from ");
		sb.append(customDb).append("FOREFRONT_STAGE a ");
		sb.append("left outer join ").append(customDb).append("FOREFRONT_TREAT_CAL b ");
		sb.append("on a.stage_id=b.stage_id ");
		sb.append("where a.program_id=? and (b.hospital_inst_id=? or b.hospital_inst_id is null) ");
		if (treatCalId != null) {
			sb.append("and b.treat_cal_id=? ");
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
			if (treatCalId != null) {
				ps.setString(3, treatCalId); //get the hospital's record (customized at this stage)
			} else if (stageId != null) {
				ps.setString(3, stageId); //get the program's record (default at this stage)
			}
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				//put all the program's records in first (SQL orderBy), then replace them with whichever got customized (via Map uniqueness)
				data.put(rs.getString("stage_id"), new TreatCalVO(rs));
			}
			
		} catch (SQLException sqle) {
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		mod.setActionData(new ArrayList<TreatCalVO>(data.values()));
		log.debug("Returned "+ data.size() + " Treatment Calendars");
		
	}
	
	
	public void build(ActionRequest req) throws ActionException {
		req.setValidateInput(Boolean.FALSE);
		log.debug("Beginning TreatCal update");
		String msg = "Treatment Calendar Saved Successfully";
		TreatCalVO vo = new TreatCalVO(req);
		Boolean cloneItems = Boolean.FALSE;
		
		StringBuilder sb = new StringBuilder();
		if (vo.getTreatCalId() != null) {
			sb.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sb.append("FOREFRONT_TREAT_CAL set stage_id=?, hospital_inst_id=?, ");
			sb.append("summary_txt=?, header_txt=?, UPDATE_DT=? where treat_cal_id=?");
		} else {
			sb.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sb.append("FOREFRONT_TREAT_CAL (stage_id, hospital_inst_id, summary_txt, ");
			sb.append("header_txt, create_dt, treat_cal_id) values (?,?,?,?,?,?)");
			vo.setTreatCalId(new UUIDGenerator().getUUID());
			//if this insert is to add a hospital's custom calendar, clone the programs entries.
			cloneItems = (vo.getHospitalInstId() != null); 
		}
		log.debug(sb);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, vo.getStageId());
			ps.setString(2, vo.getHospitalInstId());
			ps.setString(3, vo.getSummaryText());
			ps.setString(4, vo.getHeaderText());
			ps.setTimestamp(5, Convert.getCurrentTimestamp());
			ps.setString(6, vo.getTreatCalId());
			ps.executeUpdate();
			
		} catch (SQLException sqle) {
			msg = "A problem occured while saving the TreatCal.";
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		if (cloneItems && req.hasParameter("origTreatCalId")) {
			TreatCalAssocAction ai = new TreatCalAssocAction(actionInit);
			ai.setDBConnection(dbConn);
			ai.setAttributes(attributes);
			ai.cloneTreatCal(req.getParameter("origTreatCalId"), vo.getTreatCalId());
			ai = null;
		}
				
		req.setValidateInput(Boolean.TRUE);
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		super.sendRedirect(page.getFullPath() + "?actionType=" + AdminFacadeAction.TREATMENT_CALENDAR_ACTION, msg, req);
	}
}
