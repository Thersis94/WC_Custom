package com.depuysynthesinst;

import com.siliconmtn.annotations.SolrField;
import com.siliconmtn.cms.TemplateFieldVO;
import com.siliconmtn.cms.TemplateFieldVOContainer;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.cms.CMSContentVO;
import com.smt.sitebuilder.action.cms.CMSSolrDocumentVO;
import com.smt.sitebuilder.search.SearchDocumentHandler;

/****************************************************************************
 * <b>Title</b>: QuickstreamTemplate.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Mar 1, 2015
 ****************************************************************************/
public class QuickstreamTemplate extends CMSSolrDocumentVO {

	private String assetType;
	protected String assetUrl;
	private String trackingNo;
	
	/**
	 * @param solrIndex
	 */
	public QuickstreamTemplate(String indexType) {
		super(indexType);
	}
	
	public QuickstreamTemplate() {
		super();
	}
	
	
	/**
	 * extension of superclass implementation; for DSI-specific template fields
	 */
	public void setData(Object o) {
		//this is for debugging; trying to track down an NPE - JM 03.26.15
		if (o == null) {
			log.error("passed a null vo, figure out why.");
			return;
		}
		super.setData(o);
		CMSContentVO vo = (CMSContentVO) o;
		
		TemplateFieldVOContainer templateData = vo.getTemplateData();
		//this is for debugging; trying to track down an NPE - JM 03.26.15
		if (templateData == null) {
			log.error("no template data passed on VO");
			return;
		}
		
		if (vo.getArticle() != null)
			super.setSummary(StringUtil.checkVal(vo.getArticle().toString()));
		
		
		//some core fields are provided here-in:
		for (TemplateFieldVO field : templateData.getContainerData()) {
			if (field == null || field.getFieldName() == null) continue;

			switch (field.getFieldName()) {
				case "Hierarchy":
					this.parseHierarchies(StringUtil.checkVal(field.getFieldValue()));
					break;
				case "Asset URL":
					setAssetUrl(StringUtil.checkVal(field.getFieldValue()));
					break;
				case "Tracking Number":
					setTrackingNo(StringUtil.checkVal(field.getFieldValue()));
					break;
				case "Asset Type":
					setAssetType(StringUtil.checkVal(field.getFieldValue())); 
					break;
			}
		}
	}
	
	
	private void parseHierarchies(String val) {
		StringBuilder sb = null;
		for (String s : val.split("~")) { //the ~ here is NOT our hierarchy delimiter; it comes from MediaBin's business rules, not ours.
    			//need to tokenize the levels and trim spaces from each, the MB team are slobs!
    			sb = new StringBuilder();
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

	@Override
	@SolrField(name=SearchDocumentHandler.DOCUMENT_URL)
	public String getDocumentUrl() {
		//if there is nothing in assetUrl, return documentUrl, which is the /docs/ path to a likely XLS or PDF file
		if (assetUrl != null && assetUrl.length() > 0) {
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