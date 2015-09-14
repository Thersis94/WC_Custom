package com.depuysynthes.nexus;

//JDK 1.7.x
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.SolrDocument;

//SMT Base Libs
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.search.SolrActionIndexVO;
import com.smt.sitebuilder.action.search.SolrActionVO;
import com.smt.sitebuilder.action.search.SolrQueryProcessor;
import com.smt.sitebuilder.action.search.SolrResponseVO;

/****************************************************************************
 * <b>Title</b>: NexusKitImporter.java<p/>
 * <b>Description: Loads the files retrieved from the MBox and loads them 
 * into the database</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 1.0
 * @since July 16, 2015
 * @updates 
 * 
 ****************************************************************************/
public class NexusKitImporter extends CommandLineUtil {
	
	
	/**
	 * Key for the property for the location of the output excel file
	 */
	public static final String KIT_FILE_PATH = "kitFilePath";
	
	/**
	 * Key for the property for the location of the DePuy Synthes logo
	 */
	public static final String LOGO_FILE_PATH = "logoFilePath";
	
	/**
	 * File name of the JDE Kit Files
	 */
	public static final String JDE_HEADER_FILE_NAME = "jdeKitHeaderFileName";
	public static final String JDE_DETAIL_FILE_NAME = "jdeKitDetailFileName";
	
	/**
	 * File Name of the MDM Kit Files
	 */
	public static final String MDM_FILE_NAME = "mdmFileName";
	
	/**
	 * File Name of the MDM Kit Files
	 */
	public static final int SOLR_FILTER_SIZE = 100;
	
