package com.depuy.sitebuilder.datafeed;

// JDK 1.5.0
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

// Log4j 1.2.8
import org.apache.log4j.Logger;

// SMT Base Libs 2.0
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: LocationReport.java<p/>
 * <b>Description: </b> Retrieves the data for the Data Feed Qualified report
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Mar 10, 2007
 ****************************************************************************/
public class QualifiedReport implements Report {
	private Map<String, Object> attributes = null;
	private SMTDBConnection conn = null;
	private static Logger log = Logger.getLogger(QualifiedReport.class);
	private String[] colors = new String[] {"BLUE", "RED", "GREEN", "ORANGE", "YELLOW", "BROWN", "BLACK" };
	
	/**
	 * Use to group the report on a daily basis
	 */
	public static final int DAILY_GROUP_REPORT = 1;
	
	/**
	 * Use to group the report on a weekly basis
	 */
	public static final int WEEKLY_GROUP_REPORT = 2;
	
	/**
	 * Use to group the report on a monthly basis
	 */
	public static final int MONTHLY_GROUP_REPORT = 3;
	
	/**
	 * 
	 */
	public QualifiedReport() {
		attributes = new HashMap<String, Object>();
	}

	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.Report#retrieveReport(com.siliconmtn.http.SMTServletRequest)
	 */
	public Object retrieveReport(SMTServletRequest req)
	throws DatabaseException, InvalidDataException {
		log.debug("Starting Qualified Lead Report");

		// Get the parameters for the query
		String schema = StringUtil.checkVal((String)attributes.get(ReportFacadeAction.DATABASE_SCHEMA));
		Date start = Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("startDate"));
		Date end = Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("endDate"));
		
		if (end == null) end = new Date();
		if (start == null) start = Convert.formatDate(Convert.DATE_SLASH_PATTERN, "1/1/2000");
		if (schema.length() == 0 )
			throw new InvalidDataException("Schema name not provided");
		
		StringBuffer sql = new StringBuffer();
		sql.append("select toll_free_no, attempt_dt, a.profile_id, result_cd, ");
		sql.append("future_contact_flg, b.response_txt, b.question_map_id, opt_status_flg, ");
		sql.append("a.customer_id, zip_cd, allow_comm_flg, address_txt, city_nm, ");
		sql.append("state_cd, email_address_txt, area_cd, line_cd, exchange_cd, country_cd ");
		sql.append("from data_feed.dbo.customer a left outer join data_feed.dbo.customer_response b  ");
		sql.append("on a.customer_id = b.customer_id ");
		sql.append("and b.question_map_id in (98, 394,396,490,511) ");
		sql.append("left outer join profile_data c on a.profile_id = c.profile_id ");
		sql.append("where a.attempt_dt between ? and ? and toll_free_no is not null ");
		sql.append("and result_cd not in('TEST','IBHUNGUP', 'TESTCLNT','WRONG','WRG','CRANK','ADVERSE','ADVERSEAH','IBWRONG') ");
		sql.append("and product_cd = ? ");
		sql.append("order by a.customer_id ");
		log.debug("Qualified Source Report SQL: " + sql);
		
		PreparedStatement ps = null;
		Map<Date, ReportData> data = new TreeMap<Date, ReportData>();
		try {
			ps = conn.prepareStatement(sql.toString());
			ps.setDate(1, Convert.formatSQLDate(start));
			ps.setDate(2, Convert.formatSQLDate(end));
			ps.setString(3, req.getParameter("productCode"));
			
			ResultSet rs = ps.executeQuery();
			String customerId = "";
			DataVO vo = new DataVO();
			@SuppressWarnings("unused")
			int ct = 0, qt=0, one = 0, two = 0, three = 0;;
			while (rs.next()) {
				ct++;
				int qType = rs.getInt("question_map_id");
				
				// Since we can have multiple rows for a single entry,
				// only store this info once
				if (! customerId.equals(rs.getString("customer_id"))) {
					if (customerId.length() > 0) {
						// Add the elements to the map
						if (vo.getYesAnswer() > 1 && vo.getLead() > 0) {
							vo.setQualifiedLead(1);
							three ++;
						}
						
						if (vo.getQualifiedLead() > 0) qt++;
						
						ReportData rd = data.get(vo.getAttemptDate());
						if (rd != null) {
							rd.addTotal();
							rd.addQualified(vo.getQualifiedLead());
							rd.addLead(vo.getLead());
						} else {
							data.put(vo.getAttemptDate(), new ReportData(vo.getLead(), vo.getQualifiedLead()));
						}
					}
					
					StringEncrypter se = new StringEncrypter((String)attributes.get(Constants.ENCRYPT_KEY));
					UserDataVO user = new UserDataVO();
					user.setData(rs);
					user.setAddress(se.decrypt(user.getAddress()));
					user.setEmailAddress(se.decrypt(user.getEmailAddress()));
					user.setMainPhone(se.decrypt(rs.getString("area_cd")) + rs.getString("exchange_cd") + rs.getString("line_cd"));
					
					vo = new DataVO();
					vo.setPhoneNumber(rs.getString("toll_free_no"));
					vo.setAttemptDate(rs.getDate("attempt_dt"));
					vo.setProfileId(rs.getString("profile_id"));
					vo.setCustomerId(rs.getString("customer_id"));
					vo.setResultCode(rs.getString("result_cd"));
					vo.setOptIn(rs.getInt("allow_comm_flg"));
					vo.setZipCode(rs.getString("zip_cd"));
					
					// Check the country to ensure it is the US and the user is reachable
					Boolean isValid = true;
					if (! "US".equalsIgnoreCase(user.getCountryCode())) {
						isValid = false;
					} else if (! user.isUserReachable()) {
						isValid = false;
					}
					
					// If address is valid and the user is opted in, set lead to 1
					if (isValid && ! "HCP".equalsIgnoreCase(vo.getResultCode()) && ! "MEDINFO".equalsIgnoreCase(vo.getResultCode())) {
						if (vo.getOptIn() > -1) vo.setLead(1);
					}
				}
				
				// Determine if the response is qualified
				
				switch (qType) {
					case 394:
						// Check the Qualified Rules
						Integer intense = Convert.formatInteger(rs.getString("response_txt"));
						if (intense > 3) {
							vo.setQualifiedLead(1);
						}
						break;
					case 98:
						String painLevel =  rs.getString("response_txt");
						 if ("SEVERE".equalsIgnoreCase(painLevel) || "MODERATE".equalsIgnoreCase(painLevel)) {
							 vo.setQualifiedLead(1);
						 }
						 break;
					// Two of the following 3 must be answered correctly
					case 396:
						String question396 = rs.getString("response_txt");
						if("Y".equalsIgnoreCase(question396)) {
							one++;
							vo.addYesAnswer();
						}
						break;
					case 490:
						String question490 = rs.getString("response_txt");
						if("Y".equalsIgnoreCase(question490)) {
							two++;
							vo.addYesAnswer();
						}
						break;
					case 511:
						String question511 = rs.getString("response_txt");
						if("N".equalsIgnoreCase(question511)) vo.addYesAnswer();
						break;
				}
				
				// Update the customerId
				customerId = rs.getString("customer_id");
			}

			
			// Add the last entry
			if(customerId != null && customerId.length() > 0) {
				if (vo.getYesAnswer() > 1) vo.setQualifiedLead(1);
				
				ReportData rd = data.get(vo.getAttemptDate());
				if (rd != null) {
					rd.addTotal();
					rd.addQualified(vo.getQualifiedLead());
					rd.addLead(vo.getLead());
				} else {
					data.put(vo.getAttemptDate(), new ReportData(vo.getLead(), vo.getQualifiedLead()));
				}
			}
		} catch(Exception e) {
			log.error("Error Getting QualifiedReportData", e);
		}

		log.debug("Number of transaction records: " + data.size());
		//req.setAttribute("dayLabel", dateLabel);
		//req.setAttribute("serieData", serieData);
		req.setAttribute("dataSource", data);
		req.setAttribute("barColors", colors);
		//req.setAttribute("reportHeader", headers);
		
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
	 ***************************************************************************
	 */
	public class ReportData implements Serializable {
		private static final long serialVersionUID = 1l;
		private int lead = 0;
		private int qualified = 0;
		private int total = 0;
		
		public ReportData() {}
		
		public ReportData(int lead, int qualified) {
			addTotal();
			addLead(lead);
			addQualified(qualified);
		}

		/**
		 * @return the lead
		 */
		public int getLead() {
			return lead;
		}

		/**
		 * @return the qualified
		 */
		public int getQualified() {
			return qualified;
		}

		/**
		 * @param lead the lead to set
		 */
		public void setLead(int lead) {
			this.lead = lead;
		}

		/**
		 * @param qualified the qualified to set
		 */
		public void setQualified(int qualified) {
			this.qualified = qualified;
		}
		
		public void addLead(int l) {
			if (l > 0) lead++;
		}
		
		public void addQualified(int q) {
			if (q > 0) qualified++;
		}

		/**
		 * @return the total
		 */
		public int getTotal() {
			return total;
		}

		/**
		 * @param total the total to set
		 */
		public void setTotal(int total) {
			this.total = total;
		}
		
		public void addTotal() {
			total++;
		}
	}

}
