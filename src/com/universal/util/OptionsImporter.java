package com.universal.util;

// JDK 7
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Log4J 1.2.15
import org.apache.log4j.Logger;

//SMT Base Libs
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: OptionsImporter.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Parses a product options file, inserts any new options into the options table, 
 * and updated product-options associations.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Apr 03, 2014<p/>
 * <b>Changes: </b>
 * Apr 03, 2014: DBargerhuff: created class.
 ****************************************************************************/
public class OptionsImporter {

	private static final Logger log = Logger.getLogger(OptionsImporter.class);
	private Connection dbConn;
	
	/**
	 * Constructor
	 */
	public OptionsImporter(Connection dbConn) {
		this.dbConn = dbConn;
	}
		
	/**
	 * Parses options from the options source file. 
	 * @param products
	 * @param catalogSourcePath
	 * @param optionsSourceFile
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws SQLException
	 */
	public void manageOptions(String catalogSourcePath, String optionsSourceFile) 
			throws FileNotFoundException, IOException, SQLException  {
		// establish a Reader on the options file.
		BufferedReader data = null;
		String fullPath = catalogSourcePath + optionsSourceFile;
		try {
			data = new BufferedReader(new FileReader(fullPath));	
		} catch (FileNotFoundException fnfe) {
			String errMsg = "Options source file not found!  File path is: " + fullPath;
			log.error(errMsg);
			throw new FileNotFoundException(errMsg);
		}

		String temp = null;
		String option = null;
		Map<String, Integer> headers = new HashMap<>();
		List<String> options = new ArrayList<>();
		// read the file line by line
		for (int i=0; (temp = data.readLine()) != null; i++) {
			String[] fields = temp.split(USACatalogImporter.DELIMITER_SOURCE);
			// process the header row
			if (i == 0) {
				headers = new HashMap<String, Integer>();
				for (int j = 0; j < fields.length; j++) {
					headers.put(fields[j].toUpperCase(), new Integer(j));
				}
				continue;
			}
			
			// grab the option value and stash it in a List if not already there
			option = StringUtil.checkVal(fields[headers.get("TABLETYPE")]).toLowerCase();
			if (option.length() == 0) continue;
			option = option.replace(" ", "");
			option = option.replace("-", "");
			option = StringUtil.formatFileName(option);
			
			if (! options.contains(option)) {
				options.add(option);
			}
		}
		
		try {
			data.close();
		} catch (Exception e) {
			log.error("Error closing BufferedReader on file: " + fullPath);
		}
		
		insertOptions(options);
	}
		
	
	/**
	 * Attempt to insert the options.  Most, if not all of these will already exist in the PRODUCT_ATTRIBUTE table so
	 * we will catch the exceptions within this method and handle them here.
	 * @param options
	 * @throws SQLException 
	 */
	private void insertOptions(List<String> options) throws SQLException {
		StringBuilder sb = new StringBuilder();
		sb.append("insert into product_attribute (ATTRIBUTE_ID, ORGANIZATION_ID, ATTRIBUTE_NM, ");
		sb.append("ACTIVE_FLG, TYPE_NM, URL_ALIAS_TXT, CREATE_DT) values (?,?,?,?,?,?,?)");
		
		PreparedStatement ps = dbConn.prepareStatement(sb.toString());
		String optUpper = null;
		for (String opt : options) {
			optUpper = USACatalogImporter.ATTRIBUTE_PREFIX + opt.toUpperCase();
			try {
				ps.setString(1, optUpper);
				ps.setString(2, "USA");
				ps.setString(3, opt);
				ps.setInt(4, 1);
				ps.setString(5, "HTML");
				ps.setString(6,  optUpper);
				ps.setTimestamp(7,Convert.getCurrentTimestamp());
				ps.execute();
			} catch (Exception e) {
				log.error("Option already exists, " + e.getMessage());
			}
		}
		
		if (ps != null) {
			try {
				ps.close();
			} catch (Exception e) {log.error("Error closing PrepraredStatement, ", e);}
		}
	}
	
}
