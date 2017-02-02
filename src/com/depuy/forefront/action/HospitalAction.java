package com.depuy.forefront.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.depuy.forefront.action.vo.HospitalVO;
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

public class HospitalAction extends SBActionAdapter {
	
	public static final String HOSPITAL_INST_ID = "hospitalInstId"; //session constant
	public static final String HOSPITAL_ID = "hospitalId"; //session constant
	public static final String HOSPITAL_NM = "hospitalNm"; //session constant


	public HospitalAction() {
		super();
	}

	public HospitalAction(ActionInitVO ai) {
		super(ai);
	}

	public void delete(ActionRequest req) {
	}
	
	public void retrieve(ActionRequest req) throws ActionException{
		log.debug("Beginning HospitalAction retrieve");
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		List<HospitalVO> vos = new ArrayList<HospitalVO>();
		StringBuilder sb = new StringBuilder();
		String hospitalId = req.getParameter("hospitalId");
		sb.append("select * from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sb.append("FOREFRONT_HOSPITAL");
		if(StringUtil.checkVal(req.getParameter("hospitalId")).length() > 0)
		sb.append(" where HOSPITAL_ID = ?");
		sb.append(" order by HOSPITAL_NM");

		log.debug("sql = " + sb.toString());
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			if(StringUtil.checkVal(req.getParameter("hospitalId")).length() > 0)
				ps.setString(1, hospitalId);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				vos.add(new HospitalVO(rs));
		} catch (SQLException sqle) {
			log.debug(sqle);
			throw new ActionException(sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {
			}
		}

		mod.setActionData(vos);
	}

	private void updateHospital(ActionRequest req) throws ActionException{
		log.debug("Beginning HospitalAction update");
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		String msg = "Hospital Added Successfully";
		HospitalVO vo = new HospitalVO(req);

		Boolean isInsert = !(StringUtil.checkVal(vo.getHospitalId()).length() > 0);
		log.debug("hospital ID = " + vo.getHospitalId());
		StringBuilder sb = new StringBuilder();
		if (isInsert) {
			vo.setHospitalId(new UUIDGenerator().getUUID());
			sb.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sb.append("FOREFRONT_HOSPITAL (HOSPITAL_NM, CREATE_DT, HOSPITAL_ID) ");
			sb.append("values(?,?,?)");
		} else {
			sb.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sb.append("FOREFRONT_HOSPITAL set HOSPITAL_NM = ?, UPDATE_DT = ? ");
			sb.append("where HOSPITAL_ID = ?");
		}
		PreparedStatement ps = null;
		log.debug("sql = " + sb.toString());
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, vo.getHospitalName());
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, vo.getHospitalId());
			ps.execute();
		} catch (SQLException sqle) {
			msg = "An error occurred while attempting to add the hospital.";
			log.debug(sqle);
			throw new ActionException(sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {
			}
		}

		mod.setActionData(vo);
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		this.sendRedirect(page.getFullPath() + "?actionType=" + AdminFacadeAction.HOSPITAL_ACTION, msg, req);
	}

	public void build(ActionRequest req) throws ActionException{
		updateHospital(req);
	}
}
