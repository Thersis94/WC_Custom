package com.depuysynthes.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.depuysynthes.scripts.DSMediaBinImporterV2;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.commerce.catalog.ProductAttributeContainer;
import com.siliconmtn.commerce.catalog.ProductAttributeVO;
import com.siliconmtn.commerce.catalog.ProductCategoryVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.commerce.product.ProductCatalogAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: EMEAProductCatalogReportAction.java<p/>
 * <b>Description: Report/DataTool action for EMEA that returns an Excel representation
 * of their product catalog married to MediaBin for assets.  They use it for QA and
 * auditing Showpad.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 14, 2017
 * @update
 ****************************************************************************/
public class EMEAProductCatalogReportAction extends SimpleActionAdapter {

	private List<EMEAProductCatalogReportVO> data;
	private List<MediaBinAssetVO> mediabinAssets;


	/*
	 * The constant used for the MEDIABIN product attribute type - comes from the database
	 */
	//duplicated in ds-huddle-> HuddleUtils - consolidate after branches merge
	protected static final String MEDIABIN_ATTR_TYPE = "MEDIABIN";


	/*
	 * The constant used for the HTML product attribute type - comes from the database
	 */
	//duplicated in ds-huddle-> HuddleUtils
	protected static final String HTML_ATTR_TYPE = "HTML";


	/*
	 * Regex used when parsing static HTML to find links to Mediabin assets
	 * Reads: /json URL, amid=MEDIA_BIN_AJAX is present BEFORE mbid
	 * 	grab mbid value up to the next & or ' or " (single or double quote presumed to end the href attribute, & to begin the next request param)
	 * max-length of value at 32 chars, which is the pkId limit in the database 
	 */
	//duplicated in ds-huddle-> ShowpadProductDecorator
	protected static final String MB_AJAX_HREF = "/json\\?([^'\"]+)?amid=MEDIA_BIN_AJAX([^'\"]+)?&(amp;)?mbid=([^&\\s'\"]{1,32})";



	public EMEAProductCatalogReportAction() {
		super();
	}

	public EMEAProductCatalogReportAction(ActionInitVO arg0) {
		super(arg0);
	}


	/**
	 * generates the report
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		//load the entire catalog
		Tree catalog = loadCatalog(req);

		data = new ArrayList<>(); //init the data container for the report
		parseProductCatalog(catalog);

		//now that we have the list of products, let's go to Mediabin for assets for each
		mediabinAssets = loadMediabinAssets(req);
		bindMediaBinAssets();

		EMEAProductCatalogReport rpt = new EMEAProductCatalogReport();
		rpt.setData(data);
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, true);
		req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
	}


	/**
	 * loops through the products already in the report, 
	 * finds mediabin assets, either static or via SOUS Product Name, 
	 * and adds them to the VO.
	 */
	protected void bindMediaBinAssets() {
		for (EMEAProductCatalogReportVO vo : data)
			attachAssets(vo);
	}


	/**
	 * Parse the product tree (hierarchy) into a flatten list of products we care about.
	 * Care it taken here to capture and preserve the hierarchy levels, since we need to 
	 * push those across to Mediabin.
	 * @param t
	 */
	protected void parseProductCatalog(Tree t) {
		//find the "Body Region" node, and start the parsing from there.
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
	protected void parseNode(Node thisNode) {
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
			nextNode.setFullPath(path);

			parseNode(nextNode);
		}
	}


