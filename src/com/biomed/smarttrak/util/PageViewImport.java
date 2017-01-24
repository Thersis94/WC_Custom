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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

// SMTBaseLibs
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;

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

	private int lastRecordRead;
	private Timestamp importTimestamp;
	private Connection destDbConn;
	
	public PageViewImport(String[] args) {
		super(args);
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
			writeNewImportTimestamp();
			
		} catch (Exception e) {
			log.error(e.getMessage());
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
		super.loadProperties(args[0]);
		if (props.isEmpty()) 
			throw new IOException("Error: Properties file not loaded.");
	}
	
	/**
	 * Callse superclass method to obtain a db connection to the source db. 
	 * @throws SQLException
	 */
	private void getSourceDbConnection() throws SQLException {
		loadDBConnection(props);
		if (dbConn == null) 
			throw new SQLException("Error: unable to obtain source db connection.");
	}
	
	/**
	 * Connects to the database based on the values in the property file.
	 * @throws SQLException
	 */
	protected void getDestinationDbConnection() 	throws SQLException {
		if (props.get("destDbDriver") == null) {
			throw new SQLException("Error: Destination db property values not found.");
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
	}

	/**
	 * Retrieves the primary key value of the last record inserted into the 
	 * destination db in order to use that record (plus one) as the starting
	 * point for retrieving the next set of source records.
	 * @throws SQLException
	 */
	@SuppressWarnings("unused")
	private void initLastRecordRead() throws SQLException {
		if (lastRecordRead > 0) return;
		/* Read the max pkid from the destination db table to determine
		 * the last record that we read from the source db table. */
		StringBuilder sql = new StringBuilder(45);
		sql.append("select max(page_view_id) from page_view_user");
		try (PreparedStatement ps = destDbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				lastRecordRead = rs.getInt("page_view_id");
			} else {
				throw new SQLException("Error: Unable to retrieve ID of last page view record inserted.");
			}
		} catch (SQLException sqle) {
			log.error("Error obtaining ID of last destination table page view record.", sqle);
			throw new SQLException(sqle.getMessage());
		}
	}
	
	/**
	 * Retrieves the timestamp value String that we persisted from the previous import operation.
	 * @return
	 * @throws IOException
	 */
	private void retrieveImportTimestamp() throws IOException {
		try {
			Path tsPath = FileSystems.getDefault().getPath(props.getProperty("timestampLogPath"));
			byte[] tsBytes = Files.readAllBytes(tsPath);
			String tsRaw = new String(tsBytes);
			importTimestamp = Convert.formatTimestamp(Convert.DATE_TIME_DASH_PATTERN, tsRaw);
		} catch (IOException ioe) {
			log.warn("Warning: Unable to retrieve previous page view timestamp, using 'now' minus 5 minutes." + ioe.getMessage());
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MINUTE, -5);
			importTimestamp = Convert.formatTimestamp(Convert.formatSQLDate(cal.getTime()));
		}
		log.info("Using timestamp value of: " + importTimestamp);
	}
	
	/**
	 * Writes a new timestamp to 
	 * @throws IOException
	 */
	private void writeNewImportTimestamp() throws IOException {
		try {
			Path tsPath = FileSystems.getDefault().getPath(props.getProperty("timestampLogPath"));
			byte[] tsBytes = importTimestamp.toString().getBytes();
			if (Files.isWritable(tsPath)) {
				Files.deleteIfExists(tsPath);
				Files.write(tsPath, tsBytes);
			} else {
				throw new IOException("Error: Timestamp log path is not writable!, Path is: " + tsPath.toString());
			}
			
		} catch (IOException ioe) {
			log.error("Error writing next import timestamp, ", ioe);
			throw new IOException("Error writing next import timestamp, check logs!");
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
		sql.append("where wc_instance_cd = ? and visit_dt > ? order by visit_dt");
		log.debug("Source page view SQL: " + sql.toString());
		PageViewVO pvo = null;
		List<PageViewVO> pageViews = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, props.getProperty("WC_Instance_ID"));
			ps.setTimestamp(2, Convert.formatTimestamp(new Date()));
			ps.setInt(1, lastRecordRead);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				pvo = new PageViewVO();
				pvo.setPageViewId(rs.getInt("page_view_id"));
				pvo.setSiteId(rs.getString("site_id"));
				pvo.setProfileId(rs.getString("profile_id"));
				pvo.setQueryString(rs.getString("querystring_uri"));
				pvo.setVisitDate(rs.getDate("visit_dt"));
				pageViews.add(pvo);
			}
		} catch (SQLException sqle) {
			throw new SQLException("Error: Unable to retrieve source page view records: " + sqle.getMessage());
		}
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
		sql.append("insert into page_view_user (page_view_id, site_id, ");
		sql.append("profile_id, session_id, page_id, query_string_uri, visit_dt) values (?,?,?,?,?");
		log.debug("Destination table insert SQL: " + sql.toString());
		
		try (PreparedStatement ps = destDbConn.prepareStatement(sql.toString())) {
			int idx = 1;
			int recCnt = 0;
			for (PageViewVO vo : pageViews) {
				ps.setInt(idx++, vo.getPageViewId());
				ps.setString(idx++, vo.getSiteId());
				ps.setString(idx++, vo.getProfileId());
				ps.setString(idx++, vo.getSessionId());
				ps.setString(idx++, vo.getPageId());
				ps.setString(idx++, vo.getQueryString());
				ps.setDate(idx++, Convert.formatSQLDate(vo.getVisitDate()));
				ps.addBatch();
				idx = 1;
				recCnt++;
				if (recCnt % 100 == 0) ps.executeBatch();
			}
			// insert any remaining batched inserts.
			if (recCnt % 100 > 0) ps.executeBatch();
			
			// capture the timestamp from the last vo in the list.
			importTimestamp = Convert.formatTimestamp(pageViews.get(pageViews.size() -1).getVisitDate());
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
	 * Notifies admin of error
	 * @param exception
	 */
	private void notifyAdmin(Throwable exception) {
		StringBuilder errMsg = new StringBuilder(150);
		errMsg.append("Error occurred, type | message: ");
		errMsg.append(exception.getClass().getSimpleName());
		errMsg.append("|");
		errMsg.append(exception.getMessage());
		EmailMessageVO evo = new EmailMessageVO();
		evo.setSubject("Error: PageView import failed!");
		try {
			evo.setFrom(props.getProperty("adminFrom"));
			evo.addRecipient(props.getProperty("adminTo"));
		} catch (Exception e) {
			log.error("Error formatting EmailMessageVO, ", e);
			try {
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
