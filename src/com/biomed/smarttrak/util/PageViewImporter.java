package com.biomed.smarttrak.util;

//JDK 1.8.x
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//Log4j 1.2.17
import org.apache.log4j.xml.DOMConfigurator;

//SMT Base Libs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.util.PageViewVO;

/*****************************************************************************
<p><b>Title</b>: PageViewImport.java</p>
<p><b>Description: </b> Based on com.smt.sitebuilder.util.PageSummary.java.
This class embodies a command line script that retrieves page view records from a 
source db table and inserts them into a destination db table.</p>
<p> 
<p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
<p>Company: Silicon Mountain Technologies</p>
@author groot
@version 1.0
@since Jan 24, 2017
<b>Changes:</b> 
***************************************************************************/
public class PageViewImporter extends CommandLineUtil {
	
	// SQLLite database connection
	private Connection sourceConn = null;

	// Instances to write the summary data
	private Map<String, Connection> destConns = new HashMap<>();

	private Map<String, Object> messages = new LinkedHashMap<>();

	private List<StringBuilder>eMessages = new ArrayList<>();
	private List<Throwable> exceptions = new ArrayList<>();

	/**
	 * @param args
	 */
	public PageViewImporter(String[] args) 
			throws DatabaseException, InvalidDataException, SQLException {
		super(args);
		
		log.debug("Initializing Page View Importer");
		
		// Initialize the logger
		DOMConfigurator.configure("scripts/pageViewImporter_log4j.xml");
		
		try {
			init();
		} catch (Exception e) {
			addException(e);
			notifyAdmin();
			System.exit(-1);
		}
	}
	
	/**
	 * Initializes and checks params for this importer.
	 * @throws Exception
	 */
	private void init() throws Exception {
		// make sure the config params are loaded
		if (args.length == 0) 
			throw new InvalidDataException("Usage: PageViewImporter /path/to/configuration/properties.file");
		
		// Load the properties files
		loadProperties(args[0]);
		if (props == null)
			throw new InvalidDataException("Error: Unable to load properties file.");
		
		// Load the source and destination databases
		try {
			getDBConnections();
		} catch (Exception e) {
			closeDBConnections();
			throw new SQLException("Error: One or more db connections failed.");
		}
	}

	/**
	 * At command line pass the config file path.
	 * @param args
	 */
	public static void main(String[] args) {
		PageViewImporter pvi = null;
		
		// Get the database connections
		try {
			pvi = new PageViewImporter(args);
		} catch (DatabaseException | InvalidDataException | SQLException e) {
			log.error("Error initializing page view importer, ", e);
			// TODO notify admin
			System.exit(-1);
		}
		
		// Import page view data
		pvi.run();
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		
		// Loop the destination data and summarize
		List<PageViewVO> pageViews = null;
		String minVisitDate;
		for(String key : destConns.keySet()) {
			// NOTE: key is WC instance context (e.g. sb)
			try {
				log.info("Importing page views...");
				// 1. init the last record read value or die
				minVisitDate = retrieveMinVisitDate(destConns.get(key));

				// 2. retrieve source records or die
				pageViews = retrieveSourceRecords(sourceConn, key, minVisitDate);

				// 3. process source records
				processSourceRecords(destConns.get(key), pageViews);

			} catch(Exception e) {
				log.error("Unable to import page views for: " + key, e);
			}
		}
		
		eMessages.add(this.buildEmailMessage(true, "All"));
		
		// send the email
		StringBuilder msg = new StringBuilder(destConns.size() * 1024);
		for (StringBuilder s : eMessages) {
			msg.append("<p>").append(s).append("</p>");
		}
		
		try {
			this.sendEmail(msg, null);
		} catch(Exception e) {
			log.error("Unable to send email message", e);
		}
		
		closeDBConnections();
		
	}
	
	
	
	
	
	/**
	 * Queries the destination page view table for the latest recorded visit date
	 * so that we use that date as the starting point from which to retrieve source
	 * records.
	 * @param destDbConn
	 * @return
	 * @throws SQLException
	 */
	private String retrieveMinVisitDate(Connection destDbConn) 
			throws SQLException {
		String retVal = null;
		String sql = "select max(visit_dt) from pageview_user";
		try (PreparedStatement ps = destDbConn.prepareStatement(sql)) {
			ResultSet rs = ps.executeQuery();
			if (rs.next()) retVal = rs.getString(1);
		} catch (SQLException sqle) {
			log.error("Warning: Unable to retrieve latest recorded visit date for next import: ", sqle);
			addException(sqle);
			throw new SQLException();
		}

		return retVal;
	}
	
