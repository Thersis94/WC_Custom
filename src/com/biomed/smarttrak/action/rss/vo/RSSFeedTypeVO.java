package com.biomed.smarttrak.action.rss.vo;

import java.io.Serializable;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title:</b> RSSFeedTypeGroupVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> VO Managed Feed Type Group VO
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.0
 * @since May 18, 2017
 ****************************************************************************/
@Table(name="biomedgps_feed_type")
public class RSSFeedTypeVO implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -676279080886282336L;
	private String feedTypeId;
	private String feedTypeNm;
	private String feedTypeDesc;
	private Date createDt;

	public RSSFeedTypeVO() {
		super();
	}

	public RSSFeedTypeVO(ActionRequest req) {
		setData(req);
	}

	public void setData(ActionRequest req) {
		feedTypeId = req.getParameter("feedTypeId");
		feedTypeNm = req.getParameter("feedTypeNm");
		feedTypeDesc = req.getParameter("feedTypeDesc");
	}
	/**
	 * @return the feedTypeId
	 */
	@Column(name="feed_type_id", isPrimaryKey=true)
	public String getFeedTypeId() {
		return feedTypeId;
	}

	/**
	 * @return the feedTypeNm
	 */
	@Column(name="feed_type_nm")
	public String getFeedTypeNm() {
		return feedTypeNm;
	}

	/**
	 * @return the feedTypeDesc
	 */
	@Column(name="feed_type_desc")
	public String getFeedTypeDesc() {
		return feedTypeDesc;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @param feedTypeId the feedTypeId to set.
	 */
	public void setFeedTypeId(String feedTypeId) {
		this.feedTypeId = feedTypeId;
	}

	/**
	 * @param feedTypeNm the feedTypeNm to set.
	 */
	public void setFeedTypeNm(String feedTypeNm) {
		this.feedTypeNm = feedTypeNm;
	}

	/**
	 * @param feedTypeDesc the feedTypeDesc to set.
	 */
	public void setFeedTypeDesc(String feedTypeDesc) {
		this.feedTypeDesc = feedTypeDesc;
	}

	/**
	 * @param createDt the createDt to set.
	 */
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}
}