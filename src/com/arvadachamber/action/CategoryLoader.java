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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

// Log4J 1.2.15
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

// SMT Base Libs
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.CSVParser;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: CategoryLoader.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Retrieves category data from the Chamber Master Admin site and
 * stores the data in the WC Data models
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author DBargerhuff
 * @version 1.0
 * @since Oct 31, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class CategoryLoader extends ChamberMasterLoader {
	private static final Logger log = Logger.getLogger("CategoryLoader");
	Map<Integer,String> parentCats = new HashMap<Integer,String>();
	Map<Integer,CategoryVO> subCats  = new HashMap<Integer,CategoryVO>();
	private Map<String, Integer> parentCatsByName = new HashMap<String,Integer>();
	
	/**
	 * constructor
	 */
	public CategoryLoader() throws Exception {
		statusMessages = new ArrayList<String>();
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
		CategoryLoader cl = new CategoryLoader();
		
		// obtain DB Connection
		cl.getConnection(config);
		log.debug("opened db connection...");
		
		// Load custom schema name
		cl.setCustomDbSchema(config.getProperty("customDbSchema"));
		log.debug("loaded custom schema name");
		long start = Calendar.getInstance().getTimeInMillis();
		
		try {
			cl.importCategories();
		} catch (SQLException sqle) {
			log.error("Error importing member data, ", sqle);
			if (! cl.isErrors()) cl.setErrors(true);
		}
	
		try {
			cl.conn.close();
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
	protected void importCategories() throws IOException, SQLException {
		log.debug("importing members...");
		if (httpConn == null) { // true if running from main method
			httpConn = new SMTHttpConnectionManager();
			this.login(httpConn);
		} else {
			// means that httpConn was passed from calling class, so reassign headers
			assignConnectionHeader(httpConn);
		}
		// Load the current category data we have in the db
		this.loadExistingCategories();
		
		// retrieve parent categories
		parentCats = this.retrieveParentCategories(httpConn);
		// retrieve sub categories (and potentially some additional parent categories)
		subCats = this.retrieveSubCategories(httpConn);
		// update existing cats.
		if (parentCats.size() > 0 && subCats.size() > 0) {
			//this.deleteCategoryXR();
			//this.deleteCategories();
			this.updateParentCategories();
			this.updateSubCategories();
		}
	}
	
		
	/**
	 * Retrieves the parent categories (QuickLinks categories) from the master list.
	 * @param conn
	 * @return
	 * @throws IOException
	 */
	private Map<Integer,String> retrieveParentCategories(SMTHttpConnectionManager conn) throws IOException {
		byte[] resp = null;
		// go to the QuickLinks Categories page
		StringBuilder reportFormUrl = new StringBuilder();
		reportFormUrl.append(SECURE_BASE_URL);
		reportFormUrl.append("/directory/jsp/categories/QuickLinks.jsp");
		resp = conn.retrieveData(reportFormUrl.toString());
		Map<Integer,String> parentCats = this.parseParentCategories(resp);
		return parentCats;
	}
	
	/**
	 * Parses parent category data into a key/value pairs representing category ID/category Name.
	 * @param resp
	 * @return
	 * @throws IOException
	 */
	private Map<Integer,String> parseParentCategories(byte[] resp) throws IOException {
		Map<Integer,String> cats = new HashMap<Integer,String>();
		if (resp != null && resp.length > 0) {
			// parse the HTML response
			ByteArrayInputStream bais = new ByteArrayInputStream(resp);
			BufferedReader br = new BufferedReader(new InputStreamReader(bais));
			String temp = "";
			String idStub = "name=\"name";
			String nameStub = "value=\"";
			int idIndex = -1;
			int nameIndex = -1;
			String catId = null;
			String catName = null;
			Integer catInt = 0;
			while ((temp = br.readLine()) != null) {
				idIndex = temp.indexOf(idStub);
				if (idIndex > -1) {
					catId = temp.substring(idIndex + idStub.length());
					nameIndex = catId.indexOf(nameStub);
					catName = catId.substring(nameIndex + nameStub.length());
					catId = StringUtil.checkVal(catId.substring(0, catId.indexOf("\""))).trim();
					catName = StringUtil.checkVal(catName.substring(0, catName.indexOf("\""))).trim();
					catId = this.formatParentCategoryId(catId);
					catInt = Integer.valueOf(catId);
					cats.put(catInt, catName);
					//log.debug("parsed parent catId/catName: " + catId + "/" + catName);
					// add to the parent cat 'keyed by name' map for use later on
					parentCatsByName.put(catName, catInt);
				}				
			}
		}
		return cats;
	}
	
	private void updateParentCategories() {
		log.debug("inserting/updating parent categories...");
		boolean isInsert = false;
		for (Integer key : parentCats.keySet()) {
			if (existingCats.containsKey(key)) {
				isInsert = false;
			} else {
				isInsert = true;
			}
			this.updateExistingCategories(key, parentCats.get(key), null, isInsert, true);
		}
	}
	
	private void updateSubCategories() {
		log.debug("inserting/updating subcategories...");
		boolean isInsert = false;
		for (Integer key : subCats.keySet()) {
			CategoryVO vo = subCats.get(key);
			if (existingCats.containsKey(vo.getCategoryId())) {
				isInsert = false;
			} else {
				isInsert = true;
			}
			this.updateExistingCategories(key, vo.getCategoryName(), vo.getParentId(), isInsert, false);
		}
	}
	
	/**
	 * Formats the parent category ID based on the length of the category ID.
	 * @param catId
	 * @return
	 */
	private String formatParentCategoryId(String catId) {
		switch(catId.length()) {
			case 1: return "1000" + catId;
			case 2: return "100" + catId;
			case 3: return "10" + catId;
			case 4: return "1" + catId;
			default: return catId;
		}
	}
	
	/**
	 * Inserts or updates a single parent category
	 * @param catId
	 * @param catName
	 * @param insert
	 */
	private void updateExistingCategories(Integer catId, String catName, Integer parentId, boolean insert, boolean parent) {
		StringBuffer sql = new StringBuffer();
		if (insert) {
			sql.append("insert into ").append(customDbSchema).append("ARVC_CATEGORY ");
			sql.append("(CATEGORY_NM, ");
			if (parent) {
				sql.append("CREATE_DT, CATEGORY_ID) values (?,?,?)");
			} else {
				sql.append("PARENT_ID, CREATE_DT, CATEGORY_ID) values (?,?,?,?)");	
			}
			
		} else {
			sql.append("update ").append(customDbSchema).append("ARVC_CATEGORY ");
			sql.append("set CATEGORY_NM = ?, ");
			if (! parent) {
				sql.append("PARENT_ID = ?, ");
			}
			sql.append("UPDATE_DT = ? where CATEGORY_ID = ?");			
		}
		PreparedStatement ps = null;
		int index = 1;
		try {
			ps = conn.prepareStatement(sql.toString());
			ps.setString(index++, catName);
			if (! parent) {
				ps.setInt(index++, parentId);				
			}
			ps.setTimestamp(index++, Convert.getCurrentTimestamp());
			ps.setInt(index++, catId);
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("Error inserting/updating parent category, ", sqle);
		} finally {
			try {
				if (ps != null) ps.close();
			} catch (Exception e) {}
		}
		
	}
	

	/**
	 * Retrieves categories report in CSV format from the ChamberMaster site.
	 * @return
	 * @throws IOException
	 */
	private Map<Integer,CategoryVO> retrieveSubCategories(SMTHttpConnectionManager conn) throws IOException {
		byte[] resp = null;
		// go to the categories mgmt page
		String redir = getRedirLocation(conn);
		conn.addRequestHeader("Referer", redir);
		StringBuilder reportFormUrl = new StringBuilder();
		reportFormUrl.append(SECURE_BASE_URL);
		reportFormUrl.append("/directory/jsp/categories/ListCC.jsp?command=csvListing");
		resp = conn.retrieveData(reportFormUrl.toString());
		String repUrl = this.parseReportLink(new String(resp));
		log.debug("report link Url: " + repUrl);
		// retrieve the report data
		resp = httpConn.retrieveData(SECURE_BASE_URL + repUrl);
		log.debug("repData size: " + resp.length);
				
		Map<Integer,CategoryVO> subCats = this.parseSubCategories(resp);
		
		return subCats;
	}
	
	
	/**
	 * Parses the byte array of member data and stores each row into a collection of collections
	 * @param b
	 * @return
	 * @throws IOException
	 */
	protected Map<Integer,CategoryVO> parseSubCategories(byte[] b) throws IOException {
		// Parse the file
		CSVParser parser = new CSVParser();
		List<List<String>> csvData = null;
		try {
			csvData = parser.parseFile(b, true);
		} catch (IOException ioe) {
			log.error("Error parsing category data retrieved via SMTHttpConnectionManager, ", ioe);
			statusMessages.add("Error parsing category data retrieved via SMTHttpConnectionManager, " + ioe.getMessage());
			if (! isErrors()) setErrors(true);
		}
		/*
		 * col 0 = category name
		 * col 2 = name of parent category (column header is 'QuickLink')
		 * col 6 = category ID 
		 */
		Map<Integer,CategoryVO> subCats = new HashMap<Integer,CategoryVO>();
		String parentName = null;
		String catId = null;
		Integer catInt = null;
		List<String> cols;
		for (int i = 0; i < csvData.size(); i++) {
			cols = csvData.get(i);
			if (cols.size() < 7) continue;
			parentName = StringUtil.checkVal(cols.get(2));
			catId = cols.get(6);
			//log.debug("raw parentId/catId/catName: " + cols.get(2) + "/" + cols.get(6) + "/" + cols.get(0));
			if (parentName.length() > 0) {
				// it's a subcat
				CategoryVO subCat = new CategoryVO();
				catInt = Integer.valueOf(catId);
				subCat.setParentId(parentCatsByName.get(cols.get(2)));
				subCat.setCategoryId(catInt);
				subCat.setCategoryName(cols.get(0));
				subCats.put(catInt, subCat);
				//log.debug("formatted parentId/catId/catName: " + subCat.getParentId() + "/" + subCat.getCategoryId() + "/" + subCat.getCategoryName());
			} else {
				// it's really a parent cat, we format the cat ID and do not set the parent ID
				catId = this.formatParentCategoryId(catId);
				catInt = Integer.valueOf(catId);
				parentCats.put(catInt, cols.get(0));
				//log.debug("found a parent cat, catId/catName: " + catInt + "/" + parentCats.get(catInt));
			}
		}
		return subCats;
	}

}
