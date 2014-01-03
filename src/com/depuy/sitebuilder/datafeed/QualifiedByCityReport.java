package com.depuy.sitebuilder.datafeed;

import java.io.Serializable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.gis.AbstractGeocoder;
import com.siliconmtn.gis.GeocodeFactory;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.Location;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.NumberFormat;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;

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
public class QualifiedByCityReport implements Report {
	private Map<String, Object> attributes = null;
	SMTDBConnection conn = null;
	private static Logger log = Logger.getLogger(QualifiedByCityReport.class);
	
	public QualifiedByCityReport() {
		attributes = new HashMap<String, Object>();
	}
	
	/* (non-Javadoc)
	 * @see com.depuy.sitebuilder.datafeed.Report#retrieveReport(com.siliconmtn.http.SMTServletRequest)
	 */
	public Object retrieveReport(SMTServletRequest req)
	throws DatabaseException, InvalidDataException {
		List<ReportData> data = new ArrayList<ReportData>();
		
		// Make sure the db connection is present
		if (conn == null) 
			throw new DatabaseException("No Database Connection");
		
		// Get the parameters for the query
		String schema = StringUtil.checkVal((String)attributes.get(ReportFacadeAction.DATABASE_SCHEMA));
		Date start = Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("startDate"));
		Date end = Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("endDate"));
		if (end == null) end = new Date();
		if (start == null) start = Convert.formatDate(Convert.DATE_SLASH_PATTERN, "1/1/2000");
		if (schema.length() == 0 )
			throw new InvalidDataException("Schema name not provided");
		
		// Get Additional Params
		Location loc = new Location("", req.getParameter("city"), req.getParameter("state"), "");
		Integer radius = Convert.formatInteger(req.getParameter("radius"),25);
		String[] skus = req.getParameterValues("fulfillmentSku");

		
		//Create the geocoder
		String geoClass = (String) attributes.get(GlobalConfig.GEOCODER_CLASS);
		AbstractGeocoder ag = GeocodeFactory.getInstance(geoClass);
		ag.addAttribute(AbstractGeocoder.CONNECT_URL, attributes.get(GlobalConfig.GEOCODER_URL));
		GeocodeLocation gl = ag.geocodeLocation(loc).get(0);
		
		// Build the SQL Statement
		StringBuffer sql = new StringBuffer();
		sql.append("SELECT first_nm, last_nm, email_address_txt, address_txt, ");
		sql.append("city_nm, state_cd, zip_cd ");
		sql.append("from profile a left outer join profile_address b ");
		sql.append("on a.profile_id = b.profile_id ");
		sql.append("inner join ").append(schema).append("customer c ");
		sql.append("on a.profile_id = c.profile_id ");
		sql.append("inner join ").append(schema).append("fulfillment d ");
		sql.append("on c.customer_id = d.customer_id ");
		sql.append("where c.create_dt between ? and ? ");
		
		// Append the radius search for the MSA
		log.info("Radius: " + radius);
		double radDegree = radius * .014;
		sql.append("and Latitude_no > ").append(NumberFormat.round(gl.getLatitude() - radDegree)).append(" and ");
		sql.append("Latitude_no < ").append(NumberFormat.round(gl.getLatitude() + radDegree)).append(" and ");
		sql.append("Longitude_no > ").append(NumberFormat.round(gl.getLongitude() - radDegree)).append(" and ");
		sql.append("Longitude_no < ").append(NumberFormat.round(gl.getLongitude() + radDegree));
		
		// Add the Fulfillment SKUs
		if (skus != null && skus.length > 0) {
			sql.append(" and sku_cd in (");
				for (int i=0; i < skus.length; i++) {
					sql.append("?");
					if ((skus.length - 1) > i) sql.append(",");
				}
			sql.append(")");
		}
		log.info("Qualified By City Report SQL: " + sql.toString());
		
		PreparedStatement ps = null;
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		try {
			int ctr = 1;
			ps = conn.prepareStatement(sql.toString());
			ps.setDate(ctr++, Convert.formatSQLDate(start));
			ps.setDate(ctr++, Convert.formatSQLDate(end));
			for (int i=0; skus != null && i < skus.length; i++) {
				ps.setString(ctr++, skus[i]);
			}
			// Get the data
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				ReportData rd = new ReportData();
				rd.setFirstName(pm.getStringValue("first_nm", rs.getString("first_nm")));
				rd.setLastName(pm.getStringValue("last_nm", rs.getString("last_nm")));
				rd.setEmail(pm.getStringValue("email_address_txt", rs.getString("email_address_txt")));
				rd.setAddress(pm.getStringValue("address_txt", rs.getString("address_txt")));
				rd.setCity(pm.getStringValue("city_nm", rs.getString("city_nm")));
				rd.setState(pm.getStringValue("state_cd", rs.getString("state_cd")));
				rd.setZip(pm.getStringValue("zip_cd", rs.getString("zip_cd")));
				
				data.add(rd);
			}
		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to retrieve data feed Qualified By City report", sqle);
		}
		
		log.debug("Number of report records: " + data.size());
		Collections.sort(data, new ReportDataComparator());
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
		private String firstName = null;
		private String lastName = null;
		private String email = null;
		private String address = null;
		private String city = null;
		private String state = null;
		private String zip = null;
		
		/**
		 * @return the address
		 */
		public String getAddress() {
			return address;
		}
		/**
		 * @return the city
		 */
		public String getCity() {
			return city;
		}
		/**
		 * @return the email
		 */
		public String getEmail() {
			return email;
		}
		/**
		 * @return the firstName
		 */
		public String getFirstName() {
			return firstName;
		}
		/**
		 * @return the lastName
		 */
		public String getLastName() {
			return lastName;
		}
		/**
		 * @return the state
		 */
		public String getState() {
			return state;
		}
		/**
		 * @return the zip
		 */
		public String getZip() {
			return zip;
		}
		/**
		 * @param address the address to set
		 */
		public void setAddress(String address) {
			this.address = address;
		}
		/**
		 * @param city the city to set
		 */
		public void setCity(String city) {
			this.city = city;
		}
		/**
		 * @param email the email to set
		 */
		public void setEmail(String email) {
			this.email = email;
		}
		/**
		 * @param firstName the firstName to set
		 */
		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}
		/**
		 * @param lastName the lastName to set
		 */
		public void setLastName(String lastName) {
			this.lastName = lastName;
		}
		/**
		 * @param state the state to set
		 */
		public void setState(String state) {
			this.state = state;
		}
		/**
		 * @param zip the zip to set
		 */
		public void setZip(String zip) {
			this.zip = zip;
		}
		
	}
}
