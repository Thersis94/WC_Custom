package com.depuysynthes.action.nexus;

// SMT Base Libs
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.solr.client.solrj.impl.HttpSolrServer;

import com.depuy.datafeed.SFTPClient;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SecurityController;
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
	
	/**
	 * Delimiter used in the EXP file
	 */
	private String DELIMITER;
	// FTP variables
	private String user;
	private String password;
	private String fileName;
	private String hostName;
	private String directory;
	// Column names for pertinent data
	private String orgCol;
	private String orgNmCol;
	private String codeCol;
	private String descCol;
	private String statusCol;
	private String gtinCol;
	private String gtinLevelCol;
	private String deviceCol;
	private String useCol;
	private String dpmCol;
	private String quantityCol;
	private String packageCol;
	private String uomCol;
	private String regionCol;
    
	
	/**
	 * Initializes the Logger, config files and the database connection
	 * @throws Exception
	 */
	public NexusImporter(String[] args) {
		super(args);
		loadProperties("scripts/Nexus.properties");
		prepareValues();
	}
	
	
	/**
	 * Set up the class values and determine which file headers we are going
	 * to need to use in order get the product information from the supplied files
	 */
	private void prepareValues() {
		user = props.getProperty("user");
		password = props.getProperty("password");
		fileName = props.getProperty("fileName");
		hostName = props.getProperty("hostName");
		directory = props.getProperty("directory");
		
		if (fileName.contains(".zip")) {
			 orgCol = "SLS_ORG_CO_CD";
			 orgNmCol = "PROVR_SHRT_NM";
			 codeCol = "PSKU_CD";
			 descCol = "PROD_DESCN_TXT";
			 statusCol = "STAT_CD";
			 gtinCol = "GTIN_CD";
			 gtinLevelCol = "GTIN_TYP_CD";
			 deviceCol = "PRIM_DI";
			 useCol = "UNIT_OF_USE";
			 dpmCol = "DPM_GTIN_CD";
			 quantityCol = "PKGEG_LVL_QTY";
			 packageCol = "PKGEG_LVL_CD";
			 uomCol = "PKGEG_LVL_LBL_UOM_CD";
			 regionCol = "REG_CD";
			 DELIMITER = ",";
		} else {
			 orgCol = "OperatingCompany";
			 orgNmCol = "OperatingCompany";
			 codeCol = "ProductNumber";
			 descCol = "Description";
			 statusCol = "unusedStatus";
			 gtinCol = "GTIN";
			 gtinLevelCol = "GTINType";
			 deviceCol = "PrimaryDI";
			 useCol = "unitOfUse";
			 dpmCol = "DPMGTINCode";
			 quantityCol = "UOMQuantity";
			 packageCol = "Packaging Level Code";
			 uomCol = "UOM";
			 regionCol = "unusedRegion";
			 DELIMITER = "\\|";
		}
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
			Map<String, String> fileData = getFileData();
			Map<String, NexusProductVO> products = buildProducts(fileData,isZip);
	
			// initialize the connection to the solr server
			HttpSolrServer server = new HttpSolrServer(props.getProperty(Constants.SOLR_BASE_URL)+props.getProperty(Constants.SOLR_COLLECTION_NAME));
			SolrActionUtil solr = new SolrActionUtil(server);
			
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
				} catch (Exception e) {
					log.error("Unable to add products to solr", e);
				}
			}
			
			try {
				server.commit();
			} catch (Exception e) {
				log.error("Unable to commit dangling products", e);
			}
		} catch(ActionException e) {
			log.error("Failed to complete transaction", e);
		}
	}
	
	/**
	 * Get the file from the mbox and create a map out of the contained data
	 * @return
	 * @throws ActionException 
	 */
	private Map<String, String> getFileData() throws ActionException {
		log.debug("Getting file from mbox");
		Map<String, String> fileData = new TreeMap<>();
		
		SFTPClient ftp = null;
		try {
			ftp = new SFTPClient(hostName, user, password);
			log.debug("Getting File " + directory + "/" + fileName + " at " + Convert.getCurrentTimestamp());
			if (fileName.contains(".zip")) {
				getFilesFromZip(fileData, ftp.getFileData(directory+"/"+fileName));
			} else {
				fileData.put(fileName, new String(ftp.getFileData(directory+"/"+fileName)));
			}
		} catch (IOException e) { 
			log.error("Unable to get data from file", e);
			throw new ActionException(e);
		}
		return fileData;
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
				if (!ze.getName().contains(".csv")) 
					continue;

				// Get the file
				byte[] b = new byte[2048];
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				int c = 0;
				while ((c = zis.read(b, 0, BUFFER_SIZE)) != -1) {
					baos.write(b, 0, c);
				}
				fileData.put(ze.getName(), new String(baos.toByteArray()));
				
			}
		} catch (IOException e) {
			log.error("Unable to parse zip contents", e);
			throw new ActionException(e);
		}
	}
	
	/**
	 * Build a map of products out of the supplied files
	 * @param fileDataList
	 * @param isZip
	 * @return
	 */
	private Map<String, NexusProductVO> buildProducts(Map<String, String> fileDataList, boolean isZip) {
		Map<String, NexusProductVO> products = new HashMap<>();
		
		// Build the map of pertinent columns from the supplied files
		for (String fileName : fileDataList.keySet()) {
			String[] rows;
			// Due to differences in how the files are put together zip files need to be handled special when split
			if (fileName.contains(".zip")){
				rows = fileDataList.get(fileName).split("\n");
			} else {
				rows = fileDataList.get(fileName).split("\\r?\\n");
			}
			if (rows.length < 2) continue;
			String[] headers = rows[0].split(DELIMITER);
			Map<String, List<String>> data = prepareDataMap();
			

			for (int j=0; j<headers.length; j++) {
				if (!data.containsKey(headers[j])) continue;
				
				// If this column is not going to be put in the vo we skip it.
				for (int i=1; i<rows.length; i++) {
					String[] cols = rows[i].split(DELIMITER);
						data.get(headers[j]).add(cols[j]);
					}
				
			}
			
			// Loop over the columns recieved from the files and turn them into productvos
			String productCode = "";
			NexusProductVO p = null;
			for (int i=0; i<rows.length-1; i++) {
				if (products.get(getColData(i, data.get(codeCol))) != null) {
					// If we have already created this product we update it with any new information
					// This will only occur when we are getting information from multiple files
					p = products.get(getColData(i, data.get(codeCol)));
					productCode = p.getDocumentId();
					updateColData(p, data, i);
				} else if (!productCode.equals(getColData(i, data.get(codeCol)))) {
					// Since this is a new product we add the old one, if any, to the final list
					// and create the new product
					if (p != null) {
						products.put(p.getDocumentId(), p);
						productCode = getColData(i, data.get(codeCol));
					}
					p = new NexusProductVO();
					p.setOrgId(getColData(i, data.get(orgCol)));
					p.setOrgName(getColData(i, data.get(orgNmCol)));
					p.setProductName(getColData(i, data.get(codeCol)));
					p.setSummary(getColData(i, data.get(descCol)));
					p.addGtin(getColData(i, data.get(gtinCol)));
					p.addGtinLevel(getColData(i, data.get(gtinLevelCol)));
					p.setPrimaryDeviceId(getColData(i, data.get(deviceCol)));
					p.setUnitOfUse(getColData(i, data.get(useCol)));
					p.setDpmGTIN(getColData(i, data.get(dpmCol)));
					if (getColData(i, data.get(quantityCol)) != null)
						p.setQuantity(Convert.formatInteger(data.get(quantityCol).get(i)));
					p.setPackageLevel(getColData(i, data.get(packageCol)));
					p.addUOMLevel(getColData(i, data.get(uomCol)));
					p.addRole(StringUtil.checkVal(SecurityController.PUBLIC_ROLE_LEVEL));
					p.addOrganization(props.getProperty("organization"));
					p.setRegion(getColData(i, data.get(regionCol)));
					p.setStatus(getColData(i, data.get(statusCol)));
					 
					// If we have not received a primary device identifier
					// but we have a GTIN of the valid level we use that instead
					if (StringUtil.checkVal(p.getPrimaryDeviceId()).length() == 0 && "C".equals(getColData(i, data.get(gtinLevelCol)))) {
						p.setPrimaryDeviceId(getColData(i, data.get(gtinCol)));
					}
					
				} else {
					// We already have the product we are working with so we only need
					// to add the gtin information
					p.addGtin(getColData(i, data.get(gtinCol)));
					p.addGtinLevel(getColData(i, data.get(gtinLevelCol)));
					p.addUOMLevel(getColData(i, data.get(uomCol)));
					
					// If we have not received a primary device identifier
					// but we have a GTIN of the valid level we use that instead
					if (StringUtil.checkVal(p.getPrimaryDeviceId()).length() == 0 && "C".equals(getColData(i, data.get(gtinLevelCol)))) {
						p.setPrimaryDeviceId(getColData(i, data.get(gtinCol)));
					}
				}
				
			}
			// Add the dangling record
			if (p != null) {
				products.put(p.getDocumentId(), p);
			}
		}
		return products;
	}
	
	/**
	 * Check all fields in the product vo and check to see if the information on this row
	 * covers anything that has not already been set by a previous row
	 * @param p
	 * @param data
	 * @param i
	 */
	private void updateColData(NexusProductVO p, Map<String, List<String>> data, int i) {
		if (StringUtil.checkVal(p.getOrgName()).length() == 0)
			p.setOrgName(getColData(i, data.get(orgNmCol)));
		if (StringUtil.checkVal(p.getOrgId()).length() == 0)
			p.setOrgId(getColData(i, data.get(orgCol)));
		if (StringUtil.checkVal(p.getSummary()).length() == 0)
			p.setSummary(getColData(i, data.get(descCol)));
		if (!p.getGtin().contains(getColData(i, data.get(gtinCol)))) {
			p.addGtin(getColData(i, data.get(gtinCol)));
			p.addUOMLevel(getColData(i, data.get(uomCol)));
			p.addGtinLevel(getColData(i, data.get(gtinLevelCol)));
		}
		if (StringUtil.checkVal(p.getPrimaryDeviceId()).length() == 0)
			p.setPrimaryDeviceId(getColData(i, data.get(deviceCol)));
		if (StringUtil.checkVal(p.getUnitOfUse()).length() == 0)
			p.setUnitOfUse(getColData(i, data.get(useCol)));
		if (StringUtil.checkVal(p.getDpmGTIN()).length() == 0)
			p.setDpmGTIN(getColData(i, data.get(dpmCol)));
		if (getColData(i, data.get(quantityCol)) != null)
			p.setQuantity(Convert.formatInteger(data.get(quantityCol).get(i)));
		if (StringUtil.checkVal(p.getPackageLevel()).length() == 0)
			p.setPackageLevel(getColData(i, data.get(packageCol)));
		if (StringUtil.checkVal(p.getRegion()).length() == 0)
			p.setRegion(getColData(i, data.get(regionCol)));
		if (StringUtil.checkVal(p.getStatus()).length() == 0)
			p.setStatus(getColData(i, data.get(statusCol)));
		if (StringUtil.checkVal(p.getPrimaryDeviceId()).length() == 0 && "C".equals(getColData(i, data.get(gtinLevelCol)))) {
			p.setPrimaryDeviceId(getColData(i, data.get(gtinCol)));
		}
	}


	/**
	 * Since not all files will contain all the information that could be loaded
	 * into the vo we check to make sure that the column that we are querying 
	 * is capable of containing the row we want to get
	 * @param row
	 * @param col
	 * @return
	 */
	private String getColData(int row, List<String> col) {
		if (row >= col.size()) return null;
		return col.get(row);
	}


	/**
	 * Prepare the list of columns that will contain the file data
	 * @return
	 */
	private Map<String, List<String>> prepareDataMap() {
		Map<String, List<String>> data = new HashMap<>();

		data.put(orgCol, new ArrayList<String>());
		data.put(orgNmCol, new ArrayList<String>());
		data.put(codeCol, new ArrayList<String>());
		data.put(descCol, new ArrayList<String>());
		data.put(statusCol, new ArrayList<String>());
		data.put(gtinCol, new ArrayList<String>());
		data.put(gtinLevelCol, new ArrayList<String>());
		data.put(deviceCol, new ArrayList<String>());
		data.put(useCol, new ArrayList<String>());
		data.put(dpmCol, new ArrayList<String>());
		data.put(quantityCol, new ArrayList<String>());
		data.put(packageCol, new ArrayList<String>());
		data.put(uomCol, new ArrayList<String>());
		data.put(regionCol, new ArrayList<String>());
		
		return data;
	}
}
