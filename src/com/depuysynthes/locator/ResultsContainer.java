package com.depuysynthes.locator;

// Java 7
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


// Google Gson lib
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

// SMTBaseLibs 2.0
import com.siliconmtn.data.Node;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title: </b>ResultsContainer.java <p/>
 * <b>Project: </b>WC_Custom <p/>
 * <b>Description: </b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2016<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0<p/>
 * @since Feb 10, 2016<p/>
 *<b>Changes: </b>
 * Feb 10, 2016: David Bargerhuff: Created class.
 ****************************************************************************/
public class ResultsContainer implements Serializable {

	private static final long serialVersionUID = 3940243103678420431L;
	private static final String FILTER_VALUE_DELIMITER_REGEX = "\\|";
	private static final double RADIUS_SEARCH_DEFAULT = 50;
	private static final double RADIUS_EXTENDED_SEARCH_DEFAULT = 251;
	private String json;
	private int numResults;
	private int pageSize;
	private int specialtyId;
	private int procedureId;
	private int productId;
	private String resultCode;
	private String message;
	/**
	 * Hierarchy of specialties, procedures (children of specialties), and 
	 * products (children of procedures). Specialty/procedure/product IDs are 
	 * set on Node.nodeId property as String values and are set on Node.depthLevel
	 * as int values for flexibility.  Names are set on the Node.nodeName field.
	 */
	private List<Node> hierarchy;
	/**
	 * List of surgeons retrieved by locator search.  SurgeonBeans are composed
	 * of a List of LocationBeans which contain that surgeon's location associations.
	 */
	private List<SurgeonBean> results;

	private boolean isExtendedSearch;

	/**
	 * Boolean indicating whether or not the results being displayed are from the
	 * extended results set (results > than the requested search radius). This is 
	 * used to inform the JSTL view as to whether or not we had to consult the
	 * extended results set. */
	private boolean useExtendedResults;

	// JSTL helper members
	private int startVal = 1; // starting result number
	private int endVal = 1; // ending result number
	private int resultsPerPage = 3; // results per page
	private int currentPageNo = 1; // current page number
	private int lastPageNo = 1; // last page number
	private int displayTotalNo; // total number of results to display
	/**
	 * radius: search radius
	 */
	private double radius;

	/**
	 * extendedRadius: The maximum allowed search radius to use in filtering
	 * results from search.  Defaults to RADIUS_EXTENDED_SEARCH_DEFAULT 
	 * if not set. Defaults to RADIUS_EXTENDED_SEARCH_MAXIMUM if set to value
	 * greater than RADIUS_EXTENDED_SEARCH_MAXIMUM.
	 * 
	 *  Currently unused. Exists for backwards compatiblity.
	 */
	@SuppressWarnings("unused")
	private double extendedRadius;
	
	// filter members
	/**
	 * List of specialty IDs that are being filtered on. 
	 */
	private List<Integer> specFilters;
	/**
	 * List of procedures that a surgeon must match in order to be displayed
	 */
	private List<Integer> procFilters;
	/** 
	 * List of products that a surgeon must match in order to be displayed 
	 * in search results.
	 */
	private List<Integer> prodFilters;
	/**
	 * List of surgeon IDs to exclude from display.
	 */
	private List<Integer> filteredSurgeonList;
	private List<Integer> procFilterParents;
	private List<Integer> globalSpecFilters;

	public ResultsContainer() {
		hierarchy = new ArrayList<>();
		results = new ArrayList<>();
		specFilters = new ArrayList<>();
		procFilters = new ArrayList<>();
		prodFilters = new ArrayList<>();
		filteredSurgeonList = new ArrayList<>();
		procFilterParents = new ArrayList<>(5);
		globalSpecFilters = new ArrayList<>(2);
	}

	/**
	 * Receives a JSON string representing search results and calls the main 
	 * parsing method to parse the JSON into its component parts.
	 * @param json
	 */
	public void setJson(String json) {
		this.json = json;
		parseResults();
	}

