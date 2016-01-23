package com.depuysynthes.huddle;

import java.util.Collection;

import com.depuysynthes.huddle.solr.BlogSolrIndexer;
import com.depuysynthes.huddle.solr.HuddleProductCatalogSolrIndex;
import com.depuysynthes.lucene.MediaBinSolrIndex;
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
		switch(StringUtil.checkVal(sd.getFieldValue(SearchDocumentHandler.INDEX_TYPE))) {
			case HuddleProductCatalogSolrIndex.INDEX_TYPE:
				return "/product/" + super.getQsPath() + sd.getFieldValue(SearchDocumentHandler.DOCUMENT_URL);
				
			case BlogSolrIndexer.INDEX_TYPE:
				return "/news/" + super.getQsPath() + sd.getFieldValue(SearchDocumentHandler.DOCUMENT_ID);
				
			case HuddleUtils.SOLR_SALES_CONSULTANT_IDX_TYPE :
				return "/sales-consultants/" + super.getQsPath() + sd.getFieldValue(SearchDocumentHandler.DOCUMENT_ID);
	
			case MediaBinSolrIndex.INDEX_TYPE:
				//TODO anything but audio and video goes directly to the file
				//TODO accept apps, need a solution there!
				return "/asset/" + super.getQsPath() + sd.getFieldValue(SearchDocumentHandler.DOCUMENT_ID);
				
			default:
				return StringUtil.checkVal(sd.getFieldValue(SearchDocumentHandler.DOCUMENT_URL));
		}
	}
	
	/**
	 * leverage the INDEX_TYPE to determine which type of Favorite to use.  Unfortunately they don't correlate directly.
	 */
	@Override
	public String getFavoriteType() {
		String indexType = StringUtil.checkVal(getSd().get(SearchDocumentHandler.INDEX_TYPE));
		switch (indexType) {
			case "COURSE_CAL": return "EVENT";
			case "MEDIA_BIN": return "MEDIABIN";
			case "CMS_QUICKSTREAM": return "CMS";
			default:
				return indexType;
		}
	}
}
