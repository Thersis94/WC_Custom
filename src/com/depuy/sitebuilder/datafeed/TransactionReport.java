package com.depuy.sitebuilder.datafeed;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: TransactionReport.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Mar 9, 2007
 ****************************************************************************/
public class TransactionReport implements Report {
	private Map<String, Object> attributes = null;
	SMTDBConnection conn = null;
	private static Logger log = Logger.getLogger(TransactionReport.class);
	
	public TransactionReport() {
		attributes = new HashMap<String, Object>();
	}
	
	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.Report#retrieveReport(com.siliconmtn.http.SMTServletRequest)
	 */
	public Object retrieveReport(ActionRequest req)
	throws DatabaseException, InvalidDataException {
		// Make sure the db connection is present
		if (conn == null) 
			throw new DatabaseException("No Database Connection");
		
		// Get the parameters for the query
		String schema = StringUtil.checkVal((String)attributes.get(ReportFacadeAction.DATABASE_SCHEMA));
		Integer sourceId = Convert.formatInteger(req.getParameter("sourceId"), 0);
		
		// Calculate the start date
		Date start = Convert.formatDate(Convert.DATE_TIME_SLASH_PATTERN, req.getParameter("startDate") + " 24:00:00", Calendar.DAY_OF_YEAR, -1);
		if (start == null) start = Convert.formatDate(Convert.DATE_TIME_SLASH_PATTERN, "1/1/2000 23:59:59");
		
		// Calculate the end date
		Date end = Convert.formatDate(Convert.DATE_TIME_SLASH_PATTERN, req.getParameter("endDate") + " 00:00:00", Calendar.DAY_OF_YEAR, + 1);
		if (end == null) end = new Date();
		
		if (schema.length() == 0 )
			throw new InvalidDataException("Schema name not provided");
		
		// Build the SQL Statement
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT a.create_dt, ");
		sql.append("SOURCE_NM, CUSTOMER_SENT_NO, customer_process_no ");
		sql.append("FROM ").append(schema).append("TRANSACTIONS a ");
		sql.append("inner join ").append(schema).append("data_feed_source b ");
		sql.append("on a.source_id = b.source_id ");
		sql.append("where a.create_dt between ? and ? ");
		if (sourceId > 0) sql.append("and a.source_id = ").append(sourceId).append(" ");
		
		sql.append("order by a.create_dt, source_nm");
		log.debug("Transaction Report SQL: " + sql.toString());
		
		PreparedStatement ps = null;
		List<ReportData> data = new ArrayList<ReportData>();

		try {
			ps = conn.prepareStatement(sql.toString());
			ps.setDate(1, Convert.formatSQLDate(start));
			ps.setDate(2, Convert.formatSQLDate(end));
			ResultSet rs = ps.executeQuery();
			ReportData rd = new ReportData();
			while (rs.next()) {
				rd = new ReportData();
				rd.transactionDate = rs.getDate(1);
				rd.sourceName = rs.getString(2);
				rd.numberSent = rs.getInt(3);
				rd.numberStored = rs.getInt(4);

				data.add(rd);
			}
		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to retrieve data feed transaction report", sqle);
		}
		
		log.debug("Number of transaction records: " + data.size());
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
	
	/**
	 * **************************************************************************
	 * <b>Title</b>: TransactionReport.java<p/>
	 * <b>Description: </b> Stores a record of data
	 * <p/>
	 * <b>Copyright:</b> Copyright (c) 2007<p/>
	 * <b>Company:</b> Silicon Mountain Technologies<p/>
	 * @author james
	 * @version 2.0
	 * @since Mar 9, 2007
	 ***************************************************************************
	 */
	public class ReportData implements Serializable {
		private static final long serialVersionUID = 1l;
		public Date transactionDate = null;
		public String sourceName = null;
		public int numberSent = 0;
		public int numberStored = 0;
		
		/**
		 * @return the numberSent
		 */
		public int getNumberSent() {
			return numberSent;
		}
		/**
		 * @return the numberStored
		 */
		public int getNumberStored() {
			return numberStored;
		}
		/**
		 * @return the sourceName
		 */
		public String getSourceName() {
			return sourceName;
		}
		/**
		 * @return the transactionDate
		 */
		public Date getTransactionDate() {
			return transactionDate;
		}
		/**
		 * @param numberSent the numberSent to set
		 */
		public void setNumberSent(int numberSent) {
			this.numberSent = numberSent;
		}
		/**
		 * @param numberStored the numberStored to set
		 */
		public void setNumberStored(int numberStored) {
			this.numberStored = numberStored;
		}
		/**
		 * @param sourceName the sourceName to set
		 */
		public void setSourceName(String sourceName) {
			this.sourceName = sourceName;
		}
		/**
		 * @param transactionDate the transactionDate to set
		 */
		public void setTransactionDate(Date transactionDate) {
			this.transactionDate = transactionDate;
		}
	}
}
