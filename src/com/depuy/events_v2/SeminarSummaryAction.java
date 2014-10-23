/**
 * 
 */
package com.depuy.events_v2;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	 saveReport, fetchReport, fetchAllReports, setReport
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
		
		SeminarSummaryReportVO vo = new SeminarSummaryReportVO(req);
		switch(rt){
		case saveReport:
			req.setParameter("reqType", "saveReport");
			break;
		case setReport:
			Set<String> fields = new HashSet<>();
			Map<String,String> filters = new HashMap<>();
			parseParameters(req,fields,filters);
			
			UserDataVO usr = (UserDataVO) this.attributes.get(Constants.USER_DATA);
			try {
				saveReport( usr.getProfileId(), fields, filters );
			} catch (InvalidDataException | SQLException e) {
				log.error(e);
				throw new ActionException(e);
			}
			break;
		default:
			//default back to the main report page
			req.setParameter("reqType", "reportForm");
			break;
		}
		List<SeminarSummaryReportVO> voList = new ArrayList<SeminarSummaryReportVO>();
		voList.add(vo);
		this.putModuleData( voList );
	}
	
	/**
	 * Get the set of fields and filters from the request object.
	 * @param req
	 * @param fields
	 * @param filters
	 */
	private void parseParameters( SMTServletRequest req, Set<String> fields, Map<String,String>filters){
		final String FILTER_PREFIX = "by_";
		final int INCLUDE = 1, FILTER=-1;
		FieldList [] lst = FieldList.values();
		
		//For each valid field, check for values to be collected
		for( FieldList fl : lst ){
			switch( Convert.formatInteger( req.getParameter(lst.toString().toLowerCase()))){
			case FILTER:
				filters.put(fl.getFieldName(), StringUtil.checkVal(req.getParameter(
						FILTER_PREFIX+fl.getFieldName())));
				//No break, so filter params are included in the report
			case INCLUDE:
				fields.add(fl.getFieldName());
				break;
			default:
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
		//Requires primary key, in case of multiple saved reports
		if ( StringUtil.checkVal(searchId).isEmpty() ){
			log.error("No report Id.");
			throw new InvalidDataException("Missing report id.");
		}
		
		String sql = "select * from DEPUY_SEMINAR_REPORT where REPORT_ID = ?";
		log.debug(sql+" | "+searchId);
		
		SeminarSummaryReportVO vo = null;
		
		try( PreparedStatement ps = dbConn.prepareStatement(sql)){
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
		//make sql statement
		StringBuilder sql = new StringBuilder();
		sql.append("select a.* from DEPUY_SEMINAR_REPORT a ");
		sql.append("inner join DEPUY_SEMINAR_REPORT_ASSOC b on a.REPORT_ID=b.REPORT_ID ");
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
	protected void saveReport( String profileId,Set<String>fields, Map<String,String>filters ) 
	throws SQLException, InvalidDataException{
		//Profile Id is required
		if (StringUtil.checkVal(profileId).isEmpty()){
			log.error("Missing profileId.");
			throw new InvalidDataException("Missing profileId");
		}
		final String reportId = new UUIDGenerator().getUUID();
		
		//get local field list so order is preserved
		final FieldList [] cols = FieldList.values();
		StringBuilder sql = new StringBuilder();
		//build list of fields to update
		sql.append("insert into DEPUY_SEMINAR_REPORT ( REPORT_ID, ");
		sql.append("CREATE_DT, ");
		for ( int i=0; i<cols.length; ++i ){
			sql.append(cols[i]);
			if( i < cols.length-1 ){
				sql.append(",");
			}
		}
		
		//make ? marks for prepared statement
		sql.append(") values (");
		for ( int i = 0; i < cols.length+2; ++i ){
			sql.append("?");
			if (i < cols.length + 1){
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
		log.debug(sql.toString());
		try( PreparedStatement ps = dbConn.prepareStatement(sql.toString() )){
			int i = 0;
			ps.setString(++i, reportId );
			ps.setString(++i, filterString.toString() );
			ps.setTimestamp(++i, Convert.formatTimestamp(null));
			//all fields passed in will be set to 1, the rest default to 0
			for ( int index = 0; index < cols.length; ++index ){
				ps.setInt(++i, 1);
			}
			ps.executeUpdate();
		}
		sql = null;
		
		//insert into depuy_seminar_report_assoc
		sql = new StringBuilder();
		sql.append("insert into DEPUY_SEMINAR_REPORT_ASSOC (PROFILE_ID, REPORT_ID,");
		sql.append("CREATE_DT, REPORT_ASSOC_ID ) values (?,?,?,?)");
		
		try( PreparedStatement stmt = dbConn.prepareStatement(sql.toString() )){
			int i = 0;
			stmt.setString(++i, profileId);
			stmt.setString(++i, reportId);
			stmt.setTimestamp(++i, Convert.formatTimestamp(null));
			stmt.setString(++i, new UUIDGenerator().getUUID());
			
			stmt.executeUpdate();
		}
	}

}
