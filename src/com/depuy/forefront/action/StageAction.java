package com.depuy.forefront.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.depuy.forefront.action.vo.StageVO;
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

public class StageAction extends SBActionAdapter {

	public StageAction() {
		super();
	}
	public StageAction(ActionInitVO ai){
		super(ai);
	}
	
	public void delete(SMTServletRequest req){
		return;
	}
	
	
	public void retrieve(SMTServletRequest req) throws ActionException{
		log.debug("Beginning StageAction retrieve");
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		String stageId = StringUtil.checkVal(req.getParameter("stageId"));
		String programId = (String) req.getSession().getAttribute(ProgramAction.PROGRAM_ID);
		List<StageVO> data = new ArrayList<StageVO>();
		
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sb.append("FOREFRONT_STAGE where PROGRAM_ID = ?");
		if (stageId.length() > 0) sb.append(" and STAGE_ID = ?");
		sb.append(" order by surgery_week_no");
		log.debug(sb);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, programId);
			if (stageId.length() > 0) ps.setString(2, stageId);
			
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				data.add(new StageVO(rs));
			
		} catch (SQLException sqle) {
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		log.debug("retrieved " + data.size() + " stages");
		mod.setActionData(data);
	}
	
	private void updateStage(SMTServletRequest req) throws ActionException{
		log.debug("Beginning StageAction update");
		String msg = "Stage Saved Successfully";
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		StageVO vo = new StageVO(req);

		StringBuilder sb = new StringBuilder();
		Boolean isInsert = StringUtil.checkVal(vo.getStageId()).length() == 0;
		
		if (isInsert) {
			sb.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sb.append("FOREFRONT_STAGE (PROGRAM_ID, URL_ALIAS_TXT, SURGERY_WEEK_NO, ");
			sb.append("STAGE_NM, CREATE_DT, STAGE_ID) values(?,?,?,?,?,?)");
			vo.setStageId(new UUIDGenerator().getUUID());

		} else {
			sb.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sb.append("FOREFRONT_STAGE set PROGRAM_ID = ?, URL_ALIAS_TXT = ?, SURGERY_WEEK_NO = ?, ");
			sb.append("STAGE_NM = ?, UPDATE_DT = ? where STAGE_ID = ?");
		}
		log.debug(sb);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, vo.getProgramId());
			ps.setString(2, vo.getUrlAliasText());
			ps.setInt(3, vo.getSurgeryWeekNo());
			ps.setString(4, vo.getStageName());
			ps.setTimestamp(5, Convert.getCurrentTimestamp());
			ps.setString(6, vo.getStageId());
			ps.executeUpdate();
			
		} catch (SQLException sqle) {
			msg = "A problem occured while saving the Stage";
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}

		mod.setActionData(vo);
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		this.sendRedirect(page.getFullPath() + "?actionType=" + AdminFacadeAction.STAGE_ACTION, msg, req);
	}
	
	public void build(SMTServletRequest req) throws ActionException {
		updateStage(req);
	}
}
