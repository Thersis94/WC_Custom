package com.biomed.smarttrak.action.rss.vo;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.smt.sitebuilder.action.rss.RssVO;

/****************************************************************************
 * <b>Title:</b> SmarttrakRssVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Custom VO for managing RssData with Smarttrak context.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 1.0
 * @since May 9, 2017
 ****************************************************************************/
public class SmarttrakRssVO extends RssVO {

	private static final long serialVersionUID = 1L;
	private String bucketId;
	private String segmentNm;
	private String groupId;
	private String typeCd;

	public SmarttrakRssVO() {
		super();
	}

	public SmarttrakRssVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @return the bucketId
	 */
	@Column(name="bucket_id")
	public String getBucketId() {
		return bucketId;
	}

	/**
	 * @return the groupId
	 */
	@Column(name="feed_group_id")
	public String getGroupId() {
		return groupId;
	}

	/**
	 * @return the segmentNm
	 */
	@Column(name="feed_segment_nm")
	public String getSegmentNm() {
		return segmentNm;
	}
	/**
	 * @return the typeCd
	 */
	@Column(name="type_cd")
	public String getTypeCd() {
		return typeCd;
	}

	/**
	 * @param bucketId the bucketId to set.
	 */
	public void setBucketId(String bucketId) {
		this.bucketId = bucketId;
	}

	/**
	 * @param segmentNm the segmentNm to set.
	 */
	public void setSegmentNm(String segmentNm) {
		this.segmentNm = segmentNm;
	}

	/**
	 * @param groupId the groupId to set.
	 */
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	/**
	 * @param typeCd the typeCd to set.
	 */
	public void setTypeCd(String typeCd) {
		this.typeCd = typeCd;
	}
}