package com.depuysynthes.scripts;

// JDK 1.6.x
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.*;
import java.net.URL;

// SMT Base Libs
import com.depuysynthes.action.MediaBinAdminAction;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.io.mail.MailHandlerFactory;
import com.siliconmtn.io.mail.mta.MailTransportAgentIntfc;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// Web Crescendo Libs
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: DSMediaBinImporter.java<p/>
 * <b>Description: Imports data from a flat file, parses the data, and inserts the data into database
 * tables</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since May 23, 2013
 * @updates
 * James McKain - 03-03-2014 - revised to support eCopy, and be backwards compatible for both file formats.
 * 
 ****************************************************************************/
public class DSMediaBinImporter extends CommandLineUtil {

	/**
	 * Stores the URL for the US or International import file
	 */
	protected String importFile = "";
	
	/**
	 * Delimiter used in the EXP file
	 */
	protected String DELIMITER = "\\|";
	
	/**
	 * debug mode runs individual insert queries instead of a batch query, to be able to track row failures.
	 */
	private boolean DEBUG_MODE = false; 
	
	/**
	 * the rows counted from the exp file, so we can report to the admin how much data was 'sent' to us.
	 */
	private int totalRows = 0;
	
	/**
	 * List of errors 
	 */
	List <Exception> failures = new ArrayList<Exception>();
	
	// Member Variables
    private int total = 0;
    
    
    /**
     * Initializes the Logger, config files and the database connection
     * @throws Exception
     */
    public DSMediaBinImporter(String[] args) {
	    super(args);
		loadProperties("scripts/MediaBin.properties");
		loadDBConnection(props);
		
    }
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {        
		//Create an instance of the MedianBinImporter
		DSMediaBinImporter dmb = new DSMediaBinImporter(args);
		dmb.run();
	}
	
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		importFile = props.getProperty("importFile");

		// Get the type (Intl (2) or US(1))
		int type = 1;
		if (args.length > 0 && Convert.formatInteger(args[0]) == 2) type = 2;
		importFile = props.getProperty("importFile" + type);
		
		log.info("Starting Importer for " + importFile);

		if (args.length > 1)
			DEBUG_MODE = Convert.formatBoolean(args[1]);

