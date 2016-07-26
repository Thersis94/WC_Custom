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
	private final String FILTER_VALUE_DELIMITER_REGEX = "\\|";
	private final double RADIUS_SEARCH_DEFAULT = 50;
	private final double RADIUS_EXTENDED_SEARCH_DEFAULT = 251;
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
	 */
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
    
    public static void main(String[] args) {
    	ResultsContainer rs = new ResultsContainer();
    	String s = "{\"numResults\":18,\"pageSize\":5,\"resultCode\":\"zipCode\",\"message\":\"SUCCESS\",\"specialtyId\":0,\"procedureId\":0,\"productId\":0,\"productHierarchy\":[{\"specialtyId\":16,\"surgeonId\":0,\"specialtyDesc\":\"Biologics\",\"procedures\":[{\"procedureId\":16,\"specialtyId\":16,\"surgeonId\":0,\"procedureDesc\":\"Biologics\",\"products\":[{\"productId\":557,\"surgeonId\":0,\"locatorListFlag\":0,\"productDesc\":\"PEAK\u003csup\u003e\u0026reg;\u003c/sup\u003e Platelet Rich Plasma\",\"country\":\"US\",\"specialtyId\":16,\"procedureId\":16,\"statusId\":5,\"selected\":false}],\"statusId\":5,\"selected\":false}],\"statusId\":5,\"selected\":false},{\"specialtyId\":2,\"surgeonId\":0,\"specialtyDesc\":\"Elbow\",\"procedures\":[{\"procedureId\":2,\"specialtyId\":2,\"surgeonId\":0,\"procedureDesc\":\"Elbow Arthroscopy\",\"products\":[],\"statusId\":5,\"selected\":false}],\"statusId\":5,\"selected\":false},{\"specialtyId\":1,\"surgeonId\":0,\"specialtyDesc\":\"Foot \u0026amp; Ankle\",\"procedures\":[{\"procedureId\":1,\"specialtyId\":1,\"surgeonId\":0,\"procedureDesc\":\"Foot \u0026amp; Ankle\",\"products\":[],\"statusId\":5,\"selected\":false}],\"statusId\":5,\"selected\":false},{\"specialtyId\":3,\"surgeonId\":0,\"specialtyDesc\":\"Hand\",\"procedures\":[{\"procedureId\":3,\"specialtyId\":3,\"surgeonId\":0,\"procedureDesc\":\"Finger Joint Replacement\",\"products\":[],\"statusId\":5,\"selected\":false},{\"procedureId\":4,\"specialtyId\":3,\"surgeonId\":0,\"procedureDesc\":\"Hand \u0026amp; Wrist\",\"products\":[],\"statusId\":5,\"selected\":false}],\"statusId\":5,\"selected\":false},{\"specialtyId\":4,\"surgeonId\":0,\"specialtyDesc\":\"Hip\",\"procedures\":[{\"procedureId\":5,\"specialtyId\":4,\"surgeonId\":0,\"procedureDesc\":\"Anterior Approach\",\"products\":[],\"statusId\":5,\"selected\":false},{\"procedureId\":6,\"specialtyId\":4,\"surgeonId\":0,\"procedureDesc\":\"Hip Arthroscopy\",\"products\":[],\"statusId\":5,\"selected\":false},{\"procedureId\":7,\"specialtyId\":4,\"surgeonId\":0,\"procedureDesc\":\"Hip Replacement\",\"products\":[],\"statusId\":5,\"selected\":false}],\"statusId\":5,\"selected\":false},{\"specialtyId\":5,\"surgeonId\":0,\"specialtyDesc\":\"Knee\",\"procedures\":[{\"procedureId\":8,\"specialtyId\":5,\"surgeonId\":0,\"procedureDesc\":\"ACL Reconstruction\",\"products\":[],\"statusId\":5,\"selected\":false},{\"procedureId\":11,\"specialtyId\":5,\"surgeonId\":0,\"procedureDesc\":\"Knee Injections\",\"products\":[{\"productId\":555,\"surgeonId\":0,\"locatorListFlag\":1,\"productDesc\":\"MONOVISC\u003csup\u003e\u0026#174;\u003c/sup\u003e High Molecular Weight Hyaluronan\",\"country\":\"US\",\"specialtyId\":5,\"procedureId\":11,\"statusId\":5,\"selected\":false},{\"productId\":550,\"surgeonId\":0,\"locatorListFlag\":1,\"productDesc\":\"ORTHOVISC\u003csup\u003e\u0026#174;\u003c/sup\u003e High Molecular Weight Hyaluronan\",\"country\":\"US\",\"specialtyId\":5,\"procedureId\":11,\"statusId\":5,\"selected\":false}],\"statusId\":5,\"selected\":false},{\"procedureId\":10,\"specialtyId\":5,\"surgeonId\":0,\"procedureDesc\":\"Knee Replacement\",\"products\":[{\"productId\":548,\"surgeonId\":0,\"locatorListFlag\":1,\"productDesc\":\"ATTUNE\u003csup\u003e\u0026#174;\u003c/sup\u003e Knee System\",\"country\":\"US\",\"specialtyId\":5,\"procedureId\":10,\"statusId\":5,\"selected\":false},{\"productId\":1,\"surgeonId\":0,\"locatorListFlag\":1,\"productDesc\":\"Partial Knee System\",\"country\":\"US\",\"specialtyId\":5,\"procedureId\":10,\"statusId\":5,\"selected\":false}],\"statusId\":5,\"selected\":false},{\"procedureId\":9,\"specialtyId\":5,\"surgeonId\":0,\"procedureDesc\":\"Meniscus Repair\",\"products\":[],\"statusId\":5,\"selected\":false}],\"statusId\":5,\"selected\":false},{\"specialtyId\":15,\"surgeonId\":0,\"specialtyDesc\":\"Rib\",\"procedures\":[{\"procedureId\":15,\"specialtyId\":15,\"surgeonId\":0,\"procedureDesc\":\"Rib Fixation\",\"products\":[{\"productId\":556,\"surgeonId\":0,\"locatorListFlag\":1,\"productDesc\":\"MatrixRIB\u003csup\u003eTM\u003c/sup\u003e Fixation System\",\"country\":\"US\",\"specialtyId\":15,\"procedureId\":15,\"statusId\":5,\"selected\":false}],\"statusId\":5,\"selected\":false}],\"statusId\":5,\"selected\":false},{\"specialtyId\":6,\"surgeonId\":0,\"specialtyDesc\":\"Shoulder\",\"procedures\":[{\"procedureId\":12,\"specialtyId\":6,\"surgeonId\":0,\"procedureDesc\":\"Rotator Cuff Repair\",\"products\":[],\"statusId\":5,\"selected\":false},{\"procedureId\":13,\"specialtyId\":6,\"surgeonId\":0,\"procedureDesc\":\"Shoulder Arthroscopy\",\"products\":[],\"statusId\":5,\"selected\":false},{\"procedureId\":14,\"specialtyId\":6,\"surgeonId\":0,\"procedureDesc\":\"Shoulder Replacement\",\"products\":[{\"productId\":280,\"surgeonId\":0,\"locatorListFlag\":1,\"productDesc\":\"DELTA XTEND\u003csup\u003eTM\u003c/sup\u003e Reverse Shoulder System\",\"country\":\"US\",\"specialtyId\":6,\"procedureId\":14,\"statusId\":5,\"selected\":false},{\"productId\":100,\"surgeonId\":0,\"locatorListFlag\":1,\"productDesc\":\"GLOBAL UNITE\u003csup\u003e\u0026#174;\u003c/sup\u003e Platform Shoulder System\",\"country\":\"US\",\"specialtyId\":6,\"procedureId\":14,\"statusId\":5,\"selected\":false}],\"statusId\":5,\"selected\":false}],\"statusId\":5,\"selected\":false},{\"specialtyId\":9,\"surgeonId\":0,\"specialtyDesc\":\"Spine\",\"procedures\":[{\"procedureId\":18,\"specialtyId\":9,\"surgeonId\":0,\"procedureDesc\":\"Adult Deformity Surgery\",\"products\":[],\"statusId\":5,\"selected\":false},{\"procedureId\":17,\"specialtyId\":9,\"surgeonId\":0,\"procedureDesc\":\"Adult Degenerative Surgery\",\"products\":[],\"statusId\":5,\"selected\":false},{\"procedureId\":19,\"specialtyId\":9,\"surgeonId\":0,\"procedureDesc\":\"Artificial Disc Replacement\",\"products\":[{\"productId\":558,\"surgeonId\":0,\"locatorListFlag\":1,\"productDesc\":\"PRODISC\u003csup\u003e\u0026#174;\u003c/sup\u003e C Total Disc Replacement\",\"country\":\"SP\",\"specialtyId\":9,\"procedureId\":19,\"statusId\":5,\"selected\":false},{\"productId\":559,\"surgeonId\":0,\"locatorListFlag\":1,\"productDesc\":\"PRODISC\u003csup\u003e\u0026#174;\u003c/sup\u003e L Total Disc Replacement\",\"country\":\"SP\",\"specialtyId\":9,\"procedureId\":19,\"statusId\":5,\"selected\":false}],\"statusId\":5,\"selected\":false},{\"procedureId\":20,\"specialtyId\":9,\"surgeonId\":0,\"procedureDesc\":\"Fracture/Trauma Surgery\",\"products\":[],\"statusId\":5,\"selected\":false},{\"procedureId\":21,\"specialtyId\":9,\"surgeonId\":0,\"procedureDesc\":\"Less Invasive Surgery\",\"products\":[],\"statusId\":5,\"selected\":false},{\"procedureId\":22,\"specialtyId\":9,\"surgeonId\":0,\"procedureDesc\":\"Navigation\",\"products\":[],\"statusId\":5,\"selected\":false},{\"procedureId\":23,\"specialtyId\":9,\"surgeonId\":0,\"procedureDesc\":\"Pediatric/Adolescent Surgery\",\"products\":[],\"statusId\":5,\"selected\":false},{\"procedureId\":24,\"specialtyId\":9,\"surgeonId\":0,\"procedureDesc\":\"Tumor Surgery\",\"products\":[],\"statusId\":5,\"selected\":false},{\"procedureId\":25,\"specialtyId\":9,\"surgeonId\":0,\"procedureDesc\":\"Vertebral Body Augmentation\",\"products\":[],\"statusId\":5,\"selected\":false}],\"statusId\":5,\"selected\":false}],\"results\":[{\"statusId\":3,\"surgeonId\":22280,\"userName\":\"krizmanich\",\"lastName\":\"Krizmanich\",\"firstName\":\"Thomas\",\"suffixName\":\"\",\"aamdUrl\":\"http://www.allaboutmydoc.com/drkrizmanich\",\"redirectUrl\":\"http://jr.localhost/aamd?type\u003dsurgeon\u0026amp;id\u003d22280\u0026amp;site_location\u003dnull\",\"redirectUrl2\":\"\",\"degreeDesc\":\"M.D.\",\"specialties\":[4,5,16],\"procedures\":[16],\"products\":[340,360,557],\"locations\":[{\"surgeonId\":22280,\"clinicId\":20299,\"clinicName\":\"Northern Lakes Orthopaedics \u0026 Sports Medicine\",\"locationId\":21525,\"address\":\"3505 Lake City Hwy\",\"address2\":\"\",\"city\":\"Warsaw\",\"state\":\"IN\",\"country\":\"US\",\"phoneNumber\":\"574-2694144\",\"statusId\":0,\"latitude\":41.219668,\"longitude\":-85.775102,\"uniqueId\":\"22280-20299-21525\",\"distance\":4.911064031574365}],\"affiliations\":[\"Lutheran Health Network\",\"Parkview Health\"]},{\"statusId\":3,\"surgeonId\":36834,\"lastName\":\"Buck\",\"firstName\":\"Gregory\",\"aamdUrl\":\"http://www.allaboutmydoc.com/drnull\",\"redirectUrl\":\"http://jr.localhost/aamd?type\u003dsurgeon\u0026amp;id\u003d36834\u0026amp;site_location\u003dnull\",\"redirectUrl2\":\"\",\"degreeDesc\":\"M.D.\",\"specialties\":[5,16],\"procedures\":[11,16],\"products\":[550,557],\"locations\":[{\"surgeonId\":36834,\"clinicId\":31889,\"clinicName\":\"Bremen Family Medicine\",\"locationId\":33957,\"address\":\"1120 W. South St.\",\"address2\":\"\",\"city\":\"Bremen\",\"state\":\"IN\",\"country\":\"US\",\"phoneNumber\":\"5745461251\",\"statusId\":0,\"latitude\":41.4455321,\"longitude\":-86.1589051,\"uniqueId\":\"36834-31889-33957\",\"distance\":21.305903458639133}],\"affiliations\":[]},{\"statusId\":3,\"surgeonId\":37394,\"lastName\":\"Kolbe\",\"firstName\":\"Robert\",\"aamdUrl\":\"http://www.allaboutmydoc.com/drnull\",\"redirectUrl\":\"http://jr.localhost/aamd?type\u003dsurgeon\u0026amp;id\u003d37394\u0026amp;site_location\u003dnull\",\"redirectUrl2\":\"\",\"degreeDesc\":\"M.D.\",\"specialties\":[5,16],\"procedures\":[11,16],\"products\":[550,557],\"locations\":[{\"surgeonId\":37394,\"clinicId\":31889,\"clinicName\":\"Bremen Family Medicine\",\"locationId\":33957,\"address\":\"1120 W. South St.\",\"address2\":\"\",\"city\":\"Bremen\",\"state\":\"IN\",\"country\":\"US\",\"phoneNumber\":\"5745461251\",\"statusId\":0,\"latitude\":41.4455321,\"longitude\":-86.1589051,\"uniqueId\":\"37394-31889-33957\",\"distance\":21.305903458639133}],\"affiliations\":[]},{\"statusId\":3,\"surgeonId\":11675,\"userName\":\"JGraham\",\"lastName\":\"Graham\",\"firstName\":\"John\",\"suffixName\":\"\",\"aamdUrl\":\"http://www.allaboutmydoc.com/drJGraham\",\"redirectUrl\":\"http://jr.localhost/aamd?type\u003dsurgeon\u0026amp;id\u003d11675\u0026amp;site_location\u003dnull\",\"redirectUrl2\":\"\",\"degreeDesc\":\"D.O.\",\"specialties\":[5,16],\"procedures\":[16],\"products\":[205,557],\"locations\":[{\"surgeonId\":11675,\"clinicId\":11293,\"clinicName\":\"Rivervalley Orthopedics \u0026 Sports Medicine\",\"locationId\":11483,\"address\":\"320 Lincolnway East\",\"address2\":\"\",\"city\":\"Osceola\",\"state\":\"IN\",\"country\":\"US\",\"phoneNumber\":\"5746746700\",\"statusId\":0,\"latitude\":41.6631516,\"longitude\":-86.0705954,\"uniqueId\":\"11675-11293-11483\",\"distance\":31.888189703306644}],\"affiliations\":[]},{\"statusId\":3,\"surgeonId\":11677,\"userName\":\"Kibiloski\",\"lastName\":\"Kibiloski\",\"firstName\":\"Leonard\",\"aamdUrl\":\"http://www.allaboutmydoc.com/drKibiloski\",\"customUrl\":\"http://OSMC-online.com\",\"redirectUrl\":\"http://jr.localhost/aamd?type\u003dsurgeon\u0026amp;id\u003d11677\u0026amp;site_location\u003dnull\",\"redirectUrl2\":\"http://www.allaboutmydoc.com/website?referer\u003dnull\u0026amp;surgeon_id\u003d11677\u0026amp;site\u003dhttp://OSMC-online.com\",\"degreeDesc\":\"M.D.\",\"specialties\":[5,16],\"procedures\":[16],\"products\":[205,340,557],\"locations\":[{\"surgeonId\":11677,\"clinicId\":11292,\"clinicName\":\"O.S.M.C.\",\"locationId\":11482,\"address\":\"2310 California Road\",\"city\":\"Elkhart\",\"state\":\"IN\",\"country\":\"US\",\"phoneNumber\":\"5742640791\",\"statusId\":0,\"latitude\":41.6953,\"longitude\":-86.0064,\"uniqueId\":\"11677-11292-11482\",\"distance\":33.12530862985972}],\"affiliations\":[]},{\"statusId\":3,\"surgeonId\":11773,\"userName\":\"Klaassen\",\"lastName\":\"Klaassen\",\"firstName\":\"Mark\",\"aamdUrl\":\"http://www.allaboutmydoc.com/drKlaassen\",\"customUrl\":\"http://OSMC-online.com\",\"redirectUrl\":\"http://jr.localhost/aamd?type\u003dsurgeon\u0026amp;id\u003d11773\u0026amp;site_location\u003dnull\",\"redirectUrl2\":\"http://www.allaboutmydoc.com/website?referer\u003dnull\u0026amp;surgeon_id\u003d11773\u0026amp;site\u003dhttp://OSMC-online.com\",\"degreeDesc\":\"M.D.\",\"specialties\":[5,16],\"procedures\":[16],\"products\":[205,340,557],\"locations\":[{\"surgeonId\":11773,\"clinicId\":11292,\"clinicName\":\"O.S.M.C.\",\"locationId\":11482,\"address\":\"2310 California Road\",\"city\":\"Elkhart\",\"state\":\"IN\",\"country\":\"US\",\"phoneNumber\":\"5742640791\",\"statusId\":0,\"latitude\":41.6953,\"longitude\":-86.0064,\"uniqueId\":\"11773-11292-11482\",\"distance\":33.12530862985972}],\"affiliations\":[]},{\"statusId\":3,\"surgeonId\":1510,\"userName\":\"stfisher\",\"lastName\":\"Fisher\",\"firstName\":\"Steven\",\"suffixName\":\"\",\"aamdUrl\":\"http://www.allaboutmydoc.com/drstfisher\",\"customUrl\":\"http://fwortho.com\",\"redirectUrl\":\"http://jr.localhost/aamd?type\u003dsurgeon\u0026amp;id\u003d1510\u0026amp;site_location\u003dnull\",\"redirectUrl2\":\"http://www.allaboutmydoc.com/website?referer\u003dnull\u0026amp;surgeon_id\u003d1510\u0026amp;site\u003dhttp://fwortho.com\",\"degreeDesc\":\"M.D.\",\"specialties\":[4,5,6,16],\"procedures\":[10,11,14,16],\"products\":[1,100,120,121,202,340,360,440,540,550,557],\"locations\":[{\"surgeonId\":1510,\"clinicId\":868,\"clinicName\":\"Fort Wayne Orthopaedics\",\"locationId\":20611,\"address\":\"7601 W. Jefferson Blvd.\",\"city\":\"Fort Wayne\",\"state\":\"IN\",\"country\":\"US\",\"phoneNumber\":\"2604368686\",\"statusId\":0,\"latitude\":41.042468,\"longitude\":-85.240303,\"uniqueId\":\"1510-868-20611\",\"distance\":35.11856051419058},{\"surgeonId\":1510,\"clinicId\":868,\"clinicName\":\"Fort Wayne Orthopaedics\",\"locationId\":34190,\"address\":\"7601 W. Jefferson Blvd.\",\"address2\":\"\",\"city\":\"Fort Wayne\",\"state\":\"IN\",\"country\":\"US\",\"phoneNumber\":\"2604368686\",\"statusId\":0,\"latitude\":41.040273,\"longitude\":-85.2391886,\"uniqueId\":\"1510-868-34190\",\"distance\":35.228380821819165}],\"affiliations\":[]},{\"statusId\":3,\"surgeonId\":12926,\"userName\":\"kerschner\",\"lastName\":\"Kershner\",\"firstName\":\"Charles\",\"aamdUrl\":\"http://www.allaboutmydoc.com/drkerschner\",\"redirectUrl\":\"http://jr.localhost/aamd?type\u003dsurgeon\u0026amp;id\u003d12926\u0026amp;site_location\u003dnull\",\"redirectUrl2\":\"\",\"degreeDesc\":\"M.D.\",\"specialties\":[2,3,4,5,6,9,16],\"procedures\":[10,14,16],\"products\":[1,100,120,202,203,205,360,557],\"locations\":[{\"surgeonId\":12926,\"clinicId\":12325,\"clinicName\":\"Orthopaedic Specialists\",\"locationId\":12456,\"address\":\"1250 W. Main Street\",\"address2\":\"\",\"city\":\"Peru\",\"state\":\"IN\",\"country\":\"US\",\"phoneNumber\":\"7656629873\",\"statusId\":0,\"latitude\":40.7447754,\"longitude\":-86.1046567,\"uniqueId\":\"12926-12325-12456\",\"distance\":35.51425331368646}],\"affiliations\":[]},{\"statusId\":3,\"surgeonId\":37884,\"lastName\":\"Sandler\",\"firstName\":\"Brad\",\"aamdUrl\":\"http://www.allaboutmydoc.com/drnull\",\"redirectUrl\":\"http://jr.localhost/aamd?type\u003dsurgeon\u0026amp;id\u003d37884\u0026amp;site_location\u003dnull\",\"redirectUrl2\":\"\",\"degreeDesc\":\"M.D.\",\"specialties\":[5,16],\"procedures\":[11,16],\"products\":[550,557],\"locations\":[{\"surgeonId\":37884,\"clinicId\":32461,\"clinicName\":\"Spine \u0026 Orthopedic Medicine Inc.\",\"locationId\":34797,\"address\":\"3740 Edison Lakes Parkway\",\"address2\":\"\",\"city\":\"Mishawaka\",\"state\":\"IN\",\"country\":\"US\",\"phoneNumber\":\"5742557246\",\"statusId\":0,\"latitude\":41.6948477,\"longitude\":-86.1797858,\"uniqueId\":\"37884-32461-34797\",\"distance\":36.10425018576072}],\"affiliations\":[]},{\"statusId\":3,\"surgeonId\":28404,\"lastName\":\"McPherron\",\"firstName\":\"Anthony\",\"suffixName\":\"\",\"aamdUrl\":\"http://www.allaboutmydoc.com/drnull\",\"customUrl\":\"http://www.specialtyortho.com\",\"redirectUrl\":\"http://jr.localhost/aamd?type\u003dsurgeon\u0026amp;id\u003d28404\u0026amp;site_location\u003dnull\",\"redirectUrl2\":\"http://www.allaboutmydoc.com/website?referer\u003dnull\u0026amp;surgeon_id\u003d28404\u0026amp;site\u003dhttp://www.specialtyortho.com\",\"degreeDesc\":\"D.O.\",\"specialties\":[5,16],\"procedures\":[16],\"products\":[205,557],\"locations\":[{\"surgeonId\":28404,\"clinicId\":25346,\"clinicName\":\"Specialty Orthopedics\",\"locationId\":26805,\"address\":\"301 East Day Road\",\"address2\":\"\",\"city\":\"Mishawaka\",\"state\":\"IN\",\"country\":\"US\",\"phoneNumber\":\"5749359395\",\"statusId\":0,\"latitude\":41.6985628,\"longitude\":-86.1804418,\"uniqueId\":\"28404-25346-26805\",\"distance\":36.34907458084047}],\"affiliations\":[]},{\"statusId\":3,\"surgeonId\":36744,\"lastName\":\"Balog\",\"firstName\":\"Natali\",\"aamdUrl\":\"http://www.allaboutmydoc.com/drnull\",\"customUrl\":\"http://www.southbendclinic.com\",\"redirectUrl\":\"http://jr.localhost/aamd?type\u003dsurgeon\u0026amp;id\u003d36744\u0026amp;site_location\u003dnull\",\"redirectUrl2\":\"http://www.allaboutmydoc.com/website?referer\u003dnull\u0026amp;surgeon_id\u003d36744\u0026amp;site\u003dhttp://www.southbendclinic.com\",\"degreeDesc\":\"M.D.\",\"specialties\":[5,16],\"procedures\":[11,16],\"products\":[550,557],\"locations\":[{\"surgeonId\":36744,\"clinicId\":20960,\"clinicName\":\"South Bend Clinic\",\"locationId\":33862,\"address\":\"211 North Eddy St\",\"address2\":\"\",\"city\":\"South Bend\",\"state\":\"IN\",\"country\":\"US\",\"phoneNumber\":\"5742379331\",\"statusId\":0,\"latitude\":41.6784326,\"longitude\":-86.2358591,\"uniqueId\":\"36744-20960-33862\",\"distance\":36.516042601288376}],\"affiliations\":[]},{\"statusId\":3,\"surgeonId\":37612,\"lastName\":\"Mohr\",\"firstName\":\"Brent\",\"aamdUrl\":\"http://www.allaboutmydoc.com/drnull\",\"customUrl\":\"http://www.southbendclinic.com\",\"redirectUrl\":\"http://jr.localhost/aamd?type\u003dsurgeon\u0026amp;id\u003d37612\u0026amp;site_location\u003dnull\",\"redirectUrl2\":\"http://www.allaboutmydoc.com/website?referer\u003dnull\u0026amp;surgeon_id\u003d37612\u0026amp;site\u003dhttp://www.southbendclinic.com\",\"degreeDesc\":\"M.D.\",\"specialties\":[5,16],\"procedures\":[11,16],\"products\":[550,557],\"locations\":[{\"surgeonId\":37612,\"clinicId\":20960,\"clinicName\":\"South Bend Clinic\",\"locationId\":33862,\"address\":\"211 North Eddy St\",\"address2\":\"\",\"city\":\"South Bend\",\"state\":\"IN\",\"country\":\"US\",\"phoneNumber\":\"5742379331\",\"statusId\":0,\"latitude\":41.6784326,\"longitude\":-86.2358591,\"uniqueId\":\"37612-20960-33862\",\"distance\":36.516042601288376}],\"affiliations\":[]},{\"statusId\":3,\"surgeonId\":23440,\"lastName\":\"Yergler\",\"firstName\":\"Jeffery\",\"suffixName\":\"\",\"aamdUrl\":\"http://www.allaboutmydoc.com/drnull\",\"customUrl\":\"http://SBO.com\",\"redirectUrl\":\"http://jr.localhost/aamd?type\u003dsurgeon\u0026amp;id\u003d23440\u0026amp;site_location\u003dnull\",\"redirectUrl2\":\"http://www.allaboutmydoc.com/website?referer\u003dnull\u0026amp;surgeon_id\u003d23440\u0026amp;site\u003dhttp://SBO.com\",\"degreeDesc\":\"M.D.\",\"specialties\":[5,16],\"procedures\":[11,16],\"products\":[550,557],\"locations\":[{\"surgeonId\":23440,\"clinicId\":20960,\"clinicName\":\"South Bend Clinic\",\"locationId\":33862,\"address\":\"211 North Eddy St\",\"address2\":\"\",\"city\":\"South Bend\",\"state\":\"IN\",\"country\":\"US\",\"phoneNumber\":\"5742379331\",\"statusId\":0,\"latitude\":41.6784326,\"longitude\":-86.2358591,\"uniqueId\":\"23440-20960-33862\",\"distance\":36.516042601288376},{\"surgeonId\":23440,\"clinicId\":5380,\"clinicName\":\"South Bend Orthopaedics\",\"locationId\":34978,\"address\":\"53880 Carmichael Drive\",\"address2\":\"\",\"city\":\"South Bend\",\"state\":\"IN\",\"country\":\"US\",\"phoneNumber\":\"5742379331\",\"statusId\":0,\"latitude\":41.7107393,\"longitude\":-86.2061471,\"uniqueId\":\"23440-5380-34978\",\"distance\":37.701541249424274},{\"surgeonId\":23440,\"clinicId\":5380,\"clinicName\":\"South Bend Orthopaedics\",\"locationId\":5340,\"address\":\"53880 Carmichael Drive\",\"city\":\"South Bend\",\"state\":\"IN\",\"country\":\"US\",\"phoneNumber\":\"5742479441\",\"statusId\":0,\"latitude\":41.7108,\"longitude\":-86.207,\"uniqueId\":\"23440-5380-5340\",\"distance\":37.725710986861316}],\"affiliations\":[]},{\"statusId\":3,\"surgeonId\":22157,\"userName\":\"rozzi\",\"lastName\":\"Rozzi\",\"firstName\":\"William\",\"suffixName\":\"\",\"aamdUrl\":\"http://www.allaboutmydoc.com/drrozzi\",\"customUrl\":\"http://sbortho.com\",\"redirectUrl\":\"http://jr.localhost/aamd?type\u003dsurgeon\u0026amp;id\u003d22157\u0026amp;site_location\u003dnull\",\"redirectUrl2\":\"http://www.allaboutmydoc.com/website?referer\u003dnull\u0026amp;surgeon_id\u003d22157\u0026amp;site\u003dhttp://sbortho.com\",\"degreeDesc\":\"M.D.\",\"specialties\":[5,16],\"procedures\":[16],\"products\":[205,340,557],\"locations\":[{\"surgeonId\":22157,\"clinicId\":5380,\"clinicName\":\"South Bend Orthopaedics\",\"locationId\":5340,\"address\":\"53880 Carmichael Drive\",\"city\":\"South Bend\",\"state\":\"IN\",\"country\":\"US\",\"phoneNumber\":\"5742479441\",\"statusId\":0,\"latitude\":41.7108,\"longitude\":-86.207,\"uniqueId\":\"22157-5380-5340\",\"distance\":37.725710986861316}],\"affiliations\":[]},{\"statusId\":3,\"surgeonId\":33018,\"lastName\":\"Davis\",\"firstName\":\"James\",\"suffixName\":\"\",\"aamdUrl\":\"http://www.allaboutmydoc.com/drnull\",\"customUrl\":\"http://www.logansportmemorial.org\",\"redirectUrl\":\"http://jr.localhost/aamd?type\u003dsurgeon\u0026amp;id\u003d33018\u0026amp;site_location\u003dnull\",\"redirectUrl2\":\"http://www.allaboutmydoc.com/website?referer\u003dnull\u0026amp;surgeon_id\u003d33018\u0026amp;site\u003dhttp://www.logansportmemorial.org\",\"degreeDesc\":\"D.O.\",\"specialties\":[4,5,16],\"procedures\":[16],\"products\":[120,202,203,205,340,557],\"locations\":[{\"surgeonId\":33018,\"clinicId\":29313,\"clinicName\":\"Logansport Memorial Orthopedics\",\"locationId\":30913,\"address\":\"1101 Michigan Avenue\",\"address2\":\"\",\"city\":\"Logansport\",\"state\":\"IN\",\"country\":\"US\",\"phoneNumber\":\"5747222663\",\"statusId\":0,\"latitude\":40.7807397,\"longitude\":-86.3782198,\"uniqueId\":\"33018-29313-30913\",\"distance\":40.68930067370155}],\"affiliations\":[]},{\"statusId\":3,\"surgeonId\":33017,\"lastName\":\"Hogg\",\"firstName\":\"Christopher\",\"suffixName\":\"\",\"aamdUrl\":\"http://www.allaboutmydoc.com/drnull\",\"customUrl\":\"http://www.logansportmemorial.org\",\"redirectUrl\":\"http://jr.localhost/aamd?type\u003dsurgeon\u0026amp;id\u003d33017\u0026amp;site_location\u003dnull\",\"redirectUrl2\":\"http://www.allaboutmydoc.com/website?referer\u003dnull\u0026amp;surgeon_id\u003d33017\u0026amp;site\u003dhttp://www.logansportmemorial.org\",\"degreeDesc\":\"D.O.\",\"specialties\":[4,5,16],\"procedures\":[10,16],\"products\":[120,202,203,548,557],\"locations\":[{\"surgeonId\":33017,\"clinicId\":29313,\"clinicName\":\"Logansport Memorial Orthopedics\",\"locationId\":30913,\"address\":\"1101 Michigan Avenue\",\"address2\":\"\",\"city\":\"Logansport\",\"state\":\"IN\",\"country\":\"US\",\"phoneNumber\":\"5747222663\",\"statusId\":0,\"latitude\":40.7807397,\"longitude\":-86.3782198,\"uniqueId\":\"33017-29313-30913\",\"distance\":40.68930067370155}],\"affiliations\":[]},{\"statusId\":3,\"surgeonId\":37282,\"lastName\":\"Inabnit\",\"firstName\":\"Ralph\",\"aamdUrl\":\"http://www.allaboutmydoc.com/drnull\",\"redirectUrl\":\"http://jr.localhost/aamd?type\u003dsurgeon\u0026amp;id\u003d37282\u0026amp;site_location\u003dnull\",\"redirectUrl2\":\"\",\"degreeDesc\":\"M.D.\",\"specialties\":[5,16],\"procedures\":[11,16],\"products\":[550,557],\"locations\":[{\"surgeonId\":37282,\"clinicId\":20960,\"clinicName\":\"South Bend Clinic\",\"locationId\":34372,\"address\":\"8984 East US Highway 20\",\"address2\":\"\",\"city\":\"New Carlisle\",\"state\":\"IN\",\"country\":\"US\",\"phoneNumber\":\"5207427541\",\"statusId\":0,\"latitude\":41.702807,\"longitude\":-86.526168,\"uniqueId\":\"37282-20960-34372\",\"distance\":47.30688899465547}],\"affiliations\":[]},{\"statusId\":3,\"surgeonId\":37930,\"lastName\":\"Seluzhitskiy\",\"firstName\":\"Andrey\",\"aamdUrl\":\"http://www.allaboutmydoc.com/drnull\",\"redirectUrl\":\"http://jr.localhost/aamd?type\u003dsurgeon\u0026amp;id\u003d37930\u0026amp;site_location\u003dnull\",\"redirectUrl2\":\"\",\"degreeDesc\":\"M.D.\",\"specialties\":[5,16],\"procedures\":[11,16],\"products\":[550,557],\"locations\":[{\"surgeonId\":37930,\"clinicId\":20960,\"clinicName\":\"South Bend Clinic\",\"locationId\":34372,\"address\":\"8984 East US Highway 20\",\"address2\":\"\",\"city\":\"New Carlisle\",\"state\":\"IN\",\"country\":\"US\",\"phoneNumber\":\"5207427541\",\"statusId\":0,\"latitude\":41.702807,\"longitude\":-86.526168,\"uniqueId\":\"37930-20960-34372\",\"distance\":47.30688899465547}],\"affiliations\":[]}]}";
    	rs.setJson(s);
    	System.out.println(rs.getMessage());
    	for (SurgeonBean sb : rs.getResults()) {
    		System.out.println("last/first: " + sb.getLastName() + ", " + sb.getFirstName());
    	}
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
		parseHierarchy(ctnr.getAsJsonArray("productHierarchy"));
		/* NOTE: Must parse override params BEFORE parsing surgeons because 
		 * override params can affect how surgeon records are parsed for display. */
		parseOverrideParams(ctnr);
		// parse surgeon results
		parseSurgeons(ctnr.getAsJsonArray("results"));
		// call setFilters to determine what is displayed.
		this.setFilters(null);
    }
    
    /**
     * Parses the JSON hierarchy array into an list of Node objects.
     * nodeId: id as String
     * depthLevel: id as int 
     * @param hier
     */
    private void parseHierarchy(JsonArray hier) {
    	Node specNode = null;
    	Node procNode = null;
    	Node prodNode = null;
    	
    	if (hier != null) {
    		Iterator<JsonElement> hIter = hier.iterator();
    		while (hIter.hasNext()) {
    			// get the specialty
    			JsonObject spec = hIter.next().getAsJsonObject();
    			specNode = new Node();
    			specNode.setDepthLevel(spec.get("specialtyId").getAsInt());
    			specNode.setNodeId(spec.get("specialtyId").getAsString());
    			specNode.setNodeName(spec.get("specialtyDesc").getAsString());
    			
    			if (spec.has("procedures")) {
					//array of com.depuy.admin.databean.ProcedureVO
					JsonArray hProcs = spec.getAsJsonArray("procedures");
					Iterator<JsonElement> pcIter = hProcs.iterator();
					while (pcIter.hasNext()) {
						JsonObject proc = pcIter.next().getAsJsonObject(); // ProcedureVO
		    			procNode = new Node();
		    			procNode.setDepthLevel(proc.get("procedureId").getAsInt());
		    			procNode.setNodeId(proc.get("procedureId").getAsString());
		    			procNode.setNodeName(proc.get("procedureDesc").getAsString());
						
						if (proc.has("products")) {
							JsonArray hProds = proc.getAsJsonArray("products");
							Iterator<JsonElement> pdIter = hProds.iterator();
							while (pdIter.hasNext()) {
								JsonObject prod = pdIter.next().getAsJsonObject();
								prodNode = new Node();
				    			prodNode.setDepthLevel(prod.get("productId").getAsInt());
				    			prodNode.setNodeId(prod.get("productId").getAsString());
				    			prodNode.setNodeName(prod.get("productDesc").getAsString());
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
    }

    /**
     * Parses surgeon JSONElements into SurgeonBeans. Excludes surgeons
     * whose primary distance (first location) is greater than the extended
     * search radius.
     * @param surgeons
     */
    private void parseSurgeons(JsonArray surgeons) {
    	if (surgeons != null) {
    		SurgeonBean surgeon = null;
			Iterator<JsonElement> surgeonIter = surgeons.iterator();
			while (surgeonIter.hasNext()) {
				// grab the current surgeon iteration
				surgeon = new SurgeonBean(surgeonIter.next());
				if (surgeon.getPrimaryDistance() < extendedRadius);
					results.add(surgeon);
			}
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
	    		split = null;
	    	}

    	}
    	   	
    	// build list of surgeons that determine which surgeons are to display or not display
    	buildFilteredSurgeonsList(hasFilterVals);
    }
    
    /**
     * Core business logic that determines which surgeons are displayed and which
     * are not displayed.  This method builds a list of surgeons (key is surgeon ID) 
     * who should not be displayed given the search/filter parameters.
     */
    private void buildFilteredSurgeonsList(boolean hasFilterVals) {
    	filteredSurgeonList.clear();
    	int displayCount = 0;
    	useExtendedResults = false;
    	/* Perform filtering if necessary */
		if (hasProcFilters() || hasProdFilters()) {
			boolean displaySurgeon = false;
			int procMatchCount = 0;
			for (SurgeonBean surgeon : results) {
				List<Integer> surgeonFilterIds = null;
				
				/* Process procedure filters if necessary */
				if (hasProcFilters()) {
					/* loop procedure filter list, count procedures surgeon matches. */
					surgeonFilterIds = surgeon.getProcedures();
					for (Integer procFilterId : procFilters) {
						if (surgeonFilterIds.contains(procFilterId)) {
							if (! hasProdFilters()) {
								displaySurgeon = true;
								break;
							} else {
								/* Count procedure filter matches for this surgeon. */
								procMatchCount++;
							}
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
					
					if (! displaySurgeon) {
						/* No product filter match found for this surgeon, so we are going
						 * to compare the procedure match(es) with the product filter
						 * constraints in order to determine whether or not to display 
						 * this surgeon. */
						if (hasProcFilters() && procMatchCount > 0) {
							
							if (procFilterParents.isEmpty()) {
								/* Means none of the product filters are hierarchically-related
								 * to any of the procedure filters.  Therefore we display this
								 * surgeon based on the procedure match. */
								displaySurgeon= true;
							} else {
								
								if (procMatchCount == procFilters.size()) {
									/* Means the surgeon matched ALL of the procedure
									 * filters. If we matched more procedure filters than there
									 * were hierarchically-related product filters, we display
									 * the surgeon. */
									if (procFilterParents.size() < procFilters.size()) {
										displaySurgeon = true;
									}
								} else if (procMatchCount < procFilters.size()) {
									/* Means the surgeon matched 1 or more, but not ALL
									 * of the procedure filters.  We have to compare the 
									 * surgeon's procedure filter membership with the parents
									 * found and with the full procedure filter list. */
									for (Integer surgeonProcFilterId : surgeon.getProcedures()) {
										if (procFilters.contains(surgeonProcFilterId) && 
												! procFilterParents.contains(surgeonProcFilterId)) {
											displaySurgeon = true;
											break;
										}
									}
								}
							}
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
				procMatchCount = 0;
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
		if (procFilters == null || procFilters.isEmpty()) return false;
		return true;
	}
	
	private boolean hasProdFilters() {
		if (prodFilters == null || prodFilters.isEmpty()) return false;
		return true;
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
			this.radius = this.RADIUS_SEARCH_DEFAULT;
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
