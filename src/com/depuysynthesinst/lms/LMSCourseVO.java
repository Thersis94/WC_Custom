package com.depuysynthesinst.lms;

import java.io.Serializable;

import com.depuysynthesinst.TTLMSSolrIndexer;
import com.siliconmtn.annotations.SolrField;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: LMSCourseVO.java<p/>
 * <b>Description: auto-generated bean used for Reflection from the JSON returned from the LMS.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 16, 2015
 ****************************************************************************/
public class LMSCourseVO extends SolrDocumentVO implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String qsPath;
	private String assetType;
	private String assetUrl;
	private String trackingNo;
	private String C_DESCRIPTION;
	private double CREDITS;
	private String C_NAME;
	private int C_ID;
	private String SPECIALTYLIST;
	private String ACGMELIST;
	private double INDEVELOPMENT;
	private String LASTUPDATED;

	public LMSCourseVO() {
		super(TTLMSSolrIndexer.INDEX_TYPE);
	}
	
	/**
	 * @param solrIndex
	 */
	public LMSCourseVO(String solrIndex) {
		super(solrIndex);
	}

	@SolrField(name=SearchDocumentHandler.SUMMARY)
	public String getC_DESCRIPTION() {
		return C_DESCRIPTION;
	}

	@SolrField(name=SearchDocumentHandler.FILE_SIZE) //"Stuffed-in - only used to print on the page"
	public double getCREDITS() {
		return CREDITS;
	}

	@SolrField(name=SearchDocumentHandler.TITLE)
	public String getC_NAME() {
		return C_NAME;
	}

	public int getC_ID() {
		return C_ID;
	}

	public String getSPECIALTYLIST() {
		return SPECIALTYLIST;
	}

	private double getINDEVELOPMENT() {
		return INDEVELOPMENT;
	}
	
	public boolean isLive() {
		return this.getINDEVELOPMENT() == 0;
	}
	
	

	public void setC_DESCRIPTION(String c_DESCRIPTION) {
		C_DESCRIPTION = c_DESCRIPTION;
	}

	public void setCREDITS(double cREDITS) {
		CREDITS = cREDITS;
	}

	public void setC_NAME(String c_NAME) {
		C_NAME = c_NAME;
	}

	public void setC_ID(int c_ID) {
		C_ID = c_ID;
	}

	public void setSPECIALTYLIST(String sPECIALTYLIST) {
		SPECIALTYLIST = sPECIALTYLIST;
	}

	public void setINDEVELOPMENT(double iNDEVELOPMENT) {
		INDEVELOPMENT = iNDEVELOPMENT;
	}

	@SolrField(name="assetType_s")
	public String getAssetType() {
		return assetType;
	}

	@SolrField(name=SearchDocumentHandler.DOCUMENT_URL)
	public String getAssetUrl() {
		return assetUrl;
	}

	@SolrField(name="trackingNumber_s")
	public String getTrackingNo() {
		return trackingNo;
	}

	public void setAssetType(String assetType) {
		this.assetType = assetType;
	}

	public void setAssetUrl(String assetUrl) {
		this.assetUrl = assetUrl;
	}

	public void setTrackingNo(String trackingNo) {
		this.trackingNo = trackingNo;
	}

	public String getLASTUPDATED() {
		return LASTUPDATED;
	}

	public void setLASTUPDATED(String lASTUPDATED) {
		LASTUPDATED = lASTUPDATED;
	}

	public String getACGMELIST() {
		return ACGMELIST;
	}

	public void setACGMELIST(String aCGMELIST) {
		ACGMELIST = aCGMELIST;
	}

	public String getQsPath() {
		return qsPath;
	}

	public void setQsPath(String qsPath) {
		this.qsPath = qsPath;
	}
}