	/**
	 * Called when bean is instantiated, parses search results JSON, sets
	 * various display values.
	 * @param json
	 */
	private void parseResults() {
		JsonParser parser = new JsonParser();
		JsonElement jEle = null;
		try {
			jEle = parser.parse(json);
		} catch (Exception e) {
			return;
		}
		// get the results container as an object
		JsonObject ctnr = jEle.getAsJsonObject();
		// parse specialty/procedure/product hierarchy
		parseHierarchy(ctnr.getAsJsonArray("hierarchy"));
		/* NOTE: Must parse override params BEFORE parsing surgeons because 
		 * override params can affect how surgeon records are parsed for display. */
		parseOverrideParams(ctnr);
		// parse surgeon results
		parseSurgeons(ctnr.getAsJsonArray("results"));

	}

	/**
	 * Parses the JSON hierarchy array into an list of Node objects.
	 * nodeId: id as String
	 * depthLevel: id as int 
	 * @param hier
	 */
	private void parseHierarchy(JsonArray hier) {
		Node specNode;
		Node procNode;
		Node prodNode;

		if (hier == null) return;

		Iterator<JsonElement> hIter = hier.iterator();
		while (hIter.hasNext()) {
			// get the specialty
			JsonObject spec = hIter.next().getAsJsonObject();
			specNode = new Node();
			specNode.setDepthLevel(spec.get("id").getAsInt());
			specNode.setNodeId(spec.get("id").getAsString());
			specNode.setNodeName(spec.get("name").getAsString());

			if (spec.has("procedures")) {
				//array of com.depuy.admin.databean.ProcedureVO
				JsonArray hProcs = spec.getAsJsonArray("procedures");
				Iterator<JsonElement> pcIter = hProcs.iterator();
				while (pcIter.hasNext()) {
					JsonObject proc = pcIter.next().getAsJsonObject(); // ProcedureVO
					procNode = new Node();
					procNode.setDepthLevel(proc.get("id").getAsInt());
					procNode.setNodeId(proc.get("id").getAsString());
					procNode.setNodeName(proc.get("name").getAsString());

					if (proc.has("products")) {
						JsonArray hProds = proc.getAsJsonArray("products");
						Iterator<JsonElement> pdIter = hProds.iterator();
						while (pdIter.hasNext()) {
							JsonObject prod = pdIter.next().getAsJsonObject();
							prodNode = new Node();
							prodNode.setDepthLevel(prod.get("id").getAsInt());
							prodNode.setNodeId(prod.get("id").getAsString());
							prodNode.setNodeName(prod.get("name").getAsString());
							// add product to procedure
							procNode.addChild(prodNode);
						}
					}
					// add procedure to specialty
					specNode.addChild(procNode);
				}
			}
			// add specialty to list of specialties
			hierarchy.add(specNode);
		}
	}

	/**
	 * Parses surgeon JSONElements into SurgeonBeans. Excludes surgeons
	 * whose primary distance (first location) is greater than the extended
	 * search radius.
	 * @param surgeons
	 */
	private void parseSurgeons(JsonArray surgeons) {
		if (surgeons == null) return;
		
		SurgeonBean surgeon;
		Iterator<JsonElement> surgeonIter = surgeons.iterator();
		while (surgeonIter.hasNext()) {
			// grab the current surgeon iteration
			surgeon = new SurgeonBean(surgeonIter.next());
			results.add(surgeon);
		}
	}

	/**
	 * Parses certain search parameter values returned by 
	 * the search container that override defaults.
	 * @param ctnr
	 */
	private void parseOverrideParams(JsonObject ctnr) {
		if (ctnr.has("numResults")) setNumResults(ctnr.get("numResults"));
		if (ctnr.has("pageSize")) setResultsPerPage(ctnr.get("pageSize"));
		if (ctnr.has("radius")) setRadius(ctnr.get("radius"));
		if (ctnr.has("isExtendedSearch")) setExtendedSearch(ctnr.get("isExtendedSearch"));
		if (isExtendedSearch) {
			setExtendedRadius(ctnr.get("extendedRadius"));
		}
	}

	/**
	 * Receives filter values as a comma-delimited String, parses them into an
	 * array, and calls the standard setFilters method.
	 * @param filterString
	 */
	public void setFilterString(String filterString) {
		String[] filterVals = null;
		if (StringUtil.checkVal(filterString,null) != null) {
			filterVals = filterString.split(",");
		}
		setFilters(filterVals);
	}

