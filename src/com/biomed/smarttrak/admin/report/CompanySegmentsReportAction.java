package com.biomed.smarttrak.admin.report;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.vo.CompanyVO;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: CompanySegmentsReportAction.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author groot
 @version 1.0
 @since Mar 10, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class CompanySegmentsReportAction extends SimpleActionAdapter {

	/**
	* Constructor
	*/
	public CompanySegmentsReportAction() {
		super();
	}

	/**
	* Constructor
	*/
	public CompanySegmentsReportAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	public Map<String,Object> retrieveCompanySegments(ActionRequest req) {
		Map<String,String> sectionMap = retrieveSectionsMap();
		List<CompanyVO> companies = retrieveCompanies();
		Map<String,Object> dataMap = new HashMap<>();
		dataMap.put(CompanySegmentsReportVO.KEY_SEGMENTS, sectionMap);
		dataMap.put(CompanySegmentsReportVO.KEY_COMPANIES, companies);
		return dataMap;
	}
	
	protected List<CompanyVO> retrieveCompanies() {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(200);
		sql.append("select co.company_id, co.company_nm, 	cs.section_id ");
		sql.append("from ").append(schema).append("biomedgps_company co ");
		sql.append("inner join custom.biomedgps_company_section cs on co.company_id = cs.company_id ");
		sql.append("order by company_nm");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			return parseCompanies(rs);
		} catch (SQLException sqle) {
			return new ArrayList<>();
		}
		
	}
	
	protected List<CompanyVO> parseCompanies(ResultSet rs) {
		CompanyVO co;
		List<CompanyVO> companies = new ArrayList<>();
		while (rs.next()) {
			co = new CompanyVO();
		}
	}
	
	protected Map<String,String> retrieveSectionsMap() {
		StringBuilder sql = new StringBuilder(200);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			return parseSegments(rs);
		} catch (SQLException sqle) {
			log.error("Error retrieving list of sections, ", sqle);
			return new HashMap<>();
		}
	}
	
	protected Map<String,String> parseSegments(ResultSet rs) throws SQLException {
		Map<String,String> segMap = new LinkedHashMap<>();
		while(rs.next()) {
			segMap.put(rs.getString("section_id"), rs.getString("section_nm"));
		}
		return segMap;
	}

}
