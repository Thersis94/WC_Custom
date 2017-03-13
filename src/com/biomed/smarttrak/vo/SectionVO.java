package com.biomed.smarttrak.vo;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;

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
public class SectionVO implements Serializable {

	private static final long serialVersionUID = -4660750788956185315L;
	private String sectionId;
	private String parentId;
	private String sectionNm;
	private int orderNo;
	private String solrTokenTxt;
	private int fdPubYr;
	private int fdPubQtr;
	private Date createDt;
	private Date updateDt;
	private boolean isSelected;
	private String groupNm;

	public SectionVO() {
		super();
	}

	public SectionVO(ResultSet rs) throws SQLException {
		this();
		setData(rs);
	}

	public SectionVO(ActionRequest req) {
		this();
		setData(req);
	}

	/**
	 * Helper method that sets data from the ResultSet.
	 * @param rs
	 */
	public void setData(ResultSet rs) throws SQLException {
		DBUtil util = new DBUtil();
		
		setSectionId(util.getStringVal("SECTION_ID", rs));
		setParentId(util.getStringVal("PARENT_ID", rs));
		setSectionNm(util.getStringVal("SECTION_NM", rs));
		setOrderNo(util.getIntVal("ORDER_NO", rs));
		setSolrTokenTxt(util.getStringVal("SOLR_TOKEN_TXT", rs));
		
		// These values may not always be on a result set depending on where this is being set from
		setFdPubYr(util.getIntVal("FD_PUB_YR", rs));
		setFdPubQtr(util.getIntVal("FD_PUB_QTR", rs));
	}

	/**
	 * Helper method that sets data from the SMTServletRequest.
	 * @param req
	 */
	public void setData(ActionRequest req) {
		setSectionId(req.getParameter("sectionId"));
		setParentId(StringUtil.checkVal(req.getParameter("parentId"), null));
		setSectionNm(req.getParameter("sectionNm"));
		setOrderNo(Convert.formatInteger(req.getParameter("orderNo")));
		setSolrTokenTxt(req.getParameter("solrTokenTxt"));
		setFdPubQtr(Convert.formatInteger(req.getParameter("fdPubQtr")));
		setFdPubYr(Convert.formatInteger(req.getParameter("fdPubYr")));
		//if this is an insert, randomly generate the solr token.  This only happens once, ever.
		if (StringUtil.isEmpty(getSectionId()))
			setSolrTokenTxt(RandomAlphaNumeric.generateRandom(5));
	}

	/**
	 * @return the sectionId
	 */
	@Column(name="section_id", isPrimaryKey=true)
	public String getSectionId() {
		return sectionId;
	}
	/**
	 * @return the parentId
	 */
	@Column(name="parent_id")
	public String getParentId() {
		return parentId;
	}
	/**
	 * @return the sectionNm
	 */
	@Column(name="section_nm")
	public String getSectionNm() {
		return sectionNm;
	}
	/**
	 * @return the orderNo
	 */
	@Column(name="order_no")
	public int getOrderNo() {
		return orderNo;
	}
	/**
	 * @return the solrTokenTxt
	 * This gets generated once, at insertion time, and never changes again.  Used for enforcing permissions
	 */
	@Column(name="solr_token_txt", isInsertOnly=true)
	public String getSolrTokenTxt() {
		return solrTokenTxt;
	}
	
	/**
	 * @return the fdPubYr
	 */
	@Column(name="fd_pub_yr")
	public int getFdPubYr() {
		return fdPubYr;
	}

	/**
	 * @return the fdPubQtr
	 */
	@Column(name="fd_pub_qtr")
	public int getFdPubQtr() {
		return fdPubQtr;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDt() {
		return createDt;
	}
	/**
	 * @return the updateDt
	 */
	@Column(name="update_dt", isAutoGen=true, isUpdateOnly=true)
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
	 * @param fdPubYr the fdPubYr to set
	 */
	public void setFdPubYr(int fdPubYr) {
		this.fdPubYr = fdPubYr;
	}

	/**
	 * @param fdPubQtr the fdPubQtr to set
	 */
	public void setFdPubQtr(int fdPubQtr) {
		this.fdPubQtr = fdPubQtr;
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

	public boolean isSelected() {
		return isSelected;
	}

	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}

	public String getGroupNm() {
		return groupNm;
	}

	public void setGroupNm(String groupNm) {
		this.groupNm = groupNm;
	}
}