	/**
	 * Sets filters, filtered surgeon list, and 'display total' number.  Filter values
	 * are formatted as follows:
	 * - procedures (prefix 'pc'):
	 * 	--- 'prefix+procedureID|specialtyID (e.g. pc9|5)
	 * - products (prefix 'pd'):
	 * --- 'prefix+productID|procedureID|specialtyID (pd546|20|9)
	 *
	 * @param filterVals
	 */
	public void setFilters(String[] filterVals) {
		// clear filters
		specFilters.clear();
		procFilters.clear();
		prodFilters.clear();

		boolean hasFilterVals = (filterVals != null && filterVals.length > 0);
		// parse filter values.
		if (hasFilterVals) {
			// search filter values were passed in, parse them
			specFilters.addAll(globalSpecFilters);
			String[] split = null;
			Integer tmpI = null;

			// loop the filter vals
			for (String fVal : filterVals) {
				// filter out invalid filter vals (null, length, no delimiter)
				if (fVal == null) continue;
				if (fVal.startsWith("pc") || fVal.startsWith("pd")) {
					split = fVal.split(FILTER_VALUE_DELIMITER_REGEX);
					if (split == null || split.length < 2) continue;

					// determine/add the specialty
					if (split.length == 2) {
						// is procedure
						tmpI = Convert.formatInteger(split[1]);
					} else {
						// is product
						tmpI = Convert.formatInteger(split[2]);
					}
					if (tmpI == 0) continue;
					// add specialty ID to specialty filter if not already there.
					if (! specFilters.contains(tmpI)) specFilters.add(tmpI);

					// now determine/add the procedure or product.
					tmpI = Convert.formatInteger(split[0].substring(2));
					if (tmpI == 0) continue;
					if (split[0].startsWith("pc")) {
						procFilters.add(tmpI);
					} else {
						prodFilters.add(tmpI);
						// we also need to look at the product's parent procedure ID
						tmpI = Convert.formatInteger(split[1]);
						if (tmpI > 0 && ! procFilterParents.contains(tmpI)) {
							procFilterParents.add(tmpI);
						}
					}
				}
			}
		}

		// determine if all filters were requested
		boolean useAllFilters = compareFilters(hasFilterVals);

		// build list of surgeons that determine which surgeons are to display or not display
		buildFilteredSurgeonsList(useAllFilters);
	}

	/**
	 * Counts the number of possible procedures and products and compares those
	 * counts with the number of procedure filters and product filters submitted.  If 
	 * the values match, then all possible filters were submitted and all results are
	 * eligible for display consideration relative to the radius requirements.
	 * @param hasFilterVals
	 * @return
	 */
	private boolean compareFilters(boolean hasFilterVals) {
		if (! hasFilterVals) return true;
		int procMatch = 0;
		int prodMatch = 0;
		int procPossible = 0;
		int prodPossible = 0;
		for (Node spec : hierarchy) {
			if (specFilters.contains(spec.getDepthLevel())) {
				procPossible += spec.getNumberChildren();
				// loop procedures
				for (Node proc : spec.getChildren()) {
					prodPossible += proc.getNumberChildren();
					if (procFilters.contains(proc.getDepthLevel())) {
						procMatch++;
						// loop products
						for (Node prod : proc.getChildren()) {
							if (prodFilters.contains(prod.getDepthLevel())) prodMatch++;
						}
					}
				}
			}
		}

		return (procMatch >= procPossible && 
				prodMatch >= prodPossible);
	}

