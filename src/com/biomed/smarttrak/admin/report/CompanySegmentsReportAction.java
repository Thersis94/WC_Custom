package com.biomed.smarttrak.admin.report;

// Java 8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//WC custom
import com.biomed.smarttrak.admin.AbstractTreeAction;
import com.biomed.smarttrak.vo.CompanyVO;

// SMTBaseLibs
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;

// WebCrescendo
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: CompanySegmentsReportAction.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
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
	
	/**
	 * Retrieves company segments report data.
	 * @param req
	 * @return
	 */
	public Map<String,Object> retrieveCompanySegments(ActionRequest req) {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);

		Map<String,String> sectionMap = retrieveSectionsMap(schema);
		List<CompanyVO> companies = retrieveCompanies(schema);

		Map<String,Object> dataMap = new HashMap<>();
		dataMap.put(CompanySegmentsReportVO.DATA_KEY_SEGMENTS, sectionMap);
		dataMap.put(CompanySegmentsReportVO.DATA_KEY_COMPANIES, companies);
		return dataMap;
	}

	/**
	 * Retrieves company and data including the segments associated with
	 * each company.
	 * @param schema
	 * @return
	 */
	protected List<CompanyVO> retrieveCompanies(String schema) {
		StringBuilder sql = formatCompanySegmentsQuery(schema);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {

			ResultSet rs = ps.executeQuery();
			return parseCompanies(rs);

		} catch (SQLException sqle) {
			log.error("Error retrieving companies' sections, ", sqle);
			return new ArrayList<>();
		}
	}

	/**
	 * Formats the company segments query.  The query finds the  
	 * 'parent' section ID of each section associated with the company. 
	 * each 
	 * @param schema
	 * @return
	 */
	protected StringBuilder formatCompanySegmentsQuery(String schema) {
		StringBuilder sql = new StringBuilder(200);
		sql.append("select distinct(sec.parent_id), co.company_id, co.company_nm ");
		sql.append("from ").append(schema).append("biomedgps_company co ");
		sql.append("inner join ").append(schema).append("biomedgps_company_section cs ");
		sql.append("on co.company_id = cs.company_id ");
		sql.append("inner join ").append(schema).append("biomedgps_section sec ");
		sql.append("on cs.section_id = sec.section_id ");
		sql.append("order by company_nm");
		log.debug("retrieve companies sections SQL: " + sql);
		return sql;
	}

	/**
	 * Parses the company results set into a List of CompanyVO.
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	protected List<CompanyVO> parseCompanies(ResultSet rs) 
			throws SQLException {
		String prevCoId = null;
		String currCoId;
		CompanyVO co = new CompanyVO();
		List<CompanyVO> companies = new ArrayList<>();
		while (rs.next()) {
			currCoId = rs.getString("company_id");

			if (!currCoId.equals(prevCoId)) {
				// changed companies.
				if (prevCoId != null) companies.add(co);

				// init new company
				co = new CompanyVO();
				co.setCompanyId(rs.getString("company_id"));
				co.setCompanyName(rs.getString("company_nm"));
			}

			// add the segment/section to the company
			co.addCompanySection(new GenericVO(rs.getString("parent_id"),null));

			prevCoId = currCoId;
		}

		// pick up the dangler
		if (prevCoId != null) companies.add(co);

		return companies;
	}

	/**
	 * Retrieves a master list of segments.
	 * @param schema
	 * @return
	 */
	protected Map<String,String> retrieveSectionsMap(String schema) {
		StringBuilder sql = formatSectionsMapQuery(schema);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {

			ps.setString(1, AbstractTreeAction.MASTER_ROOT);
			ResultSet rs = ps.executeQuery();
			return parseSegments(rs);

		} catch (SQLException sqle) {
			log.error("Error retrieving list of sections, ", sqle);
			return new HashMap<>();
		}
	}
	
	/**
	 * Formats the sections query.
	 * @param schema
	 * @return
	 */
	protected StringBuilder formatSectionsMapQuery(String schema) {
		StringBuilder sql = new StringBuilder(200);
		sql.append("select ch.section_id, ch.section_nm ");
		sql.append("from ").append(schema).append("biomedgps_section gp ");
		sql.append("inner join ").append(schema).append("biomedgps_section p ");
		sql.append("on p.parent_id = gp.section_id ");
		sql.append("inner join ").append(schema).append("biomedgps_section ch ");
		sql.append("on ch.parent_id = p.section_id where gp.section_id = ? ");
		log.debug("sectionsMap retrieve SQL: " + sql);
		return sql;
	}

	/**
	 * Parses the sections results set into a map.
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	protected Map<String,String> parseSegments(ResultSet rs) throws SQLException {
		Map<String,String> segMap = new LinkedHashMap<>();
		while(rs.next()) {
			segMap.put(rs.getString("section_id"), rs.getString("section_nm"));
		}
		return segMap;
	}

}
