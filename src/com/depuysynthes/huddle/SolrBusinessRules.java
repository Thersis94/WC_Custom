package com.depuysynthes.huddle;

import java.util.Collection;

import com.depuysynthes.huddle.HuddleUtils.IndexType;
import com.depuysynthes.lucene.MediaBinSolrIndex.MediaBinField;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SecurityController;

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

	protected String cmsPath = "";
	protected String shareUrl; //different from pageUrl, for MB assets these reference LL directly
	
	public SolrBusinessRules() {
		super();
	}
	
	@Override
	protected void buildSectionName() {
		//overrides parent method, not needed for Huddle
	}
	
	@Override
	protected void buildPageUrl() {
		shareUrl = null; //flush this since we may not be setting it for the given asset, this bean gets reused in views
		
		IndexType type = IndexType.quietValueOf(StringUtil.checkVal(sd.getFieldValue(SearchDocumentHandler.INDEX_TYPE)));
		if (type == null) type = IndexType.MEDIA_BIN; //this should quickly fall through to the default
		
		indexType: switch(type) {
			case PRODUCT:
				pageUrl = "/product/" + super.getQsPath() + sd.getFieldValue(SearchDocumentHandler.DOCUMENT_URL);
				break;
			
			case COURSE_CAL:
				pageUrl =  "/events/" + super.getQsPath() + sd.getFieldValue(SearchDocumentHandler.DOCUMENT_ID);
				break;
			
			case HUDDLE_BLOG:
				pageUrl = "/news/" + super.getQsPath() + sd.getFieldValue(SearchDocumentHandler.DOCUMENT_ID);
				break;
				
			case HUDDLE_CONSULTANTS:
				pageUrl = "/sales-consultants/" + super.getQsPath() + sd.getFieldValue(SearchDocumentHandler.DOCUMENT_ID);
				break;

			case FORM:
				pageUrl = "/forms/" + super.getQsPath() + sd.getFieldValue(SearchDocumentHandler.DOCUMENT_ID);
				break;
					
			case MEDIA_BIN:
				String assetType = StringUtil.checkVal(sd.getFieldValue(MediaBinField.AssetType.getField())).toLowerCase();
				switch (assetType) {
					case "podcast":
					case "video":
						String url = HuddleUtils.ASSET_PG_ALIAS + super.getQsPath() + sd.getFieldValue(SearchDocumentHandler.DOCUMENT_ID);
						pageUrl = url;
						shareUrl = StringUtil.checkVal(sd.getFieldValue(SearchDocumentHandler.DOCUMENT_URL));
						break indexType;
					//default case here slips through and returns the asset's documentUrl
				}
				
			case CMS_QUICKSTREAM:
				String cmsType = StringUtil.checkVal(sd.getFieldValue(MediaBinField.AssetType.getField())).toLowerCase();
				switch(cmsType) {
					case "external site":
						// External sites will contain full urls in the document url field to be used on the page.
						pageUrl = StringUtil.checkVal(sd.getFieldValue("asset_url_s"));
						break indexType;
					//case "app":
						//let these go to the CMS document /docs/, which can explain the app and apply the link text as static html
						//it makes for a better user experience, and supports favoriting
						
					//default:
						// Internal cms documents have the correct url in their doucmentUrl, slide-through
						//pageUrl = StringUtil.checkVal(sd.getFieldValue(SearchDocumentHandler.DOCUMENT_URL));
						//break;
				}
				
			default:
				pageUrl = StringUtil.checkVal(sd.getFieldValue(SearchDocumentHandler.DOCUMENT_URL));
				break;
		}
	}
	
	
	/**
	 * returns a sometimes-different URL for SHARING the asset than viewing it on Huddle (pageUrl).
	 * This is particularly important when we share Mediabin assets via email-a-friend ('share')
	 * @return
	 */
	public String getShareUrl() {
		if (shareUrl != null) return shareUrl;
		else return getPageUrl();
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
	 * leverage the INDEX_TYPE to determine which type of Favorite to use.  Unfortunately they don't correlate directly.
	 */
	@Override
	public String getFavoriteType() {
		IndexType type = IndexType.quietValueOf(StringUtil.checkVal(sd.getFieldValue(SearchDocumentHandler.INDEX_TYPE)));
		if (type == null) type = IndexType.MEDIA_BIN; //this should quickly fall through to the default
		
		switch (type) {
			case COURSE_CAL: return "EVENT";
			case MEDIA_BIN: return "MEDIABIN";
			case CMS_QUICKSTREAM: return "CMS";
			default:
				return type.toString();
		}
	}
	
	
	/**
	 * Check the hierarchies on the document to try and find the document's family
	 * @param includeLineBreak 
	 * @return
	 */
	public String getFamilyName() {
		String family = "";
		Collection<Object> hierarchies = sd.getFieldValues(SearchDocumentHandler.HIERARCHY);
		if (hierarchies == null || hierarchies.size() == 0) return "";
		
		for (Object o : hierarchies) {
			if (o == null) continue;
			String[] split = ((String)o).split(SearchDocumentHandler.HIERARCHY_DELIMITER);
			
			// Families are always at the fourth level of the hierarchy
			if (split.length != 4) return "";
			family = split[3];
			
			// Each product will only be part of one family.
			// Once that has been found the loop can be exited.
			break;
		}
		return family;
	}
	
	
	/**
	 * returns the assetType_s value, except for CMS FILES, which look at the file type (extention)
	 * @return
	 */
	public String getAssetType() {
		String type = StringUtil.checkVal(sd.getFieldValue("assetType_s"));
		//if its a CMS file, derive type from the file name
		if ("FILE (PDF, PPT, DOC, XLS, ZIP, ETC.)".equalsIgnoreCase(type)) {
			type = StringUtil.checkVal(sd.getFieldValue(SearchDocumentHandler.FILE_EXTENSION), "FILE").toUpperCase();
		} else if (type.length() == 0) {
			type = getFavoriteType();
		}
		return type;
	}
	
	
	/**
	 * returns true if this solrDocument can be download to the user's briefcase; 
	 * implies the Briefcase supports such media type (see TDS).
	 * @return
	 */
	public boolean hasBriefcaseSupport() {
		IndexType type = IndexType.quietValueOf(StringUtil.checkVal(sd.getFieldValue(SearchDocumentHandler.INDEX_TYPE)));
		if (IndexType.MEDIA_BIN == type ) {
			return true; //public-facing PDFs, Podcasts and videos
		} else if (this.getMinRoleLevel() > SecurityController.PUBLIC_ROLE_LEVEL) {
			//secure assets are not permitted in the Briefcase
			return false;
		} else if (IndexType.CMS_QUICKSTREAM == type) {
			//need to look at AssetType, only non-html can go into the briefcase
			String cmsType = StringUtil.checkVal(sd.getFieldValue(MediaBinField.AssetType.getField())).toLowerCase();
			switch(cmsType) {
				case "file (pdf, ppt, doc, xls, zip, etc.)":
					return true;
				case "form": 
					return StringUtil.checkVal(sd.getFieldValue(SearchDocumentHandler.DOCUMENT_URL)).length() > 0;
				case "app":
				case "external site":
				default:
					return false;
			}
		} else if (IndexType.FORM == type) {
			//return true if there's a PDF attached to this form
			return (sd.getFieldValue(SearchDocumentHandler.DOCUMENT_URL) != null);
		} else {
			return false; //blog, events, sales consultants, etc.
		}
	}
	

	public void setCmsPath(String cmsPath) {
		this.cmsPath = cmsPath;
	}
	
	public String getCmsPath() {
		return cmsPath;
	}
}