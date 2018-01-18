package com.depuysynthes.nexus;

//JDK 1.7.x
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

// SOLR Libs
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

import com.depuysynthes.nexus.NexusImporter.Source;
import com.siliconmtn.io.ftp.SFTPV3Client;
//SMT Base Libs
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.solr.SolrClientBuilder;
import com.smt.sitebuilder.action.search.SolrActionIndexVO;
import com.smt.sitebuilder.action.search.SolrActionVO;
import com.smt.sitebuilder.action.search.SolrQueryProcessor;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.solr.SolrActionUtil;

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
	 * Instance of the solr server create when the program starts so that
	 * it is not recreated with every kit. 
	 */
	private SolrClient server;
	
	/**
	 * Key for the index that should be used for the solr documents
	 */
	public static final String SOLR_INDEX = "solrIndex";
	
	/**
	 * Key for the property for the location of the output excel file
	 */
	public static final String KIT_FILE_PATH = "kitFilePath";
	
	/**
	 * Key for the property for the location of the DePuy Synthes logo
	 */
	public static final String LOGO_FILE_PATH = "logoFilePath";
	
	/**
	 * Key for determining if we are getting local files or are downloading
	 * them from a remote server.
	 */
	public static final String IS_LOCAL = "isLocal";
	
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
	 * Whether or not we are loading MDM kits
	 */
	public static final String LOAD_MDM = "loadMDM";

	/**
	 * Whether or not we are loading MDM kits
	 */
	public static final String LOAD_JDE = "loadJDE";
	
	/**
	 * File Name of the MDM Kit Files
	 */
	public static final int SOLR_FILTER_SIZE = 100;
	
	/**
	 * Information pertaining to downloading remote files. 
	 */
	public static final String REMOTE_USER = "user";
	public static final String REMOTE_PASS = "password";
	public static final String REMOTE_HOST = "hostName";
	
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
		overrideDefaults(args);
		loadDBConnection(props);
		loadSolrServer(props);
	}

	/**
	 * Override properties with items from the arguments where applicable
	 * @param args
	 */
	private void overrideDefaults(String[] args) {
		switch (args.length) {
			case 7: props.put(JDE_DETAIL_FILE_NAME, args[6]);
			case 6: props.put(JDE_HEADER_FILE_NAME, args[5]);
			case 5: props.put(LOAD_JDE, args[4]);
			case 4: props.put(MDM_FILE_NAME, args[3]);
			case 3: props.put(LOAD_MDM, args[2]);
			case 2: props.put(KIT_FILE_PATH, args[1]);
			case 1: props.put(IS_LOCAL, args[0]);
			default: return;
		}
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
		try {
			if (Convert.formatBoolean(props.get(LOAD_JDE))) {
				// Process the JDE Header file
				processJDEHeaderFile(messages);
				
				// Process the JDE Detail File
				processJDEDetailFile(messages);
			}
			
			if (Convert.formatBoolean(props.get(LOAD_MDM))) {
				// Process the MDM File
				int[] mdmCnt = this.processMDMFile(messages);

				// Get the GTIN and Description from Solr
				processMDMInformation(mdmCnt, messages);
			}
			
			// Once this point has been reached all documents are ready to be committed.
			server.commit();
		} catch (Exception e) {
			log.error("Unable to process", e);
			messages.put("Error: ", e.getMessage());
		}
		
		// Send an email report
		try {
			sendEmail(getEmailMessage(messages), null);
		} catch(Exception e) {
			log.error("Unable to send email report", e);
		} finally {
			try {
				server.close();
			} catch (Exception e) {}
		}
		
		log.info("Exports Completed");
	}
	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void processMDMInformation(int[] mdmCnt, Map<String, String> messages) throws SQLException {
		// Initialize the connection to solr
		SolrQueryProcessor sqp = new SolrQueryProcessor((Map)props, "DePuy_NeXus");
		
		// Get the ids where the description or gtin is missing and format
		// the ids into solr filters
		List<String> ids = this.formatIdsIntoSolrQuery(this.getMDMIds());
		
		List<String> failures = new ArrayList<>();
		// Loop the filters and get the data
		for(String where : ids) {
			SolrActionVO qData = new SolrActionVO();
			qData.setNumberResponses(100);
			qData.setStartLocation(0);
			qData.setOrganizationId("DPY_SYN_NEXUS");
			qData.setRoleLevel(0);
			qData.addIndexType(new SolrActionIndexVO("", NexusProductVO.SOLR_IDX));
			
			Map<String, String> filter = new HashMap<>();
			filter.put("documentId", where);
			qData.setFilterQueries(filter);
			SolrResponseVO resp = sqp.processQuery(qData);
			
			// Loop the results and update the gtin/desc
			List<SolrDocument> docs = resp.getResultDocuments();
			for(SolrDocument doc : docs) {
				String desc = StringUtil.checkVal(doc.get(SearchDocumentHandler.SUMMARY));
				String sku = StringUtil.checkVal(doc.get(SearchDocumentHandler.DOCUMENT_ID));
				String gtin = StringUtil.checkVal(doc.get(NexusProductVO.DEVICE_ID));
				if (!this.updateKitRecord(sku, gtin, desc)) {
					failures.add(sku);
					continue;
				}
				this.updateSolr(sku, gtin, desc);
			}
		}
		
		int fails = getInvalidProducts();
		
		// Delete any remaining blank kits
		deleteBlankKits();
		
		mdmCnt[0] = mdmCnt[0]-fails;
		mdmCnt[1] = mdmCnt[1]+fails;
		
		// Update the counts for the counts.
		messages.put("Number of MDM Kit Items", StringUtil.checkVal(mdmCnt[0]));
		messages.put("Number of Items in MDM File", StringUtil.checkVal(mdmCnt[1] + mdmCnt[0]) );
		if (failures.isEmpty()) messages.put("MDM Kits Failed to Update", createFailureList(failures));
		
	}
	
	
	/**
	 * Get the count of all products that belong to invalid kits
	 */
	private int getInvalidProducts() throws SQLException {
		StringBuilder sql = new StringBuilder(250);
		String customDb = (String) props.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("select count(*) from ").append(customDb).append("dpy_syn_nexus_set_info s ");
		sql.append("left join ").append(customDb).append("dpy_syn_nexus_set_item i ");
		sql.append("on i.layer_id = s.set_info_id ");
		sql.append("where organization_id != 'Custom' and (description_txt is null ");
		sql.append("or description_txt = '')");

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) {
				return rs.getInt(1);
			} else {
				return 0;
			}
		}
	}

	/**
	 * Delete all kits that are missing descriptions
	 * @param ids
	 * @throws SQLException
	 */
	private void deleteBlankKits() throws SQLException {
		StringBuilder sql = new StringBuilder(150);
		sql.append("delete from ").append(props.get(Constants.CUSTOM_DB_SCHEMA)).append("dpy_syn_nexus_set_info ");
		sql.append("where organization_id != 'Custom' and (description_txt is null ");
		sql.append("or description_txt = '')");
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			log.debug(ps.executeUpdate() + " Sets Deleted for having no description");
		}
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
		for (int i=1; i <= ids.size(); i++) {
			if (! isStart) filter.append("documentId:");
			else isStart = false;
			
			filter.append(ids.get(i-1));
			
			if ((i > 0 && (i % 100) == 0) || i == ids.size()) {
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
		sql.append("select set_info_id from ").append(props.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("dpy_syn_nexus_set_info where description_txt is null ");
		sql.append("or gtin_txt is null");

		List<String> ids = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			// Set the sql data elements
			ResultSet rs = ps.executeQuery();
			
			// Load the results into the collection and return the data
			while(rs.next()) ids.add(rs.getString(1));

		}
		return ids;
	}
	
	/**
	 * Processes the MDM File and stores the kit, layer and item information
	 * @param messages 
	 * @return Array of counts.  0 location = # of Kits.  1 Location = #Kit items
	 * @throws IOException
	 * @throws SQLException
	 */
	public int[] processMDMFile(Map<String, String> messages) throws IOException, SQLException {
		String path = props.getProperty(KIT_FILE_PATH);
		String name = props.getProperty(MDM_FILE_NAME);
		BufferedReader in = getFile(path, name);
		int[] mdmCnt = { 0,0,0 };
		int success=0;
		// Fail starts at -1 in order to absorb the fail from the column headers
		int fail=-1;
		int nonBlank=0;
		int nonOrg=0;
		
		Map<String, List<String>> existingSets = getActiveMDM();
		Set<String> kitIds = new HashSet<>();
		String temp;
		int iCtr = 0, kCtr = 0;
		List<String> kitFailures = new ArrayList<>();
		List<String> productFailures = new ArrayList<>();
		while((temp = in.readLine()) != null) {
			// Make sure the feature code is empty and the opco is on the list
			String[] items = temp.split("\\|");
			if (kitFailures.contains(items[1]) || ! MDM_ORG_MAP.containsKey(items[0])) {
				fail++;
				continue;
			} else if (StringUtil.checkVal(items[3]).length() > 0) {
				nonBlank++;
				continue;
			}
			
			// If the IDs are different, that means it is the first line of a kit
			// So we're going to add the kit
			if (! kitIds.contains(items[1])) {
				NexusKitVO kit = new NexusKitVO(props.getProperty(SOLR_INDEX));
				kit.setKitId(items[1]);
				kit.setOrgId(MDM_ORG_MAP.get(items[0]));
				kit.setKitSKU(items[1]);
				
				boolean addSuccess = false;
				// Store the header and layer info to the DB
				if (existingSets.keySet().contains(kit.getKitId())) {
					addSuccess = this.updateKitHeader(kit);
				} else {
					addSuccess = this.storeKitHeader(kit, true);
				}
				
				// Add the kit to the set so this kit doesn't get built again
				kitIds.add(items[1]);
				
				if (addSuccess) {
					kCtr++; 
				} else {
					fail++;
					kitFailures.add(kit.getKitSKU());
				}
			}
			
			// There will always be a kit item added.  If the date is way out into 
			// the future, set it into the future 
			String end = StringUtil.checkVal(items[6]);
			if (! end.startsWith("20")) end = "20501231";
			if (existingSets.get(items[1]) != null) {
				if (this.storeKitItem(items[1], items[2], items[9], items[5], end, iCtr, items[7], true, existingSets.get(items[1]).contains(items[2]))) {
					success++;
				} else {
					nonOrg++;
					productFailures.add(items[2]);
				}
			} else {
				if (this.storeKitItem(items[1], items[2], items[9], items[5], end, iCtr, items[7], true, false)) {
					success++;
				} else {
					nonOrg++;
					productFailures.add(items[2]);
				}
			}
			iCtr++;
		}
		
		// Update the kit count and kit item count
		mdmCnt[0] = success;
		mdmCnt[1] = fail + nonBlank + nonOrg;

		messages.put("Number of MDM Kits", StringUtil.checkVal(kCtr));
		// Add current counts to ensure proper order in the email
		messages.put("Number of MDM Kit Items", StringUtil.checkVal(mdmCnt[0]));
		messages.put("Number of Items in MDM File", StringUtil.checkVal(mdmCnt[1] + mdmCnt[0]));
		if (kitFailures.isEmpty()) messages.put("Failed MDM Kits", createFailureList(kitFailures));
		if (productFailures.isEmpty()) messages.put("Failed MDM Kit Items", createFailureList(productFailures));
		
		// Close the stream and return
		in.close();
		return mdmCnt;
	}
	
	
	/**
	 * Update an existing kit
	 * @param kit
	 * @throws SQLException
	 */
	private boolean updateKitHeader(NexusKitVO kit) throws SQLException {
		StringBuilder sql = new StringBuilder(200);
		
		sql.append("update ").append(props.get(Constants.CUSTOM_DB_SCHEMA)).append("dpy_syn_nexus_set_info ");
		sql.append("set description_txt=?, gtin_txt=?, update_dt=? ");
		sql.append("where set_info_id = ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, kit.getKitDesc());
			ps.setString(2, kit.getKitGTIN());
			ps.setTimestamp(3, Convert.getCurrentTimestamp());
			ps.setString(4, kit.getKitId());
			
			ps.executeUpdate();
			
			// Remove all products associated with the kit.
			// The kit's current products are all in the file.
			deleteKitProducts(kit.getKitId());
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	
	/**
	 * Delete all products associated with the supplied kit.
	 * This prepares the kit to have its new collection of items listed
	 * in the loaded document added to the database without duplicating any
	 * items that are already present.
	 * @param kitId
	 */
	private void deleteKitProducts(String kitId) {
		StringBuilder sql = new StringBuilder(100);
		sql.append("delete from ").append(props.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("dpy_syn_nexus_set_item ");
		sql.append("where layer_id = ?");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, kitId);
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Unable to delete products for kit " + kitId, e);
		}
	}

	/**
	 * Get a list of all the MDM kits in the database that are available for updating
	 * @return
	 */
	private Map<String, List<String>> getActiveMDM() {
		Map<String, List<String>> existingKits = new HashMap<>();
		
		StringBuilder sql = new StringBuilder(200);
		sql.append("select set_info_id, item_id from ").append(props.get(Constants.CUSTOM_DB_SCHEMA)).append("dpy_syn_nexus_set_info s ");
		sql.append("left join ").append(props.get(Constants.CUSTOM_DB_SCHEMA)).append("dpy_syn_nexus_set_item i ");
		sql.append("on s.set_info_id = i.layer_id ");
		sql.append("where source = 'MDM' order by set_info_id");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			String kitId = "";
			List<String> items = null;
			while(rs.next()) {
				if (!kitId.equals(rs.getString("set_info_id"))) {
					if (items != null) {
						existingKits.put(kitId, items);
					}
					items = new ArrayList<>();
					kitId = rs.getString("set_info_id");
				}
				items.add(rs.getString("item_id"));
			}
			if (items != null) existingKits.put(kitId, items);
			
		} catch (Exception e) {
			log.error("Unable to get existing MDM kits", e);
		}
		
		return existingKits;
	}

	/**
	 * Processes the JDE Kit Header Data
	 * @param messages 
	 * @return
	 * @throws Exception
	 */
	public void processJDEHeaderFile(Map<String, String> messages) throws Exception {
		flushJDEKits();
		String path = props.getProperty(KIT_FILE_PATH);
		String name = props.getProperty(JDE_HEADER_FILE_NAME);
		BufferedReader in = getFile(path, name);
		
		String temp;
		int ctr = -1;
		List<String> failures = new ArrayList<>();
		while((temp = in.readLine()) != null) {
			// Skip the first row as it contains the header data
			ctr ++;
			if (ctr == 0) continue;
			
			String[] items = temp.split(",");
			
			// Map the data to the VO
			NexusKitVO kit = new NexusKitVO(props.getProperty(SOLR_INDEX));
			//kit.setKitId(ORG_MAP.get(items[0]) + "_" + items[1]);
			kit.setKitId(items[1]);
			if (JDE_ORG_MAP.containsKey(items[0])) {
				kit.setOrgId(JDE_ORG_MAP.get(items[0]));
			} else {
				kit.setOrgId(items[0]);
			}
			kit.setKitSKU(items[1]);
			kit.setKitDesc(items[2]);
			kit.setKitGTIN(items[3]);
			kit.setBranchCode(items[4]);
			kit.addOrganization(props.getProperty("organization"));
			kit.addRole(SecurityController.PUBLIC_ROLE_LEVEL);
			kit.setSource(Source.JDE);
			kit.setOrgName(kit.getOrgId());
			
			// Load the DB and solr
			if (!storeKitHeader(kit, false)) {
				failures.add(kit.getKitSKU());
			} else {
				addToSolr(kit);
			}
		}
		
		messages.put("Number of JDE Kits", StringUtil.checkVal(ctr));
		if (failures.isEmpty()) messages.put("Failed JDE Kits", createFailureList(failures));
		
		in.close();
		
	}
	
	
	/**
	 * Concatenate the list of failed items into a list
	 */
	private String createFailureList(List<String> failures) {
		StringBuilder message = new StringBuilder(failures.size()*12);
		for (String fail : failures) message.append(fail).append("<br/>");
		return message.toString();
	}
	
	/**
	 * Get a buffered reader containing the file at the location described
	 * by the supplied path and name.
	 */
	private BufferedReader getFile(String path, String name) throws IOException {
		if (Convert.formatBoolean(props.get(IS_LOCAL), true)){
			return getLocalFile(path, name);
		} else {
			return getRemoteFile(path, name);
		}
	}

	/**
	 * Delete all kits that came from JDE since these are going to come from
	 * full refreshes instead of updates
	 * @throws SQLException 
	 */
	private void flushJDEKits() throws Exception {
		StringBuilder sql = new StringBuilder(125);
		
		sql.append("delete from ").append(props.get(Constants.CUSTOM_DB_SCHEMA)).append("dpy_syn_nexus_set_info ");
		sql.append("where source = 'JDE'");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.executeUpdate();
		}
		
		server.deleteByQuery("source:JDE, kit:true");
		server.commit();
	}

	/**
	 * Processes the details file and stores the entries in the database
	 * @param messages 
	 * @return
	 * @throws IOException
	 * @throws SQLException
	 */
	public void processJDEDetailFile(Map<String, String> messages) throws IOException, SQLException {
		// Open a stream to the file
		String path = props.getProperty(KIT_FILE_PATH);
		String name = props.getProperty(JDE_DETAIL_FILE_NAME);
		log.info("File Path: " + path + name);
		BufferedReader in = getFile(path, name);
		
		// Read in one line at a time and process it
		String temp;
		int ctr = -1;
		List<String> failures = new ArrayList<>();
		while((temp = in.readLine()) != null) {
			// Skip the first row as it contains the header data
			ctr ++;
			if (ctr == 0) continue;
			
			String[] items = temp.split(",");
			Date d = new Date();
			try {
				if (d.after(parseDate(items[6], "MM/dd/yy"))) {
					ctr--;
				} else if (!this.storeKitItem(items[0], items[1], items[4], items[5],items[6], ctr, "", false, false)) {
					ctr--;
					failures.add(items[1]);
				}
			} catch (ParseException e) {
				ctr--;
				log.error("Unable to parse product date " + items[0] + "|"+items[1]);
			}
		}
		
		in.close();
		messages.put("Number of JDE Kit Items", StringUtil.checkVal(ctr));
		if (failures.isEmpty()) messages.put("Failed JDE Kit Items", createFailureList(failures));
	}
	
	/**
	 * Adds a kit item to the database
	 * @param items
	 * @param order
	 * @throws SQLException
	 */
	public boolean storeKitItem(String layerId, String sku, String qty, String start, String end, int order, String uom, boolean isMDM, boolean update) 
	throws SQLException {
		String dateFormat;
		if (isMDM) {
			dateFormat = "yyyyMMdd";
		} else {
			dateFormat = "MM/dd/yy";
		}
		// Build the SQL Statement
		StringBuilder sql = new StringBuilder(255);
		if (!update) {
			sql.append("insert into ").append(props.get(Constants.CUSTOM_DB_SCHEMA));
			sql.append("dpy_syn_nexus_set_item (quantity_no, unit_measure_cd, effective_start_dt, ");
			sql.append("effective_end_dt, order_no, create_dt,layer_id, product_sku_txt, item_id) ");
			sql.append("values (?,?,?,?,?,?,?,?,?)");
		} else {
			sql.append("update ").append(props.get(Constants.CUSTOM_DB_SCHEMA));
			sql.append("dpy_syn_nexus_set_item set quantity_no=?, ");
			sql.append("unit_measure_cd=?, effective_start_dt=?, effective_end_dt=?, order_no=?, create_dt=? ");
			sql.append("where layer_id = ? and product_sku_txt = ? ");
		}
		//log.info("Detail SQL: " + sql);
		
		// Set the sql data elements
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, Convert.formatInteger(qty));
			ps.setString(2, "");
			ps.setDate(3, Convert.formatSQLDate(parseDate(start, dateFormat)));
			ps.setDate(4, Convert.formatSQLDate(parseDate(end, dateFormat)));
			ps.setInt(5, order);
			ps.setTimestamp(6, Convert.getCurrentTimestamp());
			ps.setString(7, layerId);
			ps.setString(8, sku);
			if (!update) ps.setString(9, new UUIDGenerator().getUUID());
			
			ps.executeUpdate();
		} catch (Exception e) {
			log.error("Invalid product " + e);
			return false;
		}
		return true;
	}
	
	/**
	 * Stores a kit in the database
	 * @param kit
	 * @throws SQLException
	 */
	public boolean storeKitHeader(NexusKitVO kit, boolean isMDM) {
		// Build the SQL Statement
		StringBuilder sql = new StringBuilder(255);
		sql.append("insert into ").append(props.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("dpy_syn_nexus_set_info (set_info_id, set_sku_txt, organization_id, ");
		sql.append("description_txt, gtin_txt, branch_plant_cd, create_dt, source) ");
		sql.append("values (?,?,?,?,?,?,?,?)");
		//log.info("kit Header SQL: " + sql);
		
		String desc = StringUtil.checkVal(kit.getKitDesc());
		CharsetEncoder asciiEncoder = Charset.forName("US-ASCII").newEncoder(); 
		if (! asciiEncoder.canEncode(desc)) desc = desc.replaceAll("[^\\p{ASCII}]", "");
		
		// Set the sql data elements
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, kit.getKitId());
			ps.setString(2, kit.getKitSKU());
			ps.setString(3, kit.getOrgId());
			ps.setString(4, desc);
			ps.setString(5, kit.getKitGTIN());
			ps.setString(6, StringUtil.checkVal(kit.getBranchCode()).trim());
			ps.setTimestamp(7, Convert.getCurrentTimestamp());
			if (isMDM) {
				ps.setString(8, Source.MDM.toString());
			} else {
				ps.setString(8, Source.JDE.toString());
			}
			
			// Store the data
			ps.executeUpdate();
			
			// Add the layer
			storeKitLayer(kit.getKitId());
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	/**
	 * Creates a default layer for each kit
	 * @param kitId ID of the kit.  USes the opco_sku as the key
	 * @throws SQLException
	 */
	public void storeKitLayer(String kitId) throws SQLException{
		// Build the SQL Statement
		StringBuilder sql = new StringBuilder(255);
		sql.append("insert into ").append(props.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("dpy_syn_nexus_set_layer (layer_id, set_info_id, layer_nm, ");
		sql.append("order_no, create_dt) ");
		sql.append("values (?,?,?,?,?)");
		//log.info("Kit Layer Header SQL: " + sql);
		
		// Set the sql data elements
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, kitId);
			ps.setString(2, kitId);
			ps.setString(3, props.getProperty("defaultLayerName"));
			ps.setInt(4, 1);
			ps.setTimestamp(5, Convert.getCurrentTimestamp());
			
			// Store the data
			ps.executeUpdate();
		}
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
	public boolean updateKitRecord(String sku, String gtin, String desc) {
		StringBuilder sql = new StringBuilder(512);
		sql.append("update ").append(props.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("dpy_syn_nexus_set_info set gtin_txt = ?, description_txt = ? ");
		sql.append("where set_info_id = ? ");
		// Set the sql data elements
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, gtin);
			ps.setString(2, desc);
			ps.setString(3, sku);
			
			// Store the data
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			return false;
		}
		return true;
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
	
	
	/**
	 * Build the solr server for later use in the importer
	 * @param props
	 */
	private void loadSolrServer(Properties props) {
		String baseUrl = props.getProperty(Constants.SOLR_BASE_URL);
		String collection =  props.getProperty(Constants.SOLR_COLLECTION_NAME);
		server = SolrClientBuilder.build(baseUrl, collection);
	}
	
	
	/**
	 * Update the product with the supplied sku to show it is a kit
	 * @param sku
	 * @param gtin
	 * @param desc
	 */
	private void updateSolr(String sku, String gtin, String desc) {
		SolrInputDocument doc = new SolrInputDocument();
		Map<String,Object> fieldModifier = new HashMap<>(1);
		fieldModifier.put("set","true");
		doc.addField("kit", fieldModifier);
		doc.addField("documentId",sku);
		// Contents is used to create the autocomplete every time the document
		// is updated.  Since it isn't actually stored by the index any updates
		// need to containe the contents or it will be lost.
		doc.addField("contents", sku + " " + desc + " " + gtin + " ");
		try {
			server.add(doc);
		} catch (Exception e) {
			log.error("Unable to add kit " + sku + " to solr.", e);
		}

	}

	
	/**
	 * Add the supplied kit to solr
	 * @param kit
	 */
	@SuppressWarnings("resource")
	private void addToSolr(NexusKitVO kit) {
		SolrActionUtil util = new SolrActionUtil(server);
		try {
			util.addDocument(kit);
		} catch (Exception e) {
			log.error("Unable to add kit " + kit.getKitSKU() + " to solr.", e);
		}
	}
	
	
	/**
	 * Parse the date in such a way that the proper century is used for
	 * two digit date formats.
	 * @param date
	 * @param format
	 * @return
	 * @throws ParseException
	 */
	private Date parseDate(String date, String format) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		sdf.set2DigitYearStart(new GregorianCalendar(1990, 1, 1).getTime());
		return sdf.parse(date);
	}
	
	
	/**
	 * Get the file from the mbox
	 * @param path
	 * @param name
	 * @return
	 * @throws IOException 
	 */
	private BufferedReader getRemoteFile(String path, String fileName) throws IOException {
		SFTPV3Client ftp = null;
		ftp = new SFTPV3Client(props.getProperty(REMOTE_HOST), props.getProperty(REMOTE_USER), props.getProperty(REMOTE_PASS));
		byte[] data = ftp.getFileData(path + fileName);
		return new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data)));
	}
	
	/**
	 * Get the file from the local location
	 * @param path
	 * @param name
	 * @return
	 * @throws IOException
	 */
	private BufferedReader getLocalFile(String path, String name) throws IOException {
		return new BufferedReader(new FileReader((path + name)));
	}
}
