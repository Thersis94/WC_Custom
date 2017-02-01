package com.depuy.forefront.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.depuy.forefront.action.vo.ListItemVO;
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

public class ActionPlanAssocAction extends SBActionAdapter {

	public ActionPlanAssocAction() {
		super();
	}
	public ActionPlanAssocAction(ActionInitVO ai) {
		super(ai);
	}
	
	public void delete(ActionRequest req) {
	}
	
	public void retrieve(ActionRequest req) throws ActionException{
		log.debug("Beginning ActionPlanAssocAction retrieve");
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String programId = (String) req.getSession().getAttribute(ProgramAction.PROGRAM_ID);
		String hospitalId = StringUtil.checkVal(req.getSession().getAttribute("hospitalId"));
		String actionPlanId = StringUtil.checkVal(req.getParameter("actionPlanId"));
		Map<String, ListItemVO> data = new LinkedHashMap<String, ListItemVO>();
		
		StringBuilder sb = new StringBuilder();
		String orderBy = "a.LIST_NM, b.ORDER_NO";
		sb.append("select a.LIST_NM, a.LIST_ITEM_ID, b.ACTION_PLAN_XR_ID, b.order_no, a.summary_txt ");
		sb.append("from ").append(customDb).append("FOREFRONT_LIST_ITEM a ");
		if (req.hasParameter("rearrange")) {
			sb.append("inner join ");
			orderBy = "b.order_no, a.list_nm";
		} else {
			sb.append("left outer join ");
		}
		sb.append(customDb).append("FOREFRONT_ACTION_PLAN_XR b ");
		sb.append("on a.LIST_ITEM_ID=b.LIST_ITEM_ID and b.ACTION_PLAN_ID=? and b.featured_flg=0 and b.caregiver_flg=0 ");
		sb.append("where a.PROGRAM_ID = ? and (a.HOSPITAL_ID is NULL or a.HOSPITAL_ID = ?) ");
		sb.append("order by ").append(orderBy);
		log.debug(sb);

		PreparedStatement ps = null;
		int i = 1;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(i++, actionPlanId);
			ps.setString(i++, programId);
			ps.setString(i++, hospitalId);
			
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				data.put(rs.getString("list_item_id"), new ListItemVO(rs));
			
		} catch (SQLException sqle) {
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		log.debug("Retrieved " + data.size() + " ActionPlan Items");
		mod.setActionData(new ArrayList<ListItemVO>(data.values()));
	}
	

	private void updatePlanAssoc(ActionRequest req) throws ActionException {
		log.debug("Beginning ActionPlanAction update");
		String msg = "Item added to Action Plan successfully";
		final String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		ListItemVO vo = new ListItemVO(req);

		StringBuilder sb = new StringBuilder();
		PreparedStatement ps = null;
		
		//delete all existing action plan associations
		sb.append("delete from ").append(customDb).append("FOREFRONT_ACTION_PLAN_XR where action_plan_id=? ");
		sb.append("and caregiver_flg=0 and featured_flg=0"); //don't purge the ones tied to other components!
		log.debug(sb);
		
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, vo.getActionPlanId());
			ps.executeUpdate();
			
		} catch (SQLException sqle) {
			log.error("could not delete old action plan items", sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		
		//loop and insert all as new
		sb = new StringBuilder("insert into ").append(customDb);
		sb.append("FOREFRONT_ACTION_PLAN_XR (ACTION_PLAN_ID, LIST_ITEM_ID, ");
		sb.append("ORDER_NO, CAREGIVER_FLG, FEATURED_FLG, CREATE_DT, ACTION_PLAN_XR_ID) ");
		sb.append("values(?,?,?,?,?,?,?)");
		log.debug(sb);
		
		UUIDGenerator uuid = new UUIDGenerator();
		try {
			ps = dbConn.prepareStatement(sb.toString());
			String[] items = req.getParameterValues("listItemId");
			for (String item : items) {
				String[] valArr = item.split("~");
				
				int orderNo = (valArr.length > 1) ? Convert.formatInteger(valArr[1]) : 1;
				
				ps.setString(1, vo.getActionPlanId());
				ps.setString(2, valArr[0]);
				ps.setInt(3, orderNo);
				ps.setInt(4, 0);
				ps.setInt(5, 0);
				ps.setTimestamp(6, Convert.getCurrentTimestamp());
				ps.setString(7, uuid.getUUID());
				ps.addBatch();
			}
			ps.executeBatch();
			
		} catch (SQLException sqle) {
			msg = "An error occured while saving the action plan.";
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		super.sendRedirect(page.getFullPath() + "?actionType=8", msg, req);
	}
	
	
	
	private void reorderPlan(ActionRequest req) throws ActionException{
		log.debug("Beginning ActionPlan Reorder update");
		String msg = "Action Plan Re-ordered Successfully";
		List<ListItemVO> vos = new ArrayList<ListItemVO>();
		int i = 0;
		ListItemVO v = getNextUpdate(req, i);
		
		while (v != null) {
			vos.add(v);
			i++;
			v = getNextUpdate(req, i);
			log.debug(i + ": " + v);
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sb.append("FOREFRONT_ACTION_PLAN_XR set ORDER_NO = ?, UPDATE_DT = ? where ACTION_PLAN_XR_ID = ?");
		log.debug(sb);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			for (ListItemVO vo : vos) {
				ps.setInt(1, vo.getOrderNo());
				ps.setTimestamp(2, Convert.getCurrentTimestamp());
				ps.setString(3, vo.getActionPlanId());
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
		super.sendRedirect(page.getFullPath() + "?actionType=8", msg, req);
	}
	

	
	private ListItemVO getNextUpdate(ActionRequest req, int i) {
		String ers = StringUtil.checkVal(req.getParameter("actionPlanXrId_" + i));
		log.debug(i + " : " + ers);
		if (ers.length() == 0) return null;
		
		req.setParameter("actionPlanId", ers.substring(0, ers.indexOf('~')));
		req.setParameter("orderNo", ers.substring(ers.indexOf('~')));
		
		return new ListItemVO(req);
	}
	
	
	/**
	 * when a customized action plan is added for a hosp-instance, we want to clone the program's 
	 * default list item _XR records.  The hospital can still go in and customize their
	 * action plans, but at least this gives them a place to start.
	 * This method gets called from ActionPlanAction, when a new, hosp-specific ActionPlan is created.
	 * @param req
	 * @throws ActionException
	 */
	protected void cloneActionPlan(String origPlan, String newPlan) throws ActionException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder();
		sql.append("insert into ").append(customDb).append("FOREFRONT_ACTION_PLAN_XR ");
		sql.append("(ACTION_PLAN_XR_ID, ACTION_PLAN_ID, LIST_ITEM_ID, ORDER_NO, ");
		sql.append("CAREGIVER_FLG, FEATURED_FLG, CREATE_DT) ");
		sql.append("select replace(newid(),'-',''),?,LIST_ITEM_ID,ORDER_NO, ");
		sql.append("CAREGIVER_FLG, FEATURED_FLG, ? ");
		sql.append("from ").append(customDb).append("FOREFRONT_ACTION_PLAN_XR ");
		sql.append("where action_plan_id=?");
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
