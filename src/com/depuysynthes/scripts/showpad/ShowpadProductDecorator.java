package com.depuysynthes.scripts.showpad;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.depuysynthes.action.EMEACarouselAction;
import com.depuysynthes.huddle.HuddleUtils;
import com.depuysynthes.huddle.solr.HuddleProductCatalogSolrIndex;
import com.depuysynthes.scripts.DSMediaBinImporterV2;
import com.depuysynthes.scripts.MediaBinDeltaVO;
import com.depuysynthes.scripts.MediaBinDeltaVO.State;
import com.siliconmtn.commerce.catalog.ProductAttributeContainer;
import com.siliconmtn.commerce.catalog.ProductAttributeVO;
import com.siliconmtn.commerce.catalog.ProductCategoryVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.commerce.product.ProductCatalogAction;

/****************************************************************************
 * <b>Title</b>: ShowpadProductDecorator.java<p/>
 * <b>Description: Reads the EMEA Product catalog and performs 3 tasks with the data:
 * 		1) Pushes to Showpad the hierarchy levels for each product, as mediabin asset tags.</b>
 * 		2) Reports products with no mediabin assets that are using the dynamic Product Attribute.
 * 		3) Reports Mediabin SOUS Product Names not used (referenced) by any products. 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Aug 29, 2016
 ****************************************************************************/
public class ShowpadProductDecorator extends ShowpadMediaBinDecorator {

	/*
	 * Contains products with their flattened hierarchy, who use the special 
	 * MEDIABIN Attribute.
	 */
	protected List<ProductVO> products = new ArrayList<>();

	/*
	 * data containers for the issues we find that eventaully get reported in the 
	 * Admin email.
	 * contents = <[mediabin|product] SOUS Name, String[] tracking#s>
	 */
	protected Map<String, Set<String>> mediabinSOUSNames = new HashMap<>();
	protected Map<String, Set<String>> productSOUSNames = new HashMap<>();

	/*
	 * The product catalog this script is hard-coded around
	 */
	protected static final String catalogId = EMEACarouselAction.CATALOG_ID;

	/*
	 * The constant used for the MEDIABIN product attribute type - comes from the database
	 */
	protected static final String MEDIABIN_ATTR_TYPE = HuddleUtils.PROD_ATTR_MB_TYPE;

	/*
	 * The constant used for the HTML product attribute type - comes from the database
	 */
	protected static final String HTML_ATTR_TYPE = HuddleUtils.PROD_ATTR_HTML_TYPE;

	
	/*
	 * Regex used when parsing static HTML to find links to Mediabin assets
	 * Reads: /json URL, amid=MEDIA_BIN_AJAX is present BEFORE mbid
	 * 	grab mbid value up to the next & or ' or " (single or double quote presumed to end the href attribute, & to begin the next request param)
	 * max-length of value at 32 chars, which is the pkId limit in the database 
	 */
	protected static final String MB_AJAX_HREF = "/json\\?([^'\"]+)?amid=MEDIA_BIN_AJAX([^'\"]+)?&(amp;)?mbid=([^&\\s'\"]{1,32})";

	/**
	 * @param args
	 * @throws IOException
	 */
	public ShowpadProductDecorator(String[] args) throws IOException {
		super(args);
	}


