package com.arvadachamber.action;

// JDK 1.6.x
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

// Log4J 1.2.15
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// SMT Base Libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.FileException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.gis.Location;
import com.siliconmtn.io.FileManager;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.CSVParser;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: DataLoader.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Loads the data from the Chamber Master Admin site and
 * stores the data in the WC Data models
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Mar 5, 2012<p/>
 * <b>Changes: </b>
 * 07-20-2012: Dave Bargerhuff: updated event data import processing.
 ****************************************************************************/
public class DataLoader {
	public static final String USER_AGENT = "User-Agent:Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.142 Safari/535.19";
	public static final String BASE_URL = "http://secure2.chambermaster.com";
	public static final String SECURE_BASE_URL = "https://secure2.chambermaster.com";
	private static final Logger log = Logger.getLogger("DataLoader");
	public static final String LISTING_PATH = "C:\\SMT\\accounts\\Arvada Chamber\\Listing.csv";
	public static final String EVENT_TYPE_ID = "c0a802413f97ab1153fd69d11c7e8d2";
	public static final String EVENT_ACTION_ID = "c0a802413f9763c5797fd740a09d1fe5";
	private Connection conn = null;
	private SMTHttpConnectionManager httpConn = null;
	private Properties config = new Properties();
	private String customDbSchema = null;
	private String eventFromDate;
	private String eventToDate;
	private List<String> statusMessages = null;
	/**
	 * 
	 */
	public DataLoader() throws Exception {
		PropertyConfigurator.configure("scripts/arvc_importer_log4j.properties");
		// Load the config file
		config.load(new FileInputStream(new File("scripts/arvc_importer.properties")));
		log.debug("Config Loaded");
		
		// Load the DB Connection
		conn = this.getConnection();
		log.debug("opened db connection...");
		
		httpConn = new SMTHttpConnectionManager();
		this.executeSecurelogin(httpConn);
		// Load custom schema name
		customDbSchema = config.getProperty("customDbSchema");
		//this.loadEventDates(config.getProperty("eventDateRangeInDays"));
		statusMessages = new ArrayList<String>();
		this.loadEventDates();
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();
		long start = Calendar.getInstance().getTimeInMillis();
		DataLoader dl = new DataLoader();
		
		// Load the members
		MemberLoader mems = new MemberLoader();
		mems.setConn(dl.conn);
		mems.setCustomDbSchema(dl.customDbSchema);
		mems.setHttpConn(dl.httpConn);
		mems.importMembers();
		dl.addStatusMessages(mems.getStatusMessages());
		mems = null;
		
		
		// Load the hot deals
		HotDealsLoader hdm = new HotDealsLoader();
		hdm.setConn(dl.conn);
		hdm.setCustomDbSchema(dl.customDbSchema);
		hdm.setHttpConn(dl.httpConn);
		hdm.importHotDeals();
		dl.addStatusMessages(hdm.getStatusMessages());
		hdm = null;
		
		// Load the calendar data
		//dl.loadEvents();
	
		try {
			dl.conn.close();
			log.info("db connection closed.");
		} catch(Exception e) {
			log.error("Error closing db connection, ", e);
		}
		long end = Calendar.getInstance().getTimeInMillis();
		log.debug("elapsed time: " + (end - start)/1000 + " seconds");				
		log.info("exiting loader.");
	}
		
	/**
	 * Loads the event data from the file system and adds each event into the database
	 * @param filePath Fully qualified path to the file
	 * @throws FileException
	 * @throws IOException
	 * @throws SQLException
	 */
	public void storeEvents(String filePath) throws FileException, IOException, SQLException {
		FileManager fm = new FileManager();
		byte[] data = fm.retrieveFile(filePath);
		this.storeEvents(data);
	}
	
