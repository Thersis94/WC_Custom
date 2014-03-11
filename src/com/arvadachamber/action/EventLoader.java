package com.arvadachamber.action;

// JDK 1.6.x
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

// Log4J 1.2.15
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// SMT Base Libs
import com.siliconmtn.gis.Location;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.CSVParser;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: EventLoader.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Loads the event data from the Chamber Master Admin site and
 * stores the data in the WC Data models
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Oct 2, 2012<p/>
 * <b>Changes: </b>
 * 10/02/2012: David Bargerhuff; created EventLoader class to isolate the event loading methods.
 ****************************************************************************/
public class EventLoader extends ChamberMasterLoader {
	private static final Logger log = Logger.getLogger("EventLoader");
	private String eventFromDate;
	private String eventToDate;
	
	/**
	 * 
	 */
	public EventLoader() throws Exception {
		this.loadEventDates();
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// configure logging
		PropertyConfigurator.configure("scripts/arvc_importer_log4j.properties");
		BasicConfigurator.configure();
		
		// Load the config file
		Properties config = new Properties();
		config.load(new FileInputStream(new File("scripts/arvc_importer.properties")));
		log.debug("Config Loaded");
		
		// instantiate the manager
		EventLoader ed = new EventLoader();
		
		// obtain DB Connection
		ed.getConnection(config);
		log.debug("opened db connection...");
		
		// Load custom schema name
		ed.setCustomDbSchema(config.getProperty("customDbSchema"));
		log.debug("loaded custom schema name");
		long start = Calendar.getInstance().getTimeInMillis();
		// Load the hot deals
		try {
			ed.importEvents();
		} catch (IOException ioe) {
			log.error("Error logging in to retrieve event data, ", ioe);
			if (! ed.isErrors()) ed.setErrors(true);
		}
		// clean-up
		try {
			ed.conn.close();
			log.info("db connection closed.");
		} catch(Exception e) {
			log.error("Error closing db connection, ", e);
		}
		long end = Calendar.getInstance().getTimeInMillis();
		log.debug("elapsed time: " + (end - start)/1000 + " seconds");				
		log.info("exiting loader.");
	}
	
	/**
	 * Imports and processes event data.
	 * @throws IOException
	 */
	protected void importEvents() throws IOException {
		if (httpConn == null) {
			httpConn = new SMTHttpConnectionManager();
			this.login(httpConn);
		} else {
			// reset the headers and make sure not to follow redirects.
			this.assignConnectionHeader(httpConn);
			httpConn.setFollowRedirects(false);
		}
		byte[] events = null;
		try {
			events = this.loadEvents(httpConn);
			this.storeEvents(events);
			//this.storeEventsAsFile(events);
		} catch(IOException ioe) {
			log.error("Error retrieving or parsing event data, ", ioe);
			addStatusMessage("Error retrieving or parsing event data, " + ioe.getMessage());
			if (! isErrors()) setErrors(true);
		} catch(Exception sqle) {
			log.error("Error inserting event data, ", sqle);
			addStatusMessage("Error inserting event data, " + sqle.getMessage());
			if (! isErrors()) setErrors(true);
		}
	}
	
	/**
	 * Loads the events file
	 * @throws IOException
	 */
	private byte[] loadEvents(SMTHttpConnectionManager httpConn) throws IOException {
		log.info("loading events via HTTP...");

		// Create the URL to generate the report
		/*
		 * Flag notes:
		 * isPublic: -1 = not specified, 0 = exclude 'public' events, 1 = only include 'public' events
		 * lstVisibility: same as 'isPublic', must match 'isPublic' value.
		 */
		String sUrl = "https://secure2.chambermaster.com/directory/jsp/reports/events/Listing.jsp";
		StringBuilder params = new StringBuilder();
		params.append("command=deliver&qualifier=&destination=&eventId=0&typeId=-1");
		params.append("&isPublic=1&lstEventTypes=0&lstVisibility=1&chkDescription=on");
		params.append("&chkDate=on&chkContact=on&chkLocation=on&chkTime=on&chkEmail=on");
		params.append("&chkAdmission=on&chkHours=on&chkWebsite=on");
		params.append("&dateTo=" + eventToDate);
		params.append("&dateFrom=" + eventFromDate);
		params.append("&page=%2Fdirectory%2Fjsp%2Freports%2Fevents%2FListing.jsp");
		log.debug("Params: " + params);
		
		// retrieve the report response
		byte[] b = httpConn.retrieveDataViaPost(sUrl, params.toString());

		// parse out the report link (dir and filename)
		String repUrl = this.parseReportLink(new String(b));
		log.debug("report link Url: " + repUrl);
		b = null;
		// retrieve the report data
		//byte[] repData = conn.retrieveData(SECURE_BASE_URL + repUrl);
		byte[] repData = httpConn.retrieveData(BASE_URL + repUrl);
		return repData;
	}
	