	/**
	 * Create an instance of the ShowpadProductDecorator.
	 * Note this wraps ShowpadMediaBinDecorator, which wraps the DSMediaBinImporterV2 script.
	 * All three are run here-in.
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		ShowpadProductDecorator dmb = new ShowpadProductDecorator(args);
		dmb.run(); //run() is part of the superclass, not implemented here.
	}


	@Override
	protected Map<String, MediaBinDeltaVO> loadManifest() {
		//convert the properties object to a Map, required by the WC Action framework
		Map<String, Object> attributes = new HashMap<>(props.size());
		for (Entry<Object, Object> entry : props.entrySet())
			attributes.put((String) entry.getKey(), entry.getValue());

		//load the entire product catalog for EMEA
		ProductCatalogAction pca = new ProductCatalogAction();
		pca.setDBConnection(new SMTDBConnection(dbConn));
		pca.setAttributes(attributes);
		Tree t = pca.loadEntireCatalog(catalogId, true, null, null);

		parseProductCatalog(t);
		log.info("loaded " + products.size() + " mediabin-using products in catalog " + catalogId);

		return super.loadManifest();
	}


	@Override
	public void saveRecords(Map<String, MediaBinDeltaVO> masterRecords, boolean isInsert) {
		super.saveRecords(masterRecords, isInsert);

		//this method gets called for both inserts & updates (superclass reusability!).
		//Block here for updates so we don't process the records twice.
		//Insert runs after deletes & updates, so wait for the 'inserts' invocation so 
		//all the mediabin records are already in our database.
		if (!isInsert) return;

		//replicate product (tag) changes out to Showpad
		syncTags(masterRecords);
		
		//now that all products have attached their tags to the mediabin 
		//assets, iterate through them and push to Showpad.
		try {
			pushTagsToShowpad(masterRecords);
		} catch (QuotaException qe) {
			failures.add(qe);
			log.error(qe);
		}

		//parse the product list for SOUS products containing no MediaBin assets
		findEmptyProducts(masterRecords);

		//parse the Mediabin database for Assets who's SOUS Product Name is not used in the product catalog
		findUnusedAssets(masterRecords);
	}


	/**
	 * Iterates the Mediabin Records against the product list to find matches (bindings).
	 * For each one found, take the product hierarchy and push them to Showpad as Tags.
	 * Calls the ShowpadUtil in a way that tells it we want to delete any tags that 
	 * shouldn't be there, but were added by us.  We can only delete tags we've added.
	 * @param masterRecords
	 */
	private void syncTags(Map<String, MediaBinDeltaVO> masterRecords) {
		//for each product containing the dynamic Product Attribute
		for (ProductVO prod : products) {
			//get the Mediabin attributes off the product.  There could be several.
			List<ProductAttributeVO> mbAttribs = getMediabinAttributes(prod, false); //this will never be null or empty, per early executed code
			String[] hierarchy = StringUtil.checkVal(prod.getAttrib1Txt()).split(DSMediaBinImporterV2.TOKENIZER);
			
			List<String> assetIds = new ArrayList<>();
			for (ProductAttributeVO attrVo : mbAttribs) {
				Collection<String> foundIds;
				switch (StringUtil.checkVal(attrVo.getTitle())) {
					case "mediabin-static":
						foundIds = convertFromJson(attrVo.getValueText());
						break;
					case "mediabin-sous":
						//take the update date of the Attribute if it's newer than the product and contains assets.
						foundIds = findAssetsForSous(masterRecords, prod.getFullProductName());
						break;
					default:
						//static HTML, use a regex to parse the HTML for mediabin links
						foundIds = findAssetsInHtml(attrVo.getValueText());
				}
				setProdUpdDate(prod, attrVo, foundIds);
				assetIds.addAll(foundIds);
			}
			//At this point we know all the mediabin assets that require the hierachy of 'this' product.
			marryProductTagsToAssets(masterRecords, hierarchy, assetIds, prod.getLastUpdate());
		}
	}
	
	
	/**
	 * set the update date of the product to that of the Attribute if it's newer than the product and contains assets.
	 * @param prod
	 * @param attr
	 * @param ids
	 */
	private void setProdUpdDate(ProductVO prod, ProductAttributeVO attr, Collection<String> ids) {
		if (ids == null || ids.isEmpty()) return; //this attribute has no assets we care about.
		if (attr.getUpdateDt() == null) return; //this attribute does not have an update date, for whatever reason
		if (attr.getUpdateDt().after(prod.getLastUpdate())) //the attribute has a more recent updateDt than the product.
			prod.setLastUpdate(attr.getUpdateDt());
	}


