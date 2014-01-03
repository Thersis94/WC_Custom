package com.depuy.sitebuilder.datafeed;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>:FulfillmentReport.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Aug 26, 2008
 * <b>Changes: </b>
 ****************************************************************************/
public class FulfillmentReport implements Report {
	private Map<String, Object> attributes = null;
	private SMTDBConnection conn = null;
	private static Logger log = Logger.getLogger(FulfillmentReport.class);
	
	/**
	 * 
	 */
	public FulfillmentReport() {
		
	}

	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.Report#retrieveReport(com.siliconmtn.http.SMTServletRequest)
	 */
	public Object retrieveReport(SMTServletRequest req)
	throws DatabaseException, InvalidDataException {
		// Calculate the start date
		Date start = Convert.formatEndDate(req.getParameter("startDate"));
		if (start == null) start = Convert.formatStartDate("01/01/2000");
		
		// Calculate the end date
		Date end = Convert.formatEndDate(req.getParameter("endDate"));
		if (end == null) end = Convert.formatEndDate(new Date());
		
		// Get the schema for the query
		String schema = StringUtil.checkVal((String)attributes.get(ReportFacadeAction.DATABASE_SCHEMA));
		
		// Build the sql statement
		StringBuffer sb = new StringBuffer();
		sb.append("select * from ").append(schema).append("fulfillment ");
		sb.append("where process_start_dt is not null ");
		sb.append("and process_complete_dt is null ");
		sb.append("and create_dt between ? and ? order by process_start_dt ");
		log.debug("Fulfillment Unconfirmed SQL: " + sb + "|" + start + "|" + end);
		
		Map<String, SummaryData> summData = getSummaryData(start, end, schema, req);
		Map<String, Date> unconfirmedList = new LinkedHashMap<String, Date>();
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sb.toString());
			ps.setDate(1, Convert.formatSQLDate(start));
			ps.setDate(2, Convert.formatSQLDate(end));
			
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				unconfirmedList.put(rs.getString("fulfillment_id"), rs.getDate("process_start_dt"));
			}
		}  catch(Exception e) {
			log.error("Error retrieving fulfillment summary data", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		req.setAttribute("unconfirmedList", unconfirmedList);
		return summData;
	}
	
	/**
	 * Retrieves the summary stats 
	 * @param start
	 * @param end
	 * @param schema
	 * @return
	 */
	protected Map<String, SummaryData> getSummaryData(Date start, Date end, String schema, SMTServletRequest req) {
		
		String[] codes = req.getParameterValues("callSourceCode");
		Boolean showCodes = (codes != null && codes.length > 0);
		
		StringBuffer sum = new StringBuffer();
		sum.append("select count(*), process_start_dt, ");
		sum.append("case when process_complete_dt is not null then 0 else 1 end as type");
		if (showCodes) sum.append(", call_source_cd");
		sum.append(" from ").append(schema).append("fulfillment a ");
		if (showCodes) {
			sum.append("inner join ").append(schema).append("customer b ");
			sum.append("on a.customer_id=b.customer_id ");
		}
		sum.append("where a.create_dt between ? and ? ");
		if (showCodes) {
			sum.append("and call_source_cd in (");
			for (int x=0; x < codes.length; x++) {
				if (x > 0) sum.append(",");
				sum.append("?");
			}
			sum.append(") ");
		}
		sum.append("group by process_start_dt, case when process_complete_dt is not null then 0 else 1 end");
		if (showCodes) sum.append(", call_source_cd");

		sum.append(" order by process_start_dt, type");
		if (showCodes) sum.append(", call_source_cd ");
		
		log.debug("Sum Data SQL: " + sum + "|" + start + "|" + end);
		Integer notStarted = 0;
		
		Map<String, SummaryData> data =  new TreeMap<String, SummaryData>();
		PreparedStatement ps = null;
		int i = 0;
		try {
			ps = conn.prepareStatement(sum.toString());
			ps.setDate(++i, Convert.formatSQLDate(start));
			ps.setDate(++i, Convert.formatSQLDate(end));
			if (showCodes) {
				for (int x=0; x < codes.length; x++) {
					ps.setString(++i, codes[x]);
				}
			}
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				int count = rs.getInt(1);
				Date d = rs.getDate(2);
				int type = rs.getInt(3);
				String callSourceCode = "";
				
				if (d == null) {
					notStarted = count;
					continue;
				}
				
				String key = d.toString();
				if (showCodes) {
					callSourceCode = rs.getString(4);
					key += callSourceCode;
				}
				
				if (data.containsKey(key)) {
					SummaryData sd = data.get(key);
					if (type == 0) sd.addData(count, 0);
					else sd.addData(0, count);
					data.put(key, sd);
				} else {
					// If there is no entry, add a new one
					if (type == 0) data.put(key, new SummaryData(count, 0, callSourceCode, d));
					else data.put(key, new SummaryData(0, count, callSourceCode, d));
				}
			}
		} catch(Exception e) {
			log.error("Error retrieving fulfillment summary data", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		req.setAttribute("notStarted", notStarted);
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

	public class ReportData implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
	}
	
	public class SummaryData implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		int success = 0;
		int unconfirmed = 0;
		String callSourceCode = null;
		Date processDate = null;
		
		SummaryData(int success, int unconfirmed, String csc, Date d) {
			this.success = success;
			this.unconfirmed = unconfirmed;
			this.callSourceCode = csc;
			this.processDate = d;
		}
		
		/**
		 * Adds a data element
		 * @param success
		 * @param unconfirmed
		 */
		public void addData(int success, int unconfirmed) {
			this.success += success;
			this.unconfirmed += unconfirmed;
		}

		/**
		 * @return the success
		 */
		public int getSuccess() {
			return success;
		}

		/**
		 * @param success the success to set
		 */
		public void setSuccess(int success) {
			this.success = success;
		}

		/**
		 * @return the unconfirmed
		 */
		public int getUnconfirmed() {
			return unconfirmed;
		}

		/**
		 * @param unconfirmed the unconfirmed to set
		 */
		public void setUnconfirmed(int unconfirmed) {
			this.unconfirmed = unconfirmed;
		}
		
		public void setCallSourceCode(String s) {
			callSourceCode = s;
		}
		public String getCallSourceCode() { return callSourceCode; }
		
		public Date getProcessDate() { return processDate; }

	}
}
