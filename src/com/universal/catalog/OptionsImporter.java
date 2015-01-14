package com.universal.catalog;

// JDK 7
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;



// Log4J 1.2.15
import org.apache.log4j.Logger;



//SMT Base Libs
import com.siliconmtn.commerce.catalog.ProductAttributeVO;
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
 * Dec 11, 2014: DBargerhuff: changed source field used for importing attribute cost
 ****************************************************************************/
public class OptionsImporter extends AbstractImporter {

	private static final Logger log = Logger.getLogger(OptionsImporter.class);
	private static final String ATTRIBUTE_PREFIX = "USA_";
	private Set<String> misMatchedOptions = null;
	private Set<String> misMatchedAttributes = null;
	private Map<String,List<Map<String,List<String>>>> optionsIndexHierarchy = null;
	
	public OptionsImporter() {
		misMatchedOptions = new LinkedHashSet<>();
		misMatchedAttributes = new LinkedHashSet<>();
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
	 * @throws FileNotFoundException
	 * @throws IOException
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
			String[] fields = temp.split(CatalogImportManager.DELIMITER_SOURCE);
			// process the header row
			if (i == 0) {
				headers = new HashMap<String, Integer>();
				for (int j = 0; j < fields.length; j++) {
					headers.put(fields[j].toUpperCase(), new Integer(j));
					log.info("options headers | index: " + fields[j] + "|" + j);
				}
				continue;
			}
			
			// grab the option value and stash it in a List if not already there
			try {
				option = StringUtil.checkVal(fields[headers.get("TABLETYPE")]).toLowerCase();
				if (option.length() == 0) continue;
			} catch (Exception e) {
				//log.error("Skipping doodad options record #: " + i + ", " + e);
				continue;
			}
			
			// format option value
			option = option.replace(" ", "");
			option = option.replace("-", "");
			option = StringUtil.formatFileName(option);
			
			// add to list if not already there
			if (! options.contains(option)) options.add(option);
		}
		
		try {
			data.close();
		} catch (Exception e) {
			log.error("Error closing BufferedReader on file: " + fullPath);
		}
		
