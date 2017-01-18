package com.depuy.forefront.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.depuy.forefront.action.vo.HospitalInstanceVO;
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

public class HospitalInstanceAction extends SBActionAdapter {
	
	public static final String HOSP_INST_ID = "hospitalInstId"; //session constant
	
	public HospitalInstanceAction() {
		super();
	}
	public HospitalInstanceAction(ActionInitVO ai) {
		super(ai);
	}
	
	public void delete(ActionRequest req) {
	}
	
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("Beginning HospitalInstanceAction retrieve");
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		String hospitalInstId = StringUtil.checkVal(req.getParameter("hospitalInstId"));
		List<HospitalInstanceVO> data = new ArrayList<HospitalInstanceVO>();
		
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ").append(customDb);
		sb.append("FOREFRONT_HOSPITAL_INST a inner join ").append(customDb);
		sb.append("FOREFRONT_HOSPITAL b on a.HOSPITAL_ID = b.HOSPITAL_ID inner join ");
		sb.append(customDb).append("FOREFRONT_PROGRAM c on a.PROGRAM_ID = c.PROGRAM_ID ");
		sb.append("where c.program_id=? ");
		if (hospitalInstId.length() > 0) sb.append(" and a.HOSPITAL_INST_ID = ? ");
		sb.append("order by b.HOSPITAL_NM, c.PROGRAM_NM");
		log.debug(sb);

		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, (String)req.getSession().getAttribute(ProgramAction.PROGRAM_ID));
			if (hospitalInstId.length() > 0) ps.setString(2, hospitalInstId);
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				data.add(new HospitalInstanceVO(rs));
			
		} catch (SQLException sqle) {
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) { }
		}
		mod.setActionData(data);
	}
	
	private void updateHospitalInst(ActionRequest req) throws ActionException {
		log.debug("Beginning HospitalInstanceAction update");
		String msg = "Hospital Instance Added Successfully";
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		HospitalInstanceVO vo = new HospitalInstanceVO(req);

		StringBuilder sb = new StringBuilder();
		Boolean isInsert = StringUtil.checkVal(vo.getHospitalInstId()).length() == 0;
		
		if (isInsert) {
			sb.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sb.append("FOREFRONT_HOSPITAL_INST (HOSPITAL_ID, PROGRAM_ID, ");
			sb.append("SITE_ID, CREATE_DT, HOSPITAL_INST_ID) values(?,?,?,?,?)");
			vo.setHospitalInstId(new UUIDGenerator().getUUID());

		} else {
			sb.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sb.append("FOREFRONT_HOSPITAL_INST set HOSPITAL_ID = ?, PROGRAM_ID = ?, ");
			sb.append("SITE_ID = ?, UPDATE_DT = ? where HOSPITAL_INST_ID = ?");
		}
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, vo.getHospitalId());
			ps.setString(2, vo.getProgramId());
			ps.setString(3, vo.getSiteId());
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
			ps.setString(5, vo.getHospitalInstId());
			ps.executeUpdate();
			
		} catch (SQLException sqle) {
			msg = "A problem occured when saving the Hospital Instance.";
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
		this.sendRedirect(page.getFullPath() + "?actionType=" + AdminFacadeAction.HOSPITAL_INST_ACTION, msg, req);
	}
	
	public void build(ActionRequest req) throws ActionException{
		updateHospitalInst(req);
	}
}
