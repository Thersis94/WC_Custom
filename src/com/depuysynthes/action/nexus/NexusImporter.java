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

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;

import com.depuy.datafeed.SFTPClient;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
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
		//loadDBConnection(props);
	}
	
	
	private void prepareValues() {

		user = props.getProperty("user");
		password = props.getProperty("password");
		fileName = props.getProperty("fileName");
		hostName = props.getProperty("hostName");
		directory = props.getProperty("directory");
		
		if (fileName.contains(".zip")) {
			 orgCol = "SLS_ORG_CO_CD";
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
		boolean isZip = props.getProperty("fileName").contains(".zip");
		Map<String, String> fileData = getFileData();
		Map<String, NexusProductVO> products = buildProducts(fileData,isZip);

		// initialize the connection to the solr server
		
		HttpSolrServer server = new HttpSolrServer(props.getProperty(Constants.SOLR_BASE_URL)+props.getProperty(Constants.SOLR_COLLECTION_NAME));
		SolrActionUtil solr = new SolrActionUtil(server);
		int counter = 0;
		try {
			server.deleteByQuery(SearchDocumentHandler.INDEX_TYPE + ":DEPUY_NEXUS");
		} catch (SolrServerException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		for (String key : products.keySet()) {
			NexusProductVO p = products.get(key);
			try {
				System.out.println(p.getOrgName()+"|"+p.getStatus()+"|"+p.getRegion());
				if (isZip && (!"DO,DS,DM,DC".contains(StringUtil.checkVal(p.getOrgName(), "SKIP")) ||
						!"AC,CT,DP,DS".contains(StringUtil.checkVal(p.getStatus(), "SKIP")) ||
						!"USA".equals(StringUtil.checkVal(p.getRegion(), "SKIP")))) {
					continue;
				}
				
				
				
				if (counter == 5000) {
					server.commit();
					counter = 0;
				}
				solr.addDocument(p);
				counter++;
			} catch (ActionException | SolrServerException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			server.commit();
		} catch (SolrServerException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	private Map<String, String> getFileData() {
		System.out.println("Getting file from mbox");
		Map<String, String> fileData = new TreeMap<>();
		
		SFTPClient ftp = null;
		try {
			ftp = new SFTPClient(hostName, user, password);
			System.out.println("Getting File " + directory + "/" + fileName + " at " + Convert.getCurrentTimestamp());
			if (fileName.contains(".zip")) {
				getFilesFromZip(fileData, ftp.getFileData(directory+"/"+fileName));
			} else {
				fileData.put(fileName, new String(ftp.getFileData(directory+"/"+fileName)));
			}
		} catch (IOException e) { 
			e.printStackTrace();
		}
		return fileData;
	}
	
	
	private void getFilesFromZip(Map<String, String> fileData, byte[] zipData) {
		System.out.println("Got File From mbox at " + Convert.getCurrentTimestamp());
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private Map<String, NexusProductVO> buildProducts(Map<String, String> fileDataList, boolean isZip) {
		Map<String, NexusProductVO> products = new HashMap<>();
		
		for (String fileName : fileDataList.keySet()) {
			System.out.println(fileName);
			String[] rows = fileDataList.get(fileName).split("\n");
			if (rows.length < 2) continue;
			String[] headers = rows[0].split(DELIMITER);
			System.out.println(headers.length);
			System.out.println(rows.length);
			Map<String, List<String>> data = prepareDataMap();
			

			for (int j=0; j<headers.length; j++) {
				if (!data.containsKey(headers[j])) continue;
				// If this column is not going to be put in the vo we skip it.
				for (int i=1; i<rows.length; i++) {
					String[] cols = rows[i].split(DELIMITER);
						data.get(headers[j]).add(cols[j]);
					}
				
			}
			System.out.print("Created headers: ");
			for(String header : headers) System.out.print(header+"|");
			System.out.println("");
			
			String productCode = "";
			NexusProductVO p = null;
			for (int i=0; i<rows.length-1; i++) {
				if (products.get(getColData(i, data.get(codeCol))) != null) {
					System.out.println("Updating product " + getColData(i, data.get(codeCol)));
					updateColData(products.get(getColData(i, data.get(codeCol))), data, i);
					productCode = getColData(i, data.get(codeCol));
				} else if (!productCode.equals(getColData(i, data.get(codeCol)))) {
					if (p != null) {
						products.put(p.getDocumentId(), p);
						System.out.println("Added " + p.getDocumentId());
						productCode = getColData(i, data.get(codeCol));
					}
					p = new NexusProductVO();
					p.setOrgName(getColData(i, data.get(orgCol)));
					p.setProductName(getColData(i, data.get(codeCol)));
					p.setSummary(getColData(i, data.get(descCol)));
					p.addGtin(getColData(i, data.get(gtinCol)));
					p.setGtinLevel(getColData(i, data.get(gtinLevelCol)));
					p.setPrimaryDeviceId(getColData(i, data.get(deviceCol)));
					p.setUnitOfUse(getColData(i, data.get(useCol)));
					p.setDpmGTIN(getColData(i, data.get(dpmCol)));
					if (getColData(i, data.get(quantityCol)) != null)
						p.setQuantity(Convert.formatInteger(data.get(quantityCol).get(i)));
					p.setPackageLevel(getColData(i, data.get(packageCol)));
					p.setUomLevel(getColData(i, data.get(uomCol)));
					p.addRole("0");
					p.addOrganization("DPY_SYN");
					p.setRegion(getColData(i, data.get(regionCol)));
					p.setState(getColData(i, data.get(statusCol)));
				} else {
					p.addGtin(getColData(i, data.get(gtinCol)));
				}
				
			}
			if (p != null) {
				products.put(p.getDocumentId(), p);
			}
		}
		return products;
	}
	
	
	private void updateColData(NexusProductVO p, Map<String, List<String>> data, int i) {
		if (StringUtil.checkVal(p.getOrgName()).length() == 0)
			p.setOrgName(getColData(i, data.get(orgCol)));
		if (StringUtil.checkVal(p.getSummary()).length() == 0)
			p.setSummary(getColData(i, data.get(descCol)));
		if (!p.getGtin().contains(getColData(i, data.get(gtinCol))))
			p.addGtin(getColData(i, data.get(gtinCol)));
		if (StringUtil.checkVal(p.getGtinLevel()).length() == 0)
			p.setGtinLevel(getColData(i, data.get(gtinLevelCol)));
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
		if (StringUtil.checkVal(p.getUomLevel()).length() == 0)
			p.setUomLevel(getColData(i, data.get(uomCol)));
		if (StringUtil.checkVal(p.getRegion()).length() == 0)
			p.setRegion(getColData(i, data.get(regionCol)));
		if (StringUtil.checkVal(p.getStatus()).length() == 0)
			p.setStatus(getColData(i, data.get(statusCol)));
	}


	private String getColData(int row, List<String> col) {
		if (row >= col.size()) return null;
		return col.get(row);
	}


	private Map<String, List<String>> prepareDataMap() {
		Map<String, List<String>> data = new HashMap<>();

		data.put(orgCol, new ArrayList<String>());
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
