package com.depuysynthes.scripts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.depuysynthes.action.EMEACarouselAction;
import com.depuysynthes.scripts.MediaBinDeltaVO.State;
import com.siliconmtn.commerce.catalog.ProductAttributeContainer;
import com.siliconmtn.commerce.catalog.ProductAttributeVO;
import com.siliconmtn.commerce.catalog.ProductCategoryVO;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.pool.SMTDBConnection;
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

	protected List<ProductVO> products = new ArrayList<>(); //contains products with their flattened hierarchy, who use the special MEDIABIN Attribute.
	protected Map<String, Set<String>> mediabinSOUSNames = new HashMap<>();  //contents = <SOUS Name, String[] tracking#s>
	protected Map<String, Set<String>> productSOUSNames = new HashMap<>(); //contents = <SOUS Name, String[] productName>
	
	protected final String catalogId = EMEACarouselAction.CATALOG_ID; //the product catalog this script is hard-coded around
	
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
		pca = null;
		
		parseProductCatalog(t);
		log.info("loaded " + products.size() + " mediabin-using products in catalog " + catalogId);
		
		return super.loadManifest();
	}
	

	@Override
	public void saveRecords(Map<String, MediaBinDeltaVO> masterRecords, boolean isInsert) {
		super.saveRecords(masterRecords, isInsert);

		//the below logic will process both inserts & updates at once.  
		//Block here for updates so we don't process the records twice.
		//Insert runs after deletes & updates, so wait for the 'inserts' invocation so 
		//all the mediabin records are already in our database.
		if (!isInsert) return;
		
		//replicate product (tag) changes out to Showpad
		
		//parse the product list for SOUS products containing no MediaBin assets
		findEmptyProducts(masterRecords);
		
		//parse the Mediabin database for Assets who's SOUS Product Name is not used in the product catalog
		findUnusedAssets(masterRecords);
	}
	
	
	/**
	 * Parse the product tree (hierarchy) into a flatten list of products we care about.
	 * Care it taken here to capture and preserve the hierarchy levels, since we need to 
	 * push those across to Mediabin.
	 * @param t
	 */
	private void parseProductCatalog(Tree t) {
		parseNode(t.getRootNode());
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
			String path = StringUtil.checkVal(nextNode.getFullPath());
			if (!path.isEmpty()) path += DSMediaBinImporterV2.TOKENIZER;
			path += thisNode.getNodeName();
			log.debug("path=" + path);
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
		
		for (ProductVO prod : cat.getProducts()) {
			prod.setProductName(n.getNodeName()); //preserve the name, it is not populated by the initial catalog load
			prod.setAttrib1Txt(n.getFullPath()); //pass the parent hierarchy from the Node down to the Product.
			
			testAttributes(prod);
		}
	}
	
	
	/**
	 * tests the product to ensure it has at least one "MEDIABIN" Attribute.
	 * If it does we're going to keep it for future tasks.  If it doesn't its dead to us and can be discarded.
	 * @param prod
	 */
	private void testAttributes(ProductVO prod) {
		ProductAttributeVO attrVo;
		ProductAttributeContainer pac = prod.getAttributes();
		for (Node n : pac.getAllAttributes()) {
			attrVo = (ProductAttributeVO) n.getUserObject();
			if (attrVo.getProductAttributeId() == null || ! "MEDIABIN".equals(attrVo.getAttributeType())) continue; //not what we're looking for
			
			//found one, exit the loop b/c this product needs to be tested.
			products.add(prod);
			break;
		}
	}

	
	/**
	 * Iterate the products. For each determine if we have Mediabin assets matching
	 * it's SOUS Product Name.  If we don't report them to the Admins.
	 * @param masterRecords
	 */
	private void findEmptyProducts(Map<String, MediaBinDeltaVO> masterRecords) {
		String name, sousName;
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
	private void checkProdSousAgainstMediabin(Map<String, MediaBinDeltaVO> masterRecords, String sousName, String prodName) {
			for (MediaBinDeltaVO mbAsset : masterRecords.values()) {
				if (State.Delete == mbAsset.getRecordState() || StringUtil.checkVal(mbAsset.getProdNm()).isEmpty())
					continue; //nothing to do here, this asset has no SOUS value, or is being deleted.

				//split the product name field using the tokenizer, and see if any of the values match our SOUS name.
				String[] sousVals = mbAsset.getProdNm().split(DSMediaBinImporterV2.TOKENIZER);
				if (Arrays.asList(sousVals).contains(sousName)) {
					//Product's SOUS matches at least one Mediabin record.  
					//Success!  Return; nothing else we need to do besides verifying we have at least one match.
					log.debug("Product SOUS matches Mediabin SOUS using " + sousName);
					return;
				}
			}
			//finished checking all assets.  Apparently none of them use the same 
			//SOUS name the product uses.  Let's report these to the Admins.
			log.debug("no Mediabin matches for PROD_NM=" + sousName);
			Set<String> prodNames = productSOUSNames.get(sousName);
			if (prodNames == null) prodNames = new HashSet<>();
			prodNames.add(prodName);
			productSOUSNames.put(sousName, prodNames);
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
				Set<String> trackingNos = mediabinSOUSNames.get(val);
				if (trackingNos == null) trackingNos = new HashSet<>();
				trackingNos.add(mbAsset.getTrackingNoTxt());
				mediabinSOUSNames.put(val, trackingNos);
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
		html.append("<h4>Products with no MediaBin Assets</h4>");
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
		html.append("</tbody></table><hr/>");
	}
	
	
	/**
	 * Appends a table to the email notification for products containing SOUS product name
	 * that doesn't match any mediabin assets.
	 * @param html
	 */
	private void addAssetsWithNoProductsToEmail(StringBuilder html) {
		html.append("<h4>Mediabin SOUS Values not used by Products</h4>");
		html.append("The following MediaBin assets indicate a SOUS ");
		html.append("Product Name not matching any Web Crescendo products.<br/>");
		html.append("<table border='1' width='95%' align='center'><thead><tr>");
		html.append("<th>SOUS Product Name</th>");
		html.append("<th>Mediabin Tracking #(s)</th>");
		html.append("</tr></thead><tbody>");
		
		for (Entry<String, Set<String>> entry : mediabinSOUSNames.entrySet()) {
			Set<String> prodNames = entry.getValue();
			String[] array = prodNames.toArray(new String[prodNames.size()]);
			html.append("<tr><td>").append(entry.getKey()).append("</td>");
			html.append("<td>").append(StringUtil.getToString(array, false, false, ", ")).append("</td></tr>");
		}
		html.append("</tbody></table><hr/>");
	}
}