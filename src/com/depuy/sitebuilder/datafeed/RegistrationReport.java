package com.depuy.sitebuilder.datafeed;

// JDK 1.6.0
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

// SMT Base Libs
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
//import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>:RegistrationReport.java<p/>
 * <b>Description: Returns grouped registration data (and summaries) collected from the call sources.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Aug 14, 2008
 ****************************************************************************/
public class RegistrationReport implements Report {
	//private Map<String, Object> attributes = null;
	private SMTDBConnection conn = null;
	private static Logger log = Logger.getLogger(RegistrationReport.class);
	
	/**
	 * 
	 */
	public RegistrationReport() {
		//attributes = new HashMap<String, Object>();
	}

	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.Report#retrieveReport(com.siliconmtn.http.SMTServletRequest)
	 */
	public Object retrieveReport(ActionRequest req)
	throws DatabaseException, InvalidDataException {
		final String dfSchema = ReportFacadeAction.DF_SCHEMA;
		log.debug("Retrieving Registration Report: " + dfSchema);
		
		List<RegistrationVO> data = new ArrayList<RegistrationVO>();
		Map<String, Integer> stats = new LinkedHashMap<String, Integer>();
		
		// Get the Dates
		Date startDate = Convert.formatStartDate(req.getParameter("startDate"));
		Date endDate = Convert.formatEndDate(req.getParameter("endDate"));
		
		// Get the channels requested
		String[] channels = req.getParameterValues("callSourceCode");
		if (channels == null) channels = new String[0];
		
		// Build the SQL Statement
		StringBuffer sql = new StringBuffer();
		sql.append("select f.question_txt, e.question_cd, response_txt,	");
		sql.append("count(response_txt) as cnt, a.call_source_cd ");
		sql.append("from ").append(dfSchema).append("customer a ");
		sql.append("inner join ").append(dfSchema).append("customer_response d on a.customer_id=d.customer_id ");
		sql.append("inner join ").append(dfSchema).append("question_map e on d.question_map_id=e.question_map_id ");
		sql.append("inner join ").append(dfSchema).append("question f on e.question_id=f.question_id ");
		sql.append("where response_txt is not null and len(response_txt) > 0 and a.result_cd='QUALIFIED' ");
		if (startDate != null) sql.append("and a.attempt_dt >= ? ");
		if (endDate != null) sql.append("and a.attempt_dt <= ? ");
		if (channels.length > 0) {
			sql.append("and a.call_source_cd in (");
			for (int i = 0; i < channels.length; i++) {
				if (i > 0) sql.append(",");
				sql.append("?");
			}		
			sql.append(") ");
		}
		sql.append("group by call_source_cd, question_cd, question_txt, response_txt ");
		//UNION Profile for GENDER_CD
		sql.append("union ");
		sql.append("select 'Gender' as question_txt,'GENDER_CD', a.gender_cd, count(*), b.call_source_cd ");
		sql.append("from profile a ");
		sql.append("inner join ").append(dfSchema).append("customer b on a.profile_id=b.profile_id ");
		sql.append("where a.gender_cd is not null and b.result_cd='QUALIFIED' ");
		if (startDate != null) sql.append("and b.attempt_dt >= ? ");
		if (endDate != null) sql.append("and b.attempt_dt <= ? ");
		if (channels.length > 0) {
			sql.append("and b.call_source_cd in (");
			for (int i = 0; i < channels.length; i++) {
				if (i > 0) sql.append(",");
				sql.append("?");
			}		
			sql.append(") ");
		}
		sql.append("group by b.call_source_cd, a.gender_cd ");
		//UNION Profile for BIRTH_YEAR_NO
		sql.append("union ");
		sql.append("select 'Birth Year' as question_txt,'BIRTH_YEAR_NO', ");
		sql.append("cast(a.birth_year_no as varchar), count(*), b.call_source_cd ");
		sql.append("from profile a ");
		sql.append("inner join ").append(dfSchema).append("customer b on a.profile_id=b.profile_id ");
		sql.append("where a.birth_year_no is not null and a.birth_year_no > 0 and b.result_cd='QUALIFIED' ");
		if (startDate != null) sql.append("and b.attempt_dt >= ? ");
		if (endDate != null) sql.append("and b.attempt_dt <= ? ");
		if (channels.length > 0) {
			sql.append("and b.call_source_cd in (");
			for (int i = 0; i < channels.length; i++) {
				if (i > 0) sql.append(",");
				sql.append("?");
			}		
			sql.append(") ");
		}
		sql.append("group by b.call_source_cd, a.birth_year_no ");
		sql.append("order by call_source_cd, question_txt");

		log.info("Registration sql: " + sql + "|" + startDate + "|" + endDate);
		
		PreparedStatement ps = null;
		int ctr = 1;
		try {
			ps = conn.prepareStatement(sql.toString());
			//1st query
			if (startDate != null) ps.setDate(ctr++, Convert.formatSQLDate(startDate));
			if (endDate != null) ps.setDate(ctr++, Convert.formatSQLDate(endDate));
			for (int i = 0; i < channels.length; i++) {
				ps.setString(ctr++, channels[i]);
			}
			
			//2nd query
			if (startDate != null) ps.setDate(ctr++, Convert.formatSQLDate(startDate));
			if (endDate != null) ps.setDate(ctr++, Convert.formatSQLDate(endDate));
			for (int i = 0; i < channels.length; i++) {
				ps.setString(ctr++, channels[i]);
			}
			
			//3rd query
			if (startDate != null) ps.setDate(ctr++, Convert.formatSQLDate(startDate));
			if (endDate != null) ps.setDate(ctr++, Convert.formatSQLDate(endDate));
			for (int i = 0; i < channels.length; i++) {
				ps.setString(ctr++, channels[i]);
			}
			
			ResultSet rs = ps.executeQuery();
			
			RegistrationVO vo = null;
			while (rs.next()) {
				vo = new RegistrationVO();
				vo.setQuestionName(rs.getString(1));
				vo.setQuestionCode(rs.getString(2));
				vo.setResponseText(rs.getString(3));
				vo.setResponseCount(rs.getInt(4));
				vo.setChannelCode(rs.getString(5));
				data.add(vo);
				
				//add this entry to the stats summary
				int cnt = vo.getResponseCount();
				if (stats.containsKey(vo.getKeyName()))
					cnt += stats.get(vo.getKeyName());
				
				stats.put(vo.getKeyName(), cnt);
			}
		} catch(Exception e) {
			log.error("Error retrieving registration report", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		//collate the response data
		Map<String, Object> retVal = new HashMap<String, Object>();
		retVal.put("stats", stats);
		retVal.put("data", data);
		
		//grab leads and qualified leads
		sql = new StringBuffer();
		sql.append("select case when lead_type_id=10 then 'qualLeads' else 'leads' end, ");
		sql.append("count(lead_type_id) from ").append(dfSchema);
		sql.append("customer where lead_type_id in (5,10) and result_cd='QUALIFIED' ");
		if (channels.length > 0) {
			sql.append("and call_source_cd in (");
			for (int i = 0; i < channels.length; i++) {
				if (i > 0) sql.append(",");
				sql.append("?");
			}		
			sql.append(") ");
		}
		if (startDate != null) sql.append("and attempt_dt >= ? ");
		if (endDate != null) sql.append("and attempt_dt <= ? ");
		sql.append("group by lead_type_id");
		ctr = 1;
		try {
			ps = conn.prepareStatement(sql.toString());
			//1st query
			for (int i = 0; i < channels.length; i++) {
				ps.setString(ctr++, channels[i]);
			}
			if (startDate != null) ps.setDate(ctr++, Convert.formatSQLDate(startDate));
			if (endDate != null) ps.setDate(ctr++, Convert.formatSQLDate(endDate));
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				retVal.put(rs.getString(1), rs.getInt(2));
			}
		} catch(Exception e) {
			log.error("Error retrieving registration report", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		return retVal;
	}

	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.Report#setAttibutes(java.util.Map)
	 */
	public void setAttibutes(Map<String, Object> attributes) {
		//this.attributes = attributes;
	}

	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.Report#setDatabaseConnection(com.siliconmtn.db.pool.SMTDBConnection)
	 */
	public void setDatabaseConnection(SMTDBConnection conn) {
		this.conn = conn;

	}

	
	/**
	 * inner class Value Object holds the data retrieved by the RegistrationReport query
	 * @author jackson
	 *
	 */
	public class RegistrationVO {
		private String questionNm = null;
		private String questionCd = null;
		private String channelCd = null;
		private String responseText = null;
		private int responseCnt = 0;
		
		public void setQuestionName(String q) 	{	questionNm = q; }
		public void setQuestionCode(String q) 	{ questionCd = q; 	}
		public void setChannelCode(String c)	{ channelCd = c;	}
		public void setResponseCount(int c)		{ responseCnt = c;	}
		public void setResponseText(String r)	{ responseText = r;	}
		
		public String getQuestionName()		{ return questionNm;	}
		public String getQuestionCode()		{ return questionCd;	}
		public String getChannelCode()		{ return channelCd;		}
		public int getResponseCount()		{ return responseCnt;	}
		public String getResponseText()		{ return responseText;	}
		
		public String getKeyName() { return questionNm + "|" + questionCd + "|" + channelCd;	}
	}
}

