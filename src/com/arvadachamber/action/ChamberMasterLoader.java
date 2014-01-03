package com.arvadachamber.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.SMTMail;
import com.smt.sitebuilder.common.constants.Constants;


/****************************************************************************
 * <b>Title</b>: ChamberMasterLoader.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Manages data imports from the Arvada Chamber's 'chambermaster' site.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Sep 28, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class ChamberMasterLoader {
	private static final Logger log = Logger.getLogger("ChamberMasterLoader");
	public static final String BASE_URL = "http://secure2.chambermaster.com";
	public static final String SECURE_BASE_URL = "https://secure2.chambermaster.com";
	public static final String USER_AGENT = "User-Agent:Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/535.19 (KHTML, like Gecko) Chrome/18.0.1025.142 Safari/535.19";
	public static final String LISTING_PATH = "C:\\SMT\\accounts\\Arvada Chamber\\Listing.csv";
	public static final String EVENT_TYPE_ID = "c0a802413f97ab1153fd69d11c7e8d2";
	public static final String EVENT_ACTION_ID = "c0a802413f9763c5797fd740a09d1fe5";
	protected Connection conn = null;
	protected SMTHttpConnectionManager httpConn = null;
	protected String customDbSchema = null;
	protected List<String> statusMessages = null;
	private Properties config = null;
	protected boolean errors = false;
	//protected Map<String, Integer> cats = new HashMap<String, Integer>(); // used certain subclasses
	protected Map<Integer,String> existingCats = new HashMap<Integer,String>(); // used certain subclasses
	
	public ChamberMasterLoader() throws Exception {
		statusMessages = new ArrayList<String>();
	}
	
	public static void main(String[] args) throws Exception {
		PropertyConfigurator.configure("scripts/arvc_importer_log4j.properties");
		// Load the DB Connection
		ChamberMasterLoader cm = null;
		cm = new ChamberMasterLoader();
		cm.config = new Properties();
		cm.config.load(new FileInputStream(new File("scripts/arvc_importer.properties")));
		// Load the config file
		log.info("Config Loaded");
		cm.getConnection(cm.config);
		log.info("opened db connection...");
		cm.login(cm.getHttpConn());
		// Load custom schema name
		cm.setCustomDbSchema(cm.config.getProperty("customDbSchema"));
		long start = Calendar.getInstance().getTimeInMillis();
		
		// load categories
		log.info("loading categories");
		CategoryLoader c = new CategoryLoader();
		c.setConn(cm.getConn());
		c.setHttpConn(cm.getHttpConn());
		c.setCustomDbSchema(cm.getCustomDbSchema());
		c.importCategories();
		cm.showStatusMessages(c.getStatusMessages());
		cm.addStatusMessages(c.getStatusMessages());
		if (c.isErrors()) cm.setErrors(true);
		log.info("finished loading categories, errors = " + c.isErrors());
				
		// load members
		log.info("loading members");
		MemberLoader m = new MemberLoader();
		m.setConn(cm.getConn());
		m.setHttpConn(cm.getHttpConn());
		m.setCustomDbSchema(cm.getCustomDbSchema());
		m.importMembers();
		cm.showStatusMessages(m.getStatusMessages());
		cm.addStatusMessages(m.getStatusMessages());
		if (m.isErrors()) cm.setErrors(true);
		log.info("finished loading members, errors = " + m.isErrors());
		
		// load hot deals
		log.info("loading hot deals");
		HotDealsLoader hd = new HotDealsLoader();
		hd.setConn(cm.getConn());
		hd.setHttpConn(cm.getHttpConn());
		hd.setCustomDbSchema(cm.getCustomDbSchema());
		hd.importHotDeals();
		cm.showStatusMessages(hd.getStatusMessages());
		cm.addStatusMessages(hd.getStatusMessages());
		if (hd.isErrors()) cm.setErrors(true);
		log.info("finished importing hot deals, errors = " + hd.isErrors());
		
		// load events
		log.info("loading events");
		EventLoader el = new EventLoader();
		el.setConn(cm.getConn());
		el.setHttpConn(cm.getHttpConn());
		el.setCustomDbSchema(cm.getCustomDbSchema());
		el.importEvents();
		cm.showStatusMessages(el.getStatusMessages());
		cm.addStatusMessages(el.getStatusMessages());
		if (el.isErrors()) cm.setErrors(true);
		log.info("finished importing events, errors = " + el.isErrors());
		long end = Calendar.getInstance().getTimeInMillis();
		log.info("Import elapsed time: " + ((end - start)/1000) + " seconds.");
		// clean-up
		try {
			cm.conn.close();
			log.info("Closed db connection");
		} catch (SQLException sqle) {
			log.error("Error closing db connection", sqle);
			cm.addStatusMessage("Error closing db connection, " + sqle.getMessage());
		}
		
		// mail admin
		try {
			cm.sendAdminEmail(cm.config, cm.getStatusMessages(), cm.isErrors());
			log.info("Sent admin email, exiting.");
		} catch (MailException me) {
			log.error("Error sending admin email, ", me);
		}

	}
	
	
	/**
	 * Loads the existing category info from the db into a map
	 * @throws SQLException
	 */
	protected void loadExistingCategories() throws SQLException {
		String s = "select category_id, category_nm from " + customDbSchema + "arvc_category ";
		//s += "where category_id < 5000 ";
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(s);
			ResultSet rs = ps.executeQuery();
			while(rs.next()) existingCats.put(rs.getInt(1), rs.getString(2));
		} catch (SQLException sqle) {
			log.error("Error retrieving categories, ", sqle);
			statusMessages.add("Error retrieving categories: " + sqle.getMessage());
			if (! isErrors()) setErrors(true);
			throw new SQLException(sqle.getMessage());
		}
		log.debug("Loaded Cats: " + existingCats.size());
	}
	
	/**
	 * Displays messages
	 * @param messages
	 */
	private void showStatusMessages(List<String> messages) {
		for (String s : messages) {
			log.debug("Msg: " + s);
		}
	}
	
	/**
	 * Obtain db connection
	 * @return
	 * @throws DatabaseException
	 * @throws InvalidDataException
	 */
	protected void getConnection(Properties config) 
			throws InvalidDataException, DatabaseException {
		DatabaseConnection dbc = new DatabaseConnection();
		dbc.setDriverClass(config.getProperty("dbDriver"));
		dbc.setUrl(config.getProperty("dbUrl"));
		dbc.setUserName(config.getProperty("dbUser"));
		dbc.setPassword(config.getProperty("dbPassword"));
		conn = dbc.getConnection();
	}
	
	/**
	 * Sends administrative email.
	 * @param props
	 * @param body
	 * @param hasErrors
	 * @throws MailException
	 */
	private void sendAdminEmail (Properties props, List<String> body, boolean hasErrors) throws MailException {
		SMTMail mail = new SMTMail(props.getProperty(Constants.CFG_SMTP_SERVER));
		mail.setUser(props.getProperty(Constants.CFG_SMTP_USER));
		mail.setPassword(props.getProperty(Constants.CFG_SMTP_PASSWORD));
		mail.setRecpt(new String[]{"dave@siliconmtn.com"});
		String subject = "Arvada Chamber Master Loader Results";
		if (hasErrors) {
			subject += ": ERRORS occurred, please review logs";
		} else {
			subject += ": Success!";
		}
		mail.setSubject(subject);
		mail.setFrom("script.master@siliconmtn.com");
		mail.setHtmlBody(this.formatHtmlBody(body, hasErrors));
		mail.postMail();
		mail = null;
	}
	
	private String formatHtmlBody(List<String> body, boolean hasErrors) {
		StringBuilder s = new StringBuilder();
		for (String m : body) {
			s.append(m).append("<br/>");
		}
		s.append("Errors occurred?: " + hasErrors);
		return s.toString();
	}
	
	/**
	 * Logs into website in order to retrieve Hot Deals data.
	 */
	protected String login(SMTHttpConnectionManager httpConn) throws IOException {
		// Set up connection
		this.assignConnectionHeader(httpConn);
		httpConn.setConnectionTimeout(3000000);
		httpConn.setFollowRedirects(false);
		
		// get login page redirect
		httpConn.retrieveData(ChamberMasterLoader.SECURE_BASE_URL);
		//showConnInfo(conn);
		
		// get login page
		String redir = this.getRedirLocation(httpConn);
		httpConn.retrieveData(ChamberMasterLoader.SECURE_BASE_URL + redir);
		//showConnInfo(conn);
		
		// attempt login
		String loginUrl = ChamberMasterLoader.SECURE_BASE_URL + "/Login/Authenticate";
		StringBuilder params= new StringBuilder();
		params.append("LandingURL=%2F&AllowExternalLogins=False&UserName=dwright&Password=dotndan&Submit=Log+In");
		httpConn.retrieveDataViaPost(loginUrl, params.toString());
		//showConnInfo(conn);
		return redir;
	}
	
	/**
	 * Adds the header parameters to the url connection
	 * @param conn
	 */
	protected void assignConnectionHeader(SMTHttpConnectionManager conn) {
		conn.addRequestHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		conn.addRequestHeader("Accept-Charset","ISO-8859-1,utf-8;q=0.7,*;q=0.3");
		conn.addRequestHeader("Accept-Language","en-US,en;q=0.8");
		conn.addRequestHeader("Connection","keep-alive");
		conn.addRequestHeader("Host","secure2.chambermaster.com");
		conn.addRequestHeader("DNT", "1");
		conn.addRequestHeader("User-Agent",ChamberMasterLoader.USER_AGENT);
	}
	
	/**
	 * Helper method for retrieving redirection value.
	 * @param conn
	 * @return
	 */
	protected String getRedirLocation(SMTHttpConnectionManager conn) {
		log.debug("redir: " + conn.getHeaderMap().get("Location"));
		return conn.getHeaderMap().get("Location");
	}

	/**
	 * Helper method for retrieving report link value.
	 * @param data
	 * @return
	 */
	protected String parseReportLink(String data) {
		int start = data.indexOf("?") + 6;
		int end = data.indexOf("\"", start);
		return data.substring(start, end);
	}

	
	/**
	 * Helper method for debugging HTTP responses
	 * @param conn
	 */
	@SuppressWarnings("unused")
	private void showConnInfo(SMTHttpConnectionManager conn) {
		log.debug("\n");
		log.debug("Response code: " + conn.getResponseCode());
		showHeaderMap(conn);
		log.debug("\n");
		showCookies(conn);
		log.debug("\n");
	}
	
	private void showHeaderMap(SMTHttpConnectionManager conn) {
		log.debug("*** HEADER MAP (START) ****");
		for (String key : conn.getHeaderMap().keySet()) {
			log.debug("key/val: " + key + "/" + conn.getHeaderMap().get(key));
		}
		log.debug("*** HEADER MAP (END) ****");
	}
	
	private void showCookies(SMTHttpConnectionManager conn) {
		log.debug("**** COOKIE (START) ****");
		for (String key : conn.getCookies().keySet()) {
			log.debug("cookie key/val: " + key + " / " + conn.getCookies().get(key));
		}
		log.debug("**** COOKIE (END) ****");
	}
	
	public Connection getConn() {
		return conn;
	}

	public void setConn(Connection conn) {
		this.conn = conn;
	}

	public String getCustomDbSchema() {
		return customDbSchema;
	}

	public SMTHttpConnectionManager getHttpConn() {
		if (httpConn == null) {
			httpConn = new SMTHttpConnectionManager();
		}
		return httpConn;
	}

	public void setHttpConn(SMTHttpConnectionManager httpConn) {
		this.httpConn = httpConn;
	}

	public void setCustomDbSchema(String customDbSchema) {
		this.customDbSchema = customDbSchema;
	}

	public List<String> getStatusMessages() {
		return statusMessages;
	}

	public void setStatusMessages(List<String> statusMessages) {
		this.statusMessages = statusMessages;
	}
	
	public void addStatusMessage(String message) {
		this.statusMessages.add(message);
	}
	
	public void addStatusMessages(List<String> messages) {
		this.statusMessages.addAll(messages);
	}
	
	public boolean isErrors() {
		return errors;
	}

	public void setErrors(boolean errors) {
		this.errors = errors;
	}
	
}