	/**
	 * Maps the JDE OpCos to the Opcos in use
	 */
	public static final Map<String, String> JDE_ORG_MAP = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L; {
			put("100", "Trauma");
			put("150", "CMF");
			put("200", "Spine");
		}
	};
	
	/**
	 * Maps the JDE OpCos to the Opcos in use
	 */
	public static final Map<String, String> MDM_ORG_MAP = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L; {
			put("OPCO_DOH", "Orthopaedics");
			put("OPCO_DPM", "Mitek");
			put("OPCO_DSP", "Spine");
			put("OPCO_DCD", "Codman");
		}
	};
	
	/**
	 * 
	 * @param args
	 */
	public NexusKitImporter(String[] args) {
		super(args);
		loadProperties("scripts/Nexus.properties");
		loadDBConnection(props);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		NexusKitImporter bldr = new NexusKitImporter(args);
		bldr.run();
		long end = System.currentTimeMillis();
				
		log.info("Time to build: " + ((end - start) / 1000));
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		log.info("Starting Application at: " + new Date());
		Map<String, String> messages = new HashMap<>();
		int cnt = 0;
		try {
			// Process the JDE Header file
			//cnt = processJDEHeaderFile();
			messages.put("Number of JDE Kits", cnt + "");
			
			// Process the JDE Detail File
			//cnt = processJDEDetailFile();
			messages.put("Number of JDE Kit Items", cnt + "");
			
			// Process the MDM File
			int[] mdmCnt = this.processMDMFile();
			messages.put("Number of MDM Kits", mdmCnt[0] + "");
			messages.put("Number of MDM Kit Items", mdmCnt[1] + "");
			
			// Get the GTIN and Description from Solr
			cnt = processMDMInformation();
			messages.put("MDM Kit Data Updated From Solr", "");
		} catch (Exception e) {
			log.error("Unable to process", e);
			messages.put("Error: ", e.getMessage());
		}
		
		// Send an email report
		try {
			this.sendEmail(getEmailMessage(messages), null);
		} catch(Exception e) {
			log.error("Unable to send email report", e);
		}
		
		log.info("Exports Completed");
	}
	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public int processMDMInformation() throws SQLException {
		// Initialize the connection to solr
		SolrQueryProcessor sqp = new SolrQueryProcessor((Map)props, "DePuy_NeXus");
		
		// Get the ids hwere the description or gtin is missing and format
		// the ids into solr filters
		List<String> ids = this.formatIdsIntoSolrQuery(this.getMDMIds());
		
		// Loop the filters and get the data
		for(String where : ids) {
			SolrActionVO qData = new SolrActionVO();
			qData.setNumberResponses(100);
			qData.setStartLocation(0);
			qData.setOrganizationId("DPY_SYN_NEXUS");
			qData.setRoleLevel(0);
			qData.addIndexType(new SolrActionIndexVO("", NexusProductVO.solrIndex));
			
			Map<String, String> filter = new HashMap<>();
			filter.put("documentId", where);
			qData.setFilterQueries(filter);
			SolrResponseVO resp = sqp.processQuery(qData);
			
			// Loop the results and update the gtin/desc
			List<SolrDocument> docs = resp.getResultDocuments();
			for(SolrDocument doc : docs) {
				String desc = (String)doc.get("summary");
				String sku = (String)doc.get("documentId");
				String gtin = (String)doc.get("deviceId");
				this.updateKitRecord(sku, gtin, desc);
			}
		}

		return 0;
	}
	
	/**
	 * Creates a collection of filters (100 items in each) in the following format:
	 * documentId: #### or documentId #####
	 * @param ids
	 * @return Collection of filters
	 */
	private List<String> formatIdsIntoSolrQuery(List<String> ids) {
		List<String> where = new ArrayList<>();
		StringBuilder filter = new StringBuilder(4096);
		boolean isStart = true;
		for (int i=0; i < ids.size(); i++) {
			if (! isStart) filter.append("documentId:");
			else isStart = false;
			
			filter.append(ids.get(i));
			
			if ((i > 0 && (i % 100) == 0) || (i + 1) == ids.size()) {
				where.add(filter.toString());
				filter = new StringBuilder(4096);
				isStart = true;
			} else filter.append(" or ");
			
		}
		
		return where;
	}
	
	/**
	 * Retrieves the ids of any kits missing a gtin or description
	 * @return
	 * @throws SQLException
	 */
	public List<String> getMDMIds() throws SQLException {
		StringBuilder sql = new StringBuilder(512);
		sql.append("select set_info_id from ").append(props.get("customDbSchema"));
		sql.append("DPY_SYN_NEXUS_SET_INFO where description_txt is null ");
		sql.append("or gtin_txt is null");
		
		// Set the sql data elements
		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ResultSet rs = ps.executeQuery();
		
		// Load the results into the collection and return the data
		List<String> ids = new ArrayList<>();
		while(rs.next()) ids.add(rs.getString(1));

		return ids;
	}
	
	/**
	 * Processes the MDM File and stores the kit, layer and item information
	 * @return Array of counts.  0 location = # of Kits.  1 Location = #Kit items
	 * @throws IOException
	 * @throws SQLException
	 */
	public int[] processMDMFile() throws IOException, SQLException {
		String path = props.getProperty(KIT_FILE_PATH);
		String name = props.getProperty(MDM_FILE_NAME);
		BufferedReader in = new BufferedReader(new FileReader((path + name)));
		int[] mdmCnt = { 0,0 };
		
		Set<String> kitIds = new HashSet<>();
		String temp;
		int iCtr = 0, kCtr = 0;
		while((temp = in.readLine()) != null) {
			// Make sure the feature code is empty and the opco is on the list
			String[] items = temp.split("\\|");
			if (StringUtil.checkVal(items[3]).length() > 0 || ! MDM_ORG_MAP.containsKey(items[0])) 
				continue;
			
			// If the IDs are different, that means it is the first line of a kit
			// So we're going to add the kit
			if (! kitIds.contains(items[1])) {
				NexusKitVO kit = new NexusKitVO(null);
				kit.setKitId(items[1]);
				kit.setOrgId(MDM_ORG_MAP.get(items[0]));
				kit.setKitSKU(items[1]);
				
				// Store the header and layer info to the DB
				this.storeKitHeader(kit);
				
				// Add the kit to the set so this kit doesn't get built again
				kitIds.add(items[1]);
				kCtr++; 
			}
			
			// There will always be a kit item added
			this.storeKitItem(items[1], items[2], items[6], items[8], items[9], iCtr);
			iCtr++;
		}
		
		// Update the kit count and kit item count
		mdmCnt[0] = kCtr;
		mdmCnt[1] = iCtr;
		
		// Close the stream and return
		in.close();
		return mdmCnt;
	}
	
	/**
	 * Processes the JDE Kit Header Data
	 * @return
	 * @throws Exception
	 */
	public int processJDEHeaderFile() throws Exception {
		String path = props.getProperty(KIT_FILE_PATH);
		String name = props.getProperty(JDE_HEADER_FILE_NAME);
		BufferedReader in = new BufferedReader(new FileReader((path + name)));
		
		String temp;
		int ctr = -1;
		while((temp = in.readLine()) != null) {
			// Skip the first row as it contains the header data
			ctr ++;
			if (ctr == 0) continue;
			
			String[] items = temp.split(",");
			
			// Map the data to the VO
			NexusKitVO kit = new NexusKitVO(null);
			//kit.setKitId(ORG_MAP.get(items[0]) + "_" + items[1]);
			kit.setKitId(items[1]);
			kit.setOrgId(JDE_ORG_MAP.get(items[0]));
			kit.setKitSKU(items[1]);
			kit.setKitDesc(items[2]);
			kit.setKitGTIN(items[3]);
			kit.setBranchCode(items[4]);
			
			// Load the DB
			storeKitHeader(kit);
		}
		
		in.close();
		
		return ctr;
	}
	
	/**
	 * Processes the details file and stores the entries in the database
	 * @return
	 * @throws IOException
	 * @throws SQLException
	 */
	public int processJDEDetailFile() throws IOException, SQLException {
		// Open a stream to the file
		String path = props.getProperty(KIT_FILE_PATH);
		String name = props.getProperty(JDE_DETAIL_FILE_NAME);
		log.info("File Path: " + path + name);
		BufferedReader in = new BufferedReader(new FileReader((path + name)));
		
		// Read in one line at a time and process it
		String temp;
		int ctr = -1;
		while((temp = in.readLine()) != null) {
			// Skip the first row as it contains the header data
			ctr ++;
			if (ctr == 0) continue;
			
			String[] items = temp.split(",");
			this.storeKitItem(items[0], items[1], items[2], items[3], items[4], ctr);
		}
		
		in.close();
		return ctr;
	}
	
	/**
	 * Adds a kit item to the database
	 * @param items
	 * @param order
	 * @throws SQLException
	 */
	public void storeKitItem(String layerId, String sku, String qty, String start, String end, int order) 
	throws SQLException {
		// Build the SQL Statement
		StringBuilder sql = new StringBuilder(255);
		sql.append("insert into ").append(props.get("customDbSchema"));
		sql.append("DPY_SYN_NEXUS_SET_ITEM (item_id, layer_id, product_sku_txt, quantity_no, ");
		sql.append("unit_measure_cd, effective_start_dt, effective_end_dt, order_no, create_dt) ");
		sql.append("values (?,?,?,?,?,?,?,?,?)");
		//log.info("Detail SQL: " + sql);
		
		// Set the sql data elements
		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ps.setString(1, new UUIDGenerator().getUUID());
		ps.setString(2, layerId);
		ps.setString(3, sku);
		ps.setInt(4, Convert.formatInteger(qty));
		ps.setString(5, "");
		ps.setDate(6, Convert.formatSQLDate(Convert.parseDateUnknownPattern(start)));
		ps.setDate(7, Convert.formatSQLDate(Convert.parseDateUnknownPattern(end)));
		ps.setInt(8, order);
		ps.setTimestamp(9, Convert.getCurrentTimestamp());
		
		// Store the data
		ps.executeUpdate();
		ps.close();
	}
	
	/**
	 * Stores a kit in the database
	 * @param kit
	 * @throws SQLException
	 */
	public void storeKitHeader(NexusKitVO kit) throws SQLException {
		// Build the SQL Statement
		StringBuilder sql = new StringBuilder(255);
		sql.append("insert into ").append(props.get("customDbSchema"));
		sql.append("DPY_SYN_NEXUS_SET_INFO (set_info_id, set_sku_txt, organization_id, ");
		sql.append("description_txt, gtin_txt, branch_plant_cd, create_dt) ");
		sql.append("values (?,?,?,?,?,?,?)");
		//log.info("kit Header SQL: " + sql);
		
		// Set the sql data elements
		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ps.setString(1, kit.getKitId());
		ps.setString(2, kit.getKitSKU());
		ps.setString(3, kit.getOrgId());
		ps.setString(4, kit.getKitDesc());
		ps.setString(5, kit.getKitGTIN());
		ps.setString(6, StringUtil.checkVal(kit.getBranchCode()).trim());
		ps.setTimestamp(7, Convert.getCurrentTimestamp());
		
		// Store the data
		ps.executeUpdate();
		ps.close();
		
		// Add the layer
		storeKitLayer(kit.getKitId());
	}
	
	/**
	 * Creates a default layer for each kit
	 * @param kitId ID of the kit.  USes the opco_sku as the key
	 * @throws SQLException
	 */
	public void storeKitLayer(String kitId) throws SQLException{
		// Build the SQL Statement
		StringBuilder sql = new StringBuilder(255);
		sql.append("insert into ").append(props.get("customDbSchema"));
		sql.append("DPY_SYN_NEXUS_SET_LAYER (layer_id, set_info_id, layer_nm, ");
		sql.append("order_no, create_dt) ");
		sql.append("values (?,?,?,?,?)");
		//log.info("Kit Layer Header SQL: " + sql);
		
		// Set the sql data elements
		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ps.setString(1, kitId);
		ps.setString(2, kitId);
		ps.setString(3, props.getProperty("defaultLayerName"));
		ps.setInt(4, 1);
		ps.setTimestamp(5, Convert.getCurrentTimestamp());
		
		// Store the data
		ps.executeUpdate();
		ps.close();
	}
	
	/**
	 * Updates the Kit record for MDM with the description and GTIN which were not provided
	 * in the data feed
	 * @param sku
	 * @param gtin
	 * @param desc
	 * @return
	 * @throws SQLException
	 */
	public int updateKitRecord(String sku, String gtin, String desc) throws SQLException {
		StringBuilder sql = new StringBuilder(512);
		sql.append("update ").append(props.get("customDbSchema"));
		sql.append("DPY_SYN_NEXUS_SET_INFO set gtin_txt = ?, description_txt = ? ");
		sql.append("where set_info_id = ? ");
		
		// Set the sql data elements
		PreparedStatement ps = dbConn.prepareStatement(sql.toString());
		ps.setString(1, gtin);
		ps.setString(2, desc);
		ps.setString(3, sku);
		
		// Store the data
		int cnt = ps.executeUpdate();
		ps.close();
		
		return cnt;
	}
	
	/**
	 * Builds the HTML info for the email report.  Each file import will be placed
	 * in the message (inside a table row) with the message (success or failure).
	 * If the org processed successfully, the number of records in the file will be displayed.
	 * If the process errored on the org, the error message will be displayed
	 * @param messages
	 * @return
	 */
	protected StringBuilder getEmailMessage(Map<String, String> messages) {
		StringBuilder sb = new StringBuilder();
		sb.append("<p>The Kit Loading File Process has finished processing data for <b>");
		sb.append(Convert.formatDate(new Date(), Convert.DATE_FULL_MONTH));
		sb.append("</b></p><p>&nbsp;</p>");
		
		// set styles
		sb.append("<style>\n");
		sb.append("table { width: 500px; border-spacing: 0;border-collapse: collapse;} \n");
		sb.append("td { border: solid 1px black; padding: 5px;} \n");
		sb.append(".hdr { background: gray; color:white;text-align:center; } \n");
		sb.append(".err { background: red; } \n");
		sb.append(".normal { background: lightgreen; } \n");
		sb.append("</style> \n");
		
		sb.append("<table><tr><td class='hdr' colspan='2'><b>NeXus GTIN Download Report</b></td></tr>");
		
		for (String  key: messages.keySet()) {
			String message = StringUtil.checkVal(messages.get(key));
			String rowType = "<tr class='normal'>";
			if (message.startsWith("Error")) rowType = "<tr class='err'>";
			
			sb.append(rowType).append("<td>").append(key).append("</td><td >");
			sb.append(message).append("</td></tr>");
		}
		
		sb.append("</table>");
		return sb;
	}
}