		try {
			// Attempt to retrieve and order the data
			List<Map<String, String>> data = null;
			data = parseFile(importFile);

			// Make sure some data was retrieved and then delete the current
			// records
			log.debug("** Number of entries: " + data.size());
			if (data.size() > 10) {
				deleteCurrentRecords(type);
			} else {
				throw new SQLException("Not enough records");
			}

			// Attempt to insert the data we retrieved
			insertData(data, type);

		} catch (Exception e) {
			log.error("Error parsing file... " + e.getMessage(), e);
			failures.add(new Exception("Error parsing file: " + e.getMessage(), e));

		} finally {
			try {
				dbConn.close();
			} catch (Exception e) { }
			
			sendEmail();
		}
	}
	
	
	/**
	 * Delete the current rows for the provided type
	 * @param type
	 * @throws SQLException
	 */
	protected void deleteCurrentRecords(int type) throws SQLException {
		StringBuilder sql = new StringBuilder();
		sql.append("delete from ").append(props.getProperty(Constants.CUSTOM_DB_SCHEMA));
		sql.append("dpy_syn_mediabin where import_file_cd = ?");

		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ps.setInt(1, type);
		ps.executeUpdate();
		ps.close();
		log.debug("purged old records.  SQL was: " + sql);
	}
	
	
	/**
	 * parses the import file.  Import text file format - first row contains column headers:
	 * 1> EMAIL_ADDRESS_TXT|PASSWORD_TXT|FIRST_NM|LAST_NM
	 * 2> jmckain@siliconmtn.com|mckain|James|McKain
	 * 
	 * @param String importFile file path
	 * @throws Exception
	 */
	public List<Map<String, String>> parseFile(String url) throws IOException {
		log.info("starting file parser");

		// Set the importFile so we can access it for the success email
		// append a randomized value to the URL to bypass CDN proxies
		importFile = url + "?t=" + System.currentTimeMillis();

		URL page = new URL(importFile);
		BufferedReader buffer = new BufferedReader(new InputStreamReader(
				page.openStream(), "UTF-16"));

		// first row contains column names; must match UserDataVO mappings
		String line = StringUtil.checkVal(buffer.readLine());
		String tokens[] = new String[0];
		if (line != null) tokens = line.split(DELIMITER, -1);
		String[] columns = new String[tokens.length];
		for (int i = 0; i < tokens.length; i++) {
			columns[i] = tokens[i];
		}

		String rowStr = null;
		Map<String, String> entry = null;
		List<Map<String, String>> data = new ArrayList<Map<String, String>>();
		// Map<String,Integer> colSizes = new HashMap<String,Integer>();

		// execution in this loop WILL throw NoSuchElementException.
		// This is not trapped so you can cleanup data issue prior to import
		for (int y = 0; (rowStr = buffer.readLine()) != null; y++) {
			tokens = rowStr.split(DELIMITER, -1);
			++totalRows;

			// test quality of data
			if (tokens.length != columns.length) {
				log.error("Not loading row# " + y + " " + rowStr);
				continue;
			}

			entry = new HashMap<String, String>(20);
			for (int x = 0; x < tokens.length; x++) {
				String value = StringUtil.checkVal(tokens[x].trim());

				// remove surrounding quotes if they exist
				if (value.startsWith("\"") && value.endsWith("\""))
					value = value.substring(1, value.length() - 1);

				if (value.equals("null")) value = null;
				
				entry.put(columns[x], value);
			}
			data.add(entry);
			entry = null;
		}
		// log.error(colSizes);

		log.debug("file size is " + data.size() + " rows");
		return data;
	}
	
	/**
	 * Inserts the data in the supplied list of maps into the database
	 * @param data
	 * @return
	 */
	public void insertData(List<Map<String,String>> data, int type) {
		List<String> acceptedAssets = new ArrayList<String>();
		acceptedAssets.addAll(java.util.Arrays.asList(MediaBinAdminAction.VIDEO_ASSETS));
		acceptedAssets.addAll(java.util.Arrays.asList(MediaBinAdminAction.PDF_ASSETS));
		
		
		// Build the SQL Statement
		StringBuilder sql = new StringBuilder();
		sql.append("insert into ").append(props.getProperty(Constants.CUSTOM_DB_SCHEMA)).append("dpy_syn_mediabin ");
		sql.append("(dpy_syn_mediabin_id, asset_nm, asset_desc, asset_type, body_region_txt, ");
		sql.append("business_unit_nm, business_unit_id, literature_type_txt, ");
		sql.append("modified_dt, file_nm, dimensions_txt, orig_file_size_no, prod_family, ");
		sql.append("prod_nm, revision_lvl_txt, opco_nm, title_txt, tracking_no_txt, import_file_cd) ");
		sql.append("values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)" );

		int recordCnt = 0;
		PreparedStatement ps  = null;
		if (!DEBUG_MODE) {  //open the PreparedStatement
			try { 
				ps = dbConn.prepareStatement(sql.toString()); 
			} catch (Exception e) { }
		}
		
		String tn = "", pkId = "";
		String requiredOpCo = (type == 2) ? "INTDS.com" : "USDS.com";
		
		// Loop the list and parse out each map item for inserting into the db
		for (Map<String, String> row : data) {
			try {
				// Make sure the files are for Synthes.com, and in the Types we're authorized to use.
				if (StringUtil.checkVal(row.get("Distribution Channel")).length() == 0 ||
						!acceptedAssets.contains(row.get("Asset Type").toLowerCase()) ||
						!row.get("Distribution Channel").contains(requiredOpCo)) {
					
					if (DEBUG_MODE) { //if we're in debug mode, report why we're skipping this record.
						String reason = " || ";
						if (row.get("Distribution Channel") == null || row.get("Distribution Channel").length() == 0) {
							reason += "No dist channel";
						} else if (!acceptedAssets.contains(row.get("Asset Type").toLowerCase())) {
							reason += "wrong asset type: " + row.get("Asset Type");
						} else {
							reason += "unauthorized opCo: " + row.get("Distribution Channel");
						}
						log.info("skipping asset " + row.get("Asset Name") + reason);
					}
					continue;
				}
				
				// load the tracking number, support eCopy and MediaBin file layouts
				tn = StringUtil.checkVal(row.get("Tracking Number"));
				if (tn.length() > 0) {
					pkId = tn;
					if (type == 1) pkId = StringUtil.checkVal(row.get("Business Unit ID")) + pkId; //US assets get business unit as part of pKey.
					
				} else {
					//no legacy#, use eCopy
					tn = StringUtil.checkVal(row.get("eCopy Tracking Number"));
					pkId = tn;
				}
				
				//for INTL, use the file name as a tracking number (final fallback).
				//NOTE: once eCopy launch this becomes unreachable code.  All assets will have one of the two above.
				if (type == 2 && tn.length() == 0) {
					tn  = loadLegacyTrackingNumberFromFileName(row);
					pkId = tn;
				}
				
				//still no tracking number, this asset is invalid!
				if (tn.length() == 0)
					throw new SQLException("Tracking number missing for " + row.get("Asset Name"));
				
				
				//determine Modification Date for the record. -- displays in site-search results
				Date modDt = Convert.formatDate(Convert.DATE_TIME_SLASH_PATTERN_FULL_12HR, row.get("Check In Time"));
				if (modDt == null) modDt = Convert.formatDate(Convert.DATE_TIME_SLASH_PATTERN_FULL_12HR, row.get("Insertion Time"));
				
				// Insert the record
				if (DEBUG_MODE) ps = dbConn.prepareStatement(sql.toString());
				ps.setString(1, pkId);
				ps.setString(2, row.get("Asset Name").replace('\\','/'));
				ps.setString(3, StringUtil.checkVal(row.get("Asset Description"), row.get("SOUS - Literature Category")));
				ps.setString(4, row.get("Asset Type").toLowerCase());
				ps.setString(5, StringUtil.checkVal(row.get("Body Region"), StringUtil.checkVal(row.get("SOUS - Body Region Spine"), row.get("SOUS - Body Region Trauma"))));
				ps.setString(6, StringUtil.checkVal(row.get("BUSINESS UNIT"),row.get("SOUS - Business Unit")));
				ps.setString(7, row.get("Business Unit ID"));
				ps.setString(8, StringUtil.checkVal(row.get("Literature Type"), row.get("SOUS - Literature Category")));
				ps.setTimestamp(9, Convert.getTimestamp(modDt, true));
				ps.setString(10, row.get("Name"));
				ps.setString(11, row.get("Dimensions (pixels)"));
				ps.setInt(12, Convert.formatInteger(row.get("Original File Size")));
				ps.setString(13, getProductFamily(row));
				ps.setString(14, StringUtil.checkVal(row.get("Product Name"), row.get("SOUS - Product Name")));
				ps.setString(15, StringUtil.checkVal(row.get("Revision Level"), row.get("Current Revision")));
				ps.setString(16, row.get("Distribution Channel"));
				ps.setString(17, row.get("Title"));
				ps.setString(18, tn);
				ps.setInt(19, type);
				
				if (DEBUG_MODE) {
					ps.executeUpdate();
				} else {
					ps.addBatch();
					recordCnt++;
					
					// Execute the batch as it fills up
					if(recordCnt == Constants.MAX_SQL_BATCH_SIZE) {
						log.error("Max Batch size reached, " + recordCnt + " items Logged");
						ps.executeBatch();
						recordCnt = 0;
						ps.clearBatch();
					}
				}
				++total;
				
			} catch (SQLException e) {
				--total;
				log.error("*************** Could not complete insert: " + tn, e);
				String msg = "Error Inserting data: " + e.getMessage();
				for (String s : row.keySet()) {
					if (row.get(s).length() > 0) {
						log.error(s + "=" + row.get(s));
						msg += "<br/>" + s + "=" + row.get(s);
					}
				}
				failures.add(new Exception(msg, e));
				log.error("*************** end failed row ******************");
			}
		}
		
		//push any uncommitted transactions
		if (!DEBUG_MODE) {
			try { ps.executeBatch(); } catch (Exception e) {
				log.error("*************** Could not complete insert", e);
				failures.add(e);
				log.error("*************** end failed row ******************");
			}
		}
		
		try { 
			ps.close();
		} catch (Exception e) {}
		
		log.debug(total + " total items logged.");
	}
	
	
	
	/**
	 * parses the tracking number from the old MediaBin file format
	 * @param data
	 * @return
	 */
	//TODO this should be removed once Angie has tracking numbers populated for all legacy INT assets.
	//They're the only ones falling-back to Name and max 18 chars.
	private String loadLegacyTrackingNumberFromFileName(Map<String, String> data) {
		String tn = StringUtil.checkVal(data.get("Name"));
		if (tn.lastIndexOf(".") > -1) 
			tn = tn.substring(0, tn.lastIndexOf("."));
		
		if (tn.length() > 18) tn = tn.substring(0, 18); //INT assets only use the first 18chars
		return tn;
	}
	
	/**
	 * Looks at multiple columns and returns when one of the columns has data
	 * @param row
	 * @return
	 */
	private String getProductFamily(Map<String, String> row) {
		String pf = StringUtil.checkVal(row.get("Product Family"));
		if (pf.length() > 0) return pf;
		
		pf = StringUtil.checkVal(row.get("SOUS - Product Family CMF"));
		if (pf.length() > 0) return pf;
		
		pf = StringUtil.checkVal(row.get("SOUS - Product Family Spine"));
		if (pf.length() > 0) return pf;
		
		pf = StringUtil.checkVal(row.get("SOUS - Product Family Trauma"));
		if (pf.length() > 0) return pf;
		
		return pf;
	}
	
	
	
	/**
	 * Sends an email to the person specified in the properties file as to whether 
	 * the insert was a success or a failure.
	 */
	private void sendEmail() {
		try {
			// Build the email message
			EmailMessageVO msg = new EmailMessageVO(); 
			msg.addRecipient(props.getProperty("adminEmail"));
			msg.setSubject("MediaBin Import");
			msg.setFrom("appsupport@siliconmtn.com");
			
			StringBuilder html= new StringBuilder();
			html.append("<h3>Import File Name: " + importFile + "</h3>");
			html.append("<h4>EXP file: " + totalRows + " rows<br/>");
			html.append("Imported: " + total + " rows</h4>");
			
			if (failures.size() > 0) {
				html.append("<b>MediaBin failed to imported the following records:</b><br/><br/>");
			
				// loop the errors and display them
				for (int i=0; i < failures.size(); i++) {
					html.append(failures.get(i).getMessage()).append("<hr/>\r\n");
				}
			}
			msg.setHtmlBody(html.toString());
			
			MailTransportAgentIntfc mail = MailHandlerFactory.getDefaultMTA(props);
			mail.sendMessage(msg);
		} catch (Exception e) {
			log.error("Could not send completion email, ", e);
		}
	}
}
