/**
 * 
 */
package com.depuy.events_v2;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import com.depuy.events_v2.vo.report.SeminarSummaryReportVO;
import com.depuy.events_v2.vo.report.SeminarSummaryReportVO.FieldList;
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
public class SeminarSummaryAction extends SimpleActionAdapter {

	public enum ReqType{
	 fetchReport, fetchAllReports, setReport, generateReport, reportForm, deleteReport
	}
	
	/**
	 * 
	 */
	public SeminarSummaryAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public SeminarSummaryAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	public void list(SMTServletRequest req) throws ActionException{
		ReqType rt = ReqType.valueOf(StringUtil.checkVal( req.getParameter("reqType")));
		HttpSession ses = req.getSession();
		UserDataVO usr = (UserDataVO) ses.getAttribute(Constants.USER_DATA);
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		ReportBuilder rb = null;
		
		try{
			switch(rt){
			case deleteReport:
				deleteReport( req.getParameter("reportId") );
				//After deleting the report, fetch the list again to re-populate the view
			case fetchAllReports:
				log.debug("Getting saved reports");
				//get all reports for this user
				List<SeminarSummaryReportVO> voList = getAllSavedReports( usr.getProfileId() );
				mod.setActionData(voList); 
				break;
			case setReport:
				//save the report
				Map<String,Integer> fields = new LinkedHashMap<>();
				Map<String,String> filters = new LinkedHashMap<>();
				
				parseParameters(req,fields,filters);
				saveReport( usr.getProfileId(), fields, filters, req.getParameter("reportName") );
				//Save button is part of the generate form, so cascade from save case to default (list) case
			case fetchReport:
			case generateReport:
				req.setParameter("rptType", "customSummary");
				req.setParameter("reqType", "report");
				
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
			case reportForm:
				break;
			}
		}catch( InvalidDataException | SQLException e ){
			log.error("Error processing report: "+e);
			throw new ActionException(e);
		}
		req.setParameter("reqType", "saveReport");
	}
	
