package com.depuysynthesinst;

import com.depuysynthes.scripts.DSMediaBinImporterV2;
import com.siliconmtn.annotations.SolrField;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.content.DocumentSolrVO;
import com.smt.sitebuilder.search.SearchDocumentHandler;

/****************************************************************************
 * <b>Title</b>: DSIDocumentTemplate.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Mar 1, 2015
 ****************************************************************************/
public class DSIDocumentTemplate extends DocumentSolrVO {

	private String assetType;
	protected String assetUrl;
	private String trackingNo;

	public DSIDocumentTemplate() {
		super();
		this.initDSI(); //'this' used intentionally
	}

	/**
	 * populates some custom meta-data setter methods for the superclass to invoke dynamically.
	 * put in an init method because we have two constructors that both need to support this. 
	 */
	private final void initDSI() {
		attributeSetters.put("Hierarchy", "parseHierarchies");
		attributeSetters.put("Asset URL", "setAssetUrl");
		attributeSetters.put("Tracking Number", "setTrackingNo");
		attributeSetters.put("Asset Type", "setAssetType");
	}


	public void parseHierarchies(String val) {
		StringBuilder sb;
		for (String s : val.split(DSMediaBinImporterV2.TOKENIZER)) { //the ~ comes from MediaBin's business rules, not ours.
			//need to tokenize the levels and trim spaces from each, the MB team are slobs!
			sb = new StringBuilder(200);
			for (String subStr : s.split(",")) {
				sb.append(StringUtil.checkVal(subStr).trim()).append(SearchDocumentHandler.HIERARCHY_DELIMITER);
			}
			if (sb.length() >= SearchDocumentHandler.HIERARCHY_DELIMITER.length())
				sb.deleteCharAt(sb.length()-SearchDocumentHandler.HIERARCHY_DELIMITER.length());

			super.addHierarchies(sb.toString());
		}
	}


	@SolrField(name="assetType_s")
	public String getAssetType() {
		return assetType;
	}
	public void setAssetType(String assetType) {
		this.assetType = assetType;
	}


	@SolrField(name=SearchDocumentHandler.DOCUMENT_URL)
	@Override
	public String getDocumentUrl() {
		//if there is nothing in assetUrl, return documentUrl, which is the /docs/ path to a likely XLS or PDF file
		if (!StringUtil.isEmpty(assetUrl)) {
			return assetUrl;
		} else {
			return super.getDocumentUrl();
		}
	}
	public void setAssetUrl(String assetUrl) {
		this.assetUrl = assetUrl;
	}


	@SolrField(name="trackingNumber_s")
	public String getTrackingNo() {
		return trackingNo;
	}
	public void setTrackingNo(String trackingNo) {
		this.trackingNo = trackingNo;
	}
}