	/**
	 * Iterates the products found in the given lowest-level Category (Node).
	 * @param n
	 */
	protected void parseProduct(Node n) {
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

			List<ProductAttributeVO> eligibleAttrs = getMediabinAttributes(prod, false);


			data.add(new EMEAProductCatalogReportVO(prod, eligibleAttrs));
		}
	}


	/**
	 * tests the product to ensure it has at least one "MEDIABIN" Attribute.
	 * If it does we're going to keep it for future tasks.  If it doesn't its dead to us and can be discarded.
	 * @param prod
	 */
	protected List<ProductAttributeVO> getMediabinAttributes(ProductVO prod, boolean firstOnly) {
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
	 * marries mediabin assets to the product/VO either by parsing the static html,
	 * or via SOUS Product Name.
	 * @param vo
	 */
	protected void attachAssets(EMEAProductCatalogReportVO vo) {
		if (vo.getAttrs() == null || vo.getAttrs().isEmpty()) return;

		Set<String> assetIds = new HashSet<>();
		for (ProductAttributeVO attr : vo.getAttrs()) {
			Collection<String> foundIds;
			switch (StringUtil.checkVal(attr.getTitle())) {
			case "mediabin-static":
				foundIds = convertFromJson(attr.getValueText());
				break;
			case "mediabin-sous":
				//take the update date of the Attribute if it's newer than the product and contains assets.
				foundIds = findAssetsForSous(vo.getSousProductName());
				break;
			default:
				//static HTML, use a regex to parse the HTML for mediabin links
				foundIds = findAssetsInHtml(attr.getValueText());
			}
			assetIds.addAll(foundIds);
		}

		if (!assetIds.isEmpty())
			marryAssetsToProduct(vo, assetIds);
	}


	/**
	 * loops the mediabin assets and attaches the necessary ones onto the product
	 * @param vo
	 * @param assetIds
	 */
	protected void marryAssetsToProduct(EMEAProductCatalogReportVO vo, Set<String> assetIds) {
		List<MediaBinAssetVO> assets = new ArrayList<>(assetIds.size());
		for (MediaBinAssetVO mbAsset : mediabinAssets) {
			if (assetIds.contains(mbAsset.getDpySynMediaBinId()))
				assets.add(mbAsset);
		}
		vo.setAssets(assets);
	}


	/**
	 * parses the json object from a String and returns a Set of dpy_syn_mediabin_id values
	 * @param jsonText
	 * @return
	 */
	protected List<String> convertFromJson(String jsonText) {
		try {
			return DSProductCatalogAction.convertFromJSON(jsonText);
		} catch (InvalidDataException ide) {
			log.error("could not parse JSON stored on the Product record", ide);
		}
		return Collections.emptyList();
	}


	/**
	 * finds mediabin assets matching the given sous product name.
	 * returns a List<String> of the assetIds found.
	 * @param masterRecords
	 * @param prodSousName
	 * @return
	 */
	protected List<String> findAssetsForSous(String prodSousName) {
		List<String> data = new ArrayList<>();

		for (MediaBinAssetVO mbAsset : mediabinAssets) {

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
	 * finds assets in HTML using a regex and returns them in a List<String>
	 * @param masterRecords
	 * @param prodSousName
	 * @return
	 */
	protected List<String> findAssetsInHtml(String html) {
		List<String> data = new ArrayList<>();

		Matcher m = Pattern.compile(MB_AJAX_HREF).matcher(html);
		while (m.find()) {
			data.add(m.group(4));
			log.debug("found embedded link to asset " + m.group(4));
		}

		return data;
	}


	/**
	 * loads the entire catalog using the core action
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	protected Tree loadCatalog(SMTServletRequest req) throws ActionException {
		String catalogId = req.getParameter("catalogId");

		ProductCatalogAction act = new ProductCatalogAction();
		act.setAttributes(getAttributes());
		act.setDBConnection(dbConn);
		return act.loadEntireCatalog(catalogId, true, req);
	}


	/**
	 * loads the list of mediabin assets
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	@SuppressWarnings("unchecked")
	protected List<MediaBinAssetVO> loadMediabinAssets(SMTServletRequest req) throws ActionException {
		MediaBinAdminAction act = new MediaBinAdminAction();
		act.setAttributes(getAttributes());
		act.setDBConnection(dbConn);
		act.list(req);
		ModuleVO mod = (ModuleVO) act.getAttribute(Constants.MODULE_DATA);
		return (List<MediaBinAssetVO>) mod.getActionData();
	}
}