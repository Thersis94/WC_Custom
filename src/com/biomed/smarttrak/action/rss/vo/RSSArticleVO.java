package com.biomed.smarttrak.action.rss.vo;

import java.io.Serializable;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title:</b> RSSArticleVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> VO Manages Smarttrak RSS Article Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.0
 * @since May 19, 2017
 ****************************************************************************/
@Table(name="biomedgps_rss_article")
public class RSSArticleVO implements Serializable {

	public enum ArticleSourceType {RSS, PUBMED, QUERTLE}
	private static final long serialVersionUID = 3528944442225589967L;
	private String rssArticleId;
	private String articleStatusCd;
	private String feedGroupId;
	private String rssEntityId;
	private String articleGuid;
	private String articleTxt;
	private String filterArticleTxt;
	private String titleTxt;
	private String filterTitleTxt;
	private String articleUrl;
	private String publicationName;
	private String bucketId;
	private String attribute1Txt;
	private String attribute2Txt;
	private ArticleSourceType articleSourceType;
	private Date publishDt;
	private Date createDt;

	public RSSArticleVO() {
		
	}

	public RSSArticleVO(ActionRequest req) {
		rssArticleId = req.getParameter("rssArticleId");
		articleStatusCd = req.getParameter("articleStatusCd");
	}

	/**
	 * @return the rssArticleId
	 */
	@Column(name="rss_article_id", isPrimaryKey = true)
	public String getRssArticleId() {
		return rssArticleId;
	}

	/**
	 * @return the articleStatusCd
	 */
	@Column(name="article_status_cd")
	public String getArticleStatusCd() {
		return articleStatusCd;
	}

	/**
	 * @return the feedGroupId
	 */
	@Column(name="feed_group_id")
	public String getFeedGroupId() {
		return feedGroupId;
	}

	/**
	 * @return the rssEntityId
	 */
	@Column(name="rss_entity_id")
	public String getRssEntityId() {
		return rssEntityId;
	}

	/**
	 * @return the articleGuid
	 */
	@Column(name="article_guid")
	public String getArticleGuid() {
		return articleGuid;
	}

	/**
	 * @return the articleTxt
	 */
	@Column(name="article_txt")
	public String getArticleTxt() {
		return articleTxt;
	}

	/**
	 * @return the filterArticleTxt
	 */
	@Column(name="filter_article_txt")
	public String getFilterArticleTxt() {
		return filterArticleTxt;
	}

	/**
	 * @return the titleTxt
	 */
	@Column(name="title_txt")
	public String getTitleTxt() {
		return titleTxt;
	}

	/**
	 * @return the filterTitleTxt
	 */
	@Column(name="filter_title_txt")
	public String getFilterTitleTxt() {
		return filterTitleTxt;
	}

	/**
	 * @return the articleUrl
	 */
	@Column(name="article_url")
	public String getArticleUrl() {
		return articleUrl;
	}

	@Column(name="article_source_type")
	public String getArticleSourceTypeNm() {
		return articleSourceType != null ? articleSourceType.name() : "";
	}

	public ArticleSourceType getArticleSourceType() {
		return articleSourceType;
	}

	@Column(name="bucket_id")
	public String getBucketId() {
		return bucketId;
	}

	/**
	 * @return the publishDt
	 */
	@Column(name="publish_dt")
	public Date getPublishDt() {
		return publishDt;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @return the publicationNm
	 */
	@Column(name="publication_nm")
	public String getPublicationName() {
		return publicationName;
	}

	@Column(name="attribute1_txt")
	public String getAttribute1Txt() {
		return attribute1Txt;
	}

	@Column(name="attribute2_txt")
	public String getAttribute2Txt() {
		return attribute2Txt;
	}

	/**
	 * @param rssArticleId the rssArticleId to set.
	 */
	public void setRssArticleId(String rssArticleId) {
		this.rssArticleId = rssArticleId;
	}

	/**
	 * @param articleStatusCd the articleStatusCd to set.
	 */
	public void setArticleStatusCd(String articleStatusCd) {
		this.articleStatusCd = articleStatusCd;
	}

	/**
	 * @param feedGroupId the feedGroupId to set.
	 */
	public void setFeedGroupId(String feedGroupId) {
		this.feedGroupId = feedGroupId;
	}

	/**
	 * @param rssEntityId the rssEntityId to set.
	 */
	public void setRssEntityId(String rssEntityId) {
		this.rssEntityId = rssEntityId;
	}

	/**
	 * @param articleGuid the articleGuid to set.
	 */
	public void setArticleGuid(String articleGuid) {
		this.articleGuid = articleGuid;
	}

	/**
	 * @param articleTxt the articleTxt to set.
	 */
	public void setArticleTxt(String articleTxt) {
		this.articleTxt = articleTxt;
	}

	/**
	 * @param filterArticleTxt the filterArticleTxt to set.
	 */
	public void setFilterArticleTxt(String filterArticleTxt) {
		this.filterArticleTxt = filterArticleTxt;
	}

	/**
	 * @param titleTxt the titleTxt to set.
	 */
	public void setTitleTxt(String titleTxt) {
		this.titleTxt = titleTxt;
	}

	/**
	 * @param filterTitleTxt the filterTitleTxt to set.
	 */
	public void setFilterTitleTxt(String filterTitleTxt) {
		this.filterTitleTxt = filterTitleTxt;
	}

	/**
	 * @param articleUrl the articleUrl to set.
	 */
	public void setArticleUrl(String articleUrl) {
		this.articleUrl = articleUrl;
	}

	public void setArticleSourceTypeNm(String articleSourceTypeNm) {
		this.articleSourceType = ArticleSourceType.valueOf(articleSourceTypeNm);
	}

	public void setArticleSourceType(ArticleSourceType articleSourceType) {
		this.articleSourceType = articleSourceType;
	}

	public void setBucketId(String bucketId) {
		this.bucketId = bucketId;
	}

	/**
	 * @param publishDt the publishDt to set.
	 */
	public void setPublishDt(Date publishDt) {
		this.publishDt = publishDt;
	}

	/**
	 * @param createDt the createDt to set.
	 */
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}

	public void setPublicationName(String publicationName) {
		this.publicationName = publicationName;
	}

	public void setAttribute1Txt(String attribute1Txt) {
		this.attribute1Txt = attribute1Txt;
	}

	public void setAttribute2Txt(String attribute2Txt) {
		this.attribute2Txt = attribute2Txt;
	}

}