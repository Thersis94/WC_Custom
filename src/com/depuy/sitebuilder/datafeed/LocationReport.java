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
 * <b>Title</b>: LocationReport.java<p/>
 * <b>Description: </b> Retrieves the data for the Data Feed location report
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Mar 10, 2007
 ****************************************************************************/
public class LocationReport implements Report {
	private Map<String, Object> attributes = null;
	SMTDBConnection conn = null;
	private static Logger log = Logger.getLogger(LocationReport.class);
	
	/**
	 * 
	 */
	public LocationReport() {
		attributes = new HashMap<String, Object>();
	}

	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.Report#retrieveReport(com.siliconmtn.http.SMTServletRequest)
	 */
	public Object retrieveReport(SMTServletRequest req)
	throws DatabaseException, InvalidDataException {
		log.debug("Starting Location Report");
		
		// Get the parameters for the query
		String schema = StringUtil.checkVal((String)attributes.get(ReportFacadeAction.DATABASE_SCHEMA));
		Date start = Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("startDate"));
		Date end = Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("endDate"));
		if (end == null) end = new Date();
		if (start == null) start = Convert.formatDate(Convert.DATE_SLASH_PATTERN, "1/1/2000");
		if (schema.length() == 0 )
			throw new InvalidDataException("Schema name not provided");
		
		StringBuffer sb = new StringBuffer();
		sb.append("select city_nm, state_cd, rtrim(call_source_cd), count(*) ");
		sb.append("from ").append(schema).append("customer a inner join profile_address b ");
		sb.append("on a.profile_id = b.profile_id ");
		sb.append("inner join ").append(schema).append("fulfillment c ");
		sb.append("on a.customer_id = c.customer_id ");
		sb.append("where cast(convert(varchar, a.attempt_dt,101) AS DATETIME) between ? and ? ");
		sb.append("and product_cd = ? ");
		sb.append("group by city_nm, state_cd, call_source_cd ");
		sb.append("order by city_nm, state_cd ");
		log.info("Location Report SQL: " + sb.toString());
		
		PreparedStatement ps = null;
		Map<String, Integer> headers = new TreeMap<String, Integer>();
		List<ReportData> data = new ArrayList<ReportData>();

		try {
			ps = conn.prepareStatement(sb.toString());
			ps.setDate(1, Convert.formatSQLDate(start));
			ps.setDate(2, Convert.formatSQLDate(end));
			ps.setString(3, req.getParameter("productCode"));
			ResultSet rs = ps.executeQuery();
			String loc = null, newLocation = null;
			ReportData rd = null;
			boolean firstPass = true;
			
			while (rs.next()) {
				String city = StringUtil.checkVal(rs.getString(1)).trim().toLowerCase();
				String state = StringUtil.checkVal(rs.getString(2)).trim().toLowerCase();
				loc = city + ", " + state;
				if (! loc.equalsIgnoreCase(newLocation)) {
					if(firstPass) {
						firstPass=false;
					} else {
						data.add(rd);
					}
					rd = new ReportData(loc, rs.getString(3), rs.getInt(4));
				} else {
					rd.addDataSource(rs.getString(3),rs.getInt(4));
				}
				
				//	 Get the headers and keep a running count for each source type
				if (headers.containsKey(rs.getString(3))) {
					Integer tot = headers.get(rs.getString(3)) + rs.getInt(4);
					headers.put(rs.getString(3), tot);
				} else {
					headers.put(rs.getString(3), rs.getInt((4)));
				}
				
				newLocation = loc;
				firstPass = false;
			}

			// Get the last hanging element
			data.add(rd);
		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to retrieve data feed transaction report", sqle);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {}
			}
		}
		
		log.info("headers: " + headers);
		log.info("Number of transaction records: " + data.size());
		req.setAttribute("reportTotal", headers);
		req.setAttribute("reportHeader", new ArrayList<String>(headers.keySet()));
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
	 * <b>Title</b>: LocationReport.java<p/>
	 * <b>Description: </b> Holds a row of data from the report
	 * <p/>
	 * <b>Copyright:</b> Copyright (c) 2007<p/>
	 * <b>Company:</b> Silicon Mountain Technologies<p/>
	 * @author james
	 * @version 2.0
	 * @since Mar 11, 2007
	 ***************************************************************************/
	public class ReportData implements Serializable {
		private static final long serialVersionUID = 1l;
		Map<String, Integer> dataSource = new HashMap<String, Integer>();
		String location = null;
		
		public ReportData() {}
		
		public ReportData(String loc, String source, Integer total) {
			location = loc;
			this.dataSource.put(source, total);
		}
		
		public void addDataSource(String source, Integer total) {
			this.dataSource.put(source, total);
		}
		
		public String toString() {
			StringBuffer sb = new StringBuffer();
			sb.append(location).append(" - ");
			
			Set<String> s = dataSource.keySet();
			for (Iterator<String> iter = s.iterator(); iter.hasNext(); ) {
				String t = iter.next();
				sb.append(t).append("[").append(dataSource.get(t)).append("],");
			}
			return sb.toString();
		}
		
		/**
		 * @return the source
		 */
		public Map<String, Integer> getDataSource() {
			return dataSource;
		}
		
		public Integer getElement(String ele) {
			return Convert.formatInteger(dataSource.get(ele));
		}
		
		public void setLocation(String loc) { this.location = loc; }
		public String getLocation() { return this.location; }
	}

}
