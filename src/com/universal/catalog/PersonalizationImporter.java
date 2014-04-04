package com.universal.catalog;

// JDK 7
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Log4J 1.2.15
import org.apache.log4j.Logger;

//SMT Base Libs
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;

/****************************************************************************
 * <b>Title</b>: PersonalizationImporter.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Parses a personalization options file, inserts any new options into the options table, 
 * and updated product-options associations.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Apr 04, 2014<p/>
 * <b>Changes: </b>
 * Apr 04, 2014: DBargerhuff: created class.
 ****************************************************************************/
public class PersonalizationImporter extends AbstractImporter {

	private static final Logger log = Logger.getLogger(PersonalizationImporter.class);
	private Set<String> misMatchedPersonalization = null;
	
	public PersonalizationImporter() {
		misMatchedPersonalization = new HashSet<>();
	}
	
	/**
	 * Constructor
	 */
	public PersonalizationImporter(Connection dbConn) {
		this();
		this.dbConn = dbConn;
	}
	
	/** 
	 * @param catalog
	 * @param products
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws SQLException
	 */
	public void managePersonalization(CatalogImportVO catalog, List<ProductVO> products) 
			throws FileNotFoundException, IOException, SQLException  {
		StringBuilder sb = new StringBuilder();
		sb.append("insert into product_attribute_xr (product_attribute_id, attribute_id, ");
		sb.append("product_id, model_year_no, value_txt, create_dt, currency_type_id, msrp_cost_no, attrib1_txt, attrib2_txt, attrib3_txt) ");
		sb.append("values (?,?,?,?,?,?,?,?,?,?,?)");
		
		BufferedReader data = null;
		String fullPath = catalog.getSourceFilePath() + catalog.getSourceFileName();
		try {
			data = new BufferedReader(new FileReader(fullPath));
		} catch (FileNotFoundException fnfe) {
			String errMsg = "Personalization source file not found, file path: " + fullPath;
			log.error(errMsg);
			throw new FileNotFoundException(errMsg);
		}

		int ctr = 0;
		PreparedStatement ps = dbConn.prepareStatement(sb.toString());
		String temp = null;
		Map<String, Integer> headers = null;
		for (int i=0; (temp = data.readLine()) != null; i++) {
			String[] fields = temp.split(catalog.getSourceFileDelimiter());
			if (i == 0) {
				headers = parseHeaderRow(fields);
				continue; // skip to next row
			}
			String prodId = catalog.getCatalogPrefix() + fields[headers.get("CUSTOM")];
			try {
				ps.setString(1, new UUIDGenerator().getUUID());	//product_attribute_id
				ps.setString(2, "USA_CUSTOM");	//attribute_id
				ps.setString(3, prodId);	//product_id
				ps.setString(4, catalog.getCatalogModelYear());	//model_year_no
				ps.setString(5, fields[headers.get("DATA")]); //value_txt
				ps.setTimestamp(6, Convert.getCurrentTimestamp());	//create_dt
				ps.setString(7, "DOLLARS");	//currency_type_id
				ps.setInt(8, 0);	//msrp_cost_no
				ps.setString(9, fields[headers.get("PROMPT")]);		//attrib1_txt
				ps.setString(10, fields[headers.get("MAXLENGTH")]);	//attrib2_txt
				if(fields.length > 5) {
					ps.setString(11, fields[headers.get("REQUIRED")]);	//attrib3_txt
				} else {
					ps.setString(11, "0");	//attrib3_txt
				}
				ps.executeUpdate();
				ctr++;

			} catch (Exception e) {
				if (e.getMessage().contains("column 'PRODUCT_ID'")) {
					//cause = "product ID foreign key not found";
					misMatchedPersonalization.add(prodId);
				} else {
					log.error("Error adding personalization option for product: " + prodId + ", " + e.getMessage());
				}
			}
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
		log.info("Personalization options added: " + ctr);
		
	}

	/**
	 * @return the misMatchedPersonalization
	 */
	public Set<String> getMisMatchedPersonalization() {
		return misMatchedPersonalization;
	}

	/**
	 * @param misMatchedPersonalization the misMatchedPersonalization to set
	 */
	public void setMisMatchedPersonalization(Set<String> misMatchedPersonalization) {
		this.misMatchedPersonalization = misMatchedPersonalization;
	}
	
	
}
