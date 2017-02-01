package com.depuy.sitebuilder.datafeed;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: ShareMyStoryReport.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Oct 23, 2012
 ****************************************************************************/
public class ShareMyStoryReport implements Report {
	private SMTDBConnection conn = null;
	protected static Logger log = Logger.getLogger(ShareMyStoryReport.class);

	public ShareMyStoryReport() {;
	}
	
	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.Report#setAttibutes(java.util.Map)
	 */
	@Override
	public void setAttibutes(Map<String, Object> attributes) {
	}

	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.Report#setDatabaseConnection(com.siliconmtn.db.pool.SMTDBConnection)
	 */
	@Override
	public void setDatabaseConnection(SMTDBConnection conn) {
		this.conn = conn;
	}

	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.Report#retrieveReport(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public Object retrieveReport(ActionRequest req) throws DatabaseException, InvalidDataException {
		final String dfSchema = ReportFacadeAction.DF_SCHEMA;
		log.debug("Retrieving ShareMyStory Report: " + dfSchema);
		
		// Get the Dates
		Date startDate = Convert.formatSQLDate(Convert.formatStartDate(req.getParameter("startDate"), "01/01/2000"));
		Date endDate = Convert.formatSQLDate(Convert.formatEndDate(req.getParameter("endDate")));

		Map<String, Integer> retVal = new HashMap<String, Integer>();
		
		// Build the SQL Statement
		StringBuffer sql = new StringBuffer();
		//valid/invalid email addresses tied to the user's data_feed interest
		sql.append("select COUNT(c.profile_id), ");
		sql.append("case when valid_email_flg=1 then 'VALID_EMAIL' else 'INVALID_EMAIL' end ");
		sql.append("from ").append(dfSchema).append("CUSTOMER a ");
		sql.append("inner join ").append(dfSchema).append("CUSTOMER_RESPONSE b on a.customer_id=b.customer_id  ");
		sql.append("and b.QUESTION_MAP_ID=551 ");
		sql.append("inner join profile c on a.PROFILE_ID=c.profile_id ");
		sql.append("where a.ATTEMPT_DT between ? and ? ");
		sql.append("and upper(rtrim(b.RESPONSE_TXT))='Y' ");
		sql.append("group by c.valid_email_flg ");
		sql.append("union ");
		//emails sent
		sql.append("select count(*), 'SENT' from EMAIL_CAMPAIGN_LOG where ");
		sql.append("CAMPAIGN_INSTANCE_ID='c0a8023788c43753a64c2f4bff08c075'  ");
		sql.append("and CREATE_DT between ? and ? and SUCCESS_FLG=1 ");
		sql.append("union ");
		//emails opened
		sql.append("select count(*), 'OPEN' from EMAIL_RESPONSE a ");
		sql.append("inner join EMAIL_CAMPAIGN_LOG b on a.CAMPAIGN_LOG_ID=b.CAMPAIGN_LOG_ID ");
		sql.append("and b.CAMPAIGN_INSTANCE_ID='c0a8023788c43753a64c2f4bff08c075' ");
		sql.append("where a.RESPONSE_TYPE_ID='EMAIL_OPEN' and a.CREATE_DT between ? and ? ");
		sql.append("union ");
		//email redirects
		sql.append("select COUNT(*), 'REDIRECT' from EMAIL_RESPONSE a ");
		sql.append("inner join EMAIL_CAMPAIGN_LOG b on a.CAMPAIGN_LOG_ID=b.CAMPAIGN_LOG_ID ");
		sql.append("and b.CAMPAIGN_INSTANCE_ID='c0a8023788c43753a64c2f4bff08c075' ");
		sql.append("where a.RESPONSE_TYPE_ID='REDIRECT' and a.CREATE_DT between ? and ? ");
		sql.append("union ");
		//Contact-form submittals
		sql.append("select COUNT(*), 'FORMS' from CONTACT_SUBMITTAL where ");
		sql.append("action_id='c0a80228ab880f2e9935b1e3851c316' and CREATE_DT between ? and ?");
		log.info("Registration Data sql: " + sql + "|" + startDate + "|" + endDate);
		
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql.toString());
			ps.setDate(1, startDate);
			ps.setDate(2, endDate);
			ps.setDate(3, startDate);
			ps.setDate(4, endDate);
			ps.setDate(5, startDate);
			ps.setDate(6, endDate);
			ps.setDate(7, startDate);
			ps.setDate(8, endDate);
			ps.setDate(9, startDate);
			ps.setDate(10, endDate);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				retVal.put(rs.getString(2), rs.getInt(1));
			}
			
		} catch(Exception e) {
			log.error("Error retrieving registration data report", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		return retVal;
	}

}
