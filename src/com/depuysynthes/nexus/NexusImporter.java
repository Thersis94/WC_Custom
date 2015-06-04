package com.depuysynthes.nexus;

// SMT Base Libs
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.solr.client.solrj.impl.HttpSolrServer;

import com.depuy.datafeed.SFTPClient;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.io.mail.SMTMailHandler;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.MessageSender;
import com.smt.sitebuilder.util.solr.SolrActionUtil;

/****************************************************************************
 * <b>Title</b>: NexusImporter.java<p/>
 * <b>Description: Pulls down a file from DePuy's secure server, parses it into
 * vos, and puts that data into the solr server</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Apr 28, 2015
 * @updates 
 * 
 ****************************************************************************/
public class NexusImporter extends CommandLineUtil {
	private static final int BUFFER_SIZE = 2048;
	
	private String DELIMITER;
	
	// Used to store variables that are used in the MessageSender constructor
	private HashMap<String, Object> attributes;
	
	// Check if we are getting a local file
	private boolean isLocal = false;
	
	// FTP variables
	private String user;
	private String password;
	private String fileName;
	private String hostName;
	private String directory;
	
	// Stores the organizations code mappings
	public enum organizations {
		DO("Orthopaedics"),
		Orthopaedics("Orthopaedics"),
		DC("Codman"),
		Codman("Codman"),
		Trauma("Trauma"),
		Spine("Spine"),
		CMF("CMF"),
		DM("Mitek"),
		Mitek("Mitek"),
		DS("Spine");
		private final String name;
		organizations(String name) {
			this.name=name;
		}
		public String getName() {
			return name;
		}
	}
	
	private int org;
	private int status;
	private int region;
	private int code;
	private int desc;
	private int gtin;
	private int gtinLvl;
	private int device;
	private int use;
	private int dpm;
	private int qty;
	private int pkg;
	private int uom;
	
	// Stores any errors that we run into throughout the import
	private List<String> errors;
	
