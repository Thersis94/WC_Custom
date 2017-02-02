package com.depuy.forefront.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import com.smt.sitebuilder.common.constants.Constants;

public class TreatCalAssocAction extends SBActionAdapter {

	public TreatCalAssocAction() {
		super();
	}
	public TreatCalAssocAction(ActionInitVO ai) {
		super(ai);
	}
	
	public void delete(ActionRequest req) {
	}
	
	public void retrieve(ActionRequest req) throws ActionException{
		log.debug("Beginning TreatCalAssocAction retrieve");
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String programId = (String) req.getSession().getAttribute(ProgramAction.PROGRAM_ID);
		String hospitalId = StringUtil.checkVal(req.getSession().getAttribute("hospitalId"));
		String treatCalId = StringUtil.checkVal(req.getParameter("treatCalId"));
		Map<String, TreatCalItemVO> data = new LinkedHashMap<String, TreatCalItemVO>();
		
		StringBuilder sb = new StringBuilder();
		String orderBy = "a.ENTRY_NM, b.ORDER_NO";
		sb.append("select a.ENTRY_NM, a.TREAT_CAL_ITEM_ID, b.TREAT_CAL_XR_ID, b.order_no, a.summary_txt ");
		sb.append("from ").append(customDb).append("FOREFRONT_TREAT_CAL_ITEM a ");
		if (req.hasParameter("rearrange")) {
			sb.append("inner join ");
			orderBy = "b.order_no, a.ENTRY_NM";
		} else {
			sb.append("left outer join ");
		}
		sb.append(customDb).append("FOREFRONT_TREAT_CAL_XR b ");
		sb.append("on a.TREAT_CAL_ITEM_ID=b.TREAT_CAL_ITEM_ID and b.TREAT_CAL_ID=? ");
		sb.append("where a.PROGRAM_ID = ? and (a.HOSPITAL_ID is NULL or a.HOSPITAL_ID = ?) ");
		sb.append("order by ").append(orderBy);
		log.debug(sb);

		PreparedStatement ps = null;
		int i = 1;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(i++, treatCalId);
			ps.setString(i++, programId);
			ps.setString(i++, hospitalId);
			
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				data.put(rs.getString("treat_cal_item_id"), new TreatCalItemVO(rs));
			
		} catch (SQLException sqle) {
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		log.debug("Retrieved " + data.size() + " TreatCal Items");
		mod.setActionData(new ArrayList<TreatCalItemVO>(data.values()));
	}
	

	private void updatePlanAssoc(ActionRequest req) throws ActionException {
		log.debug("Beginning TreatCalAction update");
		String msg = "Item added to TreatCal successfully";
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		TreatCalItemVO vo = new TreatCalItemVO(req);

		StringBuilder sb = new StringBuilder();
		PreparedStatement ps = null;
		
		//delete all existing action plan associations
		sb.append("delete from ").append(customDb).append("FOREFRONT_TREAT_CAL_XR where treat_cal_id=? ");
		log.debug(sb);
		
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, vo.getTreatCalId());
			ps.executeUpdate();
			
		} catch (SQLException sqle) {
			log.error("could not delete old treat cal items", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		
		//loop and insert all as new
		sb = new StringBuilder("insert into ").append(customDb);
		sb.append("FOREFRONT_TREAT_CAL_XR (TREAT_CAL_ID, TREAT_CAL_ITEM_ID, ");
		sb.append("ORDER_NO, CREATE_DT, TREAT_CAL_XR_ID) ");
		sb.append("values(?,?,?,?,?)");
		log.debug(sb);
		
		UUIDGenerator uuid = new UUIDGenerator();
		try {
			ps = dbConn.prepareStatement(sb.toString());
			String[] items = req.getParameterValues("treatCalItemId");
			for (String item : items) {
				String[] valArr = item.split("~");
				
				int orderNo = (valArr.length > 1) ? Convert.formatInteger(valArr[1]) : 1;
				
				ps.setString(1, vo.getTreatCalId());
				ps.setString(2, valArr[0]);
				ps.setInt(3, orderNo);
				ps.setTimestamp(4, Convert.getCurrentTimestamp());
				ps.setString(5, uuid.getUUID());
				ps.addBatch();
			}
			ps.executeBatch();
			
		} catch (SQLException sqle) {
			msg = "An error occured while saving the treatment calendar assoc.";
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		super.sendRedirect(page.getFullPath() + "?actionType=" + AdminFacadeAction.TREATMENT_CALENDAR_ACTION, msg, req);
	}
	
	
	
	private void reorderPlan(ActionRequest req) throws ActionException{
		log.debug("Beginning TreatCal Reorder update");
		String msg = "Treat Cal Re-ordered Successfully";
		List<TreatCalItemVO> vos = new ArrayList<TreatCalItemVO>();
		int i = 0;
		TreatCalItemVO v = getNextUpdate(req, i);
		
		while (v != null) {
			vos.add(v);
			i++;
			v = getNextUpdate(req, i);
			log.debug(i + ": " + v);
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sb.append("FOREFRONT_TREAT_CAL_XR set ORDER_NO = ?, UPDATE_DT = ? where TREAT_CAL_XR_ID = ?");
		log.debug(sb);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			for (TreatCalItemVO vo : vos) {
				ps.setInt(1, vo.getOrderNo());
				ps.setTimestamp(2, Convert.getCurrentTimestamp());
				ps.setString(3, vo.getTreatCalId());
				ps.addBatch();
			}
			ps.executeBatch();
			
		} catch (SQLException sqle) {
			msg = "A problem occured when re-ordering the action plan.";
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}

		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		super.sendRedirect(page.getFullPath() + "?actionType=" + AdminFacadeAction.TREATMENT_CALENDAR_ACTION, msg, req);
	}
	

	
	private TreatCalItemVO getNextUpdate(ActionRequest req, int i) {
		String ers = StringUtil.checkVal(req.getParameter("treatCalXrId_" + i));
		log.debug(i + " : " + ers);
		if (ers.length() == 0) return null;
		
		req.setParameter("treatCalId", ers.substring(0, ers.indexOf('~')));
		req.setParameter("orderNo", ers.substring(ers.indexOf('~')));
		
		return new TreatCalItemVO(req);
	}
	
	
	/**
	 * when a customized treat cal is added for a hosp-instance, we want to clone the program's 
	 * default list item _XR records.  The hospital can still go in and customize their
	 * action plans, but at least this gives them a place to start.
	 * This method gets called from TreatCalAction, when a new, hosp-specific TreatCal is created.
	 * @param req
	 * @throws ActionException
	 */
	protected void cloneTreatCal(String origPlan, String newPlan) throws ActionException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		sql.append("insert into ").append(customDb).append("FOREFRONT_TREAT_CAL_XR ");
		sql.append("(TREAT_CAL_XR_ID, TREAT_CAL_ID, TREAT_CAL_ITEM_ID, ORDER_NO, CREATE_DT) ");
		sql.append("select replace(newid(),'-',''),?,TREAT_CAL_ITEM_ID,ORDER_NO, ? ");
		sql.append("from ").append(customDb).append("FOREFRONT_TREAT_CAL_XR ");
		sql.append("where TREAT_CAL_ID=?");
		log.debug(sql);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, newPlan);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, origPlan);
			ps.executeUpdate();
			
		} catch (SQLException sqle) {
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}	
	

	public void build(ActionRequest req) throws ActionException {
		if (Boolean.parseBoolean(req.getParameter("reorder"))) {
			reorderPlan(req);
		} else if (Boolean.parseBoolean(req.getParameter("assoc"))) {
			updatePlanAssoc(req);
		}
	}
}
