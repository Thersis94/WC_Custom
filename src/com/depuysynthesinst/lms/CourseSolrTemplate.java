package com.depuysynthesinst.lms;

import java.util.Date;

import com.depuysynthesinst.SolrSearchWrapper;
import com.depuysynthesinst.TTLMSSolrIndexer;
import com.siliconmtn.annotations.SolrField;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: CourseSolrTemplate.java<p/>
 * <b>Description: transposes the JSON CourseVO into a SolrDocument</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 17, 2015
 ****************************************************************************/
public class CourseSolrTemplate extends SolrDocumentVO {
	
	private static final String ORG_ID = "DPY_SYN_INST"; //in WC, must match.

	private String assetType;
	private String assetUrl;
	private String trackingNo;
	private int fileSize;
	private int courseId;
	
	/**
	 * @param solrIndex
	 */
	public CourseSolrTemplate() {
		super(TTLMSSolrIndexer.INDEX_TYPE);
		super.setLanguage("en");
		super.addRole(SecurityController.PUBLIC_ROLE_LEVEL);
		super.addOrganization(ORG_ID);
		setAssetType("eModule");
		setModule("COURSE");
	}
	
	/**
	 * extension of superclass implementation; for DSI-specific template fields
	 */
	public void setData(Object o) {
		LMSCourseVO vo = (LMSCourseVO) o;
		if (vo == null)
			return;
		
		setCourseId(vo.getC_ID());
		setDocumentId("LMS" + getCourseId());
		setTrackingNo("LMS" + getCourseId()); //same as documentId - used for Favoriting & thumbnails
		setCredits(Convert.formatInteger("" + vo.getCREDITS()));
		super.setTitle(StringUtil.checkVal(vo.getC_NAME()));
		super.setSummary(StringUtil.checkVal(vo.getC_DESCRIPTION()));
		this.parseHierarchies(StringUtil.checkVal(vo.getACGMELIST()));
		
		if (vo.getLASTUPDATED() != null) {
			Date d = Convert.formatDate("MMMM, dd yyyy HH:mm:ss", vo.getLASTUPDATED());
			if (d != null) super.setUpdateDt(d);
		}
		
		
		if (super.getHierarchies() != null && super.getHierarchies().size() > 0)
			setAssetUrl(SolrSearchWrapper.buildDSIUrl(super.getHierarchies().get(0), getDocumentId(), vo.getQsPath(), getModule()));
		
		//super.setMetaKeywords(StringUtil.checkVal(field.getFieldValue()));
		//super.setMetaDesc(StringUtil.checkVal(field.getFieldValue()));
		
		//save Specialty into categories - these match the values in Registration for Specialty
		for (String s : StringUtil.checkVal(vo.getSPECIALTYLIST()).split(","))
			super.addCategories(s.trim().toUpperCase()); //upper-case these for equality to what we keep in Registration
	}
	
	
	private void parseHierarchies(String val) {
		for (String subStr : val.split(",")) {
			String h = makeDSIHierarchy(subStr);
			if (h != null && h.length() > 0) {
				super.addHierarchies(h);
			}
    		}
	}
	
	
	/**
	 * turn the TTLMS abbreviations into hierarchies to be used/understood on the website.
	 * These values come from an "ACGME" Excel file, part of the DSI TDS for phase 2.
	 * @param abbrv
	 * @return
	 */
	private String makeDSIHierarchy(String abbrv) {
		if (abbrv == null || abbrv.length() == 0) return "";
		return FutureLeaderACGME.getHierarchyFromCode(abbrv);
	}


	@SolrField(name="assetType_s")
	public String getAssetType() {
		return assetType;
	}

	public void setAssetType(String assetType) {
		this.assetType = assetType;
	}

	@SolrField(name=SearchDocumentHandler.DOCUMENT_URL)
	public String getAssetUrl() {
		return assetUrl;
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

	@SolrField(name="credits_i")
	public int getCredits() {
		return fileSize;
	}

	public void setCredits(int fileSize) {
		this.fileSize = fileSize;
	}
	
	@SolrField(name="assetDesc_s")
	public String getAssetDesc() {
		return "eModule";
	}

	@SolrField(name="courseId_i")
	public int getCourseId() {
		return courseId;
	}

	public void setCourseId(int courseId) {
		this.courseId = courseId;
	}
}
