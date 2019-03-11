package com.depuysynthes.scripts.showpad;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.depuysynthes.scripts.DSMediaBinImporterV2;
import com.depuysynthes.scripts.MediaBinDeltaVO;
import com.depuysynthes.scripts.MediaBinDeltaVO.State;
import com.depuysynthes.solr.ProductCatalogSolrIndex;
import com.siliconmtn.commerce.catalog.ProductAttributeContainer;
import com.siliconmtn.commerce.catalog.ProductAttributeVO;
import com.siliconmtn.commerce.catalog.ProductCategoryVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.commerce.product.ProductCatalogAction;

/****************************************************************************
 * <b>Title:</b> CatalogReconcileUtil.java<br/>
 * <b>Description:</b> This class gets used by the MediabinReportEmail class.  Its purpose 
 * is to help marry and highlight products and/or assets used within a WC product catalog. (or not used)
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Jan 17, 2018
 ****************************************************************************/
public class CatalogReconcileUtil {

	/*
	 * The constant used for the MEDIABIN product attribute type - comes from the database
	 */
	protected static final String MEDIABIN_ATTR_TYPE = "MEDIABIN";

	/*
	 * The constant used for the HTML product attribute type - comes from the database
	 */
	protected static final String HTML_ATTR_TYPE = "HTML";


	/*
	 * Regex used when parsing static HTML to find links to Mediabin assets
	 * Reads: /json URL, amid=MEDIA_BIN_AJAX is present BEFORE mbid
	 * 	grab mbid value up to the next & or ' or " (single or double quote presumed to end the href attribute, & to begin the next request param)
	 * max-length of value at 32 chars, which is the pkId limit in the database 
	 */
	protected static final String MB_AJAX_HREF = "/json\\?([^'\"]+)?amid=MEDIA_BIN_AJAX([^'\"]+)?&(amp;)?mbid=([^&\\s'\"]{1,32})";

	protected static Logger log = Logger.getLogger(CatalogReconcileUtil.class);

	private Connection dbConn;

	private Properties props;

	/*
	 * whether or not we want to keep ALL products on the list - used for the report emails 
	 * when we want to know which products don't have assets.
	 */
	private boolean preserveProducts = false;


	/**
	 * effective default constructor used on the normal workflow
	 * @param dbConn
	 * @param props
	 */
	public CatalogReconcileUtil(Connection dbConn, Properties props) {
		this(dbConn, props, false);
	}


	/**
	 * constructor uses for the report emails - when we want to save all products
	 * @param dbConn
	 * @param props
	 */
	public CatalogReconcileUtil(Connection dbConn, Properties props, boolean preserveProducts) {
		this.dbConn = dbConn;
		this.props = props;
		this.preserveProducts = preserveProducts;
	}


	/**
	 * reusable method to load product catalogs from WC
	 * @param catalogId
	 * @return
	 */
	public Tree loadProductCatalog(String catalogId) {
		ProductCatalogAction pca = new ProductCatalogAction();
		pca.setDBConnection(new SMTDBConnection(dbConn));
		pca.setAttributes(convertPropertiesToMap());
		return pca.loadEntireCatalog(catalogId, true, null, null);
	}


	/**
	 * converts the properties object to a Map, required by the WC Action framework
	 * @return
	 */
	private Map<String, Object> convertPropertiesToMap() {
		Map<String, Object>  attributes = new HashMap<>(props.size());
		for (Entry<Object, Object> entry : props.entrySet())
			attributes.put((String) entry.getKey(), entry.getValue());
		return attributes;
	}


	/**
	 * Parse the product tree (hierarchy) into a flatten list of products we care about.
	 * Care it taken here to capture and preserve the hierarchy levels, since we need to 
	 * push those across to Mediabin.
	 * @param t
	 */
	public void parseProductCatalog(Tree t, List<ProductVO> products) {
		//find the "Body Region" node, and start the parsing from there.  Only these descendants get pushed to Showpad
		for (Node n : t.getRootNode().getChildren()) {
			if ("Body Region".equals(n.getNodeName())) {
				parseNode(n, products);
				break;
			}
		}
	}


	/**
	 * parses a node of the Tree.  
	 * Note: This is a recursive method.
	 * @param thisNode
	 */
	private void parseNode(Node thisNode, List<ProductVO> products) {
		if (thisNode.getNumberChildren() == 0) {
			//this is the lowest level of 'this' branch.  Perform work on the product.
			parseProduct(thisNode, products);
			return;
		}

		for (Node nextNode : thisNode.getChildren()) {
			//append 'this' node to the full path (hierarchy).  We'll use this later when pushing tags to Showpad.
			String path = StringUtil.checkVal(thisNode.getFullPath());
			if (!path.isEmpty()) path += DSMediaBinImporterV2.TOKENIZER;
			path += nextNode.getNodeName();
			nextNode.setFullPath(path);

			parseNode(nextNode, products);
		}
	}


