package com.depuysynthesinst;

import java.util.List;

//import org.apache.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: SolrBusinessRules.java<p/>
 * <b>Description: Contains the logic we need to extract DSI display components 
 * and pageURls from the SolrDocument. - used in multiple views and MyAssignmentsAdminAction</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 12, 2015
 ****************************************************************************/
public class SolrBusinessRules {

	//protected static Logger log;
	private String pageUrl;
	private String sectionNm;
	private String thumbnailImg;
	private SolrDocument sd;
	private String moduleType;
	private String qsPath = "";

	public SolrBusinessRules() {
		//log = Logger.getLogger(getClass());
	}

	public void setQsPath(String qsPath) {
		this.qsPath = qsPath;
	}
	
	public void setSolrDocument(SolrDocument sd) {
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
	private void buildSectionName() {
		//hierarchy is a multi-valued field; if multiple values exist the object is an array instead of a String
		//in either case we take the first one
		try {
			Object obj = (Object)sd.get("hierarchy");
			if (obj instanceof List) {
				List<String> lst = (List<String>) obj;
				sectionNm = StringUtil.checkVal(lst.get(0)).toLowerCase();
			} else {
				sectionNm = StringUtil.checkVal(obj).toLowerCase();
			}
		} catch (Exception e) {
			//log.error("could not parse sectionName from SolrDoc", e);
		}

		//for Vet and Nursing sections, bring level 2 of the hierarchy up to the surface
		if (sectionNm.startsWith("vet~small")) {
			sectionNm = "Small Animal";
		} else if (sectionNm.startsWith("vet~large")) {
			sectionNm = "Large Animal";
		} else if (sectionNm.startsWith("nurs")) {
			sectionNm = "Nurse Education";
		} else if ("EVENT".equals(moduleType)) {
			sectionNm = StringUtil.checkVal(sd.get("eventType_s")).toLowerCase();
			if (sectionNm.equals("surgeon")) {
				sectionNm = "Surgeon";
			} else if (sectionNm.equals("nurse")) {
				sectionNm ="Nurse Education";
			} else if (sectionNm.equals("vet")) {
				sectionNm ="Veterinary";
			}
		}

		if (sectionNm.indexOf("~") > -1) {
			// not a 2nd level hierarchy, trim and use the top level only.
			sectionNm = sectionNm.substring(0, sectionNm.indexOf("~"));
		}
	}

	
	private void buildPageUrl() {
		if ("EVENT".equals(moduleType)) {
			if (sectionNm.equals("Surgeon")) {
				pageUrl = "/calendar";
			} else if (sectionNm.equals("Nurse Education")) {
				pageUrl ="/nurse-education/calendar";
			} else if (sectionNm.equals("Veterinary")) {
				pageUrl ="/veterinary/calendar";
			}
		} else if (sectionNm.equals("Small Animal")) {
			pageUrl = "/veterinary/small-animal";
		} else if (sectionNm.equals("Large Animal")) {
			pageUrl = "/veterinary/large-animal";
		} else if (sectionNm.startsWith("chest")) {
			pageUrl = "/chest-wall";
		} else if (sectionNm.startsWith("hand")) {
			pageUrl = "/hand-wrist";
		} else if (sectionNm.startsWith("foot")) {
			pageUrl = "/foot-ankle";
		} else if (sectionNm.equals("Nurse Education")) {
			pageUrl = "/nurse-education/resource-library";
		} else {
			pageUrl = "/" + sectionNm.toLowerCase();
		}

		pageUrl += "/" + qsPath + sd.get("documentId");
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
}
