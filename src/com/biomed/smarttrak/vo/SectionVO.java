/**
 *
 */
package com.biomed.smarttrak.vo;

import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: SectionVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> DataContainer for Biomed Smarttrak Sections.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author raptor
 * @version 1.0
 * @since Jan 6, 2017
 ****************************************************************************/
@Table(name="BIOMEDGPS_SECTION")
public class SectionVO {

	private String sectionId;
	private String parentId;
	private String sectionNm;
	private int orderNo;
	private String solrTokenTxt;
	private Date createDt;
	private Date updateDt;

	public SectionVO() {}
	public SectionVO(ResultSet rs) {
		setData(rs);
	}
	public SectionVO(SMTServletRequest req) {
		setData(req);
	}

	/**
	 * Helper method that sets data from the ResultSet.
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		DBUtil util = new DBUtil();
		this.sectionId = util.getStringVal("SECTION_ID", rs);
		this.parentId = util.getStringVal("PARENT_ID", rs);
		this.sectionNm = util.getStringVal("SECTION_NM", rs);
		this.orderNo = util.getIntVal("ORDER_NO", rs);
		this.solrTokenTxt = util.getStringVal("SOLR_TOKEN_TXT", rs);
		this.createDt = util.getDateVal("CREATE_DT", rs);
		this.updateDt = util.getDateVal("UPDATE_DT", rs);
	}

	/**
	 * Helper method that sets data from the SMTServletRequest.
	 * @param req
	 */
	public void setData(SMTServletRequest req) {
		this.sectionId = req.getParameter("sectionId");
		this.parentId = req.getParameter("parentId");
		this.sectionNm = req.getParameter("sectionNm");
		this.orderNo = Convert.formatInteger(req.getParameter("orderNo"));
		this.solrTokenTxt = req.getParameter("solrTokenTxt");
	}

	/**
	 * @return the sectionId
	 */
	@Column(name="SECTION_ID", isPrimaryKey=true)
	public String getSectionId() {
		return sectionId;
	}
	/**
	 * @return the parentId
	 */
	@Column(name="PARENT_ID")
	public String getParentId() {
		return parentId;
	}
	/**
	 * @return the sectionNm
	 */
	@Column(name="SECTION_NM")
	public String getSectionNm() {
		return sectionNm;
	}
	/**
	 * @return the orderNo
	 */
	@Column(name="ORDER_NO")
	public Integer getOrderNo() {
		return Integer.valueOf(orderNo);
	}
	/**
	 * @return the solrTokenTxt
	 */
	@Column(name="SOLR_TOKEN_TXT")
	public String getSolrTokenTxt() {
		return solrTokenTxt;
	}
	/**
	 * @return the createDt
	 */
	@Column(name="CREATE_DT")
	public Date getCreateDt() {
		return createDt;
	}
	/**
	 * @return the updateDt
	 */
	@Column(name="UPDATE_DT")
	public Date getUpdateDt() {
		return updateDt;
	}
	/**
	 * @param sectionId the sectionId to set.
	 */
	public void setSectionId(String sectionId) {
		this.sectionId = sectionId;
	}
	/**
	 * @param parentId the parentId to set.
	 */
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}
	/**
	 * @param sectionNm the sectionNm to set.
	 */
	public void setSectionNm(String sectionNm) {
		this.sectionNm = sectionNm;
	}
	/**
	 * @param orderNo the orderNo to set.
	 */
	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}
	/**
	 * @param solrTokenTxt the solrTokenTxt to set.
	 */
	public void setSolrTokenTxt(String solrTokenTxt) {
		this.solrTokenTxt = solrTokenTxt;
	}
	/**
	 * @param createDt the createDt to set.
	 */
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}
	/**
	 * @param updateDt the updateDt to set.
	 */
	public void setUpdateDt(Date updateDt) {
		this.updateDt = updateDt;
	}
}