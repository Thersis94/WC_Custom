package com.depuysynthes.huddle;

import java.util.Collection;

import org.apache.solr.common.SolrDocument;

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
	public String getFirstImage(String imageField) {
		Collection<Object> images = super.getSd().getFieldValues(imageField);
		if (images != null) return (String) images.toArray()[0];		
		return "";
	}
	
	
	public String getThumbnailPath() {
		String trackingNo = super.getThumbnailImg();
		if (trackingNo == null || trackingNo.length() < 3) return trackingNo;
		
		return "/binary/org/DPY_SYN_HUDDLE/mediabin/" + trackingNo.substring(0, 3) + "/" + trackingNo + ".jpg";
	}
	
	/**
	 * Get the proper destination url for the current solrDocument
	 */
	public String getPageUrl() {
		SolrDocument sd = super.getSd();
		
		switch((String)sd.getFieldValue(SearchDocumentHandler.INDEX_TYPE)) {
		case MediaBinSolrIndex.INDEX_TYPE:
			return "/asset/" + super.getQsPath() + sd.getFieldValue(SearchDocumentHandler.DOCUMENT_ID);
			
		case HuddleProductCatalogSolrIndex.INDEX_TYPE:
			return "/product/" + super.getQsPath() + sd.getFieldValue(SearchDocumentHandler.DOCUMENT_ID);
			
		case BlogSolrIndexer.INDEX_TYPE:
			return "/news/" + super.getQsPath() + sd.getFieldValue(SearchDocumentHandler.DOCUMENT_ID);
			
		case HuddleUtils.SOLR_SALES_CONSULTANT_IDEX_TYPE :
			return "/sales-consultants/" + super.getQsPath() + sd.getFieldValue(SearchDocumentHandler.DOCUMENT_ID);
			
		default:
			return "";
			
		}
	}
	
	/**
	 * leverage the INDEX_TYPE to determine which type of Favorite to use.  Unfortunately they don't correlate directly.
	 */
	public String getFavoriteType() {
		String indexType = StringUtil.checkVal(getSd().get(SearchDocumentHandler.INDEX_TYPE));
		switch (indexType) {
			case "COURSE_CAL": return "EVENT";
			case "MEDIA_BIN": return "MEDIABIN";
			default:
				return indexType;
		}
	}
}
