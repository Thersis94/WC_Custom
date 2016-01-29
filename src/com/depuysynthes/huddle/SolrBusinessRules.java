package com.depuysynthes.huddle;

import java.util.Collection;

import com.depuysynthes.huddle.HuddleUtils.IndexType;
import com.depuysynthes.lucene.MediaBinSolrIndex.MediaBinField;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.search.SearchDocumentHandler;

/****************************************************************************
 * <b>Title</b>: SolrBusinessRules.java<p/>
 * <b>Description: leverage the rules we have for DSI, customized for Huddle.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Nov 25, 2015
 ****************************************************************************/
public class SolrBusinessRules extends com.depuysynthesinst.SolrBusinessRules {

	public SolrBusinessRules() {
		super();
	}
	
	/**
	 * Get the first image from the potential list in the supplied field.
	 */
	public String getFirstImage() {
		Collection<Object> images = sd.getFieldValues(HuddleUtils.SOLR_IMAGE_FIELD);
		if (images != null && images.size() > 0) return (String) images.toArray()[0];
		
		//return the system's default as a last resort
		return "/binary/themes/CUSTOM/DEPUY/DPY_SYN_HUDDLE/images/default-thumbnail.png";
	}
	
	public String getThumbnailPath() {
		String trackingNo = super.getThumbnailImg();
		if (trackingNo == null || trackingNo.length() < 3) return trackingNo;
		return "/binary/org/DPY_SYN_HUDDLE/mediabin/" + trackingNo.substring(0, 3) + "/" + trackingNo + ".jpg";
	}
	
	
	/**
	 * Get the proper destination url for the current solrDocument
	 */
	@Override
	public String getPageUrl() {
		IndexType type = IndexType.quietValueOf(StringUtil.checkVal(sd.getFieldValue(SearchDocumentHandler.INDEX_TYPE)));
		if (type == null) type = IndexType.MEDIA_BIN; //this should quickly fall through to the default
		
		switch(type) {
			case PRODUCT:
				return "/product/" + super.getQsPath() + sd.getFieldValue(SearchDocumentHandler.DOCUMENT_URL);
			
			case COURSE_CAL:
				return "/events/" + super.getQsPath() + sd.getFieldValue(SearchDocumentHandler.DOCUMENT_ID);
			
			case HUDDLE_BLOG:
				return "/news/" + super.getQsPath() + sd.getFieldValue(SearchDocumentHandler.DOCUMENT_ID);
				
			case HUDDLE_CONSULTANTS:
				return "/sales-consultants/" + super.getQsPath() + sd.getFieldValue(SearchDocumentHandler.DOCUMENT_ID);
					
			case MEDIA_BIN:
				String assetType = StringUtil.checkVal(sd.getFieldValue(MediaBinField.AssetType.getField())).toLowerCase();
				switch (assetType) {
					case "podcast":
					case "video":
						return HuddleUtils.ASSET_PG_ALIAS + super.getQsPath() + sd.getFieldValue(SearchDocumentHandler.DOCUMENT_ID);
					//default case here slips through and returns the asset's documentUrl
				}
			case QUICKSTREAM_DSI:
				String cmsType = StringUtil.checkVal(sd.getFieldValue(MediaBinField.AssetType.getField())).toLowerCase();
				switch(cmsType) {
					case "external site":
						// External sites will contain full urls in the document url field to be used on the page.
						return (String) sd.getFieldValue(SearchDocumentHandler.DOCUMENT_URL);
					default:
						// Internal cms documents will be found under the cmsPath via their title.
						return cmsPath + "/" + sd.getFieldValue(SearchDocumentHandler.TITLE);
				}
			default:
				return StringUtil.checkVal(sd.getFieldValue(SearchDocumentHandler.DOCUMENT_URL));
		}
	}
	
	
	/**
	 * regex the URL to see if we recognize the protocol.  App's have unique ones like depuy://
	 * @return
	 */
	public boolean isApp() {
		String url = StringUtil.checkVal(sd.getFieldValue(SearchDocumentHandler.DOCUMENT_URL));
		return (!url.matches("^(http://|https://|/(.*)$"));
	}
	
	
	/**
	 * abstract the URL we use for testing app-opens (like a constant)
	 * @return
	 */
	public String getAppWrapperUrl() {
		String url = StringUtil.checkVal(sd.getFieldValue(SearchDocumentHandler.DOCUMENT_URL));
		return "javascript:launchApp('" + url + "');";
	}
	
	
	/**
	 * leverage the INDEX_TYPE to determine which type of Favorite to use.  Unfortunately they don't correlate directly.
	 */
	@Override
	public String getFavoriteType() {
		IndexType type = IndexType.quietValueOf(StringUtil.checkVal(sd.getFieldValue(SearchDocumentHandler.INDEX_TYPE)));
		if (type == null) type = IndexType.MEDIA_BIN; //this should quickly fall through to the default
		
		switch (type) {
			case COURSE_CAL: return "EVENT";
			case MEDIA_BIN: return "MEDIABIN";
			case QUICKSTREAM_DSI: return "CMS";
			default:
				return type.toString();
		}
	}
}
