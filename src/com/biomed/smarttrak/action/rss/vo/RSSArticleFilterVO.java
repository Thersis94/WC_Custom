package com.biomed.smarttrak.action.rss.vo;

import java.sql.ResultSet;
import java.util.Date;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.action.rss.RSSDataAction.ArticleStatus;
import com.biomed.smarttrak.util.RSSArticleIndexer;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.annotations.SolrField;
import com.siliconmtn.data.parser.AutoPopulateIntfc;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title:</b> RSSFilterArticleVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages the MarkedUp Article/Title Text after the FeedGroup
 * Filter is applied to the original Article Text.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Oct 3, 2017
 ****************************************************************************/
@Table(name="biomedgps_rss_filtered_article")
public class RSSArticleFilterVO extends SolrDocumentVO implements AutoPopulateIntfc {

	private String articleFilterId;
	private String rssArticleId;
	private String filterArticleTxt;
	private String filterTitleTxt;
	private String feedGroupId;
	private String bucketId;
	private int matchCount;
	private ArticleStatus articleStatus;
	private Date createDt;
	private int completeFlg;

	//Temp Variables
	private String articleUrl;
	private String articleTxt;
	private String fullArticleTxt;
	private String titleTxt;
	private String feedGroupNm;

	public RSSArticleFilterVO() {
		super(RSSArticleIndexer.INDEX_TYPE);
		addOrganization(AdminControllerAction.BIOMED_ORG_ID);
		addRole(SecurityController.PUBLIC_ROLE_LEVEL);
		setContentType(RSSArticleIndexer.INDEX_TYPE);
	}

	public RSSArticleFilterVO(ActionRequest req) {
		this();
		populateData(req);
	}

	public RSSArticleFilterVO(ResultSet rs) {
		this();
		populateData(rs);
	}

	/**
	 * @param article
	 * @param feedGroupId2
	 */
	public RSSArticleFilterVO(RSSArticleVO article, String feedGroupId, String feedGroupNm) {
		this.feedGroupId = feedGroupId;
		this.rssArticleId = article.getRssArticleId();
		this.fullArticleTxt = article.getFullArticleTxt();
		this.titleTxt = article.getTitleTxt();
		this.articleTxt = article.getArticleTxt();
		this.articleStatus = ArticleStatus.O;
		this.feedGroupNm = feedGroupNm;
	}

	/**
	 * @return the articleFilterId
	 */
	@Column(name="rss_article_filter_id", isPrimaryKey=true)
	public String getArticleFilterId() {
		return articleFilterId;
	}

	/**
	 * @return the rssArticleId
	 */
	@Column(name="rss_article_id")
	@SolrField(name="rssArticleId_s")
	public String getRssArticleId() {
		return rssArticleId;
	}

	/**
	 * @return the filterArticleTxt
	 */
	@Column(name="filter_article_txt")
	public String getFilterArticleTxt() {
		return filterArticleTxt;
	}

	/**
	 * @return the filterTitleTxt
	 */
	@Column(name="filter_title_txt")
	public String getFilterTitleTxt() {
		return filterTitleTxt;
	}

	/**
	 * @return the feedGroupId
	 */
	@Column(name="feed_group_id")
	@SolrField(name="feedGroupId_s")
	public String getFeedGroupId() {
		return feedGroupId;
	}

	@Column(name="bucket_id")
	@SolrField(name="bucketId_s")
	public String getBucketId() {
		return bucketId;
	}

	/**
	 * @return the articleStatus
	 */
	@Column(name="article_status_cd")
	public ArticleStatus getArticleStatus() {
		return articleStatus;
	}

	@SolrField(name="articleStatus_s")
	public String getArticleStatusTxt() {
		if(articleStatus != null)
			return articleStatus.name();
		else {
			return ArticleStatus.O.name();
		}
	}

	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDt() {
		return createDt;
	}

	@SolrField(name="completeFlag_i")
	@Column(name="complete_flg")
	public int getCompleteFlg() {
		return completeFlg;
	}

	/**
	 * @param articleFilterId the articleFilterId to set.
	 */
	public void setArticleFilterId(String articleFilterId) {
		this.articleFilterId = articleFilterId;
		setDocumentId(articleFilterId);
	}

	/**
	 * @param rssArticleId the rssArticleId to set.
	 */
	public void setRssArticleId(String rssArticleId) {
		this.rssArticleId = rssArticleId;
	}

	/**
	 * @param filterArticleTxt the filterArticleTxt to set.
	 */
	public void setFilterArticleTxt(String filterArticleTxt) {
		this.filterArticleTxt = filterArticleTxt;
		super.setContents(filterArticleTxt);
	}

	/**
	 * @param filterTitleTxt the filterTitleTxt to set.
	 */
	public void setFilterTitleTxt(String filterTitleTxt) {
		this.filterTitleTxt = filterTitleTxt;
		this.setTitle(filterTitleTxt);
	}

	/**
	 * @param feedGroupId the feedGroupId to set.
	 */
	public void setFeedGroupId(String feedGroupId) {
		this.feedGroupId = feedGroupId;
	}

	public void setBucketId(String bucketId) {
		this.bucketId = bucketId;
	}
	/**
	 * @param articleStatus the articleStatus to set.
	 */
	public void setArticleStatus(ArticleStatus articleStatus) {
		this.articleStatus = articleStatus;
	}

	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
		this.setPublishDate(createDt);
	}

	public void setCompleteFlg(int completeFlg) {
		this.completeFlg = completeFlg;
	}

	/**
	 * @return
	 */
	public String getArticleUrl() {
		return articleUrl;
	}

	public void setArticleUrl(String articleUrl) {
		this.articleUrl = articleUrl;
		this.setDocumentUrl(articleUrl);
	}

	/**
	 * @return
	 */
	public String getArticleTxt() {
		return articleTxt;
	}

	public void setArticleTxt(String articleTxt) {
		this.articleTxt = articleTxt;
	}

	/**
	 * @return
	 */
	public String getTitleTxt() {
		return titleTxt;
	}

	public void setTitleTxt(String titleTxt) {
		this.titleTxt = titleTxt;
	}

	/**
	 * @return
	 */
	public String getFullArticleTxt() {
		return fullArticleTxt;
	}

	public void setFullArticleTxt(String fullArticleTxt) {
		this.fullArticleTxt = fullArticleTxt;
	}

	@Column(name="match_no")
	@SolrField(name="matchCount_i")
	public int getMatchCount() {
		return matchCount;
	}

	public void setMatchCount(int matchCount) {
		this.matchCount = matchCount;
	}

	public String getFeedGroupNm() {
		return this.feedGroupNm;
	}

	public void setFeedGroupNm(String feedGroupNm) {
		this.feedGroupNm = feedGroupNm;
	}
}