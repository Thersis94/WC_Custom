package com.biomed.smarttrak;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
//import com.smt.sitebuilder.common.ModuleVO;
//import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: FinancialDashAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Jan 04, 2017
 ****************************************************************************/

public class FinancialDashAction extends SBActionAdapter {

	public FinancialDashAction() {
		super();
	}

	public FinancialDashAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	public void delete(SMTServletRequest req) throws ActionException {
		super.delete(req);
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
		//ModuleVO modVo = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		
		String displayType = StringUtil.checkVal(req.getParameter("displayType"), FinancialDashColumnSet.DEFAULT_DISPLAY_TYPE);
		Integer calendarYear = Convert.formatInteger(req.getParameter("calendarYear"), Convert.getCurrentYear());
		String tableType = StringUtil.checkVal(req.getParameter("tableType"), FinancialDashVO.DEFAULT_TABLE_TYPE);
		String[] countryTypes = req.getParameterValues("countryTypes[]") == null ? new String[]{FinancialDashVO.DEFAULT_COUNTRY_TYPE} : req.getParameterValues("countryTypes[]");
		String sectionId = StringUtil.checkVal(req.getParameter("sectionId"), "MASTER_ROOT");
		
		FinancialDashVO dash = new FinancialDashVO();
		dash.setTableType(tableType);
		dash.setColHeaders(displayType, calendarYear);
		for(String countryType : countryTypes) {
			dash.addCountryType(countryType);
		}
		dash.setSectionId(sectionId);
		
		String sql = getFinancialDataSql();
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			int idx = 0;
			for (int i = 0; i < 7; i++) {
				ps.setString(++idx, dash.getSectionId());
			}
			ps.setString(++idx, dash.getCountryTypes().get(0).name());
			ps.setInt(++idx, calendarYear);
			
			ResultSet rs = ps.executeQuery();
			dash.setData(rs);
		} catch (SQLException sqle) {
			log.error("Unable to get financial dashboard data", sqle);
		}

		this.putModuleData(dash);
	}
	
	/**
	 * Returns the sql for retrieving financial data. 
	 * @return
	 */
	private String getFinancialDataSql() {
		String custom = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		
		StringBuilder sql = new StringBuilder(1200);
		sql.append("select r.COMPANY_ID, c.COMPANY_NM, r.YEAR_NO, sum(r.Q1_NO) as q1_y1, sum(r.Q2_NO) as q2_y1, sum(r.Q3_NO) as q3_y1, sum(r.Q4_NO) as q4_y1, ");
		sql.append("sum(r2.Q1_NO) as q1_y2, sum(r2.Q2_NO) as q2_y2, sum(r2.Q3_NO) as q3_y2, sum(r2.Q4_NO) as q4_y2 ");
		sql.append("from ").append(custom).append("BIOMEDGPS_FD_REVENUE r ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_FD_REVENUE r2 on r.COMPANY_ID = r2.COMPANY_ID and r.REGION_CD = r2.REGION_CD and r.SECTION_ID = r2.SECTION_ID and r.YEAR_NO - 1 = r2.YEAR_NO ");
		sql.append("inner join ").append(custom).append("BIOMEDGPS_COMPANY c on r.COMPANY_ID = c.COMPANY_ID ");
		sql.append("inner join ").append(custom).append("BIOMEDGPS_SECTION s1 on r.SECTION_ID = s1.SECTION_ID ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_SECTION s2 on s1.PARENT_ID = s2.SECTION_ID ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_SECTION s3 on s2.PARENT_ID = s3.SECTION_ID ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_SECTION s4 on s3.PARENT_ID = s4.SECTION_ID ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_SECTION s5 on s4.PARENT_ID = s5.SECTION_ID ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_SECTION s6 on s5.PARENT_ID = s6.SECTION_ID ");
		sql.append("left join ").append(custom).append("BIOMEDGPS_SECTION s7 on s6.PARENT_ID = s7.SECTION_ID ");
		sql.append("where (s1.SECTION_ID = ? OR s2.SECTION_ID = ? OR s3.SECTION_ID = ? OR s4.SECTION_ID = ? OR s5.SECTION_ID = ? OR s6.SECTION_ID = ? OR s7.SECTION_ID = ?) ");
		sql.append("and r.REGION_CD = ? and r.YEAR_NO = ? ");
		sql.append("group by r.COMPANY_ID, c.COMPANY_NM, r.YEAR_NO ");
		sql.append("order by c.COMPANY_NM ");

		return sql.toString();
	}

	public void build(SMTServletRequest req) throws ActionException {
		super.build(req);
		
		String priKey = StringUtil.checkVal(req.getParameter("pk"));
		String fieldName = StringUtil.checkVal(req.getParameter("name"));
		String updateValue = StringUtil.checkVal(req.getParameter("value")); 
		
		log.debug("Updating Record: " + priKey + " | " + fieldName + "=" + updateValue + " ********************************");
	}
	
	public void list(SMTServletRequest req) throws ActionException {
		super.list(req);
	}

	public void update(SMTServletRequest req) throws ActionException {
		super.update(req);
	}
}
