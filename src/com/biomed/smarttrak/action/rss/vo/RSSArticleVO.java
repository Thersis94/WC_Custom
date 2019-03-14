package com.biomed.smarttrak.action.rss.vo;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.ernieyu.feedparser.Item;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

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
public class RSSArticleVO extends BeanDataVO implements Serializable {

	public enum ArticleSourceType {RSS, PUBMED, QUERTLE}
	private static final long serialVersionUID = 3528944442225589967L;
	private Map<String, RSSArticleFilterVO> filteredText;
	private String rssArticleId;
	private String rssEntityId;
	private String articleGuid;
	private String articleTxt;
	private String fullArticleTxt;
	private String titleTxt;
	private String articleUrl;
	private String publicationName;
	private String attribute1Txt;
	private ArticleSourceType articleSourceType;
	private Date publishDt;
	private Date createDt;

	public RSSArticleVO() {
		super();
		filteredText = new HashMap<>();
	}

	public RSSArticleVO(ActionRequest req) {
		this();
		populateData(req);
	}

	public RSSArticleVO(ResultSet rs) {
		this();
		populateData(rs);
	}

	/**
	 * Convert the given Feed Item from parser library to SMT RSSArticleVO.
	 * @param i
	 */
	public RSSArticleVO(Item i) {
		this();
		setArticleSourceType(ArticleSourceType.RSS);
		setArticleGuid(StringUtil.checkVal(i.getGuid()));
		setArticleTxt(StringUtil.checkVal(i.getDescription()).replace("\u00a0"," "));
		setPublishDt(Convert.parseDateUnknownPattern(i.getElement("pubDate").getContent()));
		setArticleUrl(i.getLink());
		if(StringUtil.isEmpty(getArticleGuid())) {
			setArticleGuid(i.getLink());
		}
		setTitleTxt(StringUtil.checkVal(i.getTitle()).replace("\u00a0"," "));
		setPublicationName(i.getTitle());
	}

	/**
	 * @return the rssArticleId
	 */
	@Column(name="rss_article_id", isPrimaryKey = true)
	public String getRssArticleId() {
		return rssArticleId;
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
	 * @return the titleTxt
	 */
	@Column(name="title_txt")
	public String getTitleTxt() {
		return titleTxt;
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

	public Map<String, RSSArticleFilterVO> getFilterVOs() {
		return filteredText;
	}

	public RSSArticleFilterVO getFilterText() {
		return filteredText != null && filteredText.size() == 1 ? filteredText.entrySet().iterator().next().getValue() : null;
	}

	public String getFullArticleTxt() {
		return fullArticleTxt;
	}
	/**
	 * @param rssArticleId the rssArticleId to set.
	 */
	public void setRssArticleId(String rssArticleId) {
		this.rssArticleId = rssArticleId;
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
		this.articleGuid = StringUtil.checkVal(articleGuid);
	}

	/**
	 * @param articleTxt the articleTxt to set.
	 */
	public void setArticleTxt(String articleTxt) {
		this.articleTxt = articleTxt;
	}

	/**
	 * @param titleTxt the titleTxt to set.
	 */
	public void setTitleTxt(String titleTxt) {
		this.titleTxt = titleTxt;
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

	public void setFilteredText(Map<String, RSSArticleFilterVO> filteredText) {
		this.filteredText = filteredText;
	}

	@BeanSubElement
	public void addFilteredText(RSSArticleFilterVO vo) {
		this.filteredText.put(vo.getFeedGroupId(), vo);
	}

	/**
	 * @param loadArticle
	 */
	public void setFullArticleTxt(String fullArticleTxt) {
		this.fullArticleTxt = fullArticleTxt;
	}

	/**
	 * Flush out Filtered Articles.
	 */
	public void flushFilteredText() {
		this.filteredText.clear();
	}
}