	/**
	 * Retrieves page view records from the source table and compiles them into a 
	 * List of PageViewVO objects.
	 * @return
	 * @throws SQLException
	 */
	private List<PageViewVO> retrieveSourceRecords(Connection sourceConn, 
			String wcInstance, String minVisitDate) throws SQLException {
		StringBuilder sql = new StringBuilder(120);
		sql.append("select pageview_id, site_id, profile_id, session_id, page_id, request_uri_txt, ");
		sql.append("query_str_txt, visit_dt from pageview ");
		sql.append("where wc_instance_cd = ? and visit_dt > ? and profile_id is not null order by visit_dt");
		log.debug("Source page view SQL: " + sql.toString());
		PageViewVO pvo = null;
		List<PageViewVO> pageViews = new ArrayList<>();
		try (PreparedStatement ps = sourceConn.prepareStatement(sql.toString())) {
			ps.setString(1, wcInstance);
			ps.setString(2, minVisitDate);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				pvo = new PageViewVO();
				pvo.setPageViewId(rs.getInt("pageview_id"));
				pvo.setSiteId(rs.getString("site_id"));
				pvo.setProfileId(rs.getString("profile_id"));
				pvo.setSessionId(rs.getString("session_id"));
				pvo.setPageId(rs.getString("page_id"));
				pvo.setRequestUri(rs.getString("request_uri_txt"));
				pvo.setQueryString(rs.getString("query_str_txt"));
				pvo.setVisitDate(Convert.formatDate(Convert.DATE_TIME_DASH_PATTERN,rs.getString("visit_dt")));
				pageViews.add(pvo);
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving source page view records, ", sqle);
			throw new SQLException("Error: Unable to retrieve source page view records, " + sqle.getMessage());
		}

		log.debug("pageViews retrieved: " + pageViews.size());
		return pageViews;
	}
	
	/**
	 * Processes the page views retrieved from the source table and
	 * inserts them into the destination table.
	 * @param destDbConn
	 * @param pageViews
	 * @throws SQLException
	 */
	private void processSourceRecords(Connection destDbConn, List<PageViewVO> pageViews) 
			throws SQLException {
		if (pageViews.isEmpty()) return;
		StringBuilder sql = new StringBuilder(200);
		sql.append("insert into pageview_user (pageview_user_id, site_id, profile_id, session_id, ");
		sql.append("page_id, request_uri_txt, query_str_txt, src_pageview_id, visit_dt, create_dt) ");
		sql.append("values (?,?,?,?,?,?,?,?,?,?)");
		log.debug("Destination table insert SQL: " + sql.toString());
		
		try (PreparedStatement ps = destDbConn.prepareStatement(sql.toString())) {
			int idx = 1;
			int recCnt = 0;
			for (PageViewVO vo : pageViews) {
				ps.setString(idx++, new UUIDGenerator().getUUID());
				ps.setString(idx++, vo.getSiteId());
				ps.setString(idx++, vo.getProfileId());
				ps.setString(idx++, vo.getSessionId());
				ps.setString(idx++, vo.getPageId());
				ps.setString(idx++, vo.getRequestUri());
				ps.setString(idx++, vo.getQueryString());
				ps.setInt(idx++, vo.getPageViewId());
				ps.setTimestamp(idx++, Convert.formatTimestamp(vo.getVisitDate()));
				ps.setTimestamp(idx++, Convert.getCurrentTimestamp());
				ps.addBatch();
				idx = 1;
				recCnt++;
				if (recCnt % 100 == 0) ps.executeBatch();
			}
			// insert any remaining batched inserts.
			if (recCnt % 100 > 0) ps.executeBatch();
			
		} catch (BatchUpdateException bue) {
			throw new SQLException(bue.getNextException().getMessage());
		}

	}
	
