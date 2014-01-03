package com.depuy.sitebuilder.datafeed;

// JDK 1.5.0
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

// Log4j 1.2.8
import org.apache.log4j.Logger;

// SMT Base Libs 2.0
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: TollFreeSourceReport.java<p/>
 * <b>Description: </b> Retrieves the data for the Data Feed location report
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Mar 10, 2007
 ****************************************************************************/
public class TollFreeSourceReport implements Report {
	private Map<String, Object> attributes = null;
	private SMTDBConnection conn = null;
	private static Logger log = Logger.getLogger(TollFreeSourceReport.class);
	//private List<ReportData> sourceData = new ArrayList<ReportData>();
	private String[] colors = new String[] {"BLUE", "RED", "GREEN", "ORANGE", "YELLOW", "BROWN", "BLACK" };
	
	/**
	 * 
	 */
	public TollFreeSourceReport() {
		attributes = new HashMap<String, Object>();
	}

	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.Report#retrieveReport(com.siliconmtn.http.SMTServletRequest)
	 */
	public Object retrieveReport(SMTServletRequest req)
	throws DatabaseException, InvalidDataException {
		log.debug("Starting Toll Free Source Report");
		
		// Get the parameters for the query
		String schema = StringUtil.checkVal((String)attributes.get(ReportFacadeAction.DATABASE_SCHEMA));
		Date start = Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("startDate"));
		Date end = Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("endDate"));
		
		if (end == null) end = new Date();
		if (start == null) start = Convert.formatDate(Convert.DATE_SLASH_PATTERN, "1/1/2000");
		if (schema.length() == 0 )
			throw new InvalidDataException("Schema name not provided");
		
		StringBuffer sb = new StringBuffer();
		sb.append("select toll_free_no, call_source_cd, count(*) ");
		sb.append("from ").append(schema).append("customer ");
		sb.append("where cast(convert(varchar, attempt_dt,101) AS DATETIME) ");
		sb.append("between ? and ? and product_cd = ?  and toll_free_no is not null ");
		sb.append("group by toll_free_no, call_source_cd ");
		sb.append("order by toll_free_no, call_source_cd ");
		log.debug("Toll Free Source Report SQL: " + sb);
		
		PreparedStatement ps = null;
		Set<String> tollFree = new TreeSet<String>();
		Map<String, ReportData> dataSource = new TreeMap<String, ReportData>();
		Map<String, Integer> headers = new TreeMap<String, Integer>();
		try {
			ps = conn.prepareStatement(sb.toString());
			ps.setDate(1, Convert.formatSQLDate(start));
			ps.setDate(2, Convert.formatSQLDate(end));
			ps.setString(3, req.getParameter("productCode"));
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String now = rs.getString(1);
				tollFree.add(now);
				
				// Get the headers and keep a running count for each soure type
				if (headers.containsKey(rs.getString(2))) {
					Integer tot = headers.get(rs.getString(2)) + rs.getInt(3);
					headers.put(rs.getString(2), tot);
				} else {
					headers.put(rs.getString(2), rs.getInt((3)));
				}
				
				if (dataSource.containsKey(now)) {
					dataSource.get(now).addDataSource(rs.getString(2),rs.getInt(3));
					
				} else {
					dataSource.put(now, new ReportData(rs.getString(2),rs.getInt(3)));
				}
			}
		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to retrieve daily source report", sqle);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {}
			}
		}
		
		// Create serie data for the chart
		List <StringBuffer> serieData = new ArrayList<StringBuffer>();
		for (Iterator<String> iter = headers.keySet().iterator(); iter.hasNext(); ) {
			StringBuffer t = new StringBuffer();
			String source = iter.next();
			for (Iterator<String> stIter = tollFree.iterator(); stIter.hasNext(); ) {
				ReportData rd = dataSource.get(stIter.next());
				if (rd != null) {
					t.append(Convert.formatInteger(rd.dataSource.get(source))).append("|");
				} else {
					continue;
				}
			}
			
			serieData.add(t);
		}
		
		// Create the state list for the labels
		StringBuffer dateLabel = new StringBuffer();
		for (Iterator<String> stIter = tollFree.iterator(); stIter.hasNext(); ) {
			dateLabel.append(stIter.next()).append("|");
		}
		
		log.debug("Number of transaction records: " + dataSource.size());
		req.setAttribute("dayLabel", dateLabel);
		req.setAttribute("serieData", serieData);
		req.setAttribute("dataSource", dataSource);
		req.setAttribute("barColors", colors);
		req.setAttribute("reportHeader", headers);
		return dataSource;
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
	 * <b>Title</b>: LocationReport.java<p/>
	 * <b>Description: </b> Holds a row of data from the report
	 * <p/>
	 * <b>Copyright:</b> Copyright (c) 2007<p/>
	 * <b>Company:</b> Silicon Mountain Technologies<p/>
	 * @author james
	 * @version 2.0
	 * @since Mar 11, 2007
	 ***************************************************************************
	 */
	public class ReportData implements Serializable {
		private static final long serialVersionUID = 1l;
		Map<String, Integer> dataSource = new HashMap<String, Integer>();
		
		public ReportData() {}
		
		public ReportData(String source, Integer total) {
			this.dataSource.put(source, total);
		}
		
		public void addDataSource(String source, Integer total) {
			this.dataSource.put(source, total);
		}
		
		/**
		 * @return the source
		 */
		public Map<String, Integer> getDataSource() {
			return dataSource;
		}
	}

}
