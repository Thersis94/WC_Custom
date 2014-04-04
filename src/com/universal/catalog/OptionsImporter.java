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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Log4J 1.2.15
import org.apache.log4j.Logger;

//SMT Base Libs
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

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
public class OptionsImporter extends AbstractImporter {

	private static final Logger log = Logger.getLogger(OptionsImporter.class);
	private static final String ATTRIBUTE_PREFIX = "USA";
	private Set<String> misMatchedOptions = null;
	private Set<String> misMatchedAttributes = null;
	
	public OptionsImporter() {
		misMatchedOptions = new HashSet<>();
		misMatchedAttributes = new HashSet<>();
	}
	
	/**
	 * Constructor
	 */
	public OptionsImporter(Connection dbConn) {
		this();
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
	public void manageOptions() 
			throws FileNotFoundException, IOException  {
		// establish a Reader on the options file.
		BufferedReader data = null;
		String fullPath = catalog.getSourceFilePath() + catalog.getSourceFileName();
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
		
		try {
			insertOptions(options);
		} catch (SQLException sqle) {
			// log but nothing else
			log.error("Warning: Unable to set PreparedStatement, " + sqle.getMessage());
		}
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
	
	/**
	 * Inserts product-options associations into the appropriate table. 
	 * @param products
	 * @param catalogSourcePath
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws SQLException
	 */
	public void insertProductOptions(List<ProductVO> products) 
			throws FileNotFoundException, IOException, SQLException  {
		BufferedReader data = null;
		String fullPath = catalog.getSourceFilePath() + catalog.getSourceFileName();
		try {
			data = new BufferedReader(new FileReader(fullPath));	
		} catch (FileNotFoundException fnfe) {
			String errMsg = "Options source file not found!  File path is: " + fullPath;
			log.error(errMsg);
			throw new FileNotFoundException(errMsg);
		}

		StringBuilder s = new StringBuilder();
		s.append("insert into product_attribute_xr (product_attribute_id, attribute_id, ");
		s.append("product_id, model_year_no, value_txt, create_dt, currency_type_id, ");
		s.append("msrp_cost_no, attrib1_txt, attrib2_txt, order_no) values (?,?,?,?,?,?,?,?,?,?,?)");
		
		int ctr = 0;
		PreparedStatement ps = dbConn.prepareStatement(s.toString());
		String temp = null;
		String prevSrcProdId = "";
		String attribSelectLvl = null; //corresponds to a List index value
		int attribSelectOrder = 0;
		Map<String, Integer> headers = null;
		for (int i=0; (temp = data.readLine()) != null; i++) {
			attribSelectLvl = "0";
			String[] fields = temp.split(catalog.getSourceFileDelimiter());
			if (i == 0) {
				headers = parseHeaderRow(fields);
				continue; // skip to next row
			}
			String srcProdId = fields[headers.get("SKU")];
			String prodId = null;
			if (srcProdId.indexOf("_") > -1) {
				attribSelectLvl = srcProdId.substring(srcProdId.length() - 1);
				prodId = srcProdId.substring(0, (srcProdId.length() - 2));
			} else {
				prodId = srcProdId;
			}
			// add prefix to prodId
			prodId = catalog.getCatalogPrefix() + prodId;
			if (srcProdId.equals(prevSrcProdId)) {
				attribSelectOrder++; // increment select order
			} else {
				attribSelectOrder = 0; // reset select order
			}
			// retrieve attribId
			String attribId = null;
			try { // enclosed in try/catch in case of index array out of bounds due to missing field val.
				attribId = fields[headers.get("TABLETYPE")];
			} catch (Exception e) {
				log.error("Error getting product attribute ID value, source field is blank for product ID: " + prodId);
			}
			attribId = this.formatAttribute(attribId);
			if (attribId == null) continue;
			try {
				ps.setString(1, new UUIDGenerator().getUUID());	//product_attribute_id
				ps.setString(2, attribId); //attribute_id
				ps.setString(3, prodId);	//product_id
				ps.setString(4, catalog.getCatalogModelYear());	//model_year_no
				ps.setString(5, fields[headers.get("CODE")]);	// value_txt
				ps.setTimestamp(6, Convert.getCurrentTimestamp());	//create_dt
				ps.setString(7, "dollars");	//curr_type
				ps.setInt(8, Convert.formatInteger(fields[headers.get("PRICEMOD")]));		//msrp_cost_no
				ps.setString(9, fields[headers.get("DESCRIPTION")]);	//attrib1
				ps.setString(10, attribSelectLvl); //attrib2
				ps.setInt(11, attribSelectOrder); //order_no
				ps.executeUpdate();
				ctr++;

			} catch (Exception e) {
				String cause = null;
				if (e.getMessage().contains("column 'PRODUCT_ID'")) {
					//cause = "product ID foreign key not found";
					misMatchedOptions.add(prodId);
				} else if (e.getMessage().contains("column 'ATTRIBUTE_ID'")) {
					//cause = "attribute ID foreign key not found";
					log.error("misMatcheded attribute ID at line: " + (i + 1) + " in input file:prodId/attribId: " + prodId + "/" + attribId);
					misMatchedAttributes.add(attribId);
				} else {
					cause = e.getMessage();
					log.error("Error adding attribute XR: prodId/attribId: " + prodId + "/" + attribId + ", " + cause);
				}
			}
			prevSrcProdId = srcProdId;
		}

		try {
			data.close();
		} catch (Exception e) {
			log.error("Error closing BufferedReader, ", e);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {log.error("Error closing PreparedStatement, ", e);}
			}
		}
		log.info("Options added: " + ctr);
	}
	
	/**
	 * Formats an attribute String
	 * @param attrib
	 * @return
	 */
	private String formatAttribute(String attrib) {
		String newAttr = null;
		if (attrib != null && attrib.length() > 0) {
			newAttr = attrib.replace(" ", "");
			newAttr = newAttr.replace("-", "");
			newAttr = StringUtil.formatFileName(newAttr);
			newAttr = ATTRIBUTE_PREFIX + newAttr.toUpperCase();	
		}
		return newAttr;
	}

	/**
	 * @return the misMatchedOptions
	 */
	public Set<String> getMisMatchedOptions() {
		return misMatchedOptions;
	}

	/**
	 * @return the misMatchedAttributes
	 */
	public Set<String> getMisMatchedAttributes() {
		return misMatchedAttributes;
	}
	
	
}