	public NexusImporter(String[] args) {
		super(args);
		loadProperties("scripts/Nexus.properties");
		loadDBConnection(props);

		if (args.length > 0) {
			isLocal = Convert.formatBoolean(args[0]);
		}
		
		if (args.length == 3) {
			directory = args[1];
			fileName = args[2];
		} else {
			fileName = props.getProperty("fileName");
			directory = props.getProperty("directory");
		}
		
		prepareValues();
		errors = new ArrayList<>();
	}
	
	
	/**
	 * Set up the class values and determine which file headers we are going
	 * to need to use in order get the product information from the supplied files
	 */
	private void prepareValues() {
		attributes = new HashMap<>();
		attributes.put("defaultMailHandler", new SMTMailHandler(props));
		attributes.put("instanceName", props.get("instanceName"));
		attributes.put("appName", props.get("appName"));
		attributes.put("adminEmail", props.get("adminEmail"));
		
		
		
		DELIMITER = props.getProperty("delimiter");
		user = props.getProperty("user");
		password = props.getProperty("password");
		hostName = props.getProperty("hostName");
		
	}
    
    
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {        
		//Create an instance of the MedianBinImporter
		NexusImporter dmb = new NexusImporter(args);
		dmb.run();
	}
	
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		try {
			boolean isZip = props.getProperty("fileName").contains(".zip");
			// Get the files and parse them into products
			Map<String, NexusProductVO> products;
			if (isLocal) {
				products = getFilesFromLocalZip();
			} else {
				products = getProductsFromFile();
			}
			
			// initialize the connection to the solr server
			HttpSolrServer server = new HttpSolrServer(props.getProperty(Constants.SOLR_BASE_URL)+props.getProperty(Constants.SOLR_COLLECTION_NAME));
			SolrActionUtil solr = new SolrActionUtil(server);
			int cnt=0;
			for (String key : products.keySet()) {
				NexusProductVO p = products.get(key);
				try {
					// If we are dealing with a zip file we need to filter out the unneeded products
					if (isZip && (!"DO,DS,DM,DC".contains(StringUtil.checkVal(p.getOrgId(), "SKIP")) ||
							!"AC,CT,DP,DS".contains(StringUtil.checkVal(p.getStatus(), "SKIP")) ||
							!"USA".equals(StringUtil.checkVal(p.getRegion(), "SKIP")))) {
						continue;
					}
					
					solr.addDocument(p);
					cnt++;
				} catch (Exception e) {
					errors.add("Unable to add product with product code of " + p.getDocumentId() + " to solr server.");
					log.error("Unable to add product to solr", e);
				}
			}
			sendAlertEmail(cnt, products.size());
			try {
				server.commit();
			} catch (Exception e) {
				log.error("Unable to commit documents", e);
			}
		} catch(ActionException e) {
			log.error("Failed to complete transaction", e);
		}
		log.debug("Ended at " + Convert.getCurrentTimestamp());
	}
	
	
	/**
	 * Get the file from the mbox and create a map out of the contained data
	 * @return
	 * @throws ActionException 
	 */
	private Map<String, NexusProductVO> getProductsFromFile() throws ActionException {
		log.debug("Getting file from mbox");
		boolean isZip = fileName.contains(".zip");
		Map<String, String> fileData = new TreeMap<>();
		Map<String, NexusProductVO> products = new HashMap<>();
		SFTPClient ftp = null;
		try {
			
			ftp = new SFTPClient(hostName, user, password);
			log.debug("Getting File " + directory + "/" + fileName + " at " + Convert.getCurrentTimestamp());
			if (isZip) {
				getFilesFromZip(fileData, ftp.getFileData(directory+"/"+fileName));
			} else {
				fileData.put(fileName, new String(ftp.getFileData(directory+"/"+fileName)));
			}
			
			for (String key : fileData.keySet()) {
				buildProducts(key, fileData.get(key), isZip, products);
			}
			
			
		} catch (IOException e) { 
			log.error("Unable to get data from file", e);
			throw new ActionException(e);
		}
		return products;
	}
	
	
	/**
	 * Get all the files out of the supplied zip and 
	 * @param fileData
	 * @param zipData
	 * @throws ActionException 
	 */
	private void getFilesFromZip(Map<String, String> fileData, byte[] zipData) throws ActionException {
		log.debug("Got File From mbox at " + Convert.getCurrentTimestamp());
		ZipEntry ze = null;
		ByteArrayInputStream bais = new ByteArrayInputStream(zipData);
		ZipInputStream zis = new ZipInputStream(bais);

		zis = new ZipInputStream(bais);
		
		try {
			while ((ze = zis.getNextEntry()) != null) {
				if (!ze.getName().contains(".OUT")) 
					continue;

				// Get the file
				byte[] b = new byte[2048];
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				int c = 0;
				while ((c = zis.read(b, 0, BUFFER_SIZE)) != -1) {
					baos.write(b, 0, c);
				}
				fileData.put(ze.getName(), new String(baos.toByteArray(),"ISO-8859-15"));
				
			}
		} catch (IOException e) {
			log.error("Unable to parse zip contents", e);
			throw new ActionException(e);
		}
	}
	
	/**
	 * Gets the files from a local zip file instead of pulling it down from the mbox server
	 * @return
	 * @throws ActionException
	 */
	private Map<String, NexusProductVO> getFilesFromLocalZip() throws ActionException {
		log.debug("Got File From mbox at " + Convert.getCurrentTimestamp());
		ZipEntry ze = null;
		Map<String, NexusProductVO> map = new HashMap<>();
		try {
			InputStream input = new FileInputStream(directory+""+fileName);
			ZipInputStream zis = new ZipInputStream(input);
			while ((ze = zis.getNextEntry()) != null) {
				if (!ze.getName().contains(".OUT")) 
					continue;

				// Get the file
				byte[] b = new byte[2048];
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				int c = 0;
				while ((c = zis.read(b, 0, BUFFER_SIZE)) != -1) {
					baos.write(b, 0, c);
				}
				buildProducts(ze.getName(), new String(baos.toByteArray()), true, map);
			}
			input.close();
			zis.close();
		} catch (IOException e) {
			log.error("Unable to parse zip contents", e);
			throw new ActionException(e);
		}
		return map;
	}
	
	
	/**
	 * Build a map of products out of the supplied files
	 * @param fileDataList
	 * @param isZip
	 * @return
	 */
	private Map<String, NexusProductVO> buildProducts(String fileName, String fileData, boolean isZip, Map<String, NexusProductVO> products) {
		// Build the map of pertinent columns from the supplied files
		String[] rows;
		// Due to differences in how the files are put together zip files need to be handled special when split
		if (fileName.contains(".zip")){
			rows = fileData.split("\n", -1);
		} else {
			rows = fileData.split("\\r?\\n", -1);
		}
		log.debug(fileName +"|"+rows.length);
		String[] headers = rows[0].split(DELIMITER, -1);
		getColumns(Arrays.asList(headers));
		
		NexusProductVO p = null;
		String[] cols;
		
		for (int i=1; i<rows.length; i++) {
			cols = rows[i].split(DELIMITER, -1);
			if (cols.length == 1) continue;
			if (cols.length <headers.length) {
				errors.add("Invalid data at line " + i+1 +" in file " + fileName + ".  Recieved "+cols.length+" columns, expected "+headers.length + " columns.");
				continue;
			}
			
			if (isZip && org != -1 && !"DO,DS,DM,DC".contains(StringUtil.checkVal(cols[org], "DO"))) continue;
			if (isZip && status != -1 && !"AC,CT,DP,DS".contains(StringUtil.checkVal(cols[status], "AC"))) continue;
			if (isZip && region != -1 && !"USA".contains(StringUtil.checkVal(cols[region], "USA"))) continue;
			
			if (products.get(cols[code]) != null) {
				p = products.get(cols[code]);
				updateProduct(p, cols);
				products.put(p.getDocumentId(), p);
			} else {
				p = new NexusProductVO();
				updateProduct(p, cols);
				p.addOrganization("DPY_SYN_NEXUS");
				p.addRole("0");
				products.put(p.getDocumentId(), p);
			}
		}
		headers = null;
		rows=null;
		return products;
	}
	
	
	/**
	 * Get the columns that contain the information that will be used during
	 * the file processing.
	 * @param headerList
	 */
	private void getColumns(List<String> headerList) {
		if (fileName.contains(".zip")) {
			org=headerList.indexOf("SLS_ORG_CO_CD");
			status=headerList.indexOf("STAT_CD");
			region=headerList.indexOf("REG_CD");
			code=headerList.indexOf("PSKU_CD");
			desc=headerList.indexOf("PROD_DESCN_TXT");
			gtin=headerList.indexOf("GTIN_CD");
			gtinLvl=headerList.indexOf("GTIN_TYP_CD");
			device=headerList.indexOf("PRIM_DI");
			use=headerList.indexOf("UNIT_OF_USE");
			dpm=headerList.indexOf("DPM_GTIN_CD");
			qty=headerList.indexOf("PKGEG_LVL_QTY");
			pkg=headerList.indexOf("PKGEG_LVL_CD");
			uom=headerList.indexOf("PKGEG_LVL_LBL_UOM_CD");
		} else {
			org=headerList.indexOf("OperatingCompany");
			status=headerList.indexOf("UNUSED");
			region=headerList.indexOf("UNUSED");
			code=headerList.indexOf("ProductNumber");
			desc=headerList.indexOf("Description");
			gtin=headerList.indexOf("GTIN");
			gtinLvl=headerList.indexOf("GTINType");
			device=headerList.indexOf("PrimaryDI");
			use=headerList.indexOf("unitOfUse");
			dpm=headerList.indexOf("DPMGTINCode");
			qty=headerList.indexOf("UOMQuantity");
			pkg=headerList.indexOf("Packaging Level Code");
			uom=headerList.indexOf("UOM");
		}
	}
	
	
	/**
	 * Update the supplied product with information from a string array
	 * @param p
	 * @param cols
	 */
	private void updateProduct(NexusProductVO p, String[] cols) {
		if (StringUtil.checkVal(p.getOrgId()).length() == 0 && org != -1 && cols[org].length() > 0) {
			p.setOrgId(cols[org]);
			p.setOrgName(organizations.valueOf(cols[org]).getName());
		}
		if (StringUtil.checkVal(p.getProductName()).length() == 0 && code != -1) p.setProductName(cols[code]);
		if (StringUtil.checkVal(p.getSummary()).length() == 0 && desc != -1) p.setSummary(cols[desc]);
		if (gtin != -1 && cols[gtin].length() > 0 && !p.getGtin().contains(cols[gtin])) {
			p.addGtin(cols[gtin]);
			if (gtinLvl != -1 && cols[gtinLvl].length() > 0 && !p.getGtinLevel().contains(cols[gtinLvl]))p.addGtinLevel(cols[gtinLvl]);
			if (uom != -1 && cols[uom].length() > 0 && !p.getUomLevel().contains(cols[uom]))p.addUOMLevel(cols[uom]);
			if (pkg != -1 && cols[pkg].length() > 0 && !p.getPackageLevel().contains(cols[pkg]))p.addPackageLevel(cols[pkg]);
		}
		if (StringUtil.checkVal(p.getPrimaryDeviceId()).length() == 0 && device != -1 && cols[device].length() > 0) {
			p.setPrimaryDeviceId(cols[device]);
			if (!p.getGtin().contains(cols[device])) {
				p.addGtin(cols[device]);
				if (gtinLvl != -1 && cols[gtinLvl].length() > 0 && !p.getGtinLevel().contains(cols[gtinLvl]))p.addGtinLevel(cols[gtinLvl]);
				if (uom != -1 && cols[uom].length() > 0 && !p.getUomLevel().contains(cols[uom]))p.addUOMLevel(cols[uom]);
				if (pkg != -1 && cols[pkg].length() > 0 && !p.getPackageLevel().contains(cols[pkg]))p.addPackageLevel(cols[pkg]);
			}
		}
		if (StringUtil.checkVal(p.getUnitOfUse()).length() == 0 && use != -1)p.setUnitOfUse(cols[use]);
		if (StringUtil.checkVal(p.getDpmGTIN()).length() == 0 && dpm != -1)p.setDpmGTIN(cols[dpm]);
		if (StringUtil.checkVal(p.getQuantity()).length() == 0 && qty != -1)p.setQuantity(Convert.formatInteger(cols[qty]));
		if (StringUtil.checkVal(p.getRegion()).length() == 0 && region != -1)p.setRegion(cols[region]);
		if (StringUtil.checkVal(p.getStatus()).length() == 0 && status != -1)p.setStatus(cols[status]);
		 
		// If we have not received a primary device identifier
		// but we have a GTIN of the valid level we use that instead
		if (StringUtil.checkVal(p.getPrimaryDeviceId()).length() == 0 && pkg != -1 && ("1".equals(cols[pkg]) || "C".equals(cols[pkg]))
				&& gtin != -1 && cols[gtin].length() > 0) {
			p.setPrimaryDeviceId(cols[gtin]);
		}
	}
	
	
	/**
	 * Create and send an email with information pertaining to what was uploaded
	 * and any errors that were run into during the upload.
	 * @param itemsAdded
	 * @param itemsProcessed
	 */
	private void sendAlertEmail(int itemsAdded, int itemsProcessed) {
		try {
			EmailMessageVO mail = new EmailMessageVO();
			mail.addRecipients(props.getProperty("toAddress"));
			mail.setSubject("DePuy NeXus Import Report");
			mail.setFrom(props.getProperty("fromAddress"));
			mail.setHtmlBody(buildBody(itemsAdded, itemsProcessed));

			MessageSender ms = new MessageSender(attributes, dbConn);
			ms.sendMessage(mail);
		} catch (InvalidDataException ide) {
			log.error("could not send contact email", ide);
		}
	}
	
	
	/**
	 * Build the html body of the email
	 * @param itemsAdded
	 * @param itemsProcessed
	 * @return
	 */
	private String buildBody(int itemsAdded, int itemsProcessed) {
		StringBuilder body = new StringBuilder(300);
		
		body.append("DePuy NeXus import has been completed for file").append(fileName).append(".\n");
		body.append(itemsAdded).append(" products were succesfully added to the solr server out of ");
		body.append(itemsProcessed).append(" total products processed from the file\n");
		if (errors.size() > 0) {
			body.append("\nThe following errors occured during the product upload:\n");
			for (String s : errors) {
				body.append(s).append("\n");
			}
		}
		
		return body.toString();
	}
}
