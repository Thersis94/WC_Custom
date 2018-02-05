package com.depuysynthes.scripts;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.depuysynthes.scripts.showpad.CatalogReconcileUtil;
import com.depuysynthes.scripts.showpad.ShowpadProductDecorator;
import com.depuysynthes.scripts.showpad.ShowpadProductDecoratorUnpublished;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.data.Tree;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> MediaBinReportEmail.java<br/>
 * <b>Description:</b> Sends an email to the EMEA team after the Showpad/Mediabin imports
 * have completed.  The email summarizes conjointed public & private data w/Showpad data into
 * a meaningful format (for EMEA).
 * This class simply loads data from the database and sends an email.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Jan 17, 2018
 ****************************************************************************/
public class MediaBinReportEmail extends CommandLineUtil {

	protected List<ProductVO> unpublishedProducts = new ArrayList<>(2000);
	protected List<ProductVO> publicProducts = new ArrayList<>(2000);

	private Map<String, MediaBinDeltaVO> publicAssets = new HashMap<>(4000);
	private Map<String, MediaBinDeltaVO> privateAssets = new HashMap<>(1000);

	protected CatalogReconcileUtil prodUtil;

	/**
	 * @param args
	 */
	public MediaBinReportEmail(String[] args) {
		super(args);
		loadProperties("scripts/MediaBin.properties");
		loadDBConnection(props);
		prodUtil = new CatalogReconcileUtil(dbConn, props, true);
		
		//reposition values needed for the email
		props.put("toAddress", props.get("reportEmailRcpt"));
		props.put("subject", props.get("reportEmailSubj"));
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MediaBinReportEmail eml = new MediaBinReportEmail(args);
		eml.run();
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		StringBuilder html = new StringBuilder(10000);

		//populate the public product catalog
		Tree t = prodUtil.loadProductCatalog(ShowpadProductDecorator.CATALOG_ID);
		prodUtil.parseProductCatalog(t, publicProducts);
		log.info("loaded " + publicProducts.size() + " mediabin-using products in catalog " + ShowpadProductDecorator.CATALOG_ID);

		//load the public assets from the DB
		loadAssets(2, publicAssets); //import_type_cd=2 is EMEA public assets

		//for each public products that we know has assets, tag those assets.
		//create a list of products that have NO assets based off this marriage
		List<ProductVO> unwedProducts = pruneCatalogOfAssets(publicProducts, publicAssets);

		//add table to email
		addProductsWithNoAssetsToEmail("Public", html, unwedProducts);

		//populate the unpublished product catalog
		t = prodUtil.loadProductCatalog(ShowpadProductDecoratorUnpublished.PRIV_CATALOG_ID);
		prodUtil.parseProductCatalog(t, unpublishedProducts);
		log.info("loaded " + unpublishedProducts.size() + " mediabin-using products in catalog " + ShowpadProductDecoratorUnpublished.PRIV_CATALOG_ID);

		//load the private assets from the DB
		loadAssets(3, privateAssets);

		//for each unpublished products that we know has assets, tag those assets.
		//create a list of products that have NO assets based off this marriage
		Map<String, MediaBinDeltaVO> allAssets = new HashMap<>(privateAssets.size()+publicAssets.size(),100f);
		allAssets.putAll(publicAssets);
		allAssets.putAll(privateAssets);
		unwedProducts = pruneCatalogOfAssets(unpublishedProducts, allAssets);

		//add table to email
		addProductsWithNoAssetsToEmail("Unpublished", html, unwedProducts);

		//isolate assets not used by products in either catalog
		pruneAssetsUsedInCatalogs(allAssets, publicProducts);
		pruneAssetsUsedInCatalogs(allAssets, unpublishedProducts);

		//add table to email
		addAssetsWithNoProductsToEmail(html, allAssets);

		//send the email
		try {
			sendEmail(html, null);
		} catch (Exception e) {
			log.error("could not send email report", e);
		}
	}


	/**
	 * Removes assets from the given list which are used in products
	 * @param allAssets
	 * @param products
	 */
	@SuppressWarnings("unchecked")
	private void pruneAssetsUsedInCatalogs(Map<String, MediaBinDeltaVO> allAssets, List<ProductVO> products) {
		log.debug("allSize=" + allAssets.size());
		for (ProductVO prod : products) {
			List<String> assetIds = (List<String>) prod.getProdAttribute("assetIds");
			if (assetIds != null && !assetIds.isEmpty()) {
				for (String id : assetIds)
					allAssets.remove(id);
			}
		}
		log.debug("allSize2=" + allAssets.size());
	}