	/**
	 * Parses the provided file data and stores each event into the DB
	 * @param fileData
	 * @throws IOException
	 * @throws SQLException
	 */
	public void storeEvents(byte[] fileData) throws IOException, SQLException {
		CSVParser p = new CSVParser();
		List<List<String>> data = p.parseFile(fileData, true);
		log.debug("Raw number of events: " + data.size());
		if (data.size() == 0) {
			log.info("No events found to import.");
			return;
		}
		//if (data.size() > 0) return;
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
			contactAndPhone = this.parseContactAndPhone(this.cleanTags(row.get(10)));
			
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
			} catch(Exception e) {
				log.debug("error inserting event " + row.get(0) + ", " + e.getMessage());
				log.debug("contact/phone: " + contactAndPhone[0] + "/" + contactAndPhone[1]);
				log.debug("loc address: " + loc.getAddress());
				log.debug("loc address2: " + loc.getAddress2());
				log.debug("loc city: " + loc.getCity());
				log.debug("loc state: " + loc.getState());
				log.debug("loc zip: " + loc.getZipCode());
				log.debug("row data:");
				for (int j = 0; j < row.size(); j++) {
					log.debug("column/size/data: " + j + " / " + row.get(j).length() + " | " + row.get(j));
				}
			}
		}
		if (ps != null) {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		log.debug("Rows inserted: " + validCount);
	}
	
	public void storeEventsAsFile(byte[] fileData, String fullFileName) throws IOException {
		if (fileData != null && fileData.length > 0) {
			FileOutputStream fos = new FileOutputStream(fullFileName);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			bos.write(fileData);
			bos.flush();
			bos.close();
		} else {
			log.debug("fileData is null or empty.");
		}
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
			throw new SQLException(sqle.getMessage());
		}
	}
	
	/**
	 * Loads the calendar events for the given parameters.
	 */
	public void loadEvents() {
		log.info("loading events...");
		byte[] eventData = null;
		try {
			eventData = this.loadEventsViaHTTP();
		} catch (Exception e) {
			log.error("Error retrieving events via HTTP, ", e);
		}
		if (eventData != null && eventData.length > 0) {
			try {
				this.storeEvents(eventData);
				//this.storeEventsAsFile(eventData);
			}catch(Exception e) {
				log.error("Error storing events, ", e);
			}
		}
	}
	
	/**
	 * Loads the events file
	 * @throws IOException
	 */
	public byte[] loadEventsViaHTTP() throws IOException {
		log.info("loading events via HTTP...");
		// Login to the site
		SMTHttpConnectionManager conn = new SMTHttpConnectionManager();
		this.assignConnectionHeader(conn);
		// Login to the server
		this.cmLogin(conn);
		
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
		byte[] b = conn.retrieveDataViaPost(sUrl, params.toString());

		// parse out the report link (dir and filename)
		String repUrl = this.parseReportLink(new String(b));
		log.debug("report link Url: " + repUrl);
		b = null;
		// retrieve the report data
		//byte[] repData = conn.retrieveData(SECURE_BASE_URL + repUrl);
		byte[] repData = conn.retrieveData(BASE_URL + repUrl);
		return repData;
	}
	
	
	public byte[] loadEventsViaFile(String inFile) throws IOException {
		FileReader fr = null;
		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();
		try {
			fr = new FileReader(inFile);
			br = new BufferedReader(fr);
			String strIn = "";
			while((strIn = br.readLine()) != null) {
				sb.append(strIn).append("\n");
			}
		} catch (FileNotFoundException fe) {
			log.error("Cannot load file: ", fe);
			throw new FileNotFoundException();
		} catch (IOException ioe) {
			log.error("Cannot access file: ", ioe);
			throw new IOException();
		} finally {
			try {
				br.close();
				fr.close();
			} catch (Exception e) {}
		}
		
		byte[] b = sb.toString().getBytes();
		return b;
	}
	
	/**
	 * Adds the header parameters to the url connection
	 * @param conn
	 */
	public void assignConnectionHeader(SMTHttpConnectionManager conn) {
		conn.addRequestHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		conn.addRequestHeader("Accept-Charset","ISO-8859-1,utf-8;q=0.7,*;q=0.3");
		//conn.addRequestHeader("Accept-Encoding","gzip,deflate,sdch");
		conn.addRequestHeader("Accept-Language","en-US,en;q=0.8");
		conn.addRequestHeader("Cache-Control","max-age=0");
		conn.addRequestHeader("Connection","keep-alive");
		conn.addRequestHeader("Content-Type","application/x-www-form-urlencoded");
		conn.addRequestHeader("Host","secure2.chambermaster.com");
		conn.addRequestHeader("Origin","https://secure2.chambermaster.com");
		conn.addRequestHeader("Referer","https://secure2.chambermaster.com/directory/jsp/reports/members/CustomMemberDirectory.jsp");
		conn.addRequestHeader("User-Agent",USER_AGENT);
	}
	
	/**
	 * 
	 * @param conn
	 * @return
	 * @throws IOException
	 */
	protected byte[] cmLogin(SMTHttpConnectionManager conn) throws IOException {
		String url = BASE_URL + "/directory/servlet/ChamberController";
		String params= "command=Login&qualifier=&page=Login.jsp&landingURL=&loginAccountId=0";
		params += "&txtName=dwright&txtPassword=dotndan";
		byte[] data = conn.retrieveDataViaPost(url, params);
		return data;
	}
	
	/**
	 * 
	 * @param conn
	 * @return
	 * @throws IOException
	 */
	protected byte[] cmSecureLogin(SMTHttpConnectionManager conn) throws IOException {
		String url = SECURE_BASE_URL + "/Login/Authenticate";
		String params= "LandingURL=&UserName=dwright&Password=dotndan";
		byte[] data = conn.retrieveDataViaPost(url, params);
		return data;
	}
	
	/**
	 * Logs into website in order to retrieve Hot Deals data.
	 */
	protected void executeSecurelogin(SMTHttpConnectionManager hConn) throws IOException {
		// Set up connection
		this.assignConnectionHeader(hConn);
		hConn.setConnectionTimeout(3000000);
		hConn.setFollowRedirects(false);
		log.debug("follow redirects is: " + hConn.getFollowRedirects());
		
		// get login page redirect
		hConn.retrieveData(DataLoader.SECURE_BASE_URL);
		
		// get login page
		String redir = hConn.getHeaderMap().get("Location");
		hConn.retrieveData(DataLoader.SECURE_BASE_URL + redir);
		
		// attempt login
		String loginUrl = DataLoader.SECURE_BASE_URL + "/Login/Authenticate";
		StringBuilder params= new StringBuilder();
		params.append("LandingURL=%2F&AllowExternalLogins=False&UserName=dwright&Password=dotndan&Submit=Log+In");
		hConn.retrieveDataViaPost(loginUrl, params.toString());
	}
	
	/**
	 * 
	 * @param data
	 * @return
	 */
	public String parseReportLink(String data) {
		int start = data.indexOf("?") + 6;
		int end = data.indexOf("\"", start);
		
		return data.substring(start, end);
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

	/**
	 * 
	 * @return
	 * @throws DatabaseException
	 * @throws InvalidDataException
	 */
	public Connection getConnection() {
		DatabaseConnection dbc = new DatabaseConnection();
		dbc.setDriverClass(config.getProperty("dbDriver"));
		dbc.setUrl(config.getProperty("dbUrl"));
		dbc.setUserName(config.getProperty("dbUser"));
		dbc.setPassword(config.getProperty("dbPassword"));
		try {
			return dbc.getConnection();
		} catch (Exception e) {
			log.error("Unable to get a DB Connection",e);
			System.exit(-1);
		} 
		return null;
	}

	public List<String> getStatusMessages() {
		return statusMessages;
	}

	public void setStatusMessages(List<String> statusMessages) {
		this.statusMessages = statusMessages;
	}
	public void addStatusMessages(List<String> messages) {
		this.statusMessages.addAll(messages);
	}
}