	/**
	 * Sends an email message using the messages map for the data
	 */
	public StringBuilder buildEmailMessage(boolean delete, String ctx) {
		StringBuilder msg = new StringBuilder(1024);
		try {
			
			msg.append("<style> ");
			msg.append("th { background:silver; color:black; } ");
			msg.append("td { border:solid black 1px; padding:5px; background:#F0FFFF;white-space: nowrap;} ");
			msg.append("</style>");
			msg.append("<table style='border:solid black 1px;border-collapse:collapse;'>");
			msg.append("<tr><th colspan='2'>Context: ").append(ctx).append("</th></tr>");
			msg.append("<tr><th>Type</th><th>Message</th></tr>");
			if (! delete) msg.append("<tr><td>Sites Visited</td><td>").append(messages.get("SITE_COUNT")).append("</td></tr>");
			if (! delete) msg.append("<tr><td>Pages Visited</td><td>").append(messages.get("PAGE_COUNT")).append("</td></tr>");
			if (! delete) msg.append("<tr><td>Human Pageviews</td><td>").append(messages.get("HUMAN_PAGEVIEWS")).append("</td></tr>");
			if (! delete) msg.append("<tr><td>Robot Pageviews</td><td>").append(messages.get("ROBOT_PAGEVIEWS")).append("</td></tr>");
			if (delete) msg.append("<tr><td>Rows Deleted</td><td>").append(messages.get("DELETE_COUNT")).append("</td></tr>");
			msg.append("<tr><th colspan='2'>Errors</th></tr>");
			
			// loop the errors
			for(String key : messages.keySet()) {
				if ((key.equals("SITE_COUNT") || key.equals("PAGE_COUNT") || key.equals("DELETE_COUNT") 
						|| key.equals("HUMAN_PAGEVIEWS") || key.equals("ROBOT_PAGEVIEWS")))
					continue;
				
				msg.append("<tr><td>").append(key).append("</td><td>");
				msg.append(messages.get(key)).append("</td></tr>");
			}
			msg.append("</table>");
			log.debug(msg);
		} catch(Exception e) {
			log.error("Unable to send email", e);
		}
		
		return msg;
	}
	
	/**
	 * Gets the source and destination data.  Destination data is in the following format:
	 * destDBConn_0 = ctx|Driver|Url|user|password
	 * destDBConn_1 = ctx|Driver|Url|user|password
	 * @throws DatabaseException
	 * @throws InvalidDataException
	 */
	public void getDBConnections() throws DatabaseException, InvalidDataException, SQLException  {
		// Load the SQLite Source Connection
		sourceConn = DriverManager.getConnection(props.getProperty("sourceDBUrl"));

		DatabaseConnection dbc = new DatabaseConnection();
		for (int i=0; i < 10; i++) {
			//Get the database destination 
			String data = StringUtil.checkVal(props.getProperty("destDBConn_" + i));
			if (data.length() == 0) break;
			
			String[] vals = data.split("\\|");
			dbc = new DatabaseConnection();
			dbc.setDriverClass(vals[1]);
			dbc.setUrl(vals[2]);
			dbc.setUserName(vals[3]);
			dbc.setPassword(vals[4]);
			
			// store the ctx and connection in the Map
			destConns.put(vals[0], dbc.getConnection());
			log.debug("Added DB Dest Connection for: " + vals[0]);
		}

	}
	
	/**
	 * Closes db connections.
	 */
	private void closeDBConnections() {
		if (sourceConn != null) {
			try {
				if (! sourceConn.isClosed()) sourceConn.close();
			} catch (Exception e) {}
		}
		if (! destConns.isEmpty()) {
			for (String key : destConns.keySet()) {
				if (destConns.get(key) != null) {
					try {
						if (! destConns.get(key).isClosed()) destConns.get(key).close();
					} catch (Exception e) {}
				}
			}
		}
	}
	
	/**
	 * Notifies admin if an error ocurred during import.
	 * @param exception
	 */
	private void notifyAdmin() {
		StringBuilder errMsg = new StringBuilder(150);
		for (Throwable t : exceptions) {
			errMsg.append("Error occurred, type | message: ");
			errMsg.append(t.getClass().getSimpleName());
			errMsg.append("<br/>");
			errMsg.append(t.getMessage());
			errMsg.append("<br/>").append("<br/>");
		}
		EmailMessageVO evo = new EmailMessageVO();
		evo.setSubject("Pageview Importer: Error, import failed, check logs!");
		evo.setHtmlBody(errMsg.toString());
		evo.setTextBody(errMsg.toString());
		try {
			evo.setFrom(props.getProperty("fromAddress"));
			evo.addRecipient(props.getProperty("toAddress"));
		} catch (Exception e) {
			log.error("Error formatting message: ", e);
			try {
				// try 'last resort' email addresses.
				evo.setFrom("appsupport@siliconmtn.com");
				evo.addRecipient("dave@siliconmtn.com");
			} catch (Exception ex) {}
		}
		
		if (! evo.getRecipient().isEmpty()) {
			try {
				sendEmail(evo);
			} catch (Exception e) {
				log.error("Error: Email send failed!, " + e);
			}
		}
	}
	
	/**
	 * 
	 * @param thrown
	 */
	private void addException(Throwable thrown) {
		if (exceptions == null) exceptions = new ArrayList<>();
		exceptions.add(thrown);
	}
	
}
