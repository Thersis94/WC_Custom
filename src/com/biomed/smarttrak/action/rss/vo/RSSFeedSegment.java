package com.biomed.smarttrak.action.rss.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title:</b> RSSFeedSegment.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> VO Manages Feed Segment Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 1.0
 * @since May 9, 2017
 ****************************************************************************/
@Table(name="biomedgps_feed_segment")
public class RSSFeedSegment extends BeanDataVO implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -7947498799112341039L;
	private String segmentId;
	private String segmentNm;
	private String segmentDesc;
	private String sectionId;
	private String sectionNm;
	private String profileId;
	private int articleCount;
	private int orderNo;
	private Date createDt;
	private Date updateDt;
	private List<RSSFeedGroupVO> groups;

	public RSSFeedSegment() {
		super();
		groups = new ArrayList<>();
	}

	public RSSFeedSegment(ActionRequest req) {
		populateData(req);
	}

	/**
	 * @return the segmentId
	 */
	@Column(name="feed_segment_id", isPrimaryKey=true)
	public String getSegmentId() {
		return segmentId;
	}

	/**
	 * @return the segmentNm
	 */
	@Column(name="feed_segment_nm")
	public String getSegmentNm() {
		return segmentNm;
	}

	/**
	 * @return the segmentDesc
	 */
	@Column(name="feed_segment_desc")
	public String getSegmentDesc() {
		return segmentDesc;
	}

	@Column(name="order_no")
	public int getOrderNo() {
		return orderNo;
	}

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

	/**
	 * @return the sectionId
	 */
	@Column(name="SECTION_ID")
	public String getSectionId() {
		return sectionId;
	}

	@Column(name="SECTION_NM", isReadOnly=true)
	public String getSectionNm() {
		return sectionNm;
	}

	@Column(name="PROFILE_ID")
	public String getProfileId() {
		return profileId;
	}

	public List<RSSFeedGroupVO> getGroups() {
		return groups;
	}

	/**
	 * @param segmentId the segmentId to set.
	 */
	public void setSegmentId(String segmentId) {
		this.segmentId = segmentId;
	}

	/**
	 * @param segmentNm the segmentNm to set.
	 */
	public void setSegmentNm(String segmentNm) {
		this.segmentNm = segmentNm;
	}

	/**
	 * @param segmentDesc the segmentDesc to set.
	 */
	public void setSegmentDesc(String segmentDesc) {
		this.segmentDesc = segmentDesc;
	}

	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}

	public void setArticleCount(int articleCount) {
		this.articleCount = articleCount;
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

	public void setSectionId(String sectionId) {
		this.sectionId = sectionId;
	}

	public void setSectionNm(String sectionNm) {
		this.sectionNm = sectionNm;
	}

	public void setGroups(List<RSSFeedGroupVO> groups) {
		this.groups = groups;
	}

	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	@BeanSubElement
	public void addGroup(RSSFeedGroupVO g) {
		if(g != null) {
			groups.add(g);
			articleCount += g.getArticleCount();
		}
	}
}