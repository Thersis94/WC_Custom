package com.arvadachamber.action;

// JDK 1.6.x
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

// Log4J 1.2.15
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// SMT Base Libs
import com.siliconmtn.exception.FileException;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.GoogleGeocoderV3;
import com.siliconmtn.io.FileManager;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.CSVParser;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: MemberLoader.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Retrieves member data from the Chamber Master Admin site and
 * stores the data in the WC Data models
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Sep 28, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class MemberLoader extends ChamberMasterLoader {
	private static final Logger log = Logger.getLogger("MemberLoader");
	Map<String,Integer> existingCatsNameMap = null;
	
	/**
	 * constructor
	 */
	public MemberLoader() throws Exception {
		statusMessages = new ArrayList<String>();
		existingCatsNameMap = new HashMap<String,Integer>();
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
		MemberLoader mm = new MemberLoader();
		
		// obtain DB Connection
		mm.getConnection(config);
		log.debug("opened db connection...");
		
		// Load custom schema name
		mm.setCustomDbSchema(config.getProperty("customDbSchema"));
		log.debug("loaded custom schema name");
		long start = Calendar.getInstance().getTimeInMillis();
		
		try {
			mm.importMembers();
		} catch (SQLException sqle) {
			log.error("Error importing member data, ", sqle);
			if (! mm.isErrors()) mm.setErrors(true);
		}
	
		try {
			mm.conn.close();
			log.info("db connection closed.");
		} catch(Exception e) {
			log.error("Error closing db connection, ", e);
		}
		long end = Calendar.getInstance().getTimeInMillis();
		log.debug("elapsed time: " + (end - start)/1000 + " seconds");				
		log.info("exiting loader.");
	}
	
	/**
	 * Imports member data
	 * @throws IOException
	 * @throws SQLException
	 */
	protected void importMembers() throws IOException, SQLException {
		log.debug("importing members...");
		if (httpConn == null) { // true if running from main method
			httpConn = new SMTHttpConnectionManager();
			this.login(httpConn);
		} else {
			// means that httpConn was passed from calling class
			// reassign headers
			assignConnectionHeader(httpConn);
		}
		// Load the member categories
		this.loadExistingCategories();
		this.loadExistingCatsNameMap();
		// retrieve member IDs of members we currently have in db
		Map<String, MemberInfoVO> oldMembers = this.loadAllExistingMembers(); 
		log.debug("oldMemberIds size: " + oldMembers.size());
		// Get the latest members data
		List<MemberInfoVO> newMembers = this.loadBusinessDirectory();	
		//Map<String, MemberInfoVO> currMembers = this.loadBusinessDirectory();
		log.debug("newMembers size: " + newMembers.size());
		this.storeMembers(oldMembers, newMembers);
	}
	
	/**
	 * Creates a map of the existing categories using name as the key instead of ID.
	 */
	private void loadExistingCatsNameMap() {
		for (Integer key : existingCats.keySet()) {
			existingCatsNameMap.put(existingCats.get(key), key);
		}
	}
	
	/**
	 * Retrieves existing 'active' and 'inactive' members from the database for comparison with the latest member list downloaded
	 * from the business directory.
	 * @return
	 * @throws SQLException
	 */
	private Map<String, MemberInfoVO> loadAllExistingMembers() throws SQLException {
		Map<String, MemberInfoVO> currMembers = new HashMap<String, MemberInfoVO>();
		MemberInfoVO mem = null;
		StringBuffer sql = new StringBuffer();
		sql.append("select MEMBER_ID, ADDRESS_TXT, CITY_NM, STATE_CD, ZIP_CD, ");
		sql.append("LATITUDE_NO, LONGITUDE_NO, MEMBER_STATUS_FLG from ");
		sql.append(customDbSchema).append("ARVC_MEMBER");
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				mem = new MemberInfoVO(rs);
				currMembers.put(mem.getDealerLocationId(), mem);
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving existing member IDs, ", sqle);
			statusMessages.add("Error retrieving existing member IDs: " + sqle.getMessage());
			if (! isErrors()) setErrors(true);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch(Exception e) {}
			}
		}
		return currMembers;
	}
		
	/**
	 * Retrieves the latest list of 'active' members from the business directory via HTTP(S).
	 * @throws IOException
	 */
	private List<MemberInfoVO> loadBusinessDirectory() throws IOException {
		// set connection to follow redirects
		httpConn.setFollowRedirects(true);
		// Call the reports screen
		String redir = "https://secure2.chambermaster.com/directory/index.jsp?tabset=8";
		httpConn.retrieveData(redir);
		// call the custom reports screen
		httpConn.addRequestHeader("Referer", redir);
		redir = "https://secure2.chambermaster.com/directory/jsp/reports/members/CustomReportMembers.jsp";
		httpConn.retrieveData(redir);
		
		// Choose the SMT Business Directory Report
		StringBuilder params = new StringBuilder();
		params.append("command=loadRpt&qualifier=&page=%2Fdirectory%2Fjsp%2Freports%2Fmembers%2FCustomReportMembers.jsp");
		params.append("&destination=&savedFields=&savedRpts=6&sortPrimary=0&sortSecondary=0");
		String url = "https://secure2.chambermaster.com/directory/jsp/reports/members/CustomReportMembers.jsp";
		httpConn.retrieveDataViaPost(url, params.toString());
		
		// Call the custom report members
		url = "https://secure2.chambermaster.com/directory/jsp/reports/members/CustomReportMembers.jsp";
		params = new StringBuilder();
		params.append("command=submitRpt&qualifier=&page=%2Fdirectory%2Fjsp%2Freports%2Fmembers%2FCustomReportMembers.jsp");
		params.append("&destination=%2Fdirectory%2Fjsp%2Freports%2Fmembers%2FCustomReportMembersCriteria.jsp");
		params.append("&savedFields=%7C150%7C152%7C154%7C174%7C183%7C158%7C190%7C191%7C192%7C193%7C194%7C205%7C241%7C224%7C206%7C201");
		params.append("&savedRpts=6&sortPrimary=0&sortSecondary=0&Company+Name=150");
		params.append("&Primary+Phone=152&Toll-Free+Phone=154&Website=158&Join+Date=174");
		params.append("&Member+ID=183&Displayed+Address+1=190&Displayed+Address+2=191");
		params.append("&Displayed+City=192&Displayed+State%2FProvince=193&Displayed+Postal+Code=194");
		params.append("&Hours+of+Operation=205&Keywords=206&Categories=224&Primary+Category=241&Member+Description=201");
		httpConn.retrieveDataViaPost(url, params.toString());
		
		// Follow the redirect
		httpConn.retrieveData("https://secure2.chambermaster.com/directory/jsp/reports/members/CustomReportMembersCriteria.jsp");
		
		// Generate the report
		url = "https://secure2.chambermaster.com/directory/jsp/reports/members/CustomReportMembersCriteria.jsp";
		params = new StringBuilder();
		params.append("command=deliver&qualifier=&page=%2Fdirectory%2Fjsp%2Freports%2Fmembers%2FCustomReportMembersCriteria.jsp");
		params.append("&destination=&offset=0&addins=&template=&foundFieldValues=&overwriteReport=1");
		params.append("&groupTitle=&chkStatus=2&chkStatus=4&selGroup=0&selSalesRep=0");
		params.append("&logicalOperator=None&fieldName=-1&comparisonOperator=EqualsExactly");
		params.append("&fieldValue=&logicalOperator=And&fieldName=-1&comparisonOperator=EqualsExactly");
		params.append("&fieldValue=&logicalOperator=And&fieldName=-1&comparisonOperator=EqualsExactly");
		params.append("&fieldValue=&reportStyle=table&reportName=SMT+Business+Directory");
		params.append("&groupTitleTop=&memberRepType=1");
		//conn.addRequestHeader("Content-Length", params.length() + "");
		// retrieve the report response
		byte[] resp = httpConn.retrieveDataViaPost(url, params.toString());

		// parse out the report link (dir and filename)
		String repUrl = this.parseReportLink(new String(resp));
		log.debug("report link Url: " + repUrl);
		// retrieve the report data
		 resp = httpConn.retrieveData(SECURE_BASE_URL + repUrl);
		log.debug("repData size: " + resp.length);
		//this.storeAsFile(resp);
		return this.parseBusinessDirectory(resp);
	}
	
	/**
	 * Loads business directory from a file.
	 * @return
	 * @throws FileException 
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	private List<MemberInfoVO> loadBusinessDirectory(String path) throws IOException, FileException {
		FileManager fm = new FileManager();
		byte[] b = fm.retrieveFile(path);
		return this.parseBusinessDirectory(b);
	}
	
	/**
	 * Helper method for troubleshooting issues.  Writes report data to file.
	 * @param fileData
	 */
	@SuppressWarnings("unused")
	private void storeAsFile(byte[] fileData) {
		File f = new File("C:\\Temp\\avch\\testfile.csv");
		FileOutputStream fOut = null;
		try {
			fOut = new FileOutputStream(f);
			fOut.write(fileData);
			fOut.flush();
			fOut.close();
		} catch (Exception e) {
			log.error("Error writing file to disk..., ", e);
		}
	}
	
	/**
	 * Parses the byte array of member data and stores each row into a collection of collections
	 * @param b
	 * @return
	 * @throws IOException
	 */
	private List<MemberInfoVO> parseBusinessDirectory(byte[] b) throws IOException {
		// Parse the file
		CSVParser parser = new CSVParser();
		List<List<String>> csvData = null;
		try {
			csvData = parser.parseFile(b, true);
		} catch (IOException ioe) {
			log.error("Error parsing member data retrieved via SMTHttpConnectionManager, ", ioe);
			statusMessages.add("Error parsing member data retrieved via SMTHttpConnectionManager, " + ioe.getMessage());
			if (! isErrors()) setErrors(true);
		}
		
		// Convert the map into a Member Bean
		List<MemberInfoVO> data = new ArrayList<MemberInfoVO>();
		Map<String, MemberInfoVO> newMembers = new HashMap<String, MemberInfoVO>();
		for (int i=0; i < csvData.size(); i++) {
			if (csvData.get(i).size() == 16) {
				MemberInfoVO m = this.parseListingInfo(csvData.get(i));
				newMembers.put(m.getDealerLocationId(), m);
				data.add(m);
				//listData(csvData.get(i), i);
			}
		}
		return data;
	}
	
	
	/**
	 * Parses a row (list of Strings) of data into a member bean.
	 * @param data
	 * @return
	 */
	private MemberInfoVO parseListingInfo(List<String> data) {
		MemberInfoVO dlr = new MemberInfoVO();
		dlr.setLocationName(data.get(0));
		dlr.setPhone(data.get(1));
		dlr.setTollFree(data.get(2));
		dlr.setMemberSince(Convert.formatDate(Convert.DATE_DASH_PATTERN, data.get(3)));
		dlr.setDealerLocationId(data.get(4));
		dlr.setWebsite(data.get(5));
		dlr.setAddress(data.get(6));
		dlr.setAddress2(data.get(7));
		dlr.setCity(data.get(8));
		dlr.setState(data.get(9));
		dlr.setZip(data.get(10));
		dlr.setHours(data.get(11));
		
		// Get the primary category
		Integer id = existingCatsNameMap.get(data.get(12));
		if (id != null) {
			Integer mId = Convert.formatInteger(dlr.getDealerLocationId());
			dlr.addCategory(new CategoryVO(id, mId, true));
		}
		
		// Parse the member's keywords
		String keywords = this.parseMemberKeywords(data.get(14));
		dlr.setKeywords(keywords);
		return dlr;
	}
	
	// parses the member keywords String into a new String without CR/LFs
	private String parseMemberKeywords(String rawString) {
		if (rawString == null || rawString.length() == 0) {
			return rawString;
		} else {
			StringBuffer sb = new StringBuffer();
			char c = ' ';
			final char lineFeed = (char)10;
			final char carReturn = (char)13;
			for (int i = 0; i < rawString.length(); i ++) {
				c = rawString.charAt(i);
				switch(c) {
					case lineFeed:
						if (i < (rawString.length() - 2)) sb.append(" ");
						break;
					case carReturn:
						if (i < (rawString.length() - 2)) sb.append(",");
						break;
					default:
						sb.append(c);
						break;
				}
			}
			log.debug("keywords: " + sb.toString());
			return sb.toString();	
		}
	}
	
	
	/**
	 * Compares the current 'active' members list from the db with the latest 'active' members list
	 * downloaded from the business directory.  Existing 'active' member db records are updated.  New
	 * 'active' members are inserted into the db.  If an existing 'active' member is not found in the latest
	 * downloaded 'active' members list, that member's record is updated with a 'member status flag' set to 'inactive'.
	 * @param members
	 */
	private void storeMembers(Map<String, MemberInfoVO> oldMembers, List<MemberInfoVO> newMembers) {
		GoogleGeocoderV3 gc = new GoogleGeocoderV3();
		String newMId = null;
		boolean hasChanged = false;
		List<String> newMemberIds = new ArrayList<String>();
		// loop the latest downloaded 'active' members list
		for (int i = 0; i < newMembers.size(); i++) {
			MemberInfoVO newMbr = newMembers.get(i);
			newMId = newMbr.getDealerLocationId();
			newMemberIds.add(newMId);
			if (oldMembers.keySet().contains(newMId)) { // means the 'old' member is still 'active'
				hasChanged = this.hasMemberAddressChanged(oldMembers.get(newMId), newMbr);
				this.updateMember(newMembers.get(i), gc, hasChanged);
				hasChanged = false;
			} else { // otherwise means the newMId is a new 'active' member
				this.insertMember(newMembers.get(i), gc.geocodeLocation(newMbr).get(0));	
			}
			if (i > 0 && (i % 10 == 0)) {
				try {
					Thread.sleep(1500);
				} catch (Exception e) {System.out.println("thread sleep error, " + e.getMessage());}
			}
		}
		// loop 'old' members keyset, compare to new members list
		List<String> deactivated = new ArrayList<String>();
		for (String oKey : oldMembers.keySet()) {
			if (! newMemberIds.contains(oKey)) {
				if (oldMembers.get(oKey).getActiveFlag() == 1) deactivated.add(oKey);
			}
		}
		// deactivate members that didn't appear in the latest 'active' members list
		this.deactivateMembers(deactivated);
	}
	
	/**
	 * Compares 
	 * @param oldMembers
	 * @param newMembers
	 */
	private void deactivateMembers(List<String> inactiveMembers) {
		log.debug("Deactivating 'inactive' members...");
		if (inactiveMembers.isEmpty()) return;
		StringBuffer sql = new StringBuffer();
		sql.append("update ").append(customDbSchema).append("ARVC_MEMBER ");
		sql.append("set MEMBER_STATUS_FLG = 0 where MEMBER_ID = ? ");
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(sql.toString());
			for (String mId : inactiveMembers) {
				log.info("Deactivating member ID: " + mId);
				ps.setString(1, mId);
				ps.addBatch();
			}
			int[] count = ps.executeBatch();
			log.debug("Members deactivated: " + count.length);
		} catch (SQLException sqle) {
			log.error("Error deactivating members, ", sqle);
		} finally {
			try {
				if (ps != null) ps.close();
			} catch (Exception e) {}
		}
	}
	
	/**
	 * Compares zip, state, city, and address in order to determine if the member's address
	 * has changed.  Returns true or false.  This method exists so that we can minimize geocoding
	 * calls to Google.
	 * @param oM, the member's current bean data from the db
	 * @param nM, the member's latest bean data retrieved from the master business directory
	 * @return
	 */
	private boolean hasMemberAddressChanged(MemberInfoVO oM, MemberInfoVO nM) {
		if (StringUtil.checkVal(oM.getZipCode()).equals(StringUtil.checkVal(nM.getZipCode())) &&
				StringUtil.checkVal(oM.getState()).equals(StringUtil.checkVal(nM.getState())) &&
				StringUtil.checkVal(oM.getCity()).equals(StringUtil.checkVal(nM.getCity())) &&
				StringUtil.checkVal(oM.getAddress()).equals(StringUtil.checkVal(nM.getAddress()))) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * 
	 * @param dlr
	 * @throws SQLException
	 */
	private void updateMember(MemberInfoVO dlr, GoogleGeocoderV3 gc, boolean doGeocode) {
		// Replace the "#" (Which replaced the carriage return, with a comma
		GeocodeLocation gl = null;
		if (doGeocode) {
			gl = gc.geocodeLocation(dlr).get(0);
			if (gl != null) {
				dlr.setLatitude(gl.getLatitude());
				dlr.setLongitude(gl.getLongitude());
			} else {
				dlr.setLatitude(0.0);
				dlr.setLongitude(0.0);
			}
		}

		String hours = StringUtil.checkVal(dlr.getHours());
		hours = hours.replaceAll("#", ", ");
		StringBuilder s = new StringBuilder();
		s.append("update ").append(customDbSchema).append("arvc_member ");
		s.append("set member_nm = ?, address_txt = ?, address2_txt = ?, city_nm = ?, state_cd = ?, ");
		s.append("zip_cd = ?, website_url = ?, hours_txt = ?, primary_phone_txt = ?, toll_free_txt = ?, ");
		if (doGeocode) {
			s.append("latitude_no = ?, longitude_no = ?, ");
		}
		s.append("member_status_flg = ?, keywords_txt = ?, update_dt = ? ");
		s.append("where member_id = ?");
		PreparedStatement ps = null;
		int index = 1;
		try {
			ps = conn.prepareStatement(s.toString());
			ps.setString(index++, dlr.getLocationName());
			ps.setString(index++, dlr.getAddress());
			ps.setString(index++, dlr.getAddress2());
			ps.setString(index++, dlr.getCity());
			ps.setString(index++, dlr.getState());
			ps.setString(index++, dlr.getZip());
			ps.setString(index++, dlr.getWebsite());
			ps.setString(index++, hours);
			ps.setString(index++, dlr.getPhone());
			ps.setString(index++, dlr.getTollFree());
			if (doGeocode) {
				ps.setDouble(index++, dlr.getLatitude());
				ps.setDouble(index++, dlr.getLongitude());
			}
			ps.setInt(index++, 1); // make sure if member is set to 'active'
			ps.setString(index++, dlr.getKeywords());
			ps.setTimestamp(index++, Convert.getCurrentTimestamp());
			ps.setString(index++, dlr.getDealerLocationId());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("Error updating member info for member ID " + dlr.getDealerLocationId() + ", cause: " + sqle.getMessage());
			statusMessages.add("Error updating new member record for member ID" + dlr.getDealerLocationId() + ": " + sqle.getMessage());
			if (! isErrors()) setErrors(true);
			return;
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}	
		}
		
		try {
			this.updateCategories(dlr);
		} catch (SQLException sqle) {
			log.error("Error updating existing member's categories, " + dlr.getDealerLocationId() + ", " + sqle.getMessage());
			statusMessages.add("Error updating existing member's categories, " + dlr.getDealerLocationId() + ", " + sqle.getMessage());
			if (! isErrors()) setErrors(true);
		}	
	}
	
	/**
	 * 
	 * @param dlr
	 * @throws SQLException
	 */
	private void insertMember(MemberInfoVO dlr, GeocodeLocation gl) {
		// Replace the "#" (Which replaced the carriage return, with a comma
		String hours = StringUtil.checkVal(dlr.getHours());
		hours = hours.replaceAll("#", ", ");
		
		StringBuilder s = new StringBuilder();
		s.append("insert into ").append(customDbSchema).append("arvc_member (");
		s.append("member_id,member_nm,address_txt,address2_txt,city_nm,state_cd,");
		s.append("zip_cd,member_dt,website_url,hours_txt,primary_phone_txt,");
		s.append("toll_free_txt,create_dt,latitude_no,longitude_no,member_status_flg,keywords_txt) ");
		s.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(s.toString());
			ps.setString(1, dlr.getDealerLocationId());
			ps.setString(2, dlr.getLocationName());
			ps.setString(3, dlr.getAddress());
			ps.setString(4, dlr.getAddress2());
			ps.setString(5, dlr.getCity());
			ps.setString(6, dlr.getState());
			ps.setString(7, dlr.getZip());
			ps.setDate(8, Convert.formatSQLDate(dlr.getMemberSince()));
			ps.setString(9, dlr.getWebsite());
			ps.setString(10, hours);
			ps.setString(11, dlr.getPhone());
			ps.setString(12, dlr.getTollFree());
			ps.setTimestamp(13, Convert.getCurrentTimestamp());
			ps.setDouble(14, gl.getLatitude());
			ps.setDouble(15, gl.getLongitude());
			ps.setInt(16, 1);
			ps.setString(17, dlr.getKeywords());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("Error inserting member info for member ID " + dlr.getDealerLocationId() + ", cause: " + sqle.getMessage());
			statusMessages.add("Error inserting new member record for member ID" + dlr.getDealerLocationId() + ": " + sqle.getMessage());
			if (! isErrors()) setErrors(true);
			return;
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}	
		}
		
		// Add the cats for the member
		try {
			this.storeCategories(dlr.getCategories());
		} catch (SQLException sqle) {
			log.error("Error adding categories for member ID " + dlr.getDealerLocationId() + ", cause: " + sqle.getMessage());
			statusMessages.add("Error adding categories for member ID " + dlr.getDealerLocationId() + ": " + sqle.getMessage());
			if (! isErrors()) setErrors(true);
		}
	}
	
	/**
	 * 
	 * @param mCats
	 * @throws SQLException
	 */
	private void storeCategories(List<CategoryVO> mCats) throws SQLException {
		StringBuilder s = new StringBuilder();
		s.append("insert into ").append(customDbSchema).append("arvc_xr_member_category (");
		s.append("member_id, category_id, primary_flg,create_dt) values (?,?,?,?) ");
		
		PreparedStatement ps = conn.prepareStatement(s.toString());
		for (int i=0; i < mCats.size(); i++) {
			CategoryVO vo = mCats.get(i);
			ps.setInt(1, vo.getMemberId());
			ps.setInt(2, vo.getCategoryId());
			ps.setInt(3, vo.getPrimaryFlag());
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
			ps.executeUpdate();
		}
	}
	
	/**
	 * Updates an existing member's categories
	 * @param vo
	 * @throws SQLException
	 */
	private void updateCategories(MemberInfoVO vo) throws SQLException {
		this.deleteCategoriesForMember(vo.getDealerLocationId());
		this.storeCategories(vo.getCategories());
	}
	
	/**
	 * Deletes the categories associated with a current member.
	 * @param memberId
	 * @throws SQLException
	 */
	private void deleteCategoriesForMember(String memberId) throws SQLException {
		int memId = Convert.formatInteger(memberId);
		String s = "delete from " + customDbSchema + "arvc_xr_member_category where member_id = ?";
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(s);
			ps.setInt(1, memId);
			ps.execute();
		} catch (SQLException sqle) {
			log.error("Error deleting member's category xr records for member ID: " + memberId + ", " + sqle.getMessage());
			if (! isErrors()) setErrors(true);
			throw new SQLException();
		}
	}
	
}