	/**
	 * Iterates the products found in the given lowest-level Category (Node).
	 * @param n
	 */
	private void parseProduct(Node n, List<ProductVO> products) {
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

			boolean hasMediabinAttributes = !getMediabinAttributes(prod, true).isEmpty();
			prod.addProdAttribute("hasAttributes", hasMediabinAttributes);
			if (preserveProducts || !hasMediabinAttributes)
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

			//make sure it's one of our special attributes, or HTML we can parse for mediabin assets
			if (attrVo.getProductAttributeId() == null || 
					(!MEDIABIN_ATTR_TYPE.equals(attrVo.getAttributeType()) && !HTML_ATTR_TYPE.equals(attrVo.getAttributeType())))
				continue;

			data.add(attrVo);

			//found one, exit the loop b/c we have what we need.
			if (firstOnly)
				break;
		}
		return data;
	}


	/**
	 * Iterates the Mediabin Records against the product list to find matches (bindings).
	 * For each one found, take the product hierarchy and push them to Showpad as Tags.
	 * Calls the ShowpadUtil in a way that tells it we want to delete any tags that 
	 * shouldn't be there, but were added by us.  We can only delete tags we've added.
	 * @param masterRecords
	 */
	@SuppressWarnings("unchecked")
	protected void syncTags(Map<String, MediaBinDeltaVO> masterRecords,  List<ProductVO> products) {
		//for each product containing the dynamic Product Attribute
		for (ProductVO prod : products) {
			tagProductAssets(masterRecords, prod);

			//At this point we know all the mediabin assets that require the hierachy of 'this' product.
			String[] hierarchy = StringUtil.checkVal(prod.getAttrib1Txt()).split(DSMediaBinImporterV2.TOKENIZER);
			marryProductTagsToAssets(masterRecords, hierarchy, (List<String>)prod.getProdAttribute("assetIds"), prod.getLastUpdate());
		}
	}


	/**
	 * Identifies a list of mediabin assets tied to this product - either in static html or using the SOUS name for lookup
	 * @param masterRecords
	 * @param prod
	 */
	public void tagProductAssets(Map<String, MediaBinDeltaVO> masterRecords, ProductVO prod) {
		//get the Mediabin attributes off the product.  There could be several.
		List<ProductAttributeVO> mbAttribs = getMediabinAttributes(prod, false); //this will never be null or empty, per early executed code
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
					//tag the product as using the dynamic SOUS-value connector - this gets used when generating the report email
					prod.setImage("isDynamic");
					break;
				default:
					//static HTML, use a regex to parse the HTML for mediabin links
					foundIds = findAssetsInHtml(attrVo.getValueText());
			}
			setProdUpdDate(prod, attrVo, foundIds);
			assetIds.addAll(foundIds);
		}
		//preserve the list of assets attached to this product - the email report queues off seeing this list.
		prod.addProdAttribute("assetIds", assetIds);
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
		boolean relevant;
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
	 * parses the json object from a String and returns a Set of dpy_syn_mediabin_id values
	 * @param jsonText
	 * @return
	 */
	private Collection<String> convertFromJson(String jsonText) {
		try {
			return ProductCatalogSolrIndex.convertFromJSON(jsonText);
		} catch (InvalidDataException ide) {
			log.error("could not parse JSON stored on the Product record", ide);
		}
		return Collections.emptyList();
	}

	/**
	 * finds assets in HTML using a regex and returns them in a List<String>
	 * @param masterRecords
	 * @param prodSousName
	 * @return
	 */
	private List<String> findAssetsInHtml(String html) {
		List<String> data = new ArrayList<>();
		if (StringUtil.isEmpty(html)) return data;

		Matcher m = Pattern.compile(MB_AJAX_HREF).matcher(html);
		while (m.find()) {
			data.add(m.group(4));
			log.debug("found embedded link to asset " + m.group(4));
		}

		return data;
	}


	/**
	 * finds mediabin assets matching the given sous product name.
	 * returns a List<String> of the assetIds found.
	 * @param masterRecords
	 * @param prodSousName
	 * @return
	 */
	private List<String> findAssetsForSous(Map<String, MediaBinDeltaVO> masterRecords, String cleanSousName) {
		List<String> data = new ArrayList<>();

		for (MediaBinDeltaVO mbAsset : masterRecords.values()) {
			if (State.Delete == mbAsset.getRecordState() || StringUtil.isEmpty(mbAsset.getProdNm()))
				continue; //nothing to do here, this asset has no SOUS value, or is being deleted.

			//split the product name field using the tokenizer, and see if any of the values match our SOUS name.
			String[] sousVals = mbAsset.getProdNm().split(DSMediaBinImporterV2.TOKENIZER);
			if (Arrays.asList(sousVals).contains(cleanSousName)) {
				//Product's SOUS matches at least one Mediabin record.  
				//Success!  Return; nothing else we need to do besides verifying we have at least one match.
				log.debug("SOUS match using " + cleanSousName + " for " + mbAsset.getDpySynMediaBinId());
				data.add(mbAsset.getDpySynMediaBinId());
			}
		}
		return data;
	}
}