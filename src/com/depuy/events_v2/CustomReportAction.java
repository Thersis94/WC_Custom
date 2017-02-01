package com.depuy.events_v2;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.http.session.SMTSession;

import com.depuy.events_v2.vo.report.CustomReportVO;
import com.depuy.events_v2.vo.report.CustomReportVO.FieldList;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.action.ActionRequest;
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
 * @updates
 * 		JM 02.11.15 - completely refactored, was not extensible.
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
	public void build(ActionRequest req) throws ActionException{
		ReqType rt = ReqType.valueOf(StringUtil.checkVal( req.getParameter("reqType")));
		SMTSession ses = req.getSession();
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
						Map<FieldList,Integer> fields = new HashMap<>();
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
	private void parseParameters( ActionRequest req, Map<FieldList,Integer> fields) {
		//For each valid field, check for values to be collected
		for (FieldList fl : FieldList.values()) {
			if (req.hasParameter(fl.getFieldName()))
					fields.put(fl, 1); //1 = ORDER_NO - not implemented in the View.
		}
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
		sql.append("select * from ").append(customDB).append("DEPUY_EVENT_REPORT  a ");
		sql.append("left outer join ").append(customDB).append("DEPUY_EVENT_REPORT_FIELD b on a.report_id=b.report_id ");
		sql.append("where a.PROFILE_ID=? ");
		if (reportId != null) sql.append(" and a.report_id=?");
		log.debug(sql+" | "+profileId);
		
		List<CustomReportVO> voList = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, profileId);
			if (reportId != null) ps.setString(2, reportId);
			ResultSet rs = ps.executeQuery();
			String lastReportId = "";
			CustomReportVO vo = null;
			while (rs.next()) {
				if (!rs.getString("report_id").equals(lastReportId)) {
					if (vo != null) voList.add(vo);
					vo = new CustomReportVO(rs);
					lastReportId = rs.getString("report_id");
				}
				vo.addField(rs);
			}
			//add the trailing record
			if (vo != null) voList.add(vo);
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
	private void saveReport(String profileId, Map<FieldList, Integer>fields, String reportName) 
			throws SQLException, InvalidDataException {
		if (fields == null || fields.size() == 0) return; //nothing to save, its an empty report!
		
		final String reportId = new UUIDGenerator().getUUID();
		final String customDB = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);

		//Save the report
		StringBuilder sql = new StringBuilder(100);
		sql.append("insert into ").append(customDB).append("DEPUY_EVENT_REPORT ");
		sql.append("(REPORT_ID, REPORT_NM, PROFILE_ID, CREATE_DT) values (?,?,?,?)");
		log.debug(sql +"|"+ reportId + "|"+ reportName);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, reportId);
			ps.setString(2, reportName);
			ps.setString(3, profileId);
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
			ps.executeUpdate();
		}
		
		//save the report fields
		sql = new StringBuilder(100);
		sql.append("insert into ").append(customDB).append("DEPUY_EVENT_REPORT_FIELD ");
		sql.append("(REPORT_ID, COLUMN_NM, ORDER_NO, CREATE_DT) values (?,?,?,?)");
		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (FieldList fl : fields.keySet()) {
				ps.setString(1, reportId);
				ps.setString(2, fl.getFieldName());
				ps.setInt(3, fields.get(fl));
				ps.setTimestamp(4, Convert.getCurrentTimestamp());
				ps.addBatch();
			}
			ps.executeBatch();
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