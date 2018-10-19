package com.wsla.action;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
// Jdk 1.8.x
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SMTBase Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.report.chart.SMTChartVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.SQLTotalVO;
import com.siliconmtn.util.Convert;
// WC Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.wsla.data.ticket.TicketVO;

/****************************************************************************
 * <b>Title</b>: DashboardAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Calculates and generates the Dash board display for the
 * WSLA Portal
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 19, 2018
 * @updates:
 ****************************************************************************/

public class DashboardAction extends SimpleActionAdapter {
	
	
	public static final String REQ_KEY = "type";

	/**
	 * 
	 */
	public DashboardAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public DashboardAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		
		if (req.hasParameter("json")) {
			
			if ("attention".equalsIgnoreCase(req.getParameter(REQ_KEY))) {
				putModuleData(getAttentionOrders());
				
			} else if ("trends".equalsIgnoreCase(req.getParameter(REQ_KEY))) {
				putModuleData(getOrderTrends());
				
			} else if ("progress".equalsIgnoreCase(req.getParameter(REQ_KEY))) {
				putModuleData(getOrderProgress(req.getIntegerParameter("numDays", 30)));
				
			}
			
		} else {
			// Get the summary data
			String roleId = ((SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA)).getRoleId();
			putModuleData(getSummaryData(roleId));
		}
	}
	
	/**
	 * Gets the summary data for the dashboard and places in a hashmap
	 * @return
	 */
	public Map<String, Integer> getSummaryData(String roleId) {
		Map<String, Integer> data = new HashMap<>();
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		// Get the number of opened
		StringBuilder sql = new StringBuilder(128);
		sql.append(DBUtil.SELECT_WITH_COUNT).append(DBUtil.FROM_CLAUSE);
		sql.append(getCustomSchema()).append("wsla_ticket where status_cd != 'CLOSED'");
		data.put("open", db.executeSelect(sql.toString(), null, new SQLTotalVO()).get(0).getTotal());
		
		// Get the number closed since the first day of the week
		sql = new StringBuilder(128);
		sql.append(DBUtil.SELECT_WITH_COUNT).append(DBUtil.FROM_CLAUSE);
		sql.append(getCustomSchema()).append("wsla_ticket_ledger where status_cd = 'CLOSED' ");
		sql.append("and create_dt > ? ");
		
		// Get the start of the week
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		Date d = Convert.formatDate(cal.getTime(), Calendar.DAY_OF_YEAR, 1 - cal.get(Calendar.DAY_OF_WEEK));
		data.put("closed-week", db.executeSelect(sql.toString(), Arrays.asList(d), new SQLTotalVO()).get(0).getTotal());
		
		// Get number of my orders
		sql = new StringBuilder(164);
		sql.append(DBUtil.SELECT_WITH_COUNT).append(DBUtil.FROM_CLAUSE);
		sql.append(getCustomSchema()).append("wsla_ticket a ");
		sql.append("inner join ").append(getCustomSchema()).append("wsla_ticket_status b ");
		sql.append("on a.status_cd = b.status_cd where b.role_id = ? ");
		data.put("my-orders", db.executeSelect(sql.toString(), Arrays.asList(roleId), new SQLTotalVO()).get(0).getTotal());
		
		return data;
	}
	
	/**
	 * Gets the list of orders that need attention
	 * @return
	 */
	public List<TicketVO> getAttentionOrders() {
		StringBuilder sql = new StringBuilder(884);
		sql.append("select date_part('day',age(now(), a.create_dt)) as days_open , ");
		sql.append("days_in_status, product_nm, role_nm, d.status_nm, ");
		sql.append("c.provider_nm, b.first_nm, b.last_nm, a.ticket_id ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_user b ");
		sql.append("on a.originator_user_id = b.user_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_provider c ");
		sql.append("on a.oem_id = c.provider_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_ticket_status d ");
		sql.append("on a.status_cd = d.status_cd ");
		sql.append(DBUtil.INNER_JOIN).append("role e on d.role_id = e.role_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_product_serial f ");
		sql.append("on a.product_serial_id = f.product_serial_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_product_master g ");
		sql.append("ON g.product_id = f.product_id ");
		sql.append(DBUtil.INNER_JOIN).append(" ( ");
		sql.append("select date_part('day',age(now(), create_dt)) as days_in_status, "); 
		sql.append("ticket_id, status_cd ").append(DBUtil.FROM_CLAUSE);
		sql.append(getCustomSchema()).append("wsla_ticket_ledger where status_cd != 'CLOSED') ");
		sql.append("as h on a.ticket_id = h.ticket_id and a.status_cd = h.status_cd ");
		sql.append("where standing_cd = 'CRITICAL' ");
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), null, new TicketVO());
	}
	
	/**
	 * Gets the data for the order trends chart
	 * @return
	 */
	public SMTChartVO getOrderTrends() {
		
		return null;
	}
	
	/**
	 * Gets the data for the order progress charts
	 * @param numDays
	 * @return
	 */
	public SMTChartVO getOrderProgress(int numDays) {
		
		return null;
	}
}

