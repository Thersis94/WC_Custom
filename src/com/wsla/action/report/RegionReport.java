package com.wsla.action.report;

import java.io.Serializable;
// JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.ticket.FailureReportVO;

/****************************************************************************
 * <b>Title</b>: FailureRateReport.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Pulls the data for the failure Reports
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 31, 2019
 * @updates:
 ****************************************************************************/

public class RegionReport extends SBActionAdapter {
	/**
	 * Key to use for the report type
	 */
	public static final String AJAX_KEY = "region";
	
	/**
	 * 
	 */
	public RegionReport() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public RegionReport(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.info("region report running");
		if (! req.hasParameter("json")) return;
		
		Date startDate = req.getDateParameter("startDate");
		Date endDate = req.getDateParameter("endDate");
		String[] oemId = req.getParameterValues("oemId");
		oemId = oemId[0].split(",");
		String country = req.getParameter("country", "MX");
		String state = req.getParameter("state");
		setModuleData(getStateResultData(country));
	}
	
	
	public List<RegionReportVO> getStateResultData(String country) {
		List<Object> vals = new ArrayList<>();
		vals.add(country);
		vals.add(country);
		
		StringBuilder sql = new StringBuilder(256);
		sql.append("select newid() as id, state_nm, c.country_cd, count(*) as total_ticket_no ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_ticket_assignment b ");
		sql.append("on a.ticket_id = b.ticket_id and b.assg_type_cd = 'CAS' ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_provider_location c ");
		sql.append("on b.location_id = c.location_id ");
		sql.append("inner join state d on c.state_cd = d.state_cd and d.country_cd = ? ");
		sql.append("where c.country_cd = ? ");
		sql.append("group by state_nm, c.country_cd ");
		sql.append("order by state_nm");
		log.info(sql.length() + "|" + sql + "|" + vals);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), vals, new RegionReportVO());
	}
	
	public List<RegionReportVO> getCityResultData(String country, String state) {
		List<Object> vals = new ArrayList<>();
		vals.add(country);
		
		StringBuilder sql = new StringBuilder(256);
		sql.append("select newid() as id, city_nm, state_cd, country_cd, count(*) as total_ticket_no ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_ticket_assignment b ");
		sql.append("on a.ticket_id = b.ticket_id and b.assg_type_cd = 'CAS' ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_provider_location c ");
		sql.append("on b.location_id = c.location_id ");
		sql.append("where country_cd = ? ");
		sql.append("group by city_nm, state_cd, country_cd ");
		sql.append("order by city_nm");
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), vals, new RegionReportVO());
	}

	/**
	 * Inner class to hold the query data
	 */
	public static class RegionReportVO implements Serializable {
		private static final long serialVersionUID = 1L;
		private String id;
		private String city;
		private String state;
		private long totalTickets;
		
		public RegionReportVO() {
			super();
		}
		
		@Column(name="id", isPrimaryKey=true)
		public String getId() { return id;}
		
		@Column(name="city_nm")
		public String getCity() { return city;}
		
		@Column(name="state_nm")
		public String getState() { return state; }
		
		@Column(name="total_ticket_no")
		public long getTotalTickets() { return totalTickets; }
		
		public void setId(String id) { this.id = id; }
		public void setCity(String city) { this.city = city; }
		public void setState(String state) { this.state = state; }
		public void setTotalTickets(long totalTickets) { this.totalTickets = totalTickets; }
	}
}

