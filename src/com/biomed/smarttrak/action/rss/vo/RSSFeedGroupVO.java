package com.biomed.smarttrak.action.rss.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> RSSFeedGroupVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> VO Manages Feed Group Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 1.0
 * @since May 9, 2017
 ****************************************************************************/
@Table(name="biomedgps_feed_group")
public class RSSFeedGroupVO implements Serializable {

	private static final long serialVersionUID = 7558928863447942543L;
	private String feedGroupId;
	private String feedSegmentId;
	private String feedSegmentNm;
	private String feedGroupNm;
	private String feedGroupDesc;
	private String feedTypeId;
	private String feedFilterGroupXrId;
	private String feedEntityGroupXrId;
	private int articleCount;
	private List<RSSFilterVO> filters;
	private List<RSSFilterTerm> terms;
	private Date createDt;
	private Date updateDt;

	public RSSFeedGroupVO() {
		super();
		filters = new ArrayList<>();
		terms = new ArrayList<>();
	}

	public RSSFeedGroupVO(ActionRequest req) {
		this();
		setData(req);
	}

	/**
	 * @param req
	 */
	protected void setData(ActionRequest req) {
		feedGroupId = req.getParameter("feedGroupId");
		feedSegmentId = req.getParameter("feedSegmentId");
		feedGroupNm = req.getParameter("feedGroupNm");
		feedGroupDesc = req.getParameter("feedGroupDesc");
		feedTypeId = StringUtil.checkVal(req.getParameter("feedTypeId"), null);
	}

	/**
	 * @return the feedGroupId
	 */
	@Column(name="feed_group_id", isPrimaryKey=true)
	public String getFeedGroupId() {
		return feedGroupId;
	}

	/**
	 * @return the feedSegmentId
	 */
	@Column(name="feed_segment_id")
	public String getFeedSegmentId() {
		return feedSegmentId;
	}

	@Column(name="feed_segment_nm", isReadOnly=true)
	public String getFeedSegmentNm() {
		return feedSegmentNm;
	}
	/**
	 * @return the feedGroupNm
	 */
	@Column(name="feed_group_nm")
	public String getFeedGroupNm() {
		return feedGroupNm;
	}

	/**
	 * @return the feedGroupDesc
	 */
	@Column(name="feed_group_desc")
	public String getFeedGroupDesc() {
		return feedGroupDesc;
	}

	@Column(name="feed_type_id")
	public String getFeedTypeId() {
		return feedTypeId;
	}

	@Column(name="feed_filter_group_xr_id", isReadOnly=true)
	public String getFeedFilterGroupXRId() {
		return feedFilterGroupXrId;
	}

	@Column(name="source_group_id", isReadOnly=true)
	public String getFeedEntityGroupXrId() {
		return feedEntityGroupXrId;
	}

	@Column(name="article_count", isReadOnly=true)
	public int getArticleCount() {
		return articleCount;
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

	public List<RSSFilterVO> getFilters() {
		return filters;
	}

	public List<RSSFilterTerm> getTerms() {
		return terms;
	}

	@BeanSubElement
	public void addFilter(RSSFilterVO f) {
		if(f != null)
			filters.add(f);
	}

	@BeanSubElement
	public void addTerm(RSSFilterTerm t) {
		if(t != null)
			terms.add(t);
	}

	/**
	 * @param feedGroupId the feedGroupId to set.
	 */
	public void setFeedGroupId(String feedGroupId) {
		this.feedGroupId = feedGroupId;
	}

	/**
	 * @param feedSegmentId the feedSegmentId to set.
	 */
	public void setFeedSegmentId(String feedSegmentId) {
		this.feedSegmentId = feedSegmentId;
	}

	public void setFeedSegmentNm(String feedSegmentNm) {
		this.feedSegmentNm = feedSegmentNm;
	}
	/**
	 * @param feedGroupNm the feedGroupNm to set.
	 */
	public void setFeedGroupNm(String feedGroupNm) {
		this.feedGroupNm = feedGroupNm;
	}

	/**
	 * @param feedGroupDesc the feedGroupDesc to set.
	 */
	public void setFeedGroupDesc(String feedGroupDesc) {
		this.feedGroupDesc = feedGroupDesc;
	}

	public void setFeedTypeId(String feedTypeId) {
		this.feedTypeId = feedTypeId;
	}
	/**
	 * @param createDt the createDt to set.
	 */
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}

	public void setFeedFilterGroupXRId(String feedFilterGroupXrId) {
		this.feedFilterGroupXrId = feedFilterGroupXrId;
	}

	public void setFeedEntityGroupXrId(String feedEntityGroupXrId) {
		this.feedEntityGroupXrId = feedEntityGroupXrId;
	}

	public void setArticleCount(int articleCount) {
		this.articleCount = articleCount;
	}

	/**
	 * @param updateDt the updateDt to set.
	 */
	public void setUpdateDt(Date updateDt) {
		this.updateDt = updateDt;
	}

	public void setFilters(List<RSSFilterVO> filters) {
		this.filters = filters;
	}

	public void setTerms(List<RSSFilterTerm> terms) {
		this.terms = terms;
	}
}