	/**
	 * Utility file for saving events source data as a file for troubleshooting data import issues.
	 * @param fileData
	 */
	@SuppressWarnings("unused")
	private void storeEventsAsFile(byte[] fileData) {
		FileOutputStream fo = null;
		try {
			fo = new FileOutputStream("D:\\scripts\\ArvadaChamber\\logs\\EventListAsFile.csv");
			fo.write(fileData);
		} catch(Exception e) {
			log.error("Error writing file: " + e);
		} finally {
			try {
				fo.flush();
				fo.close();
			} catch (Exception e) { log.error("error flushing or closing file:, ", e); }
		}
	}
		
	/**
	 * Parses the provided file data and stores each event into the DB
	 * @param fileData
	 * @throws IOException
	 * @throws SQLException
	 */
	private void storeEvents(byte[] fileData) throws IOException, SQLException {
		if (fileData == null) return;
		CSVParser p = new CSVParser();
		List<List<String>> data = p.parseFile(fileData, true);
		log.debug("Raw number of events: " + data.size());
		if (data.size() == 0) {
			log.info("No events found to import.");
			addStatusMessage("No events found to import.");
			return;
		}
		this.deleteExistingEvents();
		int validCount = 0;
		
		// Build the SQL Statement
		StringBuilder s = new StringBuilder();
		s.append("insert into event_entry (event_entry_id, event_type_id, event_nm, ");
		s.append("start_dt, end_dt, event_desc, short_desc, location_desc, event_url, ");
		s.append("contact_nm, phone_txt, address_txt, address2_txt, city_nm, state_cd, ");
		s.append("zip_cd, create_dt, action_id, status_flg) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ");
		PreparedStatement ps = conn.prepareStatement(s.toString());
		
		Location loc = null;
		String[] contactAndPhone = null;
		
		for (int i=0; i < data.size(); i++) {
			if (data.get(i).size() < 8) continue;
			List<String> row = data.get(i);
			
			loc = new Location(this.cleanTags(row.get(6)));
			try {
			contactAndPhone = this.parseContactAndPhone(this.cleanTags(row.get(10)));
			} catch (Exception e) {
				log.info("Suppressing exception; No contact/phone info in data row for event ID: " + row.get(0));
			}
			
			// format dates as Timestamps so we can keep the time value.
			Timestamp sd = Convert.formatTimestamp(Convert.DATE_TIME_SLASH_PATTERN, row.get(2));
			Timestamp ed = null;
			if (StringUtil.checkVal(row.get(3)).length() > 0) {
				ed = Convert.formatTimestamp(Convert.DATE_TIME_SLASH_PATTERN, row.get(3));
			}

			// set params
			ps.setString(1, row.get(0)); //event_entry_id
			ps.setString(2, EVENT_TYPE_ID); // event_type_id
			ps.setString(3, row.get(1)); // event_nm
			ps.setTimestamp(4, sd); // start_dt
			ps.setTimestamp(5, ed); // end_dt
			ps.setString(6, row.get(4)); // event_desc
			ps.setString(7, row.get(7)); // short_desc
			ps.setString(8, row.get(5)); // location_desc
			ps.setString(9, row.get(9)); // event_url
			ps.setString(10, contactAndPhone[0]); // contact_nm
			ps.setString(11, contactAndPhone[1]); // phone_txt
			ps.setString(12, StringUtil.capitalizeAddress(loc.getAddress())); // address_txt
			ps.setString(13, StringUtil.capitalizeAddress(loc.getAddress2())); // address2_txt
			ps.setString(14, StringUtil.capitalizeAddress(loc.getCity())); // city_nm
			ps.setString(15, loc.getState()); // state_cd
			ps.setString(16, loc.getZipCode()); // zip_cd
			ps.setTimestamp(17, Convert.getCurrentTimestamp()); // create_dt
			ps.setString(18, EVENT_ACTION_ID); // action_id
			ps.setInt(19, 15); // status_flg
			try {
				ps.executeUpdate();
				validCount++;
			} catch(SQLException sqle) {
				addStatusMessage("error inserting event " + row.get(0) + ", " + sqle.getMessage());
				addStatusMessage("contact/phone: " + contactAndPhone[0] + "/" + contactAndPhone[1]);
				addStatusMessage("loc address: " + loc.getAddress());
				addStatusMessage("loc address2: " + loc.getAddress2());
				addStatusMessage("loc city: " + loc.getCity());
				addStatusMessage("loc state: " + loc.getState());
				addStatusMessage("loc zip: " + loc.getZipCode());
				if (! isErrors()) setErrors(true);
			}
		}
		if (ps != null) {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		log.debug("Events inserted: " + validCount);
	}
		
	/**
	 * Deletes existing event entries for the given event action and event type id.
	 * @throws SQLException
	 */
	private void deleteExistingEvents() throws SQLException {
		StringBuffer sb = new StringBuffer();
		sb.append("delete from event_entry where event_type_id = ? and action_id = ?");
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sb.toString());
			ps.setString(1, EVENT_TYPE_ID);
			ps.setString(2, EVENT_ACTION_ID);
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("Error deleting existing events, ", sqle);
			addStatusMessage("Error deleting existing events, " + sqle.getMessage());
			if (! isErrors()) setErrors(true);
			throw new SQLException(sqle.getMessage());
		}
	}
	
	/**
	 * sets date range for event retrieval
	 */
	private void loadEventDates() {
		Calendar cal = GregorianCalendar.getInstance();
		eventFromDate = (cal.get(Calendar.MONTH) + 1) + "/1/" + cal.get(Calendar.YEAR);
		cal.add(Calendar.YEAR, 1);
		eventToDate = (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.DAY_OF_MONTH) + "/" + cal.get(Calendar.YEAR);
		try {
			eventFromDate = URLEncoder.encode(eventFromDate, "utf-8");
			eventToDate = URLEncoder.encode(eventToDate, "utf-8");
		} catch (Exception e) {
			log.error("Error encoding event dates, ", e);
			addStatusMessage("Error encoding event dates, " + e.getMessage());
			if (! isErrors()) setErrors(true);
		}
		log.debug("from|to dates: " + eventFromDate + "|" + eventToDate);
	}
	
	/**
	 * Removes double quotes and HTML tags from a String and returns the cleaned String.  If 
	 * the String does not contain double quotes or HTML tags, the original String is returned.
	 * @param col
	 * @return
	 */
	private String cleanTags(String col) {
		if (StringUtil.checkVal(col).length() == 0) return col;
		if (col.indexOf("<") > -1) {
			// first replace double quotes with single quotes
			col = col.replace("\"\"", "\"");
			char[] src = col.toCharArray();
			char[] clean = new char[src.length];
			// loop chars to remove
			boolean skipChars = false;
			for (int i = 0; i < src.length; i++) {
				if (src[i] == '<') {
					skipChars = true;
				} else if (src[i] == '>') {
					skipChars = false;
				} else if (! skipChars) {
					clean[i] = src[i];
				}
			}
			return new String(clean).trim();
		} else {
			return col;
		}
	}
	
	/**
	 * Helper method to parse contact and phone data from the value passed in.
	 * @param data
	 * @return
	 */
	private String[] parseContactAndPhone(String data) {
		String[] vals = new String[2];
		if (data.indexOf(',') > -1) {
			vals = data.split(",");
			vals[0] = vals[0].trim();
			PhoneNumberFormat pnf = new PhoneNumberFormat(vals[1], 1);
			vals[1] = pnf.getFormattedNumber();
		}
		return vals;
	}

}
