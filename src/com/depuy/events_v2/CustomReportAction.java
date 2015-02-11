package com.depuy.events_v2;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import com.depuy.events_v2.vo.report.CustomReportVO;
import com.depuy.events_v2.vo.report.CustomReportVO.FieldList;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: SeminarSummaryActions.java<p/>
 * <b>Description: Used for saving and retrieving custom report parameters </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Erik Wingo
 * @version 1.0
 * @since Oct 20, 2014
 ****************************************************************************/
public class CustomReportAction extends SimpleActionAdapter {

	public enum ReqType {
		createReport, listReports, saveReport, generateReport, 
		deleteReport
	}

	/**
	 * 
	 */
	public CustomReportAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public CustomReportAction(ActionInitVO arg0) {
		super(arg0);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(SMTServletRequest req) throws ActionException{
		ReqType rt = ReqType.valueOf(StringUtil.checkVal( req.getParameter("reqType")));
		HttpSession ses = req.getSession();
		UserDataVO usr = (UserDataVO) ses.getAttribute(Constants.USER_DATA);
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		ReportBuilder rb = null;

		try{
			switch(rt) {
				case deleteReport:
					deleteReport( req.getParameter("reportId") );
					//After deleting the report, fetch the list again to re-populate the view
					req.setParameter("reqType", "listReports");
					req.setParameter("reportId",null);

				case createReport:
					//load a saved report only if an ID was passed.  Otherwise the
					//pass-through will load all reports when we don't need any.
					if (rt == ReqType.createReport && !req.hasParameter("reportId")) break;
						
				case listReports:
					log.debug("Getting saved reports");
					//get all reports for this user
					List<CustomReportVO> voList = getAllSavedReports( usr.getProfileId(), req.getParameter("reportId"));
					mod.setActionData(voList); 
					break;

				case saveReport:
					//save the report
					if (!req.hasParameter("reportId")) { //re-runs don't save the report, they just pass filters
						Map<String,Integer> fields = new LinkedHashMap<>();
						parseParameters(req,fields);
						saveReport(usr.getProfileId(), fields, req.getParameter("reportName"));
						//Save button is part of the generate form, so cascade from save case to default (list) case
					}

				case generateReport:
					req.setParameter("rptType", "customReport");
					req.setParameter("reqType", "report");
					req.setParameter("isCustomReport", "true");
					
					//get list of seminars
					PostcardSelectV2 retriever = new PostcardSelectV2(actionInit);
					retriever.setDBConnection(dbConn);
					retriever.setAttributes(attributes);
					retriever.retrieve(req);

					//generate the report
					rb = new ReportBuilder(this.actionInit);
					rb.setAttributes(attributes);
					rb.setDBConnection(dbConn);
					rb.generateReport(req, mod.getActionData());
					break;

				default:
					break;

			}


		} catch (InvalidDataException | SQLException e) {
			log.error("Error processing report: "+e);
			throw new ActionException(e);
		}
	}


	/**
	 * Get the set of fields and filters from the request object.
	 * @param req
	 * @param fields
	 * @param filters
	 */
	private void parseParameters( SMTServletRequest req, Map<String,Integer> fields) {
		//final String FILTER_PREFIX = "by_";
		final int INCLUDE = 1;//, FILTER=-1;

		//For each valid field, check for values to be collected
		for (FieldList fl : FieldList.values()) {
			switch(Convert.formatInteger(req.getParameter(fl.getFieldName().toLowerCase()))) {
				//case FILTER:
					//filters.put(fl.getFieldName(), StringUtil.checkVal(req.getParameter(
					//		FILTER_PREFIX+fl.getFieldName())));
				case INCLUDE:
					fields.put(fl.name(), 1);
					break;
				default:
					//fields.put(fl.name(), 0);
					break;
			}
		}
	}


	/**
	 * Get the parameters from a previous report.
	 * @param searchId Report_id for the search
	 * @return
	 * @throws InvalidDataException
	 * @throws SQLException
	 */
	protected CustomReportVO getSavedReport(String searchId)
			throws InvalidDataException, SQLException {
		final String customDB = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);

		StringBuilder sql = new StringBuilder(100);
		sql.append("select * from ").append(customDB).append("DEPUY_EVENT_REPORT where REPORT_ID = ?");
		log.debug(sql+" | "+searchId);

		CustomReportVO vo = null;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, searchId);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				vo = new CustomReportVO(rs);
		}
		return vo;
	}


	/**
	 * Gets a list of all saved reports for this user
	 * @param profileId
	 * @return
	 * @throws InvalidDataException
	 * @throws SQLException
	 */
	private List<CustomReportVO> getAllSavedReports(String profileId, String reportId)
			throws InvalidDataException, SQLException {
		final String customDB = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);

		//make sql statement
		StringBuilder sql = new StringBuilder(200);
		sql.append("select * from ").append(customDB).append("DEPUY_EVENT_REPORT ");
		sql.append("where PROFILE_ID=?");
		if (reportId != null) sql.append(" and report_id=?");
		log.debug(sql+" | "+profileId);
		
		List<CustomReportVO> voList = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, profileId);
			if (reportId != null) ps.setString(2, reportId);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				voList.add(new CustomReportVO(rs));
		}
		
		return voList;
	}


	/**
	 * Save a report for future use
	 * @param profileId
	 * @param fields List of fields included in the report.
	 * @param filters Map of key:value pairs used in the where clause.
	 * @throws SQLException
	 * @throws InvalidDataException
	 */
	private void saveReport( String profileId, Map<String,Integer>fields, String reportName) 
			throws SQLException, InvalidDataException {
		log.info("saveReport() starting..."); 
		//Profile Id is required
		if (StringUtil.checkVal(profileId).isEmpty()){
			log.error("Missing profileId.");
			throw new InvalidDataException("Missing profileId");
		}
		final String reportId = new UUIDGenerator().getUUID();
		final String customDB = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);

		//Map of field name:column name
		Map<String, String> colMap = new HashMap<>();
		for ( FieldList f : FieldList.values() ){
			colMap.put(f.name(), f.getDbName());
		}

		//get local field list so order is preserved
		StringBuilder sql = new StringBuilder();
		//build list of fields to update
		sql.append("insert into ").append(customDB).append("DEPUY_EVENT_REPORT ");
		sql.append("(REPORT_ID, PROFILE_ID, REPORT_NM, CREATE_DT");

		for ( String key : fields.keySet()) {
			sql.append(", ");
			//get the column name from the enum's dbName field
			sql.append(colMap.get(key));
		}

		//make ? marks for prepared statement
		sql.append(") values (?,?,?,?");
		for ( int i = 0; i < fields.size(); ++i )
			sql.append(",?");
		
		sql.append(")");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int i = 0;
			ps.setString(++i, reportId);
			ps.setString(++i, profileId);
			ps.setString(++i, reportName);
			ps.setTimestamp(++i, Convert.getCurrentTimestamp());
			//set all the fields, offset by the number of previously set params
			for (String field : fields.keySet())
				ps.setInt(++i, fields.get(field));
			
			ps.executeUpdate();
		}
	}


	/**
	 * Delete the report from the assoc table
	 * @param reportId
	 */
	private void deleteReport(String reportId) throws SQLException {		
		final String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		String rptId = StringUtil.checkVal(reportId);

		//report id is required
		if (rptId.isEmpty()) {
			log.error("Missing Report Id");
			return;
		}
		
		//delete the record from the table
		StringBuilder sql = new StringBuilder(100);
		sql.append("delete from ").append(customDb).append("DEPUY_EVENT_REPORT ");
		sql.append("where REPORT_ID=?");
		log.debug(sql+" | "+rptId);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, rptId);
			ps.executeUpdate();
		}
	}
}