	/**
	 * iterate the Mediabin assets (masterRecords).  When we find one 
	 * matching the product's mediabin assets, add our tags to it.
	 * @param masterRecords
	 * @param hierarchy
	 * @param assetIds
	 */
	private void marryProductTagsToAssets(Map<String, MediaBinDeltaVO> masterRecords, 
			String[] hierarchy, List<String> assetIds, Date prodUpdDt) {
		boolean relevant = false;
		for (MediaBinDeltaVO mbAsset : masterRecords.values()) {
			relevant = assetIds.contains(mbAsset.getDpySynMediaBinId());

			//check if this is an asset we care about
			if (!relevant || State.Delete == mbAsset.getRecordState())
				continue;

			//attach the hierarchy values as tags to this asset
			for (String tag : hierarchy) {
				mbAsset.addTag(null, tag, null, ShowpadTagManager.SMT_PRODUCT_EXTERNALID);
				log.debug("added tag " + tag + " from a product to mbAsset " + mbAsset.getDpySynMediaBinId());
			}

			//pass-down the product's last update date, so when we push to 
			//Showpad we know which assets need updated (because of updates at the product level).
			if (prodUpdDt != null && (mbAsset.getProductUpdateDt() == null || mbAsset.getProductUpdateDt().before(prodUpdDt)))
				mbAsset.setProductUpdateDt(prodUpdDt);
		}
	}


	/**
	 * pushes changes to Showpad via the ShowpadDivisionUtil
	 * @param masterRecords
	 * @throws QuotaException 
	 */
	private void pushTagsToShowpad(Map<String, MediaBinDeltaVO> masterRecords) throws QuotaException {
		Calendar cal = Calendar.getInstance();
		//consider a product change within 24hrs something we need to pay attention to.
		//set the config value to reflect the frequency of the script execution.  e.g. if we run once a week threshold should be -7.
		cal.add(Calendar.DATE, Convert.formatInteger(props.getProperty("productDateThresDays"), -1, false));
		Date thresDate = cal.getTime();

		for (ShowpadDivisionUtil util : divisions) {
			ShowpadTagManager tagMgr = util.getTagManager();
			Map<String, String> divisionAssets = util.getDivisionAssets();

			log.info("pushing product tags to Division=" + util.getDivisionNm());

			boolean needsUpdated;
			for (MediaBinDeltaVO mbAsset : masterRecords.values()) {
				//if the asset is new or updated, or the product is updated, we need to push tag changes to Showpad (all: adds/updates/deletes)
				needsUpdated = (State.Insert == mbAsset.getRecordState() || State.Update == mbAsset.getRecordState());
				if (!needsUpdated && mbAsset.getProductUpdateDt() != null)
					needsUpdated = thresDate.before(mbAsset.getProductUpdateDt());

				if (!needsUpdated) {
					log.info("asset does not need updated based on logic: " + mbAsset.getDpySynMediaBinId());
					continue;
				}

				//get the showpad asset id for this mediabin asset id, so the ShowpadTagMgr knows how to talk to Showpad
				mbAsset.setShowpadId(divisionAssets.get(mbAsset.getDpySynMediaBinId()));

				//skip any that have failed to ingest into Showpad
				if (ShowpadDivisionUtil.FAILED_PROCESSING.equals(mbAsset.getShowpadId()))
					continue;

				log.info("************************ Starting Asset *******************************");
				log.info("showpadId=" + mbAsset.getShowpadId() + " mbId=" + mbAsset.getDpySynMediaBinId());
				log.info("asset tags (" + mbAsset.getTags().size() + ") " + mbAsset.getTags());

				/**
				 * push the tags to Showpad.  Note this will do 3 things:
				 * 1) load all existing 'product' tags.
				 * 2) purge any we don't want to keep or should be removed.
				 * 3) add any we need to add that aren't already there.
				 **/
				try {
					tagMgr.updateProductTags(mbAsset);
				} catch (InvalidDataException e) {
					failures.add(e);
					log.error("data issue with " + mbAsset + " and Divsion=" + util.getDivisionId(), e);
				}
			}
		}
	}


	/**
	 * parses the json object from a String and returns a Set of dpy_syn_mediabin_id values
	 * @param jsonText
	 * @return
	 */
	private Collection<String> convertFromJson(String jsonText) {
		try {
			return HuddleProductCatalogSolrIndex.convertFromJSON(jsonText);
		} catch (InvalidDataException ide) {
			log.error("could not parse JSON stored on the Product record", ide);
		}
		return Collections.emptyList();
	}


