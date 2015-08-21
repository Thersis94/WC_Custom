package com.depuy.sitebuilder.datafeed;

// JDK 1.7
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

// Log4j 1.2.17
import org.apache.log4j.Logger;

// SMT Base Libs 2.0
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: LeadSourceReport.java<p/>
 * <b>Description: </b> Retrieves the data for the Data Feed location report
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Mar 10, 2007
 * @updates
 * 		JM 07.30.15 - added leadType=15 and joint=SHOULDER.  Also addressed performance issues.
 ****************************************************************************/
public class LeadSourceReport implements Report {
	private Map<String, Object> attributes = null;
	private SMTDBConnection conn = null;
	private static Logger log = Logger.getLogger(LeadSourceReport.class);
	private String[] colors = new String[] {"BLUE", "RED", "GREEN", "ORANGE", "YELLOW", "BROWN", "BLACK" };
	
	/**
	 * 
	 */
	public LeadSourceReport() {
		attributes = new HashMap<String, Object>();
	}

	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.Report#retrieveReport(com.siliconmtn.http.SMTServletRequest)
	 */
	public Object retrieveReport(SMTServletRequest req)
	throws DatabaseException, InvalidDataException {
		log.debug("Starting Daily Source Report");
		int groupType = Convert.formatInteger(req.getParameter("groupType"));
		
		String sqlType = "attempt_day_no, attempt_month_no, attempt_year_no ";
		switch (groupType) {
			case 2:
				sqlType = "attempt_week_no, attempt_year_no ";
				break;
			case 3:
				sqlType = "attempt_month_no, attempt_year_no ";
				break;
		}
		
		// Get the parameters for the query
		String schema = StringUtil.checkVal((String)attributes.get(ReportFacadeAction.DATABASE_SCHEMA));
		String productCode = StringUtil.checkVal(req.getParameter("productCode"));
		
		// Calculate the start and end date
		Date start = Convert.formatStartDate(req.getParameter("startDate"), "1/1/2000");
		Date end = Convert.formatEndDate(req.getParameter("endDate"));
		
		if (schema.length() == 0 )
			throw new InvalidDataException("Schema name not provided");
		
		StringBuffer sb = new StringBuffer(250);
		sb.append("select ").append(sqlType).append(", ");
		sb.append("a.product_cd, call_source_cd, lead_type_id, count(*) total ");
		sb.append("from ").append(schema).append("customer a ");
		sb.append("inner join ").append(schema).append("product b on a.product_cd = b.product_cd and organization_id = 'DEPUY' ");
		sb.append("where attempt_dt between ? and ? and lead_type_id is not null ");
		
		// add the product code to the sql statement
		if (productCode.length() > 0) sb.append("and a.product_cd = ? ");
		sb.append("group by ").append(sqlType).append(", a.product_cd, call_source_cd, lead_type_id ");
		sb.append("order by ").append(sqlType).append(" desc, a.product_cd, call_source_cd ");
		log.info("Lead Source Report SQL: " + sb + "|" + start + "|" + end);
		
		Set<Date> dates = new TreeSet<Date>();
		Map<Date, ReportData> dataSource = new TreeMap<Date, ReportData>();
		Map<String, Integer> headers = new TreeMap<String, Integer>();
		try (PreparedStatement ps = conn.prepareStatement(sb.toString())) {
			ps.setDate(1, Convert.formatSQLDate(start));
			ps.setDate(2, Convert.formatSQLDate(end));
			if (productCode.length() > 0) ps.setString(3, productCode);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Date now = null;
				if (groupType > 1) {
					now = ReportUtil.calculateDate(groupType, rs.getInt(1), 0, rs.getInt(2));
				} else {
					now = ReportUtil.calculateDate(groupType, rs.getInt(1), rs.getInt(2), rs.getInt(3));
				}
				
				// Add the date to the data map
				dates.add(now);

				// Get the headers and keep a running count for each source type
				if (headers.containsKey(rs.getString("call_source_cd"))) {
					Integer tot = headers.get(rs.getString("call_source_cd")) + rs.getInt("total");
					headers.put(rs.getString("call_source_cd"), tot);
				} else {
					headers.put(rs.getString("call_source_cd"), rs.getInt(("total")));
				}
				
				String source = rs.getString("call_source_cd");
				String product = rs.getString("product_cd");
				int leadType = rs.getInt("lead_type_id");
				int total = rs.getInt("total");
				if (dataSource.containsKey(now)) {
					dataSource.get(now).addDataSource(now, source, product, leadType, total);
					
				} else {
					dataSource.put(now, new ReportData(now, source, product, leadType, total));
				}
			}
		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to retrieve daily source report", sqle);
		}
		
