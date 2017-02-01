package com.depuy.forefront.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.depuy.forefront.action.vo.ProgramVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

public class ProgramAction extends SBActionAdapter {
	
	public static final String PROGRAM_ID = "programId"; //session constant
	public static final String PROGRAM_NM = "programNm"; //session constant

	public ProgramAction() {
		super();
	}

	public ProgramAction(ActionInitVO ai) {
		super(ai);
	}

	public void delete(ActionRequest req) {
	}

	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("Beginning ProgramAction retrieve");
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		String programId = (req.hasParameter("programId")) ? req.getParameter("programId") : null;
		List<ProgramVO> vos = new ArrayList<ProgramVO>();
		
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sb.append("FOREFRONT_PROGRAM");
		if (programId != null) sb.append(" where PROGRAM_ID = ?");
		sb.append(" order by PROGRAM_NM");

		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			if (programId != null)
				ps.setString(1, programId);
			
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				vos.add(new ProgramVO(rs));
			
		} catch (SQLException sqle) {
			throw new ActionException(sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {
			}
		}
		mod.setActionData(vos);

	}

	private void updateProgram(ActionRequest req) throws ActionException{
		log.debug("Beginning ProgramAction update");
		String msg = "Program Added Successfully";
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		ProgramVO vo = new ProgramVO(req);

		StringBuilder sb = new StringBuilder();
		Boolean isInsert = !(StringUtil.checkVal(vo.getProgramId()).length() > 0);
		
		if (isInsert) {
			sb.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sb.append("FOREFRONT_PROGRAM (PROGRAM_NM, CONTACT_NM, ");
			sb.append("CONTACT_EMAIL_TXT, CREATE_DT, PROGRAM_FULL_NM, PROGRAM_ID) values(?,?,?,?,?,?)");
			if (vo.getProgramId() == null) //avoid guids here; it gets used for /binary folder names
				vo.setProgramId(StringUtil.replace(req.getParameter("programIdCustom"), " ", "-"));

		} else {
			sb.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sb.append("FOREFRONT_PROGRAM set PROGRAM_NM = ?, CONTACT_NM = ?, ");
			sb.append("CONTACT_EMAIL_TXT = ?, UPDATE_DT = ?, PROGRAM_FULL_NM=? where PROGRAM_ID = ?");
		}

		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, vo.getProgramName());
			ps.setString(2, vo.getContactName());
			ps.setString(3, vo.getContactEmailText());
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
			ps.setString(5, vo.getFullProgramName());
			ps.setString(6, vo.getProgramId());
			ps.executeUpdate();
			
		} catch (SQLException sqle) {
			msg = "A problem occured when adding the program.";
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {
			}
		}

		mod.setActionData(vo);
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		this.sendRedirect(page.getFullPath() + "?actionType=" + AdminFacadeAction.PROGRAM_ACTION, msg, req);
	}

	public void build(ActionRequest req) throws ActionException{
		this.updateProgram(req);
	}
}