	/**
	 * Parse the product tree (hierarchy) into a flatten list of products we care about.
	 * Care it taken here to capture and preserve the hierarchy levels, since we need to 
	 * push those across to Mediabin.
	 * @param t
	 */
	private void parseProductCatalog(Tree t) {
		//find the "Body Region" node, and start the parsing from there.  Only these descendants get pushed to Showpad
		for (Node n : t.getRootNode().getChildren()) {
			if ("Body Region".equals(n.getNodeName())) {
				parseNode(n);
				break;
			}
		}
	}


	/**
	 * parses a node of the Tree.  
	 * Note: This is a recursive method.
	 * @param thisNode
	 */
	private void parseNode(Node thisNode) {
		if (thisNode.getNumberChildren() == 0) {
			//this is the lowest level of 'this' branch.  Perform work on the product.
			parseProduct(thisNode);
			return;
		}

		for (Node nextNode : thisNode.getChildren()) {
			//append 'this' node to the full path (hierarchy).  We'll use this later when pushing tags to Showpad.
			String path = StringUtil.checkVal(thisNode.getFullPath());
			if (!path.isEmpty()) path += DSMediaBinImporterV2.TOKENIZER;
			path += nextNode.getNodeName();
			//log.debug("path=" + path);
			nextNode.setFullPath(path);

			parseNode(nextNode);
		}
	}


	/**
	 * Iterates the products found in the given lowest-level Category (Node).
	 * @param n
	 */
	private void parseProduct(Node n) {
		ProductCategoryVO cat = (ProductCategoryVO) n.getUserObject();
		if (cat.getProducts() == null || StringUtil.checkVal(cat.getUrlAlias()).isEmpty()) return; //not a product!
		
		//remove the product name from the hierarchy tree - Pierre - 11.28.16
		StringBuilder path = new StringBuilder(n.getFullPath().length());
		String[] lvls = n.getFullPath().split(DSMediaBinImporterV2.TOKENIZER);
		for (int x=0; x < lvls.length-1; x++)
			path.append(lvls[x]).append(DSMediaBinImporterV2.TOKENIZER);
		if (path.length() > 0) path = new StringBuilder(path.substring(0, path.length()-1));

		for (ProductVO prod : cat.getProducts()) {
			prod.setProductName(n.getNodeName()); //preserve the name, it is not populated by the initial catalog load
			prod.setAttrib1Txt(path.toString()); //pass the parent hierarchy from the Node down to the Product.
			prod.setLastUpdate(cat.getLastUpdate()); //pass the last update date, which was put into the CategoryVO from the ResultSet.

			if (!getMediabinAttributes(prod, true).isEmpty())
				products.add(prod);
		}
	}


	/**
	 * tests the product to ensure it has at least one "MEDIABIN" Attribute.
	 * If it does we're going to keep it for future tasks.  If it doesn't its dead to us and can be discarded.
	 * @param prod
	 */
	private List<ProductAttributeVO> getMediabinAttributes(ProductVO prod, boolean firstOnly) {
		List<ProductAttributeVO> data = new ArrayList<>();
		ProductAttributeVO attrVo;
		ProductAttributeContainer pac = prod.getAttributes();
		if (pac == null) return data;
		
		for (Node n : pac.getAllAttributes()) {
			attrVo = (ProductAttributeVO) n.getUserObject();
			if (attrVo.getProductAttributeId() == null) 
				continue; //not actually bound to this product (in _XR table).
			
			//make sure it's one of our special attributes, or HTML we can parse for mediabin assets
			if (!MEDIABIN_ATTR_TYPE.equals(attrVo.getAttributeType()) && !HTML_ATTR_TYPE.equals(attrVo.getAttributeType()))
				continue;
			
			data.add(attrVo);

			//found one, exit the loop b/c we have what we need.
			if (firstOnly)
				break;
		}
		return data;
	}


	/**
	 * Iterate the products. For each determine if we have Mediabin assets matching
	 * it's SOUS Product Name.  If we don't report them to the Admins.
	 * @param masterRecords
	 */
	private void findEmptyProducts(Map<String, MediaBinDeltaVO> masterRecords) {
		String name;
		String sousName;
		for (ProductVO prod : products) {
			name = prod.getProductName();
			sousName = prod.getFullProductName();
			checkProdSousAgainstMediabin(masterRecords, sousName, name);
		}
	}