		// Create serie data for the chart
//		List <StringBuilder> serieData = new ArrayList<StringBuilder>();
/*		for (Iterator<String> iter = headers.keySet().iterator(); iter.hasNext(); ) {
			StringBuffer t = new StringBuffer();
			String source = iter.next();
			for (Iterator<Date> stIter = dates.iterator(); stIter.hasNext(); ) {
				ReportData rd = dataSource.get(stIter.next());
				if (rd != null) {
					t.append(Convert.formatInteger(rd.dataSource.get(source))).append("|");
				} else {
					continue;
				}
			}
			
			serieData.add(t);
		}
*/		
		// Create the state list for the labels
		StringBuilder dateLabel = new StringBuilder();
		for (Iterator<Date> stIter = dates.iterator(); stIter.hasNext(); ) {
			Date d = stIter.next();
			dateLabel.append(d).append("|");
		}
		
		log.info("Number of transaction records: " + dataSource.size());
		req.setAttribute("dayLabel", dateLabel);
//		req.setAttribute("serieData", serieData);
		//req.setAttribute("dataSource", dataSource);
		req.setAttribute("barColors", colors);
		req.setAttribute("reportHeader", headers);
		req.setAttribute("leadTypes", new Integer[] {0,1,5,10,15});
		req.setAttribute("jointTypes", new String[] {"HIP", "KNEE", "SHOULDER", "OTHER"});
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
		private static final long serialVersionUID = 19876543554l;
		
		// Elements Data Source -> Joint -> Lead Type -> Total
		Map<String, Map<String,Map<Integer, Integer>>> dataSource = new HashMap<String, Map<String,Map<Integer, Integer>>>();
		
		public ReportData(Date now, String source, String joint, Integer leadType, Integer total) {
			//log.debug("Putting: " + source + "|" + leadType + "|" + total);
			this.addDataSource(now, source, joint, leadType, total);
		}
		
		/**
		 * 
		 * @param source
		 * @param leadType
		 * @param total
		 */
		public void addDataSource(Date now, String source, String joint, Integer leadType, Integer total) {
			if (dataSource.containsKey(source)) {
				Map<String, Map<Integer, Integer>> vals = dataSource.get(source);
				Map<Integer, Integer> tVals = new HashMap<Integer, Integer>(5);
				int valTotal = total;
				
				if (vals.containsKey(joint)) {
					tVals = vals.get(joint);
					if (vals.containsKey(leadType + "")) {
						tVals = vals.get(leadType + "");
						valTotal = total + tVals.get(leadType);
					}
					
					tVals.put(leadType, valTotal);
					vals.put(joint, tVals);
					
				} else {
					tVals.put(leadType, valTotal);
					vals.put(joint, tVals);
				}
				
				this.dataSource.put(source,vals);
			} else {
				Map<String, Map<Integer, Integer>> vals = new HashMap<String, Map<Integer, Integer>>();
				Map<Integer, Integer> t = new HashMap<Integer, Integer>();
				t.put(leadType,total);
				vals.put(joint, t);
				this.dataSource.put(source,vals);
			}
		}
		
		
		/**
		 * @return the source
		 */
		public Map<String, Map<String,Map<Integer, Integer>>> getDataSource() {
			return dataSource;
		}
		
		
		/**
		 * manually climb the tree of Maps and fail-fast when we don't have what we need.
		 * This is 100x faster than relying on JSTL, which wraps each Map.get(k) in a try/catch and 
		 * accounts for gracefully failing. 
		 * @param callSrc
		 * @param joint
		 * @param leadType
		 * @return
		 */
		public Integer getCount(String callSrc, String joint, Integer leadType) {
			//call source map - typically contains ~10 records
			Map<String, Map<Integer, Integer>> joints = dataSource.get(callSrc);
			if (joints == null) return 0;
			
			//joints map - contains 4 records - see req.setAttribute("jointTypes") above
			Map<Integer, Integer> leads = joints.get(joint);
			if (leads == null) return 0;
			
			// leads map - contains 5 records - see req.setAttribute("leadTypes") above
			if (leads.containsKey(leadType)) return leads.get(leadType);
			
			return 0;
		}
	}

}
