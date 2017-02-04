package com.depuy.sitebuilder.datafeed;

// JDK 1.6.0
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

// SMT Base Libs
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>:ChannelReport.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Mar 28, 2008
 ****************************************************************************/
public class ChannelReport implements Report {
	private Map<String, Object> attributes = null;
	private SMTDBConnection conn = null;
	private static Logger log = Logger.getLogger(ChannelReport.class);
	
	/**
	 * 
	 */
	public ChannelReport() {
		attributes = new HashMap<String, Object>();
	}

	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.Report#retrieveReport(com.siliconmtn.http.SMTServletRequest)
	 */
	public Object retrieveReport(ActionRequest req)
	throws DatabaseException, InvalidDataException {
		log.debug("Retrieving Chanel Report: " + attributes.get(Constants.CUSTOM_DB_SCHEMA));
		
		// Calculate the start date
		Date start = Convert.formatDate(Convert.DATE_TIME_SLASH_PATTERN, req.getParameter("startDate") + " 24:00:00", Calendar.DAY_OF_YEAR, -1);
		if (start == null) start = Convert.formatDate(Convert.DATE_TIME_SLASH_PATTERN, "1/1/2000 23:59:59");
		
		// Calculate the end date
		Date end = Convert.formatDate(Convert.DATE_TIME_SLASH_PATTERN, req.getParameter("endDate") + " 00:00:00", Calendar.DAY_OF_YEAR, + 1);
		if (end == null) end = new Date();
		
		// Get the channels requested
		String[] channels = req.getParameterValues("channelCode");
		if (channels == null) channels = new String[0];
		
		// Build the SQL Statement
		StringBuffer sb = new StringBuffer();
		sb.append("select channel_cd, count(*) ");
		sb.append("from data_feed.dbo.customer a inner join data_feed.dbo.customer_channel b ");
		sb.append("on a.customer_id = b.customer_id ");
		sb.append("inner join data_feed.dbo.channel c on b.channel_id = c.channel_id ");
		
		if (channels.length > 0) sb.append("and channel_cd in (");
		for (int i = 0; i < channels.length; i++) {
			sb.append("?");
			if ((i+1) < channels.length) sb.append(",");
		}
		
		if (channels.length > 0) sb.append(") ");
		
		// Build the where statement
		sb.append("where lead_type_id > 1 ");
		sb.append("and a.attempt_dt >= ? ");
		sb.append("and a.attempt_dt <= ? ");
		
		// Order the results
		sb.append("group by channel_cd ");
		log.info("Channel sql: " + sb + "|" + start + "|" + end);
		
		PreparedStatement ps = null;
		Map<String, Integer> data = new LinkedHashMap<String, Integer>();
		int ctr = 1;
		try {
			ps = conn.prepareStatement(sb.toString());
			for (int i = 0; i < channels.length; i++) {
				ps.setString(ctr++, channels[i]);
			}
			
			ps.setDate(ctr++, Convert.formatSQLDate(start));
			ps.setDate(ctr++, Convert.formatSQLDate(end));
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				data.put(rs.getString(1), rs.getInt(2));
			}
		} catch(Exception e) {
			log.error("Error retrieving channel codes", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		return data;
	}

	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.Report#setAttibutes(java.util.Map)
	 */
	public void setAttibutes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.Report#setDatabaseConnection(com.siliconmtn.db.pool.SMTDBConnection)
	 */
	public void setDatabaseConnection(SMTDBConnection conn) {
		this.conn = conn;

	}
}