	/**
	 * Core business logic that determines which surgeons are displayed and which
	 * are not displayed.  This method builds a list of surgeons (key is surgeon ID) 
	 * who should not be displayed given the search/filter parameters.
	 */
	private void buildFilteredSurgeonsList(boolean useAllFilters) {
		filteredSurgeonList.clear();
		int displayCount = 0;
		useExtendedResults = false;
		/* Perform filtering if necessary */
		if (! useAllFilters && (hasProcFilters() || hasProdFilters())) {
			boolean displaySurgeon = false;
			for (SurgeonBean surgeon : results) {
				List<Integer> surgeonFilterIds = null;

				/* Process procedure filters if necessary */
				if (hasProcFilters()) {
					/* loop procedure filter list, count procedures surgeon matches. */
					surgeonFilterIds = surgeon.getProcedures();
					for (Integer procFilterId : procFilters) {
						if (surgeonFilterIds.contains(procFilterId)) {
								displaySurgeon = true;
								break;
						}
					}
				}

				/* Process product filters if necessary */
				if (hasProdFilters()) {
					// 1. Check product filters for match
					surgeonFilterIds = surgeon.getProducts();
					for (Integer prodFilter : prodFilters) {
						if (surgeonFilterIds.contains(prodFilter)) {
							// Found matching product filter, we will display this surgeon.
							displaySurgeon = true;
							break;
						}
					}					
				}

				/* Compare surgeon distance to radius in order to determine whether
				 * or not to display. */
				if (surgeon.getPrimaryDistance() > radius) {
					/* If we reached the search radius but found no results, set flag
					 * so that we maximize the possibility of returning at least 1 result. */
					if (displayCount == 0) {
						useExtendedResults = true;
					} else {
						/* Override display flag if appropriate to exclude results that fall
						 * beyond the search radius. */
						if (! useExtendedResults) displaySurgeon = false;
					}
				}

				if (! displaySurgeon) {
					filteredSurgeonList.add(surgeon.getSurgeonId());
				} else {
					displayCount++;
				}

				// reset flags
				displaySurgeon = false;
			}

		} else {
			// no filters, loop surgeons displaying only those within search radius
			for (SurgeonBean surgeon : results) {

				if (surgeon.getPrimaryDistance() > radius) {
					// this surgeon is outside the search radius.
					if (displayCount == 0) {
						/* we haven't found anyone yet, enable extended results. */
						useExtendedResults = true;
					}

					if (useExtendedResults) {
						// we are including extended results, so display this surgeon.
						displayCount++;
					} else {
						/* we are excluding extended results, so exclude this surgeon. */
						filteredSurgeonList.add(surgeon.getSurgeonId());
					}
				} else {
					// surgeon is within search radius, display this surgeon.
					displayCount++;
				}
			}

		}

		// calculate display values (start,end, page nav, etc.)
		calculateDisplayValues();		
	}

	/**
	 * Calculates values used to drive results-per-page, starting/ending value,
	 * current page number, etc.
	 */
	private void calculateDisplayValues() {
		// calculate display count.  We only count surgeons who DO NOT MATCH a filter.
		displayTotalNo = results.size() - filteredSurgeonList.size();

		// ensure certain default vals
		if (resultsPerPage < 1) resultsPerPage = 3;
		if (currentPageNo < 1) currentPageNo = 1;

		// calculate nav vals
		if (displayTotalNo < resultsPerPage) {
			lastPageNo = 1;
		} else {
			lastPageNo = displayTotalNo / resultsPerPage;
			if (displayTotalNo % resultsPerPage > 0) {
				lastPageNo++;
			}
		}
		// calc page number
		if (currentPageNo > lastPageNo) currentPageNo = lastPageNo;

		// calc result start value
		startVal = ((currentPageNo * resultsPerPage) - resultsPerPage) + 1;

		// calc result end value
		endVal = resultsPerPage * currentPageNo;
		if (endVal > displayTotalNo) endVal = displayTotalNo;

	}


	/**
	 * @return the numResults
	 */
	public int getNumResults() {
		return numResults;
	}