	/**
	 * Get the set of fields and filters from the request object.
	 * @param req
	 * @param fields
	 * @param filters
	 */
	public void parseParameters( SMTServletRequest req, Map<String,Integer> fields, Map<String,String>filters){
		final String FILTER_PREFIX = "by_";
		final int INCLUDE = 1, FILTER=-1;
		
		//For each valid field, check for values to be collected
		for( FieldList fl : FieldList.values() ){
			switch( Convert.formatInteger( req.getParameter(fl.getFieldName().toLowerCase()))){
			case FILTER:
				filters.put(fl.getFieldName(), StringUtil.checkVal(req.getParameter(
						FILTER_PREFIX+fl.getFieldName())));
				//No break, so filter params are included in the report
			case INCLUDE:
				fields.put(fl.name(), 1);
				break;
			default:
				fields.put(fl.name(), 0);
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
	protected SeminarSummaryReportVO getSavedReport( String searchId )
	throws InvalidDataException, SQLException{
		final String customDB = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		//Requires primary key, in case of multiple saved reports
		if ( StringUtil.checkVal(searchId).isEmpty() ){
			log.error("No report Id.");
			throw new InvalidDataException("Missing report id.");
		}
		
		StringBuilder sql = new StringBuilder();
		sql.append("select * from ").append(customDB).append("DEPUY_EVENT_REPORT where REPORT_ID = ?");
		log.debug(sql+" | "+searchId);
		
		SeminarSummaryReportVO vo = null;
		
		try( PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			ps.setString(1, searchId);
			
			ResultSet rs = ps.executeQuery();
			if (rs.next()){
				vo = new SeminarSummaryReportVO(rs);
			}
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
	protected List<SeminarSummaryReportVO> getAllSavedReports( String profileId )
	throws InvalidDataException, SQLException{
		if ( StringUtil.checkVal(profileId).isEmpty() ){
			log.error("No report Id.");
			throw new InvalidDataException("Missing report id.");
		}
		final String customDB = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		
		//make sql statement
		StringBuilder sql = new StringBuilder();
		sql.append("select a.*, b.REPORT_NM from ").append(customDB).append("DEPUY_EVENT_REPORT a ");
		sql.append("inner join ").append(customDB).append("DEPUY_EVENT_REPORT_ASSOC b on a.REPORT_ID=b.REPORT_ID ");
		sql.append("where b.PROFILE_ID=?");
		
		log.debug(sql+" | "+profileId);
		List<SeminarSummaryReportVO> voList = new ArrayList<>();
		try( PreparedStatement ps = dbConn.prepareStatement(sql.toString() )){
			ps.setString(1, profileId);
			ResultSet rs = ps.executeQuery();
			while( rs.next() ){
				//add all results to the list
				voList.add( new SeminarSummaryReportVO(rs) );
			}
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
	protected void saveReport( String profileId,Map<String,Integer>fields, Map<String,String>filters, String name ) 
	throws SQLException, InvalidDataException{
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
		sql.append("insert into ").append(customDB).append("DEPUY_EVENT_REPORT ( REPORT_ID, ");
		sql.append("FILTER_TXT, CREATE_DT, ");
		
		int fieldCount = 0;
		for ( String key : fields.keySet() ){
			fieldCount++;
			//get the column name from the enum's dbName field
			sql.append( colMap.get(key) );
			if( fieldCount < fields.size() ){
				sql.append(",");
			}
		}
		
		//make ? marks for prepared statement
		sql.append(") values (");
		for ( int i = 0; i < fields.size()+3; ++i ){
			sql.append("?");
			if (i < fields.size() + 2){
				sql.append(",");
			}
		}
		sql.append(")");
		
		StringBuilder filterString = new StringBuilder();
		//make filter list string
		int count = 0;
		for ( String key : filters.keySet() ){
			++count;
			filterString.append(key+":"+filters.get(key));
			if (count < filters.size() - 1){
				filterString.append(",");
			}
		}
		
		//insert into depuy_seminar_report
		log.debug(sql);
		
		List<String> fieldNames = new ArrayList<>( fields.keySet() );
		try( PreparedStatement ps = dbConn.prepareStatement(sql.toString() )){
			int i = 0;
			ps.setString(++i, reportId );
			ps.setString(++i, filterString.toString() );
			ps.setTimestamp(++i, Convert.formatTimestamp(new Date()));
			//set all the fields, offset by the number of previously set params
			for ( int index = 1; index <= fields.size(); ++index ){
				ps.setInt(index+i, fields.get( fieldNames.get(index-1) ));
			}
			ps.executeUpdate();
		}
		sql = null;
		
		//insert into depuy_seminar_report_assoc
		sql = new StringBuilder();
		sql.append("insert into ").append(customDB).append("DEPUY_EVENT_REPORT_ASSOC (PROFILE_ID, REPORT_ID,");
		sql.append("CREATE_DT, REPORT_ASSOC_ID, REPORT_NM ) values (?,?,?,?,?)");
		
		try( PreparedStatement stmt = dbConn.prepareStatement(sql.toString() )){
			int i = 0;
			stmt.setString(++i, profileId);
			stmt.setString(++i, reportId);
			stmt.setTimestamp(++i, Convert.formatTimestamp(new Date()));
			stmt.setString(++i, new UUIDGenerator().getUUID());
			stmt.setString(++i, name );
			
			stmt.executeUpdate();
		}
	}
	
	/**
	 * Delete the report from the assoc table
	 * @param reportId
	 */
	protected void deleteReport(String reportId) throws SQLException{		
		final String customDb = (String) attributes.get( Constants.CUSTOM_DB_SCHEMA);
		String rptId = StringUtil.checkVal( reportId );
		
		//report id is required
		if (rptId.isEmpty()){
			log.error("Missing Report Id");
			return;
		}
		//delete the record from the table
		StringBuilder sql = new StringBuilder();
		sql.append("delete from ").append(customDb).append("DEPUY_EVENT_REPORT_ASSOC ");
		sql.append("where REPORT_ID = ?");
		
		log.debug(sql+" | "+rptId);
		
		try( PreparedStatement ps = dbConn.prepareStatement(sql.toString()) ){
			int i=0;
			ps.setString(++i, rptId);
			ps.executeUpdate();
		}
	}

}
