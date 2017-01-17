package com.depuysynthes.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.depuysynthes.scripts.DSMediaBinImporterV2;
import com.siliconmtn.data.report.ExcelStyleFactory;
import com.siliconmtn.data.report.MultisheetExcelReport;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;

/****************************************************************************
 * <b>Title</b>: EMEAProductCatalogReport.java<p/>
 * <b>Description: Excel report for the EMEA product catalog.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 14, 2017
 * @update
 ****************************************************************************/
public class EMEAProductCatalogReport extends AbstractSBReportVO {

	private static final long serialVersionUID = 969372461467629288L;
	private transient List<EMEAProductCatalogReportVO> data;
	private String catalogNm;
	private transient MultisheetExcelReport rpt;

	public EMEAProductCatalogReport() {
		super();
		setContentType("application/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("EMEA Product Catalog Report.xls");
		rpt = new MultisheetExcelReport(ExcelStyleFactory.Styles.Standard);
	}

	/* (non-Javadoc)
	 * convert the html to a PDF, and return it
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("generateReport(): " + this.getClass());

		if (data != null && !data.isEmpty()) {
			rpt.addSheet("Products", "Web Crescendo Products - " + catalogNm, getProductsHdr(), getProductData());
			rpt.addSheet("Assets", "MediaBin Assets", getAssetHdr(), getAssetData());
		}

		return rpt.generateReport();
	}
	
	public void setCatalogName(String nm) {
		this.catalogNm = nm;
	}


	/**
	 * total searches column headers
	 * @return
	 */
	protected Map<String, String> getProductsHdr() {
		Map<String, String> row = new LinkedHashMap<>();
		row.put("prodId", "Product ID");
		row.put("prodNm", "Product Name");
		row.put("urlAlias", "URL Alias");
		row.put("sousName", "SOUS Product Name");
		row.put("types", "Attribute Type(s)");
		row.put("assetCnt", "Asset Count");
		row.put("hierarchy", "Body Region Hierarchy");
		return row;
	}


	/**
	 * total searches row data
	 * @return
	 */
	protected List<Map<String, Object>> getProductData() {
		List<Map<String, Object>> rows = new ArrayList<>();
		for (EMEAProductCatalogReportVO vo : data) {
			Map<String, Object> row = new HashMap<>();
			row.put("prodId", vo.getProductId());
			row.put("prodNm", vo.getProductName());
			row.put("urlAlias", vo.getUrlAlias());
			row.put("sousName", vo.getSousProductName());
			row.put("types", StringUtil.getToString(vo.getAttributeTypes(), false, false, ", "));
			row.put("assetCnt", vo.getAssetCount());
			row.put("hierarchy", StringUtil.replace(vo.getHierarchy(), DSMediaBinImporterV2.TOKENIZER, ", "));
			rows.add(row);
		}
		return rows;
	}


	/**
	 * total searches column headers
	 * @return
	 */
	protected Map<String, String> getAssetHdr() {
		Map<String, String> row = new LinkedHashMap<>();
		row.put("prodId", "Product ID");
		row.put("prodNm", "Product Name");
		row.put("trackingNo", "Tracking #");
		row.put("assetTitle", "Title");
		row.put("assetLang", "Language");
		row.put("assetDesc", "Asset Desc");
		row.put("assetUpdateDt", "Last Updated");
		row.put("assetFile", "LL File Path/Name");
		return row;
	}


	/**
	 * total searches row data
	 * @return
	 */
	protected List<Map<String, Object>> getAssetData() {
		List<Map<String, Object>> rows = new ArrayList<>(data.size());
		List<String> completedProds = new ArrayList<>(data.size());
		
		for (EMEAProductCatalogReportVO vo : data) {
			List<MediaBinAssetVO> assets = vo.getAssets();
			//skip products with no assets, or products we've already iterated. (sheet1 can contain duplicate entries)
			if (assets == null || assets.isEmpty() || completedProds.contains(vo.getProductId())) continue;
			completedProds.add(vo.getProductId());

			for (MediaBinAssetVO asset : assets) {
				Map<String, Object> row = new HashMap<>();
				row.put("prodId", vo.getProductId());
				row.put("prodNm", vo.getProductName());
				row.put("trackingNo", asset.getTrackingNoTxt());
				row.put("assetTitle", asset.getTitleTxt());
				row.put("assetLang", asset.getLanguageCode());
				row.put("assetDesc", asset.getAssetDesc());
				row.put("assetUpdateDt", Convert.formatDate(asset.getModifiedDt(), Convert.DATETIME_DASH_PATTERN));
				row.put("assetFile", asset.getAssetNm());
				rows.add(row);
			}
		}
		return rows;
	}


	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		data = (List<EMEAProductCatalogReportVO>) o;
		Collections.sort(data); //put the products into alphabetical order using the Compareable interface
	}
}