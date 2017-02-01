package com.depuy.forefront.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.depuy.forefront.action.vo.TreatCalItemVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

public class TreatCalItemAction extends SBActionAdapter {
	
	private String msg = null;
	
	public TreatCalItemAction() {
		super();
	}
	public TreatCalItemAction(ActionInitVO ai) {
		super(ai);
	}
	
	public void delete(ActionRequest req) {
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		msg = (String) getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		String pkId = req.getParameter("delId");

		StringBuilder sql = new StringBuilder();
		sql.append("delete from ").append(customDb);
		sql.append("forefront_treat_cal_item where treat_cal_item_id=?");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, pkId);
			ps.execute();
		} catch (SQLException sqle) {
			log.error("could not delete list item", sqle);
			msg = (String) getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}
	
	
	public void retrieve(ActionRequest req) throws ActionException{
		log.debug("Beginning TreatCalItem retrieve");
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String programId = (String) req.getSession().getAttribute(ProgramAction.PROGRAM_ID);
		String hospitalId = StringUtil.checkVal(req.getSession().getAttribute("hospitalId"));
		String treatCalItemId = StringUtil.checkVal(req.getParameter("treatCalItemId"));
		List<TreatCalItemVO> data = new ArrayList<TreatCalItemVO>();
		
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sb.append("FOREFRONT_TREAT_CAL_ITEM a left outer join ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sb.append("FOREFRONT_HOSPITAL b on a.hospital_id=b.hospital_id where PROGRAM_ID = ? ");
		if (hospitalId.length() > 0) {
			sb.append("and a.HOSPITAL_ID = ? ");
		} else {
			sb.append("and a.HOSPITAL_ID is null "); 
		}
		if (treatCalItemId.length() > 0) sb.append("and TREAT_CAL_ITEM_ID = ? ");
		sb.append("order by entry_nm");
		log.debug(sb);
		
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sb.toString());
			int i = 1;
			ps.setString(i++, programId);
			if (hospitalId.length() > 0) ps.setString(i++, hospitalId);
			if (treatCalItemId.length() > 0) ps.setString(i++, treatCalItemId);
			
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				data.add(new TreatCalItemVO(rs));

		} catch (SQLException sqle) {
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		//ensure at least one item is returned, so the edit form displays
		if (data.size() == 0 && treatCalItemId.length() > 0)
			data.add(new TreatCalItemVO());
		
		mod.setActionData(data);
	}
	
	private void updateList(ActionRequest req) throws ActionException{
		log.debug("Beginning TreatCalItemAction update");
		msg = "Calendar Item Added Successfully";
		TreatCalItemVO vo = new TreatCalItemVO(req);

		StringBuilder sb = new StringBuilder();
		if (StringUtil.checkVal(vo.getTreatCalItemId()).length() == 0) {
			sb.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sb.append("FOREFRONT_TREAT_CAL_ITEM (PROGRAM_ID, HOSPITAL_ID, ");
			sb.append("ENTRY_NM, SUMMARY_TXT, BODY_TXT, CREATE_DT, ");
			sb.append("TREAT_CAL_ITEM_ID) values(?,?,?,?,?,?,?)");
			vo.setTreatCalItemId(new UUIDGenerator().getUUID());

		} else {
			sb.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
			sb.append("FOREFRONT_TREAT_CAL_ITEM set PROGRAM_ID = ?, HOSPITAL_ID = ?, ");
			sb.append("ENTRY_NM = ?, SUMMARY_TXT = ?, BODY_TXT = ?, ");
			sb.append("UPDATE_DT = ? where TREAT_CAL_ITEM_ID = ?");
		}

		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, vo.getProgramId());
			ps.setString(2, vo.getHospitalId());
			ps.setString(3, vo.getEntryName());
			ps.setString(4, vo.getSummaryText());
			ps.setString(5, vo.getBodyText());
			ps.setTimestamp(6, Convert.getCurrentTimestamp());
			ps.setString(7, vo.getTreatCalItemId());
			ps.executeUpdate();
			
		} catch (SQLException sqle) {
			msg = "A problem occured when adding the TreatCal Item.";
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}
	
	public void build(ActionRequest req) throws ActionException {
		if (req.hasParameter("delId")) {
			this.delete(req);
		} else {
			req.setValidateInput(Boolean.FALSE);
			updateList(req);
			req.setValidateInput(Boolean.TRUE);
		}
		
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		this.sendRedirect(page.getFullPath() + "?actionType=" + AdminFacadeAction.TREATMENT_CALENDAR_ITEM_ACTION, msg, req);
	}
}
