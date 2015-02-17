package com.universal.catalog;

// Java 7
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


// Log4J 1.2.15
import org.apache.log4j.Logger;



//SMT Base Libs
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: OptionsIndexImporter.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Parses a product options index file.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Dec 12, 2014<p/>
 * <b>Changes: </b>
 * Dec 12, 2014: DBargerhuff: created class.
 ****************************************************************************/
public class OptionsIndexImporter extends AbstractImporter {

	private static final Logger log = Logger.getLogger(OptionsIndexImporter.class);
	private final String DELIMITER = ";";

	/**
	 * Maps product ID to a List of String arrays representing the product attributes
	 * combinations hierarchy for the product.  For example, a T-shirt, green, small
	 * might be represented by T;GR;S and would be tokenized into a String array 
	 * where index 0 = "T", index 1 = "GR", index 2 = "S".  In the JSTL, we build 
	 * select lists based on the hierarchy.  "T" would top-level.  Selecting "T" would
	 * return/build a select list of "T"s children (in this case "GR"), and selecting "GR"
	 * would return/build a select list of "GR"s children ("T"s grandchildren, in this case
	 * "S").
	 */
	
	public OptionsIndexImporter() {
	}
			
	/**
	 * Parses options from the options source file. 
	 * @param productFilter
	 * @return Map of raw product ID mapped to a List representing the options
	 * hierarchy levels for that product's options.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public Map<String,List<Map<String,List<String>>>> manageOptionsIndex(List<String> productFilter) 
			throws FileNotFoundException, IOException  {
		log.info("manageOptionsIndex...");
		// load and parse the options index file
		Map<String, List<String[]>> optionsIndex = processOptionsIndex(productFilter);
		Map<String,List<Map<String,List<String>>>> hierarchy = parseOptionsHierarchy(optionsIndex);
		//debugOptionsIndexHierarchy(hierarchy);
		return hierarchy;
	}
	
	/**
	 * Processes the options index source import file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private Map<String, List<String[]>> processOptionsIndex(List<String> productFilter) 
			throws FileNotFoundException, IOException  {
		// establish a Reader on the options file.
		log.info("loadOptionsIndex...");
		BufferedReader data = null;
		String fullPath = catalog.getSourceFilePath() + catalog.getSourceFileName();
		try {
			data = new BufferedReader(new FileReader(fullPath));	
		} catch (FileNotFoundException fnfe) {
			String errMsg = "Options index source file not found!  File path is: " + fullPath;
			log.error(errMsg);
			throw new FileNotFoundException(errMsg);
		}

		String temp = null;
		String prodId = null;
		String prevProdId = null;
		String codeList = null;
		Map<String, Integer> headers = new HashMap<>();
		List<String[]> hierarchy = null;
		Map<String, List<String[]>> optionsIndex = new HashMap<>();
		// read the file line by line
		int availabilityFieldNo = -1;
		try {
			for (int i=0; (temp = data.readLine()) != null; i++) {
				String[] fields = temp.split(CatalogImportManager.DELIMITER_SOURCE);
				// process the header row
				if (i == 0) {
					headers = new HashMap<String, Integer>();
					for (int j = 0; j < fields.length; j++) {
						headers.put(fields[j].toUpperCase(), new Integer(j));
						if (fields[j].equalsIgnoreCase("NLA")) {
							availabilityFieldNo = headers.get("NLA");
						}
						log.info("options index headers | index: " + fields[j] + "|" + j);
					}
					log.info("availabilityFieldNo: " + availabilityFieldNo);
					continue;
				}
				
				// map product ID to the flattened hierachy values 
				try {
					prodId = StringUtil.checkVal(fields[headers.get("SKUID")]);
					codeList = StringUtil.checkVal(fields[headers.get("CODE_LIST")]);
				} catch (Exception e) {
					log.error("Error mapping hierarchy for product | record #: " + prodId + i + ", " + e);
					continue;
				}
				
				// check availability
				if (availabilityFieldNo > -1) {
					try {
						if (fields[availabilityFieldNo].equals("1")) {
							log.debug("skipping product option combination: " + prodId + "|" + codeList);
							continue;
						}
					} catch (Exception e) {
						// log error but process entire field anyway...missing NLA value gives us no guidance.
						log.error("Error retrieving product option availability for: " + prodId + "|" + codeList + ", " + e.getMessage());
					}
				}				
				
				// only process options for products that were imported upstream
				if (productFilter.contains(prodId)) {
					String[] cTokens = codeList.split(DELIMITER);
					if (prodId.equals(prevProdId)) {
						// add to list for this product
						hierarchy.add(cTokens);
						
					} else {
						// product ID changed, do cleanup on previous product
						if (hierarchy != null) {
							optionsIndex.put(prevProdId, hierarchy);
						}
						
						// initialize hierarchy list and populate with tokens from this record
						hierarchy = new ArrayList<>();
						hierarchy.add(cTokens);
					}
				}
				
				prevProdId = prodId;
				
			}

			// pick up the dangling hierarchy if it exists.
			if (hierarchy != null) {
				optionsIndex.put(prevProdId, hierarchy);
			}
			
		} finally {
			try {
				data.close();
			} catch (Exception e) {
				log.error("Error closing BufferedReader on file: " + fullPath);
			}
		}
		
		return optionsIndex;
	}
	
	/**
	 * Parses the options index hierarchy for each product and builds a List that
	 * contains Maps of product ID mapped to a List of that product's 
	 * @return List of hierarchy parents mapped to their children.  The index of
	 * this List represents the hierarchy level (e.g. 0 = top-level parent, etc.)
	 */
	private Map<String, List<Map<String, List<String>>>> 
		parseOptionsHierarchy(Map<String, List<String[]>> optionsIndex) {
		
		Map<String,List<Map<String, List<String>>>> prodParentChildren = new HashMap<>();
		int depth = 0;
		// loop the optionsIndex and parse into product|parent/children maps
		for (String pId : optionsIndex.keySet()) {
			depth = optionsIndex.get(pId).get(0).length;
			List<Map<String, List<String>>> parentChildren = new ArrayList<>();
			switch(depth) {
				case 1:
					// if depth is 1, there are no children, loop the Map and add an empty
					// List to parentChildren
					Map<String, List<String>> m = new LinkedHashMap<>();
					for (int i = 0; i < optionsIndex.get(pId).size(); i++) {
						m.put(optionsIndex.get(pId).get(i)[0], new ArrayList<String>(1));
					}
					parentChildren.add(m);
					break;
				case 2:
					parentChildren.add(parseLevel(optionsIndex.get(pId),0,1));
					addDummyMap(parentChildren, parentChildren.size() - 1);
					break;
				case 3:
					parentChildren.add(parseLevel(optionsIndex.get(pId),0,1));
					parentChildren.add(parseLevel(optionsIndex.get(pId),1,2));
					addDummyMap(parentChildren, parentChildren.size() - 1);
					break;
				default:
					continue;
			}
			
			prodParentChildren.put(pId, parentChildren);
			
		}
		return prodParentChildren;
	}
	
