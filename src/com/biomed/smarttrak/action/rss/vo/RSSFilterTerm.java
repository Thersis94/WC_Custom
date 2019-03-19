package com.biomed.smarttrak.action.rss.vo;

import java.io.Serializable;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title:</b> RSSFilterTerm.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> VO Manages Feed Term Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 1.0
 * @since May 9, 2017
 ****************************************************************************/
@Table(name="biomedgps_filter_term")
public class RSSFilterTerm implements Serializable {

	private static final long serialVersionUID = 4969168227174988292L;
	private String filterTermId;
	private String filterTypeCd;
	private String filterTypeNm;
	private String filterGroupId;
	private String filterGroupNm;
	private String filterTerm;
	private String feedGroupNm;
	private Date createDt;

	public RSSFilterTerm() {
		super();
	}

	public RSSFilterTerm(ActionRequest req) {
		setData(req);
	}

	public void setData(ActionRequest req) {
		filterTermId = req.getParameter("filterTermId");
		filterTypeCd = req.getParameter("filterTypeCd");
		filterGroupId = req.getParameter("filterGroupId");
		filterTerm = req.getParameter("filterTerm");
	}

	/**
	 * @return the filterTermId
	 */
	@Column(name="filter_term_id", isPrimaryKey=true)
	public String getFilterTermId() {
		return filterTermId;
	}

	/**
	 * @return the filterTypeCd
	 */
	@Column(name="filter_type_cd")
	public String getFilterTypeCd() {
		return filterTypeCd;
	}

	/**
	 * @return the typeNm
	 */
	@Column(name="filter_type_nm", isReadOnly=true)
	public String getFilterTypeNm() {
		return filterTypeNm;
	}
	/**
	 * @return the filterGroupId
	 */
	@Column(name="feed_group_id")
	public String getFilterGroupId() {
		return filterGroupId;
	}

	/**
	 * @return the feedGroupNm
	 */
	@Column(name="feed_group_nm", isReadOnly=true)
	public String getFilterGroupNm() {
		return filterGroupNm;
	}
	/**
	 * @return the filterTerm
	 */
	@Column(name="filter_term")
	public String getFilterTerm() {
		return filterTerm;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDt() {
		return createDt;
	}

	@Column(name="feed_group_nm", isReadOnly=true)
	public String getFeedGroupNm() {
		return feedGroupNm;
	}
	/**
	 * @param filterTermId the filterTermId to set.
	 */
	public void setFilterTermId(String filterTermId) {
		this.filterTermId = filterTermId;
	}

	/**
	 * @param filterTypeCd the filterTypeCd to set.
	 */
	public void setFilterTypeCd(String filterTypeCd) {
		this.filterTypeCd = filterTypeCd;
	}

	/**
	 * @param filterTypeNm the filterTypeNm to set.
	 */
	public void setFilterTypeNm(String filterTypeNm) {
		this.filterTypeNm = filterTypeNm;
	}

	/**
	 * @param filterGroupId the filterGroupId to set.
	 */
	public void setFilterGroupId(String filterGroupId) {
		this.filterGroupId = filterGroupId;
	}

	/**
	 * @param filterGroupNm the filterGroupNm to set.
	 */
	public void setFilterGroupNm(String filterGroupNm) {
		this.filterGroupNm = filterGroupNm;
	}

	/**
	 * @param filterTerm the filterTerm to set.
	 */
	public void setFilterTerm(String filterTerm) {
		this.filterTerm = filterTerm;
	}

	/**
	 * @param createDt the createDt to set.
	 */
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}

	public void setFeedGroupNm(String feedGroupNm) {
		this.feedGroupNm = feedGroupNm;
	}
}