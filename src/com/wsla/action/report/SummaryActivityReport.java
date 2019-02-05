package com.wsla.action.report;

// JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.report.GroupReportVO;

/****************************************************************************
 * <b>Title</b>: SummaryActivityReport.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Pulls the data for the Summery Reports
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 31, 2019
 * @updates:
 ****************************************************************************/

public class SummaryActivityReport extends SBActionAdapter {
	/**
	 * Key to use for the report type
	 */
	public static final String AJAX_KEY = "summary";
	
	/**
	 * 
	 */
	public SummaryActivityReport() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public SummaryActivityReport(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (! req.hasParameter("json")) return;
		
		Date startDate = req.getDateParameter("startDate");
		Date endDate = req.getDateParameter("endDate");
		String phoneNumber = req.getParameter("phoneNumber");
		String oemId = req.getParameter("oemId");
		try {
			if (req.hasParameter("callCenter")) {
				setModuleData(getCallCenterPivot(oemId, phoneNumber, startDate, endDate));
			} else {
				setModuleData(getStatusGroupReport(oemId, phoneNumber, startDate, endDate));
			}
		} catch (Exception e) {
			log.error("Unable to get pivot", e);
		}
		
		
	}
	
	/**
	 * 
	 * @param sd
	 * @param ed
	 * @return
	 * @throws SQLException
	 */
	public Map<String, Map<String, GenericVO>> getCallCenterPivot(String oemId, String pId, Date sd, Date ed) 
	throws SQLException {
		Map<String, Map<String, GenericVO>> data = new HashMap<>();
		
		StringBuilder sql = new StringBuilder(380);
		sql.append("select first_nm || ' ' || last_nm as name, ");
		sql.append("phone_number_txt, avg(creation_time_no), count(*) as total_tickets ");
		sql.append("from ").append(getCustomSchema()).append("wsla_ticket a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_ticket_ledger b ");
		sql.append("on a.ticket_id = b.ticket_id and b.status_cd = 'OPENED' ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_user c ");
		sql.append("on b.disposition_by_id = c.user_id ");
		sql.append("where a.create_dt between ? and ? ");
		if (!StringUtil.isEmpty(pId)) sql.append("and phone_number_txt = ? ");
		if (!StringUtil.isEmpty(oemId)) sql.append("and oem_id = ? ");
		sql.append("group by name, phone_number_txt ");
		sql.append("order by name ");
		log.debug(sql.length() + "|" + sql);
		
		int ctr = 1;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setTimestamp(ctr++, Convert.formatTimestamp(sd));
			ps.setTimestamp(ctr++, Convert.formatTimestamp(ed));
			if (!StringUtil.isEmpty(pId)) ps.setString(ctr++, pId);
			if (!StringUtil.isEmpty(oemId)) ps.setString(ctr++, oemId);
			try (ResultSet rs = ps.executeQuery()) {
				while(rs.next()) {
					processMap(data, rs.getString(1), rs);
				}
			}
		}
		
		return data;
	}
	
	/**
	 * Process for the loading of the map
	 * @param data
	 * @param name
	 * @param rs
	 * @throws SQLException
	 */
	private void processMap(Map<String, Map<String, GenericVO>> data, String name, ResultSet rs) 
	throws SQLException {
		if (data.containsKey(name)) {
			Map<String, GenericVO> item = data.get(name);
			item.put(rs.getString(2), new GenericVO(rs.getDouble(3), rs.getLong(4)));
			
		} else {
			Map<String, GenericVO> item = new HashMap<>();
			item.put(rs.getString(2), new GenericVO(rs.getDouble(3), rs.getLong(4)));
			data.put(name, item);
		}
	}
	
	/**
	 * Builds the report for grouping the tickets in the date range by the status group
	 * 
	 * @param oemId
	 * @param pId
	 * @param sd
	 * @param ed
	 * @return
	 */
	public List<GroupReportVO> getStatusGroupReport(String oemId, String pId, Date sd, Date ed) {
		// Assign the PS Params
		List<Object> vals = new ArrayList<>();
		vals.add(sd);
		vals.add(ed);
		
		// Build the SQL
		StringBuilder sql = new StringBuilder(1024);
		sql.append("select a.group_status_cd, count(distinct(b.ticket_id)) as total_ticket_no, ");
		sql.append("round(coalesce(avg(date_part('day',age(c.max_dt, c.min_dt))), 0)::numeric, 1) as avg_days_status ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket_status a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema());
		sql.append("wsla_ticket b on a.status_cd = b.status_cd ");
		sql.append("and b.create_dt between ? and ? ");
		
		// If we are filtering by phone or oem, add the filters here
		if (! StringUtil.isEmpty(oemId)) {
			sql.append("and oem_id = ? ");
			vals.add(oemId);
		} else if (! StringUtil.isEmpty(pId)) {
			sql.append("and phone_number_txt = ? ");
			vals.add(pId);
		}
		
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(" ( ");
		sql.append("select ticket_id, group_status_cd, max(a.create_dt) as max_dt, min(a.create_dt) as min_dt ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket_ledger a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_ticket_status b ");
		sql.append("on a.status_cd = b.status_cd ");
		sql.append("group by ticket_id, group_status_cd ");
		sql.append(") c on b.ticket_id = c.ticket_id ");
		sql.append("group by a.group_status_cd ");
		sql.append("order by case ");
		sql.append("when a.group_status_cd = 'PROCESSING' then 0 ");
		sql.append("when a.group_status_cd = 'PICKUP' then 1 ");
		sql.append("when a.group_status_cd = 'DIAGNOSIS' then 2 ");
		sql.append("when a.group_status_cd = 'REPAIR' then 3 ");
		sql.append("when a.group_status_cd = 'DELIVERY' then 4 ");
		sql.append("when a.group_status_cd = 'COMPLETE' then 5 ");
		sql.append("end ");
		log.debug(sql.length() + "|" + sql);
		
		// Query the DB
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), vals, new GroupReportVO());
	}
}

