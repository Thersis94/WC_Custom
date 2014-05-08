package com.arvadachamber.action;

// JDK 1.6.x
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

// Log4J 1.2.15
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// SMT Base Libs
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: HotDealsLoader.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Loads the Hot Deals data from the Chamber Master Admin site and
 * stores the data in the WC Data models
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Sep 21, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class HotDealsLoader extends ChamberMasterLoader {
	private static final Logger log = Logger.getLogger(HotDealsLoader.class);
	public static final String DEFAULT_REFERER = ChamberMasterLoader.SECURE_BASE_URL + "/Dashboard/Index";
	public static final String HOT_DEALS_URL = "http://arvadachamber.chambermaster.com/directory/jsp/hotdeals/Search.jsp?ccid=342";
	public static final String HOT_DEALS_DETAIL_URL = "http://arvadachamber.chambermaster.com/directory/jsp/hotdeals/HotDeal.jsp?ccid=342";
	public static final String HOT_DEAL_EDIT_STUB = "/directory/jsp/hotdeals/EditHotDeal.jsp?hdid=";
	private static final String MEMBER_ID = "name='memid'";
	private static final String DATE_START = "name='itemStart'";
	private static final String DATE_END = "name='itemEnd'";
	
	/**
	 * 
	 */
	public HotDealsLoader() throws Exception {	}
	
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
		HotDealsLoader hdm = new HotDealsLoader();
		
		// obtain DB Connection
		hdm.getConnection(config);
		log.debug("opened db connection...");
		
		// Load custom schema name
		hdm.setCustomDbSchema(config.getProperty("customDbSchema"));
		log.debug("loaded custom schema name");
		long start = Calendar.getInstance().getTimeInMillis();
		// Load the hot deals
		try {
			hdm.importHotDeals();
		} catch (Exception e) {
			log.error("Error retrieving/storing hot deals, ", e);
			if (! hdm.isErrors()) hdm.setErrors(true);
		}
		// clean-up
		try {
			hdm.conn.close();
			log.info("db connection closed.");
		} catch(Exception e) {
			log.error("Error closing db connection, ", e);
			if (! hdm.isErrors()) hdm.setErrors(true);
		}
		long end = Calendar.getInstance().getTimeInMillis();
		log.debug("elapsed time: " + (end - start)/1000 + " seconds");				
		log.info("exiting loader.");
	}
	
	/**
	 * Imports Hot Deals
	 * @throws IOException
	 */
	protected void importHotDeals() throws IOException {
		if (httpConn == null) {
			log.debug("httpConn is null, instantiating...");
			httpConn = new SMTHttpConnectionManager();
			//this.login(httpConn);
		} else {
			log.debug("httpConn is NOT null...");
			// reset the headers and make sure not to follow redirects.
			this.assignConnectionHeader(httpConn);
			httpConn.setFollowRedirects(false);
		}
		// initial load and insert
		List<HotDealVO> deals = this.loadHotDeals(httpConn);
		//int x = 1; if (x == 1) return;
		
		try {
			this.storeHotDeals(deals);
		} catch (SQLException e) {
			log.debug("Error storing hot deals, " + e.getMessage());
			
		}
		
		// We have to login now to retrieve data
		this.login(httpConn);
		this.assignConnectionHeader(httpConn);
		httpConn.setFollowRedirects(false);
		
		
		// retrieve the list of 'approved' hot deals
		byte[] listData = this.loadHotDealsApprovedList(httpConn, DEFAULT_REFERER);
		// parse out the hot deal IDs
		List<String> dealIds = this.parseHotDealIds(listData);
		// retrieve the start/end dates for each hot deal
		List<HotDealVO> dealInfo = this.retrieveHotDealInfo(httpConn, dealIds);
		// update hot deals start/end dates
		this.updateHotDeals(dealInfo);
	}
	
	/**
	 * Loads the hot deals off the website and returns list of HotDealVOs
	 * @throws IOException
	 */
	private List<HotDealVO> loadHotDeals(SMTHttpConnectionManager conn) throws IOException {
		byte[] data = null;
		try {
			conn.addRequestHeader("Referer", DEFAULT_REFERER);
			data = conn.retrieveData(HOT_DEALS_URL);
		} catch (IOException ioe) {
			log.error("Error retrieving hot deals HTML source for initial load, " + ioe.getMessage());
			if (! isErrors()) setErrors(true);
			throw new IOException("Initial Hot Deals HTML retrieval failed.  Check log.");
		}
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		BufferedReader br = new BufferedReader(new InputStreamReader(bais));
		String temp = "";
		List<HotDealVO> deals = new ArrayList<HotDealVO>();
		while ((temp = br.readLine()) != null) {
			// Look for the hot deals line.  Exclude the JS Function at the top of the page
			if (temp.indexOf("openHotDealDetail") > -1 && temp.length() > 100) {
				// Build the indexes for splitting
				int begIndex = temp.indexOf("openHotDealDetail") + 18 ;
				int mIndex = temp.indexOf(",", begIndex);
				int lIndex = temp.indexOf(")", mIndex);
				int fSep = temp.indexOf(">", lIndex) + 1;
				int lSep = temp.indexOf("<", fSep);
				
				// Pull the data values
				int mId = Convert.formatInteger(temp.substring(begIndex, mIndex));
				int hdId = Convert.formatInteger(temp.substring(mIndex + 1, lIndex));
				String desc = temp.substring(fSep, lSep);
			
				// Create the bean and store the data in the db
				HotDealVO hdvo = new HotDealVO(hdId, mId, desc);
				deals.add(hdvo);
			}
		}
		return deals;
	}
	
	/**
	 * 
	 * @param vo
	 * @throws SQLException
	 */
	private void storeHotDeals(List<HotDealVO> deals) throws SQLException {		
		try {
			conn.setAutoCommit(false);
			this.deleteExistingDeals();
			this.storeNewDeals(deals);
		} catch (SQLException sqle) {
			log.error("Error deleting and inserting hot deals, ", sqle);
			if (! isErrors()) setErrors(true);
			conn.rollback();
		} finally {
			try {
				conn.setAutoCommit(true);
			} catch (SQLException sqle) {
				log.error("Error setting autocommit to 'true', " + sqle.getMessage());
			}
		}
	}
	
	/**
	 * Deletes existing Hot Deal records
	 * @throws SQLException
	 */
	private void deleteExistingDeals() throws SQLException {
		log.info("deleting existing hot deals...");
		String sql = "delete from " + customDbSchema + "ARVC_HOT_DEAL";
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql);
			ps.executeUpdate();
		} catch (SQLException sqle) {
			throw new SQLException(sqle.getMessage());
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {}
			}
		}
	}
	
	/**
	 * Inserts new hot deal records into the custom db table.
	 * @param deals
	 * @throws SQLException
	 */
	private void storeNewDeals(List<HotDealVO> deals) throws SQLException {
		log.info("inserting new hot deal records...");
		int successCount = 0;
		StringBuilder s = new StringBuilder();
		s.append("insert into ").append(customDbSchema).append("arvc_hot_deal (");
		s.append("hot_deal_id, member_id, hot_deal_desc, start_dt, end_dt, create_dt) ");
		s.append("values (?,?,?,?,?,?) ");
		
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(s.toString());
			for (HotDealVO vo : deals) {				
				ps.setInt(1, vo.getHotDealId());
				ps.setInt(2, vo.getMemberId());
				ps.setString(3, vo.getDesc());
				ps.setDate(4, Convert.formatSQLDate(vo.getStartDate()));
				ps.setDate(5, Convert.formatSQLDate(vo.getEndDate()));
				ps.setTimestamp(6, Convert.getCurrentTimestamp());
				try {
					successCount += ps.executeUpdate();
				} catch (SQLException sqle) {
					log.error("Error inserting hot deal, member/hotdeal IDs " + vo.getMemberId() + "/" + vo.getHotDealId() + ", " + sqle.getMessage());
					if (! isErrors()) setErrors(true);
				}
			}
		} catch (SQLException sqle) {
			throw new SQLException(sqle.getMessage());
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {}
			}
		}
		if (successCount == 0) {
			throw new SQLException("No new hot deals inserted, possible error condition, rolling back delete/insert, check logs!");
		}
		log.debug("Hot deal records inserted: " + successCount);
	}
	
	/**
	 * Retrieves hot deals report in CSV format from the ChamberMaster site.
	 * @param conn
	 * @return
	 * @throws IOException
	 */
	private byte[] loadHotDealsApprovedList(SMTHttpConnectionManager conn, String redir) throws IOException {
		// redirect to the Hot Deals list page
		log.debug("loading 'hotdeals' approved list...");
		byte[] resp = null;
		conn.addRequestHeader("Referer", redir);
		String listPostUrl = ChamberMasterLoader.SECURE_BASE_URL + "/directory/jsp/hotdeals/ListCC.jsp";
		StringBuilder params = new StringBuilder();
		params.append("command=refresh&qualifier=&page=%2Fdirectory%2Fjsp%2Fhotdeals%2FListCC.jsp");
		params.append("&destination=&hdid=0&sortValue=enddate+DESC&memid=0&catgid=0&status=2&offset=0");
		params.append("&activeDate=&createdDate=");
		params.append("");
		resp = conn.retrieveDataViaPost(listPostUrl, params.toString());
		//showConnInfo(conn);
		return resp;
	}
	
	/**
	 * Parses the hot deal ID values from the byte array data and returns a list of unique
	 * hot deal IDs.
	 * @param data
	 * @return
	 * @throws IOException
	 */
	private List<String> parseHotDealIds(byte[] data) throws IOException {		
		log.debug("parsing hotdeal IDs...");
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		BufferedReader br = new BufferedReader(new InputStreamReader(bais));
		String temp = "";
		int bIndex = 0;
		int eIndex = 0;
		List<String> hdIdList = new ArrayList<String>();
		try {
			while ((temp = br.readLine()) != null) {
				if ((bIndex = temp.indexOf("javascript:doView(")) > -1) {
					String hdid = temp.substring(bIndex + 18);
					eIndex = hdid.indexOf(")");
					hdid = hdid.substring(0, eIndex);
					if (! hdIdList.contains(hdid)) hdIdList.add(hdid);
				}
			}
		} catch (IOException ioe) {
			log.debug("Error reading input stream, " + ioe.getMessage());
			throw new IOException(ioe.getMessage());
		}
		br.close();
		return hdIdList;
	}
	
	/**
	 * Retrieves data for each hot deal, stores the data into a HotDealVO, and returns a list
	 * of HotDealVOs.
	 * @param conn
	 * @param ids
	 * @return
	 * @throws IOException
	 */
	private List<HotDealVO> retrieveHotDealInfo(SMTHttpConnectionManager conn, List<String> ids) 
	throws IOException {
		log.debug("retrieving individual hotdeal info...");
		List<HotDealVO> deals = new ArrayList<HotDealVO>();
		byte[] hdInfo = null;
		for (String id : ids) {
			hdInfo = conn.retrieveData(ChamberMasterLoader.SECURE_BASE_URL + HOT_DEAL_EDIT_STUB + id);
			if (hdInfo != null && hdInfo.length > 0) {
				HotDealVO vo = this.parseHotDealBean(hdInfo, id);
				if (vo != null) deals.add(vo);
			}
		}				
		return deals;
	}
	
	/**
	 * Parses data for an individual hot deal.
	 * @param data
	 * @param id
	 * @return
	 */
	private HotDealVO parseHotDealBean(byte[] data, String id) {
		if (data == null || data.length == 0) return null;
		log.debug("hot deal ID: " + id);
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		BufferedReader br = new BufferedReader(new InputStreamReader(bais));
		String temp = "";
		String target = "";
		int start = -1;
		int end = -1;
		boolean mIdFound = false;
		boolean dStartFound = false;
		boolean dEndFound = false;
		HotDealVO vo = new HotDealVO();
		vo.setHotDealId(Integer.valueOf(id));
		try {
			while ((temp = br.readLine()) != null) {
				
				if (!mIdFound && temp.indexOf(MEMBER_ID) > -1) {
					//log.debug("memberId source: " + temp);
					start = temp.indexOf("value='");
					target = temp.substring(start + "value='".length());
					// get the value between the single quotes.
					end = target.indexOf("'");
					if (end > -1) {
						target = target.substring(0,end);
					}
					vo.setMemberId(Integer.valueOf(target));
					//log.debug("target/member ID: " + target + " / " + vo.getMemberId());
					mIdFound = true;
				}
				
				if (!dStartFound && temp.indexOf(DATE_START) > -1) {
					//log.debug("date start source: " + temp);
					start = temp.indexOf("value=\"");
					target = temp.substring(start + "value=\"".length());
					end = target.indexOf("\"");
					if (end > -1) {
						target = StringUtil.checkVal(target.substring(0,end));
					}
					if (target.length() > 0) vo.setStartDate(Convert.formatDate(target));
					//if (vo.getStartDate() != null) log.debug("target/start date: " + target + " / " + vo.getStartDate().toString());
					dStartFound = true;
				}
				
				if (!dEndFound && temp.indexOf(DATE_END) > -1) {
					//log.debug("date end source: " + temp);
					start = temp.indexOf("value=\"");
					target = temp.substring(start + "value=\"".length());
					end = target.indexOf("\"");
					if (end > -1) {
						target = StringUtil.checkVal(target.substring(0,end));
					}
					if (target.length() > 0) vo.setEndDate(Convert.formatDate(target));
					//if (vo.getEndDate() != null) log.debug("target/end date: " + target + " / " + vo.getEndDate().toString());
					dEndFound = true;
				}				
				
				start = -1;
				end = -1;
				target = "";
				if (mIdFound && dStartFound && dEndFound) break;
			}
		} catch (IOException ioe) {
			log.error("Error parsing hot deal vo ID: " + id + ", " + ioe.getMessage());
			if (! isErrors()) setErrors(true);
		}
		
		return vo;
	}
	
	/**
	 * Parses the provided file data and stores each event into the DB
	 * @param fileData
	 * @throws IOException
	 * @throws SQLException
	 */
	public void updateHotDeals(List<HotDealVO> deals) throws IOException {
		log.debug("Raw number of hot deals: " + deals.size());
		if (deals.size() == 0) {
			log.info("No hot deals found to update.");
			return;
		}
		int validCount = 0;
		// Build the SQL Statement
		StringBuilder s = new StringBuilder();
		s.append("update ").append(customDbSchema).append("ARVC_HOT_DEAL ");
		s.append("set START_DT = ?, END_DT = ? where HOT_DEAL_ID = ?");
		log.debug("Hot Deal update SQL: " + s.toString());
		PreparedStatement ps = null;
		int hdid = 0;
		try {
			ps = conn.prepareStatement(s.toString());			
			for (HotDealVO vo : deals) {
				hdid = vo.getHotDealId();
				ps.setDate(1, Convert.formatSQLDate(vo.getStartDate())); // start_dt
				ps.setDate(2, Convert.formatSQLDate(vo.getEndDate())); // end_dt
				ps.setInt(3, vo.getHotDealId()); //hot_deal_id
				validCount += ps.executeUpdate();
			}
		} catch(SQLException sqle) {
			log.error("Error updating hot deal record for hot deal ID: " + hdid + ", " + sqle.getMessage());
			if (! isErrors()) setErrors(true);
		}
		if (ps != null) {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		log.debug("Rows updated: " + validCount);
	}
	
}
