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
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
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
public class RegistrationDataReport implements Report {
	//private Map<String, Object> attributes = null;
	private SMTDBConnection conn = null;
	private static Logger log = Logger.getLogger(RegistrationDataReport.class);
	
	/**
	 * 
	 */
	public RegistrationDataReport() {
		//attributes = new HashMap<String, Object>();
	}

	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.Report#retrieveReport(com.siliconmtn.http.SMTServletRequest)
	 */
	public Object retrieveReport(SMTServletRequest req)
	throws DatabaseException, InvalidDataException {
		final String dfSchema = ReportFacadeAction.DF_SCHEMA;
		log.debug("Retrieving Registration Data Report: " + dfSchema);
		
		// Get the Dates
		Date startDate = Convert.formatStartDate(req.getParameter("startDate"), "1/1/2000");
		Date endDate = Convert.formatEndDate(req.getParameter("endDate"));
		String gender = StringUtil.checkVal(req.getParameter("gender"));
		int birthYear = Convert.formatInteger(req.getParameter("birthYear"));
		int birthDir = Convert.formatInteger(req.getParameter("birthDir"));
		
		// Get the channels requested
		String[] channels = req.getParameterValues("callSourceCode");
		if (channels == null) channels = new String[0];

		List<RegistrationDataVO> data = new ArrayList<RegistrationDataVO>();
		
		//preload a list of questions that will display as columns but do not come from the Question table.
		Map<String, String> questions = new LinkedHashMap<String, String>(30);
		questions.put("callSource", "Call Source Code");
		questions.put("channel", "Channel");
		questions.put("scriptType", "Script Type Code");
		questions.put("leadType", "Lead Type");
		questions.put("attemptDate", "Attempt Date");
		questions.put("prefix", "Prefix Name");
		questions.put("gender", "Gender");
		questions.put("birthYear", "Birth Year");
		questions.put("cityName", "City");
		questions.put("stateCode", "State");
		questions.put("priProduct", "Primary Product");
		questions.put("secProduct", "Secondary Product");
		questions.put("callTarget", "This is For");
		questions.put("callReason", "Referrer");
		questions.put("referringPath", "Referring Path");
		questions.put("referringSite", "Referring Site");
		
		// Build the SQL Statement
		StringBuffer sql = new StringBuffer();
		sql.append("select a.*, b.gender_cd, b.birth_year_no, b.prefix_nm, e.question_cd, ");
		sql.append("f.question_txt, response_txt, g.channel_cd, h.city_nm, h.state_cd ");
		sql.append("from ").append(dfSchema).append("customer a ");
		sql.append("left outer join ").append(dfSchema).append("customer_response d on a.customer_id=d.customer_id ");
		sql.append("left outer join ").append(dfSchema).append("question_map e on d.question_map_id=e.question_map_id ");
		sql.append("left outer join ").append(dfSchema).append("question f on e.question_id=f.question_id ");
		sql.append("left outer join ").append(dfSchema).append("customer_channel c on a.customer_id=c.customer_id ");
		sql.append("left outer join ").append(dfSchema).append("channel g on c.channel_id=g.channel_id ");
		sql.append("left outer join profile b on a.profile_id=b.profile_id ");
		sql.append("left outer join profile_address h on a.profile_id=h.profile_id ");
		sql.append("where (a.script_type_cd is null or a.script_type_cd <> 'FEEDBACK_LOOP') and a.result_cd='QUALIFIED' ");
		sql.append("and a.attempt_dt between ? and ? ");
		StringBuilder chanString = new StringBuilder();
		if (channels.length > 0) {
			sql.append("and a.call_source_cd in (");
			for (int i = 0; i < channels.length; i++) {
				if (i > 0) {
					sql.append(",");
					chanString.append(",");
				}
				sql.append("?");
				chanString.append(channels[i]);
			}		
			sql.append(") ");
		}
		
		if (gender.length() > 0) sql.append("and gender_cd = ? ");
		if (birthYear > 0) {
			sql.append("and birth_year_no ");
			if (birthDir == 0) sql.append("< ? ");
			else sql.append("> ? ");
			
		}
		
		
		sql.append("order by a.call_source_cd, g.channel_cd, a.attempt_dt, ");
		sql.append("a.customer_id, question_cd");

		log.info("Registration Data sql: " + sql + "|" + startDate + "|" + endDate + "|" + chanString);
		long startTime = System.currentTimeMillis();
		RegistrationDataVO vo = null;
		PreparedStatement ps = null;
		int ctr = 1;
		try {
			ps = conn.prepareStatement(sql.toString());
			ps.setDate(ctr++, Convert.formatSQLDate(startDate));
			ps.setDate(ctr++, Convert.formatSQLDate(endDate));
			for (int i = 0; i < channels.length; i++) {
				ps.setString(ctr++, channels[i]);
			}
			
			if (gender.length() > 0) ps.setString(ctr++, gender);
			if (birthYear > 0) ps.setInt(ctr++, birthYear);
			
			ResultSet rs = ps.executeQuery();
			log.debug("query took " + (System.currentTimeMillis()-startTime));
			startTime = System.currentTimeMillis();
			String lastCustomerId = "";
			while (rs.next()) {
				if (!rs.getString("customer_id").equals(lastCustomerId)) {
					if (vo != null)	data.add(vo);  //add that last customer to the List before starting a new one

					vo = new RegistrationDataVO(30);
					vo.setAttemptDate(rs.getDate("attempt_dt"));
					vo.setBirthYear(rs.getInt("birth_year_no"));
					vo.setCallSourceCode(rs.getString("call_source_cd"));
					vo.setChannelCode(rs.getString("channel_cd"));
					vo.setPrefixName(rs.getString("prefix_nm"));
					vo.setGenderCode(rs.getString("gender_cd"));
					vo.setCityName(rs.getString("city_nm"));
					vo.setStateCode(rs.getString("state_cd"));
					vo.setProductCode(rs.getString("product_cd"));
					vo.setSecProductCode(rs.getString("sec_product_cd"));
					vo.setCallReasonCode(rs.getString("call_reason_cd"));
					vo.setCallTargetCode(rs.getString("call_target_cd"));
					vo.setReferringPath(rs.getString("referring_path_txt"));
					vo.setReferringSite(rs.getString("referring_site_txt"));
					vo.setScriptTypeCode(rs.getString("script_type_cd"));
					
					int leadType = Convert.formatInteger(rs.getInt("lead_type_id"));
					if (leadType == 10) vo.setLeadType("Qualified Lead");
					else if (leadType == 5) vo.setLeadType("Lead");
					
					lastCustomerId = rs.getString("customer_id");
				}
				
				//ensure we have this question on our master Questions list
				String questionCd = rs.getString("question_cd");
				if (questionCd != null) {
					if (questionCd.equals("Q03") || questionCd.equals("KNEE028")) 
						questionCd = "SeeingOrtho";  //same question
					
					if (!questions.containsKey(questionCd))
						questions.put(questionCd, rs.getString("question_txt"));
					
					vo.setQuestionResponse(questionCd, rs.getString("response_txt"));
				}
			
			}
			if (vo != null) data.add(vo); //add the final record that terminated the RS
			log.debug("done iterating RS in " + (System.currentTimeMillis()-startTime));
		} catch(Exception e) {
			log.error("Error retrieving registration data report", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		//collate the response data
		Map<String, Object> retVal = new HashMap<String, Object>();
		retVal.put("questions", questions);
		retVal.put("colCnt", questions.size());
		retVal.put("data", data);
		
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
	 * inner class Value Object holds the data retrieved by the RegistrationDataReport query
	 * @author James McKain
	 *
	 */
	public class RegistrationDataVO {
		private Integer birthYear = 0;
		private String genderCd = null;
		private String prefixName = null;
		private String channelCd = null;
		private String callSourceCd = null;
		private String leadType = null;
		private Date attemptDt = null;
		private String cityNm = null;
		private String stateCd = null;
		private String productCd = null;
		private String secProductCd = null;
		private String callTargetCd = null;
		private String callReasonCd = null;
		private String referringPath = null;
		private String referringSite = null;
		private Map<String, String> responses = null;
		private String questionCd = "";
		private String scriptTypeCd = null;

		public RegistrationDataVO(int loadCap) {
			responses = new LinkedHashMap<String, String>(loadCap);
		}
		public void setBirthYear(Integer b)		{ birthYear = b;	}
		public void setGenderCode(String c)		{ genderCd = c;		}
		public void setPrefixName(String p)		{ prefixName = p;	}
		public void setCallSourceCode(String c)	{ callSourceCd = c;	}
		public void setChannelCode(String c)	{ channelCd = c;	}
		public void setLeadType(String c)		{ leadType = c;		}
		public void setAttemptDate(Date d)		{ attemptDt = d;	}
		public void setCityName(String n)		{ cityNm = n;		}
		public void setStateCode(String s)		{ stateCd = s;		}
		public void setQuestionResponse(String questionCd, String resp)	{ 
			responses.put(questionCd, resp);
		}

		public int getBirthYear()			{ return birthYear;		}
		public String getGenderCode()		{ return genderCd;		}
		public String getPrefixName()		{ return prefixName;	}
		public String getCallSourceCode()	{ return callSourceCd;	}
		public String getChannelCode()		{ return channelCd;		}
		public String getLeadType()			{ return leadType;		}
		public Date getAttemptDate()		{ return attemptDt;		}
		public String getCityName()			{ return cityNm;		}
		public String getStateCode()		{ return stateCd;		}
		
		public void setQuestionCode(String id) { questionCd = id;	} //this is for JSTL which can't pass a param to getQuestionResponse()
		public String getQuestionResponse()	{ 
			String retVal = "";
			try {
				retVal = responses.get(questionCd);
			} catch (Exception e) {
				log.warn("No response found for questionId=" + questionCd);
			}
			
			return retVal;
		}
		public String getProductCode() {
			return productCd;
		}
		public void setProductCode(String productCd) {
			this.productCd = productCd;
		}
		public String getSecProductCode() {
			return secProductCd;
		}
		public void setSecProductCode(String secProductCd) {
			this.secProductCd = secProductCd;
		}
		public String getCallTargetCode() {
			return callTargetCd;
		}
		public void setCallTargetCode(String callTargetCd) {
			this.callTargetCd = callTargetCd;
		}
		public String getCallReasonCode() {
			return callReasonCd;
		}
		public void setCallReasonCode(String callReasonCd) {
			this.callReasonCd = callReasonCd;
		}
		public String getReferringPath() {
			return referringPath;
		}
		public void setReferringPath(String referringPath) {
			this.referringPath = referringPath;
		}
		public String getReferringSite() {
			return referringSite;
		}
		public void setReferringSite(String referringSite) {
			this.referringSite = referringSite;
		}
		public String getScriptTypeCode() {
			return scriptTypeCd;
		}
		public void setScriptTypeCode(String scriptTypeCd) {
			this.scriptTypeCd = scriptTypeCd;
		}
	}
}