	/**
	 * iterates the SOUS product name against the list of mediabin assets to find a match.
	 * if matches are not found, capture the product into the Map we're going to report to the Admins.
	 * @param sousName
	 * @param prodName
	 */
	private void checkProdSousAgainstMediabin(Map<String, MediaBinDeltaVO> masterRecords, String prodSousName, String prodName) {
		List<String> assetIds = findAssetsForSous(masterRecords, prodSousName);

		if (assetIds != null && !assetIds.isEmpty()) 
			return; //we have our answer; there are matches here.

		//finished checking all assets.  Apparently none of them use the same 
		//SOUS name the product uses.  Let's report these to the Admins.
		log.debug("no Mediabin matches for PROD_NM=" + prodSousName);
		Set<String> prodNames = productSOUSNames.get(prodSousName);
		if (prodNames == null) prodNames = new HashSet<>();
		prodNames.add(scrubString(prodName));
		productSOUSNames.put(scrubString(prodSousName), prodNames);
	}


	/**
	 * finds mediabin assets matching the given sous product name.
	 * returns a List<String> of the assetIds found.
	 * @param masterRecords
	 * @param prodSousName
	 * @return
	 */
	private List<String> findAssetsForSous(Map<String, MediaBinDeltaVO> masterRecords, String prodSousName) {
		List<String> data = new ArrayList<>();
		
		for (MediaBinDeltaVO mbAsset : masterRecords.values()) {
			if (State.Delete == mbAsset.getRecordState() || StringUtil.checkVal(mbAsset.getProdNm()).isEmpty())
				continue; //nothing to do here, this asset has no SOUS value, or is being deleted.

			//split the product name field using the tokenizer, and see if any of the values match our SOUS name.
			String[] sousVals = mbAsset.getProdNm().split(DSMediaBinImporterV2.TOKENIZER);
			if (Arrays.asList(sousVals).contains(prodSousName)) {
				//Product's SOUS matches at least one Mediabin record.  
				//Success!  Return; nothing else we need to do besides verifying we have at least one match.
				log.debug("SOUS match using " + prodSousName + " for " + mbAsset.getDpySynMediaBinId());
				data.add(mbAsset.getDpySynMediaBinId());
			}
		}
		return data;
	}
	
	
	/**
	 * returns a cleaned-up version of the string.  remove trademarks, etc. then lowercases
	 * @param sous
	 * @return
	 */
	private String scrubString(String sous) {
		return StringEncoder.encodeExtendedAscii(sous);
	}
	
	/**
	 * finds assets in HTML using a regex and returns them in a List<String>
	 * @param masterRecords
	 * @param prodSousName
	 * @return
	 */
	private List<String> findAssetsInHtml(String html) {
		List<String> data = new ArrayList<>();

		Matcher m = Pattern.compile(MB_AJAX_HREF).matcher(html);
		while (m.find()) {
			data.add(m.group(4));
			log.debug("found embedded link to asset " + m.group(4));
		}
			 
		return data;
	}


