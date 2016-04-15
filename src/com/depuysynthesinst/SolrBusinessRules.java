package com.depuysynthesinst;

import java.util.List;


//import org.apache.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <b>Title</b>: SolrBusinessRules.java<p/>
 * <b>Description: Contains the logic we need to extract DSI display components 
 * and pageURls from a SolrDocument. - used in multiple views and MyAssignmentsAdminAction</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 12, 2015
 ****************************************************************************/
public class SolrBusinessRules {

	//protected static Logger log;
	protected String pageUrl;
	private String sectionNm;
	private String hierarchy;
	private String thumbnailImg;
	protected SolrDocument sd;
	private String moduleType;
	protected String qsPath = "";
	private boolean isSiteSearch;
	
	public SolrBusinessRules() {
		//log = Logger.getLogger(getClass());
	}

	public void setQsPath(String qsPath) {
		this.qsPath = qsPath;
	}
	
	public String getQsPath() {
		return qsPath;
	}
	
	public void setSolrDocument(SolrDocument sd) {
		if (sd == null) sd = new SolrDocument(); //this is needed incase the asset is no longer in Solr
		this.sd = sd;
		this.moduleType = StringUtil.checkVal(sd.get("moduleType"));

		buildSectionName();
		buildPageUrl();
		buildThumbnailImg();
	}


	private void buildThumbnailImg() {
		thumbnailImg = StringUtil.checkVal(sd.get("trackingNumber_s"));
		thumbnailImg = StringUtil.replace(thumbnailImg, "/", "");
		thumbnailImg = StringUtil.replace(thumbnailImg, "-", "");
	}


	@SuppressWarnings("unchecked")
	protected void buildSectionName() {
		//hierarchy is a multi-valued field; if multiple values exist the object is an array instead of a String
		//in either case we take the first one
		try {
			Object obj = (Object)sd.get("hierarchy");
			if (obj instanceof List) {
				List<String> lst = (List<String>) obj;
				hierarchy = StringUtil.checkVal(lst.get(0)).toLowerCase();
			} else {
				hierarchy = StringUtil.checkVal(obj).toLowerCase();
			}
		} catch (Exception e) {
			//log.error("could not parse sectionName from SolrDoc", e);
		}
		sectionNm = hierarchy;

		//for Vet and Nursing sections, bring level 2 of the hierarchy up to the surface
		if (sectionNm.startsWith("vet~small")) {
			sectionNm = "Small Animal";
		} else if (sectionNm.startsWith("vet~large")) {
			sectionNm = "Large Animal";
		} else if (sectionNm.startsWith("nurs")) {
			sectionNm = "Nurse Education";
		} else if (!isSiteSearch && sectionNm.startsWith("future leaders") && sectionNm.indexOf("~") > 1) {
			//parse-out the 2nd level of the hierarchy to display, but NOT in search results, those remain level 1.
			sectionNm = sectionNm.substring(sectionNm.indexOf("~")+1);
			if (sectionNm.indexOf("~") > 0)
				sectionNm = sectionNm.substring(0, sectionNm.indexOf("~"));
		} else if ("EVENT".equals(moduleType)) {  //this use-case is ONLY search results
			sectionNm = StringUtil.checkVal(sd.get("eventType_s")).toLowerCase();
			if (sectionNm.equals("surgeon")) {
				sectionNm = "Surgeon";
			} else if (sectionNm.equals("nurse")) {
				sectionNm ="Nurse Education";
			} else if (sectionNm.equals("vet")) {
				sectionNm ="Veterinary";
			} else if (sectionNm.equals("future")) {
				sectionNm ="Future Leaders";
			}
		}

		if (sectionNm.indexOf("~") > -1) {
			// not a 2nd level hierarchy, trim and use the top level only.
			sectionNm = sectionNm.substring(0, sectionNm.indexOf("~"));
		}
	}

	
	protected void buildPageUrl() {
		if ("EVENT".equals(moduleType)) {
			pageUrl = StringUtil.checkVal(sd.get(SearchDocumentHandler.DOCUMENT_URL));
		} else {
			pageUrl = SolrSearchWrapper.buildDSIUrl(hierarchy, (String)sd.get(SearchDocumentHandler.DOCUMENT_ID), qsPath);
		}
	}


	public String getPageUrl() {
		return pageUrl;
	}


	public String getSectionNm() {
		return sectionNm;
	}


	public String getThumbnailImg() {
		return thumbnailImg;
	}


	public SolrDocument getSd() {
		return sd;
	}

	public void setSiteSearch(boolean isSiteSearch) {
		this.isSiteSearch = isSiteSearch;
	}
	
	
	/**
	 * select the proper favoriteType based on indexType - they don't align properly.
	 * @return
	 */
	public String getFavoriteType() {
		if ("MEDIA_BIN".equals(sd.get("indexType"))) return "MEDIABIN";
		if ("LMS_DSI".equals(sd.get("indexType"))) return "COURSE";
		if ("CMS_QUICKSTREAM".equals(sd.get("indexType"))) return "CMS";
		if ("COURSE_CAL".equals(sd.get("indexType"))) return "EVENT";
		else return "";
	}
	

	
	/**
	 * Get the lowest role level associated with this document
	 * @return
	 */
	public int getMinRoleLevel() {
		// If we have no roles assume public
		if (sd.getFieldValues(SearchDocumentHandler.ROLE) == null) return SecurityController.PUBLIC_ROLE_LEVEL;
		
		// Start at an unattainable value
		int role = 1000;
		for (Object o : sd.getFieldValues(SearchDocumentHandler.ROLE)) {
			if (o == null) continue;
			if ((int)o < role)
				role = (int)o;
		}
		if (role == 1000) {
			return SecurityController.PUBLIC_ROLE_LEVEL;
		} else {
			return role;
		}
	}
}