		List<String> optionsToInsert = null;
		try {
			optionsToInsert = buildInsertList(options);
			if (optionsToInsert.size() > 0) {
				insertNewOptions(optionsToInsert);
			} else {
				log.info("No new options found to insert into the PRODUCT_ATTRIBUTE table.");
			}
		} catch (SQLException sqle) {
			log.error("Warning: Unable to check options for insertion, ", sqle);
		}
	}
	
	/**
	 * Retrieves a list of existing options, loops the list of options found in the import and 
	 * checks this list against the list of existing options.  If an option in the imported list is
	 * not found in the list of existing options, that option is added to the list of options 
	 * to import.
	 * @param importedOptions
	 * @return
	 * @throws SQLException
	 */
	private List<String> buildInsertList(List<String> importedOptions) throws SQLException {
		StringBuilder sb = new StringBuilder();
		sb.append("select ATTRIBUTE_NM from PRODUCT_ATTRIBUTE where ORGANIZATION_ID = ?");
		
		List<String> existingOptions = new ArrayList<>();
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, "USA");
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				existingOptions.add(rs.getString(1));
			}
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) { log.error("Error closing PreparedStatement, ", e); }
			}
		}
		
		List<String> optionsToInsert = new ArrayList<>();
		for (String opt : importedOptions) {
			if (! existingOptions.contains(opt)) {
				optionsToInsert.add(opt);
			}
		}

		return optionsToInsert;
	}
		
	
	/**
	 * Attempt to insert the options.  Most, if not all of these will already exist in the PRODUCT_ATTRIBUTE table so
	 * we will catch the exceptions within this method and handle them here.
	 * @param options
	 * @throws SQLException 
	 */
	private void insertNewOptions(List<String> options) throws SQLException {
		StringBuilder sb = new StringBuilder();
		sb.append("insert into product_attribute (ATTRIBUTE_ID, ORGANIZATION_ID, ATTRIBUTE_NM, ");
		sb.append("ACTIVE_FLG, TYPE_NM, URL_ALIAS_TXT, CREATE_DT) values (?,?,?,?,?,?,?)");
		
		PreparedStatement ps = dbConn.prepareStatement(sb.toString());
		String optUpper = null;
		int ctr = 0;
		for (String opt : options) {
			optUpper = CatalogImportManager.ATTRIBUTE_PREFIX + opt.toUpperCase();
			try {
				ps.setString(1, optUpper);
				ps.setString(2, "USA");
				ps.setString(3, opt);
				ps.setInt(4, 1);
				ps.setString(5, "HTML");
				ps.setString(6,  optUpper);
				ps.setTimestamp(7,Convert.getCurrentTimestamp());
				ps.execute();
				ctr++;
				log.info("Added new option to product attribute table: " + opt);
			} catch (Exception e) {
				log.error("Unable to add new option to the product attribute table, " + opt + ", " + e.getMessage());
			}
		}
		
		log.info("Inserted " + ctr + " new options into the PRODUCT_ATTRIBUTE table.");
		
		if (ps != null) {
			try {
				ps.close();
			} catch (Exception e) {log.error("Error closing PrepraredStatement, ", e);}
		}
	}
	
	/**
	 * Inserts product-options associations into the appropriate table.
	 * @param catalogSourcePath
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws SQLException
	 */
	public void insertProductOptionsDEPRECATED() 
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
		String guid = null;
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
			
			double attribCost = formatAttributeCost(fields, headers);
			guid = new UUIDGenerator().getUUID();
			try {
				ps.setString(1, guid);	//product_attribute_id
				ps.setString(2, attribId); //attribute_id
				ps.setString(3, prodId);	//product_id
				ps.setString(4, catalog.getCatalogModelYear());	//model_year_no
				ps.setString(5, fields[headers.get("CODE")]);	// value_txt
				ps.setTimestamp(6, Convert.getCurrentTimestamp());	//create_dt
				ps.setString(7, "dollars");	//curr_type
				ps.setDouble(8, attribCost);		//msrp_cost_no
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
					log.error("misMatched attribute ID at line: " + (i + 1) + " in input file:prodId/attribId: " + prodId + "/" + attribId);
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
		//log.info("attrib before|after: " + attrib + "|" + newAttr);
		return newAttr;
	}
	
	/**
	 * Formats an attribute's cost using the specified source column value.
	 * @param aCost
	 * @return
	 */
	private double formatAttributeCost(String[] fields, Map<String, Integer> headers) {
		try {
			if (headers.get("PRICECHANGE") == null) return 0.00;
			return Double.parseDouble(StringUtil.checkVal(fields[headers.get("PRICECHANGE")],"0.00"));
		} catch (ArrayIndexOutOfBoundsException ae) {
			return 0.00;
		} catch (NumberFormatException nfe) {
			return 0.00;
		}
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
	
	/**
	 * @return the optionsIndexHierarchy
	 */
	public Map<String, List<Map<String, List<String>>>> getOptionsIndexHierarchy() {
		return optionsIndexHierarchy;
	}

	/**
	 * @param optionsIndexHierarchy the optionsIndexHierarchy to set
	 */
	public void setOptionsIndexHierarchy(
			Map<String, List<Map<String, List<String>>>> optionsIndexHierarchy) {
		this.optionsIndexHierarchy = optionsIndexHierarchy;
	}

	/**
	 * Inserts product-options associations into the appropriate table. 
	 * @param productFilter
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void insertProductOptions(List<String> productFilter) 
			throws FileNotFoundException, IOException  {
		BufferedReader data = null;
		String fullPath = catalog.getSourceFilePath() + catalog.getSourceFileName();
		try {
			data = new BufferedReader(new FileReader(fullPath));	
		} catch (FileNotFoundException fnfe) {
			String errMsg = "Options source file not found!  File path is: " + fullPath;
			log.error(errMsg);
			throw new FileNotFoundException(errMsg);
		}

		// initialize vars
		String temp = null;
		String prevSrcProdId = null;
		String srcProdId =  null;
		String prodId = null;
		String prevProdId = null;
		String attribSelectLvl = null; //corresponds to a List index value
		String prevAttribSelectLvl = null;
		int attribSelectOrder = 0;
		Map<String, Integer> headers = null;
		ProductAttributeVO option = null;
		Map<String,ProductAttributeVO> levelMap = null;
		List<Map<String, ProductAttributeVO>> levels = null;
		//List<List<Map<String, ProductAttributeVO>>> hLevels = null;
		Map<String, List<Map<String,ProductAttributeVO>>> prodAttrHierarchy = null;
		List<String> skipList = new ArrayList<>();
		// loop file contents and process
		try {
			for (int i=0; (temp = data.readLine()) != null; i++) {
				// parse fields
				String[] fields = temp.split(catalog.getSourceFileDelimiter());
				
				// parse headers
				if (i == 0) {
					headers = parseHeaderRow(fields);
					continue; // skip to next row
				}
				
				// format product ID and set select level
				srcProdId = fields[headers.get("SKU")];
				if (srcProdId.indexOf("_") > -1) {
					attribSelectLvl = srcProdId.substring(srcProdId.length() - 1);
					prodId = srcProdId.substring(0, (srcProdId.length() - 2));
				} else {
					attribSelectLvl = "0";
					prodId = srcProdId;
				}
				
				// filter raw product ID against list of valid product IDs for this catalog
				if (productFilter.contains(prodId)) {

					// attribId: retrieve and format
					String attribId = null;
					try { // enclosed in try/catch in case of index array out of bounds due to missing field val.
						attribId = fields[headers.get("TABLETYPE")];
					} catch (Exception e) {
						log.error("WARNING: No attribute type for product | row: " + prodId + " | " + i);
					}
					if (attribId == null || attribId.length() == 0) continue;
					attribId = this.formatAttribute(attribId);
					
					// attribute VO : build and populate
					option = new ProductAttributeVO();
					if (attribSelectLvl.equals("0") || attribSelectLvl.equals("1")) {
						// set ID for top-level attributes
						option.setProductAttributeId(new UUIDGenerator().getUUID());
					}
					option.setAttributeId(attribId);
					option.setProductId(catalog.getCatalogPrefix() + prodId); // this is the prefixed product ID
					option.setModelYearNo(catalog.getCatalogModelYear());
					option.setValueText(fields[headers.get("CODE")]);
					option.setCurrencyType("dollars");
					option.setMsrpCostNo(formatAttributeCost(fields, headers));
					option.setAttribute(ProductAttributeVO.ATTRIB_1, fields[headers.get("DESCRIPTION")]);
					option.setAttribute(ProductAttributeVO.ATTRIB_2, attribSelectLvl);
					
					/*
					// Set option's display order number
					if (srcProdId.equals(prevSrcProdId)) {
						if (attribSelectLvl.equals(prevAttribSelectLvl)) {
							// increment select order
							option.setDisplayOrderNo(++attribSelectOrder);
						} else {
							// reset select order and level list
							option.setDisplayOrderNo(0);
						}
					} */
					
					// Now determine how to store the option
					if (prodId.equals(prevProdId)) {
						// same product, check to see if level changed
						if (srcProdId.equals(prevSrcProdId)) {
							if (attribSelectLvl.equals(prevAttribSelectLvl)) {
								// increment select order
								option.setDisplayOrderNo(++attribSelectOrder);
							} else {
								// reset select order
								attribSelectOrder = 0;
								option.setDisplayOrderNo(attribSelectOrder);
							}
						} else {
							// level has changed, add current levelMap to the levels List
							levels.add(levelMap);
							// initialize the levelMap
							levelMap = new LinkedHashMap<>();
							// reset select order
							attribSelectOrder = 0;
							option.setDisplayOrderNo(attribSelectOrder);
						}
						
						// add this option to the new levelMap
						levelMap.put(option.getValueText(), option);						
						
					} else {
						// product changed
						if (prodAttrHierarchy == null) { // initialize map the first time through
							log.debug("initializing prodAttrHierarchy");
							prodAttrHierarchy = new HashMap<>();
						} else {
							// add the previous product ID's levels to the master map
							//log.debug("adding levels for prodId: " + prevProdId);
							if (! prodAttrHierarchy.containsKey(prevProdId)) {
								levels.add(levelMap);
								prodAttrHierarchy.put(prevProdId, levels);
								//if (prevProdId.equals("PS9982")) log.debug("stored hierarchy for prodId|size: " + prevProdId + "|" + levels.size());
							} else {
								log.error("ALERT: Skipping out-of-sequence options hierarchy for product: " + prevProdId);
								//levels.add(levelMap);
								//processOutOfSequenceLevels(prodAttrHierarchy.get(prevProdId), levels);								
							}
						}
						
						// initialize the levels List
						levels = new ArrayList<>();
						// initialize the levelMap
						levelMap = new LinkedHashMap<>();
						
						// reset displayOrderNo
						attribSelectOrder = 0;
						option.setDisplayOrderNo(attribSelectOrder);
						
						// add option to level map
						levelMap.put(option.getValueText(), option);
						
					}
				} else {
					// skip options for products not imported for this catalog
					//log.error("Skipping options: product ID mismatch: " + prodId);
					if (! skipList.contains(prodId)) skipList.add(prodId);
					continue;
				}
				
				// reset previous prod id
				prevSrcProdId = srcProdId;
				prevProdId = prodId;
				prevAttribSelectLvl = attribSelectLvl;
			}
			
			// pick up the dangling attribute
			if (levels != null && ! levels.isEmpty()) {
				//hLevels.add(level);
				prodAttrHierarchy.put(prevSrcProdId, levels);
			}
			
		} finally {
			try {
				data.close();
			} catch (Exception e) { log.error("Error closing BufferedReader, ", e); }
		}
		Map<String, List<ProductAttributeVO>> voHierarchy = buildInsertVOs(prodAttrHierarchy);
		//debugVOHierarchy(voHierarchy);
		insertProductOptions(voHierarchy);
	}
	
	/**
	 * Processes an out-of-sequence options hierarchy using the hierarchy for a product
	 * that was built previously in the entire process.  Attempts to match levels and 
	 * adds attributes to the appropriate level in the appropriate order.
	 * @param existingLvls
	 * @param newLvls
	 */
	@SuppressWarnings("unused")
	private void processOutOfSequenceLevels(List<Map<String, ProductAttributeVO>> existingLvls, 
			List<Map<String, ProductAttributeVO>> newLvls) {
		log.debug("processOutOfSequenceLevels...");
		int existLevelsSize = existingLvls.size();
		int currAttribLevel = -1;
		log.debug("existing levels size: " + existLevelsSize);
		
		// loop the new levels, figure out which level sequence
		for (Map<String, ProductAttributeVO> newLvl : newLvls) {
						
			// loop through the map keys, get the current new level no from the
			// first object, then break and process.
			if (currAttribLevel == -1) {
				for (String newKey : newLvl.keySet()) {
					ProductAttributeVO newPavo = newLvl.get(newKey);
					// get the real level of this map of levels
					currAttribLevel = Convert.formatInteger(newPavo.getAttribute2(), -1);
					break;
				}
				// if we didn't find a valid val, skip this data
				if (currAttribLevel == -1) break;
			}
			
			log.debug("currAttribLevel is: " + currAttribLevel);
			if (currAttribLevel <= existLevelsSize) {
				if (existLevelsSize == 0) {
					// means currAttribLevel is 0, just add it
					existingLvls.add(newLvl);
				} else {
					// this level exists, get it and add to it
					processSpecificLevel(existingLvls.get(currAttribLevel - 1), newLvl);
				}
								
			} else {
				// currNewLevelNo is a new level, just add it
				existingLvls.add(newLvl);
			}
			
		}
		
	}
	
	/**
	 * Adds hierarchy level values to an existing hierarchy level.
	 * @param oldLevel
	 * @param newLevel
	 */
	private void processSpecificLevel(Map<String, ProductAttributeVO> oldLevel, 
			Map<String, ProductAttributeVO> newLevel) {
		// add new level to old level
		int cnt = 0;
		int lastOrderNo = 0;
		// get the highest display order level no
		for (String key : oldLevel.keySet()) {
			cnt++;
			if (cnt == oldLevel.size()) {
				lastOrderNo = oldLevel.get(key).getDisplayOrderNo();
			}
		}
		// loop new level, reset display order no, add to old level map
		for (String newKey : newLevel.keySet()) {
			newLevel.get(newKey).setDisplayOrderNo(++lastOrderNo);
			oldLevel.put(newKey, newLevel.get(newKey));
		}
	}
	
	/**
	 * Loops the product options hierarchy index and builds the hierarchy using 
	 * ProductAttributeVOs.  Child product attribute VOs are populated with the 
	 * appropriate parent product attribute ID.
	 * @param attribs
	 * @return 
	 */
	private Map<String, List<ProductAttributeVO>> buildInsertVOs(Map<String, 
			List<Map<String,ProductAttributeVO>>> attribs) {
		log.debug("buildInsertVOs...");
		// loop index
		Map<String,List<ProductAttributeVO>> master = new HashMap<>();
		Map<String,Integer> voCount = new LinkedHashMap<>();
		int totalVOs = 0;
		for (String pId : optionsIndexHierarchy.keySet()) {
			//log.debug("building insert VOs for product ID: " + pId);
			
			// List of this product's attribute VOs for insert
			List<ProductAttributeVO> attVOs = new ArrayList<>();
			List<Map<String,List<String>>> oHierarchy = optionsIndexHierarchy.get(pId);
			List<Map<String,ProductAttributeVO>> aHierarchy = null;
			if (attribs.get(pId) == null) continue; // no attribs, skip this product. 
			aHierarchy = attribs.get(pId);
			
			// loop top level options and make kids
			Map<String, List<String>> parents = oHierarchy.get(0);
			Map<String, ProductAttributeVO> parentsAttribs = aHierarchy.get(0);
			for (String parentKey : parents.keySet()) {
				//log.debug("*** processing parent: " + parentKey);
				//log.debug("oHierarchy|aHierarchy sizes: " + oHierarchy.size() + "|" + aHierarchy.size());
				ProductAttributeVO parentVO = parentsAttribs.get(parentKey);
				 if (parentVO == null) {
					//log.debug("skipping this parent, no ProductAttributeVO found.");
					continue;
				}
				attVOs.add(parentVO);
				//log.debug("productAttributeId: " + parentVO.getProductAttributeId());
				List<String> children = parents.get(parentKey);
				//log.debug("------> children: " + children);
				if (children != null && ! children.isEmpty()) {
					//log.debug("-----------> parent has children: " + children);
					if (oHierarchy.size() > 1 && aHierarchy.size() > 1) {
						makeChildren(attVOs, oHierarchy, aHierarchy, 
								oHierarchy.get(1), aHierarchy.get(1), 
								parentVO.getProductAttributeId(), children, 1);
					}
					
				}
				
			}
			
			//log.debug("product's product attribute VO list size: " + attVOs.size());
			totalVOs = totalVOs + attVOs.size();
			master.put(pId, attVOs);
			voCount.put(pId,  attVOs.size());
		}
		log.debug("total VO size: " + totalVOs);
		log.debug("master map size: " + master.size());

		return master;
	}
	
	/**
	 * Recursively creates the children ProductAttributeVOs for a given parent.
	 * @param attVOs
	 * @param oHier
	 * @param aHier
	 * @param productId
	 * @param kidsOptions
	 * @param kidsAttribs
	 * @param currParentId
	 * @param currOptionLevel
	 */
	private void makeChildren(
			List<ProductAttributeVO> attVOs,
			List<Map<String, List<String>>> oHier,
			List<Map<String, ProductAttributeVO>> aHier,
			//Map<String, String> childIdMap,
			Map<String, List<String>> childOptions, 
			Map<String,ProductAttributeVO> childAttribs, 
			String currParentId, 
			List<String> parentsChildren,
			int currOptionLevel) {
		//log.debug("**** entering makeChildren:currOptionLevel: " + currOptionLevel);
		if (parentsChildren != null && ! parentsChildren.isEmpty()) {
			// loop the kids
			//log.debug("children passed in: " + parentsChildren);
			for (String child : parentsChildren) {
				if (childAttribs.get(child) == null) continue;
				//log.debug("-------> adding child: " + child);
				ProductAttributeVO newChild = cloneProductAttribute(childAttribs.get(child), currParentId);
				attVOs.add(newChild);
			
				// check for grandkids...
				List<String> grandKids = childOptions.get(child);
				if (grandKids != null && ! grandKids.isEmpty()) {
					//log.debug("---------> found children of this child: " + grandKids);
					if (oHier.size() > (currOptionLevel + 1) && aHier.size() > (currOptionLevel + 1)) {
						makeChildren(attVOs, oHier, aHier, 
								oHier.get(currOptionLevel + 1), aHier.get(currOptionLevel + 1), 
								newChild.getProductAttributeId(), grandKids, currOptionLevel + 1);
					}
				}
			}
			
		}
		//log.debug("**** exiting makeChildren...");
	}
	
	/**
	 * Inserts product options records into the product attribute xr table.
	 * @param voHierarchy
	 */
	private void insertProductOptions(Map<String, List<ProductAttributeVO>> voHierarchy) {
		log.debug("insertProductOptions...");
		if (voHierarchy == null || voHierarchy.isEmpty()) return;
		
		StringBuilder s = new StringBuilder();
		s.append("insert into product_attribute_xr (product_attribute_id, parent_id, attribute_id, ");
		s.append("product_id, model_year_no, value_txt, create_dt, currency_type_id, ");
		s.append("msrp_cost_no, attrib1_txt, attrib2_txt, order_no) values (?,?,?,?,?,?,?,?,?,?,?,?)");
		log.debug("product option attribute XR SQL: " + s.toString());
		
		PreparedStatement ps = null;
		int recCount = 0;
		int[] batchCount = null;
		int pCount = 0;
		try {
			int idx = 1;
			ps = dbConn.prepareStatement(s.toString());
			for (String hKey : voHierarchy.keySet()) {
				//if (hKey.equals("CE2838")) log.debug("inserting options XR for product ID: " + hKey);
				int limit = voHierarchy.get(hKey).size();
				pCount = 0;
				for (ProductAttributeVO pavo : voHierarchy.get(hKey)) {
					/* if (hKey.equals("CE2838")) {
						log.debug("building insert: productId|productAttribId|parentId|attrib2|value: " + pavo.getProductId() + "|" + pavo.getProductAttributeId() +"|"+pavo.getParentId()+"|"+pavo.getAttribute2()+"|"+pavo.getValueText());
					} */
					idx = 1;
					ps.setString(idx++, pavo.getProductAttributeId());
					ps.setString(idx++, pavo.getParentId());
					ps.setString(idx++, pavo.getAttributeId());
					ps.setString(idx++, pavo.getProductId());
					ps.setString(idx++, pavo.getModelYearNo());
					ps.setString(idx++, pavo.getValueText());
					ps.setTimestamp(idx++, Convert.getCurrentTimestamp());
					ps.setString(idx++, pavo.getCurrencyType().name());
					ps.setDouble(idx++, pavo.getMsrpCostNo());
					ps.setString(idx++, pavo.getAttribute1());
					ps.setString(idx++, pavo.getAttribute2());
					ps.setInt(idx++, pavo.getDisplayOrderNo());
					ps.addBatch();
					pCount++;
					recCount++;
					//log.debug("pCount: " + pCount);
					if (pCount == limit) {
						try {
							batchCount = ps.executeBatch();
							//if (hKey.equals("CE2838")) { log.debug("added records at batch count " + recCount); }
						} catch (SQLException sqle) {
							if (batchCount != null) {
								int start = recCount - 200;
								log.error("Error during options XR insert between record " + start + " and " + recCount);
								log.error("Error message is: " + sqle.getMessage());
								log.error("Looping batchCount[]: ");
								for (int cnt = 0; cnt < batchCount.length; cnt++) {
									log.debug("Record #|result: " + (start + 1) + "|" + batchCount[cnt]);
								}
							} else {
								log.error("Error inserting entire batch, failure reason: " + sqle.getMessage());
							}
						}
					}
					
				}
				
			}
			
		} catch (SQLException sqle) {
			log.error("Error inserting options XR records, ", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) { log.error("Error closing PreparedStatement, " + e.getMessage()); }
		}
		log.debug("inserted record count: " + recCount);
	}
	
	/**
	 * Makes a new ProductAttributeVO based on the child, setting the parent's
	 * product attribute ID as the child's parent ID, and setting the
	 * @param child 
	 * @param parentId
	 * @param productId
	 * @return
	 */
	private ProductAttributeVO cloneProductAttribute(ProductAttributeVO child, String parentId) {
		// copy the child, set parent's product attribute ID as the parent ID
		ProductAttributeVO newVO = new ProductAttributeVO();
		newVO.setActiveFlg(child.getActiveFlg());
		newVO.setAttributeGroupId(child.getAttributeGroupId());
		newVO.setAttributeId(child.getAttributeId());
		newVO.setAttributeName(child.getAttributeName());
		newVO.setAttributeType(child.getAttributeType());
		newVO.setAttributes(child.getAttributes());
		newVO.setCatalogId(child.getCatalogId());
		newVO.setDisplayOrderNo(child.getDisplayOrderNo());
		newVO.setModelYearNo(child.getModelYearNo());
		newVO.setMsrpCostNo(child.getMsrpCostNo());
		newVO.setOrganizationId(child.getOrganizationId());
		newVO.setParentId(parentId);
		newVO.setProductAttributeId(new UUIDGenerator().getUUID());
		newVO.setProductId(child.getProductId());
		newVO.setTitle(child.getTitle());
		newVO.setUrlAlias(child.getUrlAlias());
		newVO.setValueText(child.getValueText());
		return newVO;
	}
	
	/**
	 * Utility method for debugging the vo parent-child relationships (IDs).
	 * @param vos
	 */
	@SuppressWarnings("unused")
	private void debugVOHierarchy(Map<String, List<ProductAttributeVO>> vos) {
		for (String pId : vos.keySet()) {
			log.debug("Product ID: " + pId);
			for (ProductAttributeVO pavo : vos.get(pId)) {
				log.debug("-----> pAttrId|parentId|level|value: " + pavo.getProductAttributeId() + "|" + pavo.getParentId() + "|" + pavo.getAttribute2() + "|" + pavo.getValueText());
			}
		}
	}
	
}