	/**
	 * does a cross check of the products to see which ones DO NOT have assets associated to them.
	 * returns a pruned list of products that DO NOT have assets - which goes in the email report.
	 * @param publicProducts
	 * @param publicAssets
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<ProductVO> pruneCatalogOfAssets(List<ProductVO> products, Map<String, MediaBinDeltaVO> assets) {
		List<ProductVO> unwedProducts = new ArrayList<>(products.size());
		List<String> assetIds;
		for (ProductVO prod : products) {
			assetIds = null;
			//if the product has Attributes that likely contain Assets - parse to find out which assets.
			if (Convert.formatBoolean(prod.getProdAttribute("hasAttributes"))) {
				prodUtil.tagProductAssets(assets, prod);
				assetIds = (List<String>) prod.getProdAttribute("assetIds");
			}
			//if the product indeed has zero assets, even after parsing, it goes in the report
			if (assetIds == null || assetIds.isEmpty())
				unwedProducts.add(prod);
		}
		return unwedProducts;
	}


	/**
	 * loads the list of mediabin assets from the database
	 * @param typeCd (2=EMEA public, 3=EMEA private)
	 * @param publicAssets2
	 */
	private void loadAssets(int typeCd, Map<String, MediaBinDeltaVO> data) {
		StringBuilder sql = new StringBuilder(250);
		sql.append("select * from ").append(props.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("dpy_syn_mediabin where import_file_cd=?");
		log.debug(sql);

		MediaBinDeltaVO vo;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1,  typeCd);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				vo = new MediaBinDeltaVO(rs);
				data.put(vo.getDpySynMediaBinId(), vo);
			}

		} catch (SQLException sqle) {
			log.error("could not load assets, typeCd=" + typeCd, sqle);
		}

		log.info("loaded " + data.size() + " assets for type=" + typeCd);
	}


	/**
	 * Appends a table to the email notification for products containing SOUS product name
	 * that doesn't match any mediabin assets.
	 * @param html
	 */
	private void addProductsWithNoAssetsToEmail(String title, StringBuilder html, List<ProductVO> products) {
		if (products.isEmpty()) return;
		
		//put them in an order, and remove duplicates
		Map<String, String> orderedList = new TreeMap<>();
		for (ProductVO prod : products) {
			orderedList.put(scrubString(prod.getProductName()), scrubString(prod.getFullProductName()));
		}

		html.append("<h4>").append(title).append(" Products with no MediaBin Assets (");
		html.append(orderedList.size()).append(")</h4>");
		html.append("<table border='1' width='95%' align='center'><thead><tr>");
		html.append("<th>Product Name(s)</th>");
		html.append("<th>SOUS Product Name</th>");
		html.append("</tr></thead><tbody>");

		for (Map.Entry<String, String> entry : orderedList.entrySet()) {
			html.append("<tr><td>").append(entry.getKey()).append("</td>");
			html.append("<td>").append(entry.getValue()).append("</td></tr>");
		}
		html.append("</tbody></table><br/><br/>");
	}


	/**
	 * Appends a table to the email notification assets that aren't used by products in either catalog.
	 * We consolidate these by SOUS Name (from Mediabin), 1->N at the SOUS Name->Tracking# level.
	 * @param html
	 * @param allAssets
	 */
	private void addAssetsWithNoProductsToEmail(StringBuilder html, Map<String, MediaBinDeltaVO> allAssets) {
		if (allAssets.isEmpty()) return;
		
		List<String> privAssets = new ArrayList<>();

		//consolidate the rows to prevent duplicates
		Map<String, Set<String>> rows = new TreeMap<>();
		for (Map.Entry<String, MediaBinDeltaVO> entry : allAssets.entrySet()) {
			MediaBinDeltaVO vo = entry.getValue();
			String rawName = StringUtil.checkVal(vo.getProdNm());
			
			//split the product name field using the tokenizer - it could contain multiple SOUS names.
			String[] sousNamesArr = rawName.split(DSMediaBinImporterV2.TOKENIZER);
			for (String name : sousNamesArr) {
				if (!isQualifiedSousValue(name)) name="";
				
				Set<String> sousNames = rows.get(name);
				if (sousNames == null) sousNames = new HashSet<>();
				sousNames.add(vo.getTrackingNoTxt());
				rows.put(name, sousNames);
				if (vo.getImportFileCd() == 3)
					privAssets.add(vo.getTrackingNoTxt());
			}
		}

		html.append("<h4>Assets not used by any Products (");
		html.append(allAssets.size()).append(")</h4>"); //using allAssets here - count of mediabin tracking#s, not unique SOUS names.
		html.append("The following assets are not matching to Products in Web Crescendo:<br/>\r\n");
		html.append("Note: <font color=\"red\">Red tracking numbers</font> indicate private assets - you won't ");
		html.append("find these in Web Crescendo but they could still match using SOUS Product Name.<br/>\r\n");
		html.append("<table border='1' width='95%' align='center'><thead><tr>");
		html.append("<th>SOUS Product Name</th>");
		html.append("<th>Mediabin Tracking #(s)</th>");
		html.append("</tr></thead>\r\n<tbody>");

		for (Entry<String, Set<String>> entry : rows.entrySet()) {
			Set<String> trackingNos = entry.getValue();
			String[] array = trackingNos.toArray(new String[trackingNos.size()]);
			html.append("<tr><td>").append(scrubString(entry.getKey())).append("</td>");
			html.append("<td>");
			for (int x=0; x < array.length; x++) {
				if (x > 0) html.append(", ");
				if (privAssets.contains(array[x])) {
					html.append("<font color=\"red\">").append(scrubString(array[x])).append("</font>");
				} else {
					html.append(scrubString(array[x]));
				}
			}
			html.append("</td></tr>\r\n");
		}
		html.append("</tbody></table>\r\n<br/>");
		
		html.append("<br/>Note: SOUS Product Name matching is case sensitive.  Pay attention to font case ");
		html.append("and check for trailing white-space if an expected SOUS match is not occuring.<br/>");
	}
	
	
	/**
	 * Tests the String from the EXP file against business rules of values to ignore.
	 * Angi: It would be great if you could filter all SOUS – Product Names 
	 * only containing a number out of the report list e.g. “319.010”
	 * @param sousValue
	 * @return
	 */
	protected boolean isQualifiedSousValue(String sousVal) {
		if (StringUtil.isEmpty(sousVal)) return false;
		//remove dots and dashes that commonly appear in number sequences.  e.g. "319.010"
		String val = StringUtil.removeNonAlphaNumeric(sousVal);
		//if all we have is numbers, this is not a qualified sous value
		return !val.matches("[0-9]+");
	}


	/**
	 * returns a cleaned-up version of the string.  remove trademarks, etc. then lowercases
	 * @param sous
	 * @return
	 */
	private String scrubString(String sous) {
		return StringEncoder.encodeExtendedAscii(sous);
	}
}