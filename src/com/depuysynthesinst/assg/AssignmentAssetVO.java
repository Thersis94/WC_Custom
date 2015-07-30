package com.depuysynthesinst.assg;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.Date;

import org.apache.solr.common.SolrDocument;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: AssgAssetVO.java<p/>
 * <b>Description: represents an Asset witin an Assignement.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 7, 2015
 ****************************************************************************/
public class AssignmentAssetVO implements Serializable, Comparable<AssignmentAssetVO> {
	private static final long serialVersionUID = 1393648844170164739L;

	private String assgAssetId;
	private String assetId;
	private String assgId;
	private String solrDocumentId;
	private String mediabinId;
	private String lmsCourseId;
	private int orderNo;
	private Date completeDt; //the date the user completed the module (within an assignment)
	private SolrDocument solrDocument; //contains all the additional display info and URL to the asset
	
	public AssignmentAssetVO() {
	}
	
	public AssignmentAssetVO(ResultSet rs) {
		DBUtil util = new DBUtil();
		assgAssetId = util.getStringVal("assg_asset_id", rs);
		solrDocumentId = util.getStringVal("solr_document_id", rs);
		mediabinId = util.getStringVal("dpy_syn_mediabin_id", rs);
		lmsCourseId = util.getStringVal("ttlsm_crs_id", rs);
		orderNo = util.getIntVal("order_no", rs);
		completeDt = util.getDateVal("complete_dt", rs);
		util = null;
	}
	
	public String getAssetId() {
		return assetId;
	}


	public void setAssetId(String assetId) {
		this.assetId = assetId;
	}


	public String getAssgId() {
		return assgId;
	}


	public void setAssgId(String assgId) {
		this.assgId = assgId;
	}


	public String getSolrDocumentId() {
		return solrDocumentId;
	}


	public void setSolrDocumentId(String solrDocumentId) {
		this.solrDocumentId = solrDocumentId;
	}


	public String getMediabinId() {
		return mediabinId;
	}


	public void setMediabinId(String mediabinId) {
		this.mediabinId = mediabinId;
	}


	public String getLmsCourseId() {
		return lmsCourseId;
	}


	public void setLmsCourseId(String lmsCourseId) {
		this.lmsCourseId = lmsCourseId;
	}


	public int getOrderNo() {
		return orderNo;
	}


	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}


	public Date getCompleteDt() {
		return completeDt;
	}


	public void setCompleteDt(Date completeDt) {
		this.completeDt = completeDt;
	}

	public SolrDocument getSolrDocument() {
		return solrDocument;
	}

	public void setSolrDocument(SolrDocument solrDocument) {
		this.solrDocument = solrDocument;
	}

	public String getAssgAssetId() {
		return assgAssetId;
	}

	public void setAssgAssetId(String assgAssetId) {
		this.assgAssetId = assgAssetId;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(AssignmentAssetVO o) {
		return Convert.formatInteger(this.orderNo).compareTo(Convert.formatInteger(o.getOrderNo()));
	}

}