	/**
	 * Builds a Map for the children who have no children to map them to empty
	 * ArrayLists. 
	 * @param parentChildren
	 * @param level
	 */
	private void addDummyMap(List<Map<String, List<String>>> parentChildren, int level) {
		// add a map of empty lists for the last children who have no children
		Map<String, List<String>> lastLevelMap = parentChildren.get(level);
		Map<String, List<String>> dummyLevelMap = new LinkedHashMap<>(1);
		// loop the last level map and create a map of the children who have no children
		for (String lastLevelKey : lastLevelMap.keySet()) {
			List<String> lastLevelList = lastLevelMap.get(lastLevelKey);
			for (String lastVal : lastLevelList) {
				dummyLevelMap.put(lastVal, new ArrayList<String>(1));
			}
		}
		parentChildren.add(dummyLevelMap);
	}
	
	/**
	 * Parses the parent-child hierarchy for the parent-child level
	 * represented by the 'idx' parameters passed in.
	 * @param hierarchy
	 * @param idxParent
	 * @param idxChild
	 * @return Returns a Map representing the parent mapped to its children
	 */
	private Map<String, List<String>> parseLevel(List<String[]> hierarchy, 
			int idxParent, int idxChild) {
		
		Map<String, List<String>> parentChildren = null;
		List<String> children = null;
		
		String parent = null;
		String prevParent = null;
		String child = null;
		String prevChild = null;
		
		// loop hierarchy String values and process values for specified indices
		for (String[] s : hierarchy) {
			parent = s[idxParent];
			child = s[idxChild];
			
			// process parent/children
			if (parent.equals(prevParent)) {
				if (! child.equals(prevChild)) {
					children.add(child);
				}
				
			} else {
				// map parent to children
				if (parentChildren == null) {
					parentChildren = new LinkedHashMap<>();
				} else {
					parentChildren.put(prevParent, children);
				}
				// reset children list
				children = new ArrayList<>();
				children.add(child);
			}
			// set flags
			prevParent = parent;
			prevChild = child;
			
		}
		
		// pick up the dangling record
		if (parentChildren != null) {
			parentChildren.put(prevParent, children);
		}
		
		return parentChildren;
	}

	/**
	 * Utility method for debugging the options index hierarchy.
	 * @param optionsIndexHierarchy
	 */
	@SuppressWarnings("unused")
	private void debugOptionsIndexHierarchy(Map<String,List<Map<String,List<String>>>> optionsIndexHierarchy) {
		log.debug("debugging options index hierarchy...");
		log.debug("optionsIndexHierarchy map size is: " + optionsIndexHierarchy.size());
		for (String pId : optionsIndexHierarchy.keySet()) {
			//if (pId.equals("HP3418")) {
				log.debug("productId: "+ pId);
				
				List<Map<String,List<String>>> pChild = optionsIndexHierarchy.get(pId);
				log.debug("hierarchy levels: " + pChild.size());
	
				for (int i = 0; i < pChild.size(); i++) {
					log.debug("expanding hierarchy level " + i + "...");
					Map<String,List<String>> level = pChild.get(i);
					log.debug("level map size is: " + level.size());
					for (String parent : level.keySet()) {
						log.debug("parent val: " + parent);
						List<String> children = level.get(parent);
						log.debug("children: " + children);
						for (String child : level.get(parent)) {
							log.debug("----> child: " + child);
						}
					}
				}
			//}
		}
	}
	
}
