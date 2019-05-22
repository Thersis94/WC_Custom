package com.wsla.action.report;

// JDK 1.8.x
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.report.ReportFilterVO;

/****************************************************************************
 * <b>Title</b>: FailureRateReport.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Pulls the data for the region Reports
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
		if (! req.hasParameter("json")) return;

		ReportFilterVO rf = new ReportFilterVO(req, dbConn);
		if (StringUtil.isEmpty(rf.getState()))
			setModuleData(getStateResultData(rf));
		else 
			setModuleData(getCityResultData(rf));
	}
	
	/**
	 * Gets the total tickets opened that utilized a case by state and country 
	 * @param rf Report Filter for user defined filter params
	 * @return
	 */
	public List<RegionReportVO> getStateResultData(ReportFilterVO rf) {
		List<Object> vals = new ArrayList<>();
		vals.add(rf.getCountry());
		
		StringBuilder sql = new StringBuilder(576);
		sql.append("select newid() as id, state_nm, state_nm as display_nm, a.* ");
		sql.append("from state s ");
		sql.append("left outer join ( ");
		sql.append("select state_cd, c.country_cd, count(*) as total_ticket_no ");
		sql.append("from custom.wsla_ticket a ");
		sql.append("inner join custom.wsla_ticket_assignment b on a.ticket_id = b.ticket_id and b.assg_type_cd = 'CAS' ");  
		sql.append("inner join custom.wsla_provider_location c on b.location_id = c.location_id ");
		sql.append("where country_cd = ? ");
		
		// Add the OEM filter
		if (rf.hasOems()) {
			sql.append("and oem_id in (").append(rf.getOemPSQuestions()).append(") ");
			vals.addAll(rf.getOemIds());
		}
		
		// Add the start date filter
		if (rf.hasStartDate()) {
			sql.append("and a.create_dt >= ? ");
			vals.add(rf.getStartDate());
		}
		
		// Add the end date filter
		if (rf.hasEndDate()) {
			sql.append("and a.create_dt <= ? ");
			vals.add(rf.getEndDate());
		}
		
		sql.append("group by state_cd, c.country_cd ");
		sql.append(") as a on s.state_cd = a.state_cd ");
		sql.append("where s.country_cd = ? ");
		sql.append("order by state_nm ");
		vals.add(rf.getCountry());
		log.debug(sql.length() + "|" + sql + "|" + vals);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), vals, new RegionReportVO());
	}
	
	/**
	 * queries the database for a list of cities within the country / state
	 * @param rf
	 * @return
	 */
	public List<RegionReportVO> getCityResultData(ReportFilterVO rf) {
		List<Object> vals = new ArrayList<>();
		vals.add(rf.getState());
		vals.add(rf.getCountry());
		
		StringBuilder sql = new StringBuilder(512);
		sql.append("select newid() as id, city_nm, state_cd, country_cd, ");
		sql.append("count(*) as total_ticket_no, city_nm as display_nm ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_ticket a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_ticket_assignment b ");
		sql.append("on a.ticket_id = b.ticket_id and b.assg_type_cd = 'CAS' ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_provider_location c ");
		sql.append("on b.location_id = c.location_id ");
		sql.append("where state_cd = ? and country_cd = ? ");
		
		// Add the OEM filter
		if (rf.hasOems()) {
			sql.append("and oem_id in (").append(rf.getOemPSQuestions()).append(") ");
			vals.addAll(rf.getOemIds());
		}
		
		// Add the start date filter
		if (rf.hasStartDate()) {
			sql.append("and a.create_dt >= ? ");
			vals.add(rf.getStartDate());
		}
		
		// Add the end date filter
		if (rf.hasEndDate()) {
			sql.append("and a.create_dt <= ? ");
			vals.add(rf.getEndDate());
		}
		
		sql.append("group by city_nm, state_cd, country_cd ");
		sql.append("order by city_nm");
		log.debug("sql: " + sql.length() + "|" + sql + vals);
		
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
		private String displayName;
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
		
		@Column(name="display_nm")
		public String getDisplayName() { return displayName; }
		
		public void setId(String id) { this.id = id; }
		public void setCity(String city) { this.city = city; }
		public void setState(String state) { this.state = state; }
		public void setDisplayName(String displayName) { this.displayName = displayName; }
		public void setTotalTickets(long totalTickets) { this.totalTickets = totalTickets; }
	}
}