	/**
	 * @return the pageSize
	 */
	public int getPageSize() {
		return pageSize;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * @return the resultCode
	 */
	public String getResultCode() {
		return resultCode;
	}

	/**
	 * @param resultCode the resultCode to set
	 */
	public void setResultCode(String resultCode) {
		this.resultCode = resultCode;
	}

	/**
	 * @return the hierarchy
	 */
	public List<Node> getHierarchy() {
		return hierarchy;
	}

	/**
	 * @param hierarchy the hierarchy to set
	 */
	public void setHierarchy(List<Node> hierarchy) {
		this.hierarchy = hierarchy;
	}

	/**
	 * @return the results
	 */
	public List<SurgeonBean> getResults() {
		return results;
	}

	/**
	 * @param results the results to set
	 */
	public void setResults(List<SurgeonBean> results) {
		this.results = results;
	}

	/**
	 * @return the specialtyId
	 */
	public int getSpecialtyId() {
		return specialtyId;
	}

	/**
	 * @param specialtyId the specialtyId to set
	 */
	public void setSpecialtyId(int specialtyId) {
		this.specialtyId = specialtyId;
	}

	/**
	 * @return the procedureId
	 */
	public int getProcedureId() {
		return procedureId;
	}

	/**
	 * @param procedureId the procedureId to set
	 */
	public void setProcedureId(int procedureId) {
		this.procedureId = procedureId;
	}

	/**
	 * @return the productId
	 */
	public int getProductId() {
		return productId;
	}

	/**
	 * @param productId the productId to set
	 */
	public void setProductId(int productId) {
		this.productId = productId;
	}

	/**
	 * @return the currentPageNo
	 */
	public int getCurrentPageNo() {
		return currentPageNo;
	}

	/**
	 * @param currentPageNo the currentPageNo to set
	 */
	public void setCurrentPageNo(int currentPageNo) {
		this.currentPageNo = currentPageNo;
	}

	/**
	 * @return the startVal
	 */
	public int getStartVal() {
		return startVal;
	}

	/**
	 * @return the endVal
	 */
	public int getEndVal() {
		return endVal;
	}

	/**
	 * @return the resultsPerPage
	 */
	public int getResultsPerPage() {
		return resultsPerPage;
	}

	/**
	 * @param resultsPerPage the resultsPerPage to set
	 */
	private void setResultsPerPage(JsonElement resultsPerPage) {
		if (resultsPerPage != null) {
			this.resultsPerPage = resultsPerPage.getAsInt();
		}
	}

	/**
	 * @param numResults the numResults to set
	 */
	private void setNumResults(JsonElement numResults) {
		if (numResults != null) {
			this.numResults = numResults.getAsInt();
		} 
	}

	/**
	 * @return the lastPageNo
	 */
	public int getLastPageNo() {
		return lastPageNo;
	}

	/**
	 * @return the displayTotalNo
	 */
	public int getDisplayTotalNo() {
		return displayTotalNo;
	}

	/**
	 * @return the prodFilters
	 */
	public List<Integer> getProdFilters() {
		return prodFilters;
	}

	/**
	 * @param prodFilters the prodFilters to set
	 */
	public void setProdFilters(List<Integer> prodFilters) {
		this.prodFilters = prodFilters;
	}

	private boolean hasProcFilters() {
		return (procFilters == null || procFilters.isEmpty());
	}

	private boolean hasProdFilters() {
		return (prodFilters == null || prodFilters.isEmpty());
	}

	/**
	 * @return the filteredSurgeonList
	 */
	public List<Integer> getFilteredSurgeonList() {
		return filteredSurgeonList;
	}

	/**
	 * @return the procFilterParents
	 */
	public List<Integer> getProcFilterParents() {
		return procFilterParents;
	}
	public int getProcFilterParentsSize() {
		if (procFilterParents == null) return 0;
		return procFilterParents.size();
	}

	public void setGlobalSpecialtyFilter(String specId) {
		if (StringUtil.checkVal(specId, null) == null) return;
		String[] vals = specId.split(FILTER_VALUE_DELIMITER_REGEX);
		for (String val : vals) {
			Integer tmpId = Convert.formatInteger(val);
			if (tmpId > 0 && ! globalSpecFilters.contains(tmpId)) {
				globalSpecFilters.add(tmpId);
			}
		}
	}

	/**
	 * @return the isExtendedSearch
	 */
	public boolean isExtendedSearch() {
		return isExtendedSearch;
	}

	/**
	 * @param isExtendedSearch the isExtendedSearch to set
	 */
	public void setExtendedSearch(JsonElement isExtendedSearch) {
		if (isExtendedSearch != null) {
			this.isExtendedSearch = isExtendedSearch.getAsBoolean();
		}
	}

	/**
	 * @return the useExtendedResults
	 */
	public boolean isUseExtendedResults() {
		return useExtendedResults;
	}

	/**
	 * @param radius the radius to set
	 */
	private void setRadius(JsonElement radius) {
		if (radius != null) {
			this.radius = radius.getAsInt();
		} else {
			this.radius = RADIUS_SEARCH_DEFAULT;
		}
	}

	/**
	 * @param extendedRadius the extendedRadius to set
	 */
	private void setExtendedRadius(JsonElement extendedRadius) {
		if (extendedRadius != null) {
			this.extendedRadius = extendedRadius.getAsInt();
		} else { 
			this.extendedRadius = RADIUS_EXTENDED_SEARCH_DEFAULT;
		}
	}

}
