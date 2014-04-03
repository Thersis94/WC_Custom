package com.universal.util;

// JDK 7
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Properties;

// Log4J 1.2.15
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

//SMT Base Libs
import com.siliconmtn.http.filter.FileUploadFilter;
import com.siliconmtn.io.FileManagerFactoryImpl;
import com.siliconmtn.io.FileManagerIntfc;
import com.siliconmtn.io.http.SMTHttpConnectionManager;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: CatalogRetriever.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Retrieves the source files for a catalog and validates that they exist.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Apr 01, 2014<p/>
 * <b>Changes: </b>
 * Apr 01, 2014: DBargerhuff: created class.
 ****************************************************************************/
public class CatalogRetriever {

	private static final Logger log = Logger.getLogger(CatalogRetriever.class);
	private Properties config;
	private String destinationPath;
	private String today;
	
	/**
	 * Constructor
	 * @param config
	 */
	public CatalogRetriever(Properties config) {
		this.config = config;
	}
		
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		PropertyConfigurator.configure("scripts/USA_Auto_Importer_log4j.properties");
		Properties config = new Properties();
		config.put("sourceFileDestination", "C:\\Temp\\USA_cat_test\\");
		config.put("sourceURL", "http://www.signals.com/");
		String[] fileList = {"sm_categories.txt","sm_products.txt","sm_options.txt","sm_personalization.txt"};
		CatalogRetriever cr = new CatalogRetriever(config);
		try {
			log.info("retrieving files...");
			String destinationPath= cr.retrieveCatalogForImport("SIGNALS_CATALOG", config.getProperty("sourceURL"), fileList);
			log.info("fiinished retrieving files...wrote them to: " + destinationPath);
		} catch (IOException ioe) {
			log.error("Error retrieving files, " + ioe.getMessage());
		}
		
		
	}
	
	/**
	 * 	Retrieves and stores catalog import files onto the file system.  Loops the 
	 * file list, retrieves catalog source files, and writes each one to the destinationPath.
	 * @param catalogId
	 * @param sourceURL
	 * @param fileList
	 * @return  a String value representing the folder path where each file was stored
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public String retrieveCatalogForImport(String catalogId, String sourceURL, String[] fileList)
			throws FileNotFoundException, IOException {
		log.info("retrieving catalog source files...");
		buildDestinationPath(catalogId);
				
		String fileUrl = null;
		byte[] fileData = null;
		
		SMTHttpConnectionManager hConn = new SMTHttpConnectionManager();
		hConn.setConnectionTimeout(30000);
		// loop sources enum
		for (String fileName : fileList) {
			fileUrl = sourceURL + fileName;
			log.info("retrieving " + fileUrl);
			try {
				fileData = hConn.retrieveData(fileUrl);
				switch(hConn.getResponseCode()) {
					case 200:
						if (fileData != null) {
							writeSourceFile(fileData, destinationPath, fileName);
						}
						break;
					default:
						log.info("No import files found for this catalog.");
						throw new FileNotFoundException("No import files found for this catalog.");
				}
			} catch (IOException ioe) {
				// if we can't load a file, we bail, import fails.
				log.error("Error retrieving catalog source file: " + fileUrl + ", " + ioe);
				throw new IOException(ioe.getMessage());
			}
			
		}
		return destinationPath;
	}

	/**
	 * Writes a byte array as a file on the file system.
	 * @param fileData
	 * @param filePath
	 * @throws IOException
	 */
	private void writeSourceFile(byte[] fileData, String filePath, String fileName) 
			throws IOException {
		String fullPath = filePath + fileName;
		log.info("writing source file: " + fullPath);
		//create destination folder
		createFolderLocation(filePath);
		
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		fos = new FileOutputStream(fullPath);
		bos = new BufferedOutputStream(fos);
		// write file
		bos.write(fileData);
		bos.flush();
		// clean up
		bos.close();
		fos.close();
	}
	
	/**
	 * Creates the folder to store the file into
	 * @param fpdb
	 * @throws FileUploadException
	 */
	private void createFolderLocation(String destinationPath) throws IOException {
		FileManagerIntfc fm = null;
		try {
			fm = FileManagerFactoryImpl.getInstance(FileUploadFilter.FILE_MANAGER_TYPE);
			fm.makeDir(true, destinationPath);
		} catch (Exception e) {
			throw new IOException("Unable to create the destination folder path: " + destinationPath, e);
		}
	}
	
	/**
	 * Builds/formats the source file destination path for this catalog
	 * @param catalogId
	 */
	private void buildDestinationPath(String catalogId) {
		today = getToday();
		destinationPath = config.getProperty("sourceFileDestination") + today + "\\" + catalogId + "\\";
	}
	
	/**
	 * Helper method, get's 'today' as a String in the format YYYY-MM-DD
	 * @return
	 */
	private String getToday() {
		Calendar cal = GregorianCalendar.getInstance();
		return Convert.formatDate(cal.getTime(), Convert.DATE_DASH_PATTERN);
	}

}
