package com.biomed.smarttrak.util;

//Java 7
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

// Log4j
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// SMTBaseLibs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;

// WebCresendo libs
import com.smt.sitebuilder.util.PageViewVO;

/*****************************************************************************
 <p><b>Title</b>: PageViewImport.java</p>
 <p><b>Description: </b> This class embodies a command line script that retrieves page
 view records from a source db table and inserts them into a destination db table.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author groot
 @version 1.0
 @since Jan 24, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class PageViewImport extends CommandLineUtil {

	private String importTimestamp;
	private String lastTimestampProcessed;
	private Connection destDbConn;
	private Logger log = Logger.getLogger(PageViewImport.class);
	private final String PROP_WC_INSTANCE_CD = "wcInstanceCode";
	private final String PROP_TIMESTAMP_LOG_PATH = "timestampLogPath";
	private String debugPropsPath = "/home/groot/Downloads/smarttrak/pageViewImport.properties";
	private String debugLogPropsPath = "/home/groot/Downloads/smarttrak/log4j.properties";
	
	public PageViewImport(String[] args) {
		super(args);
		if (args.length == 0) 
			PropertyConfigurator.configure(debugLogPropsPath);
	}

	public static void main(String[] args) {
		PageViewImport pvi = new PageViewImport(args);
		pvi.run();
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		List<PageViewVO> pageViews;
		try {
			// 1. load properties or die
			loadProperties();

			// 2. get source db conn or die
			getSourceDbConnection();

			// 3. get destination db conn or die
			getDestinationDbConnection();

			// 4. init the last record read value or die
			//initLastRecordRead();
			retrieveImportTimestamp();

			// 5. retrieve source records or die
			pageViews = retrieveSourceRecords();

			// 6. process source records
			processSourceRecords(pageViews);

			// 7. persist the timestamp
			if (! pageViews.isEmpty()) persistImportTimestamp();

		} catch (Exception e) {
			log.error("Error during import, full stacktrace: " + e);
			notifyAdmin(e);
		} 

		// 7. clean up.
		closeConnections();
	}
	
	/**
	 * Calls superclass to load properties file.  If load fails, an exception
	 * is thrown.
	 * @throws IOException
	 */
	private void loadProperties() throws IOException {
		if (args.length > 0) 	
			super.loadProperties(args[0]);
		else 
			super.loadProperties(debugPropsPath);
		// superclass eats exceptions, let's check for successful load.
		if (props.isEmpty()) 
			throw new IOException("Error: Unable to load properties file.");
		log.info("Loaded properties file.");
	}
	
	/**
	 * Callse superclass method to obtain a db connection to the source db. 
	 * @throws SQLException
	 */
	private void getSourceDbConnection() throws SQLException {
		loadDBConnection(props);
		if (dbConn == null) 
			throw new SQLException("Error: Unable to obtain source db connection.");
		log.info("Obtained source db connection.");
	}
	
	/**
	 * Connects to the database based on the values in the property file.
	 * @throws SQLException
	 */
	protected void getDestinationDbConnection() 	throws SQLException {
		if (props.get("destDbDriver") == null) {
			throw new SQLException("Error: Unable to find destination db property values.");
		}
		
		DatabaseConnection dbc = new DatabaseConnection();
		dbc.setDriverClass((String) props.get("destDbDriver"));
		dbc.setUrl((String) props.get("destDbUrl"));
		dbc.setUserName((String) props.get("destDbUser"));
		dbc.setPassword((String) props.get("destDbPassword"));
		try {
			destDbConn = dbc.getConnection();
		} catch (Exception e) {
			throw new SQLException("Error: " + e.getMessage());
		}
		log.info("Obtained destination db connection.");
	}
	
	/**
	 * Retrieves the timestamp value String that we persisted from the previous import operation.
	 * @return
	 * @throws IOException
	 */
	private void retrieveImportTimestamp() throws IOException {
		try {
			Path tsPath = FileSystems.getDefault().getPath(props.getProperty(PROP_TIMESTAMP_LOG_PATH));
			byte[] tsBytes = Files.readAllBytes(tsPath);
			importTimestamp = new String(tsBytes);
		} catch (IOException ioe) {
			log.warn("Warning: Unable to retrieve previous page view timestamp: " + ioe.getMessage());
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MINUTE, -5);
			importTimestamp = getTimeString(cal);
		}
		log.info("Using import timestamp value of: " + importTimestamp);
	}
	
	/**
	 * Helper method to format time as a specific String
	 * @param cal
	 * @return
	 */
	private String getTimeString(Calendar cal) {
		StringBuilder sb = new StringBuilder(24);
		sb.append(cal.get(Calendar.YEAR));
		sb.append("-");
		int val = cal.get(Calendar.MONTH + 1);
		padTime(sb,val,"-");
		val = cal.get(Calendar.DAY_OF_MONTH);
		padTime(sb,val," ");
		val = cal.get(Calendar.HOUR_OF_DAY);
		padTime(sb,val,":");
		val = cal.get(Calendar.MINUTE);
		padTime(sb,val,":");
		val = cal.get(Calendar.SECOND);
		padTime(sb,val,".");
		val = cal.get(Calendar.MILLISECOND);
		if (val < 100) sb.append("0");
		padTime(sb,val,null);
		return sb.toString();
	}
	
	/**
	 * Pads time values with a 0 and delimiter.
	 * @param sb
	 * @param val
	 * @param delim
	 */
	private void padTime(StringBuilder sb, int val, String delim) {
		if (val < 10) sb.append("0");
		sb.append(val);
		if (delim == null) return;
		sb.append(delim);
	}
	
	/**
	 * Writes a new timestamp to 
	 * @throws IOException
	 */
	private void persistImportTimestamp() throws IOException {
		try {
			Path tsPath = FileSystems.getDefault().getPath(props.getProperty(PROP_TIMESTAMP_LOG_PATH));
			byte[] tsBytes = lastTimestampProcessed.toString().getBytes();
			if (Files.isWritable(tsPath)) {
				Files.deleteIfExists(tsPath);
				Files.write(tsPath, tsBytes);
				log.info("Persisted last timestamp found for next import start: " + lastTimestampProcessed);
			} else {
				throw new IOException("Error persisting timestamp, path is not writable: " + tsPath.toString());
			}
			
		} catch (IOException ioe) {
			log.error("Error persisting timestamp: ", ioe);
			throw new IOException("Error persisting timestamp, check logs!");
		}
	}

	/**
	 * Retrieves page view records from the source table and compiles them into a 
	 * List of PageViewVO objects.
	 * @return
	 * @throws SQLException
	 */
	private List<PageViewVO> retrieveSourceRecords() throws SQLException {
		StringBuilder sql = new StringBuilder(120);
		sql.append("select pageview_id, site_id, profile_id, session_id, page_id, request_uri_txt, ");
		sql.append("query_str_txt, visit_dt from pageview ");
		sql.append("where wc_instance_cd = ? and visit_dt > ? and profile_id is not null order by visit_dt");
		log.debug("Source page view SQL: " + sql.toString());
		PageViewVO pvo = null;
		List<PageViewVO> pageViews = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, props.getProperty(PROP_WC_INSTANCE_CD));
			ps.setString(2, importTimestamp.toString());
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
				lastTimestampProcessed = rs.getString("visit_dt");
				pvo.setVisitDate(Convert.formatDate(Convert.DATE_TIME_DASH_PATTERN,lastTimestampProcessed));
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
	 * @param pageViews
	 */
	private void processSourceRecords(List<PageViewVO> pageViews) 
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
	 * Closes source and destination db connections.
	 */
	private void closeConnections() {
		if (destDbConn != null) {
			try {
				destDbConn.close();
			} catch(Exception e) {}
		}
		closeDBConnection();
	}
	
	/**
	 * Notifies admin if an error ocurred during import.
	 * @param exception
	 */
	private void notifyAdmin(Throwable exception) {
		StringBuilder errMsg = new StringBuilder(150);
		errMsg.append("Error occurred, type | message: ");
		errMsg.append(exception.getClass().getSimpleName());
		errMsg.append("|");
		errMsg.append(exception.getMessage());
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
	
}