	/**
	 * iterates the Mediabin assets against the product catalog to see which assets
	 * are not being used (in the product catalog).  (via SOUS Product Name)
	 * @param masterRecords
	 */
	private void findUnusedAssets(Map<String, MediaBinDeltaVO> masterRecords) {
		//iterate Mediabin into a unique set of productNames.
		for (MediaBinDeltaVO mbAsset : masterRecords.values()) {
			if (State.Delete == mbAsset.getRecordState() || StringUtil.checkVal(mbAsset.getProdNm()).isEmpty())
				continue; //nothing to do here, this asset has no SOUS value, or is being deleted.

			//split the product name field using the tokenizer, and see if any of the values match our SOUS name.
			String[] sousVals = mbAsset.getProdNm().split(DSMediaBinImporterV2.TOKENIZER);
			for (String val : sousVals) {
				if (!isQualifiedSousValue(val)) continue;
				
				Set<String> trackingNos = mediabinSOUSNames.get(val);
				if (trackingNos == null) trackingNos = new HashSet<>();
				trackingNos.add(mbAsset.getTrackingNoTxt());
				mediabinSOUSNames.put(scrubString(val), trackingNos);
			}
		}
		log.info("found " + mediabinSOUSNames.size() + " unique SOUS names in Mediabin assets");


		String prodSousNm;
		for (ProductVO prod : products) {
			//remove mbSousName values if they match values at the product level
			prodSousNm = prod.getFullProductName();
			if (prodSousNm == null || prodSousNm.isEmpty()) continue;

			if (mediabinSOUSNames.containsKey(prodSousNm))
				mediabinSOUSNames.remove(prodSousNm);
		}
		log.info("still have " + mediabinSOUSNames.size() + " unique SOUS names in Mediabin assets not used by products");
	}

	
	/**
	 * Tests the String from the EXP file against business rules of values to ignore.
	 * Angi: It would be great if you could filter all SOUS – Product Names 
	 * only containing a number out of the report list e.g. “319.010”
	 * @param sousValue
	 * @return
	 */
	private boolean isQualifiedSousValue(String sousVal) {
		if (StringUtil.isEmpty(sousVal)) return false;
		//remove dots and dashes that commonly appear in number sequences.  e.g. "319.010"
		sousVal = StringUtil.removeNonAlphaNumeric(sousVal);
		//if all we have is numbers, this is not a qualified sous value
		boolean ignorable = sousVal.matches("[0-9]+");
		return !ignorable;
	}
	

	/**
	 * adds additional information to the Admin notification email.
	 * @param html
	 */
	@Override
	protected void addSupplementalDetails(StringBuilder html) {
		super.addSupplementalDetails(html);

		if (!productSOUSNames.isEmpty())
			addProductsWithNoAssetsToEmail(html);

		if (!mediabinSOUSNames.isEmpty())
			addAssetsWithNoProductsToEmail(html);
	}


	/**
	 * Appends a table to the email notification for products containing SOUS product name
	 * that doesn't match any mediabin assets.
	 * @param html
	 */
	private void addProductsWithNoAssetsToEmail(StringBuilder html) {
		html.append("<h4>Products with no MediaBin Assets (");
		html.append(productSOUSNames.size()).append(")</h4>");
		html.append("The following Web Crescendo products indicate a SOUS ");
		html.append("Product Name not matching any MediaBin assets.<br/>");
		html.append("<table border='1' width='95%' align='center'><thead><tr>");
		html.append("<th>Product Name(s)</th>");
		html.append("<th>SOUS Product Name</th>");
		html.append("</tr></thead><tbody>");

		for (Entry<String, Set<String>> entry : productSOUSNames.entrySet()) {
			Set<String> prodNames = entry.getValue();
			String[] array = prodNames.toArray(new String[prodNames.size()]);
			html.append("<tr><td>").append(StringUtil.getToString(array, false, false, ", ")).append("</td>");
			html.append("<td>").append(entry.getKey()).append("</td></tr>");
		}
		html.append("</tbody></table><br/><hr/>");
	}


	/**
	 * Appends a table to the email notification for products containing SOUS product name
	 * that doesn't match any mediabin assets.
	 * @param html
	 */
	private void addAssetsWithNoProductsToEmail(StringBuilder html) {
		html.append("<h4>Mediabin SOUS Values not used by Products (");
		html.append(mediabinSOUSNames.size()).append(")</h4>");
		html.append("The following MediaBin assets indicate a SOUS ");
		html.append("Product Name not matching any Web Crescendo products.<br/>\r\n");
		html.append("<table border='1' width='95%' align='center'><thead><tr>");
		html.append("<th>SOUS Product Name</th>");
		html.append("<th>Mediabin Tracking #(s)</th>");
		html.append("</tr></thead>\r\n<tbody>");

		for (Entry<String, Set<String>> entry : mediabinSOUSNames.entrySet()) {
			Set<String> prodNames = entry.getValue();
			String[] array = prodNames.toArray(new String[prodNames.size()]);
			html.append("<tr><td>").append(entry.getKey()).append("</td>");
			html.append("<td>").append(StringUtil.getToString(array, false, false, ", ")).append("</td></tr>\r\n");
		}
		html.append("</tbody></table>\r\n<br/><hr/>");
	}
}