package com.biomed.smarttrak.action.rss.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.siliconmtn.annotations.SolrField;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title:</b> RSSSolrDocumentVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Specific VO for handling rss feed data in a solr compliant fashion
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Eric Damschroder
 * @since Nov 15, 2019
 ****************************************************************************/
@Table(name="biomedgps_rss_article")
public class RSSSolrDocumentVO extends SolrDocumentVO {

	private String articleTxt;
	private String titleTxt;
	private String affiliationTxt;
	private List<String> comboKeys;
	private List<String> filterIds;
	
	public RSSSolrDocumentVO() {
		super();
		comboKeys = new ArrayList<>();
		filterIds = new ArrayList<>();
		addOrganization(AdminControllerAction.BIOMED_ORG_ID);
		addRole(SecurityController.PUBLIC_ROLE_LEVEL);
	}

	/**
	 * @return the rssArticleId
	 */
	@Column(name="rss_article_id", isPrimaryKey=true)
	public String getRssArticleId() {
		return super.getDocumentId();
	}
	

	public void setRssArticleId(String rssArticleId) {
		super.setDocumentId(rssArticleId);
	}

	/**
	 * @return
	 */
	@SolrField(name="contents")
	@Column(name="article_txt")
	public String getArticleTxt() {
		return articleTxt;
	}

	public void setArticleTxt(String articleTxt) {
		this.articleTxt = articleTxt;
	}

	/**
	 * @return
	 */
	@SolrField(name="title")
	@Column(name="title_txt")
	public String getTitleTxt() {
		return titleTxt;
	}

	public void setTitleTxt(String titleTxt) {
		this.titleTxt = titleTxt;
	}

	@SolrField(name="affiliationTxt")
	@Column(name="affiliation_txt")
	public String getAffiliationTxt() {
		return this.affiliationTxt;
	}

	public void setAffiliationTxt(String affiliationTxt) {
		this.affiliationTxt = affiliationTxt;
	}
	
	/**
	 * Overrides the parent method with no solr annotation
	 * in order to prevent that value from being added to 
	 * all solr records
	 */
	@Override
	public int getFileSize() {
		return 0;
	}

	@Column(name="combo_key")
	public String getComboKeysString() {
		StringBuilder keyString = new StringBuilder(comboKeys.size()*40);
		for (String comboKey : comboKeys) {
			if (keyString.length() > 0) keyString.append(",");
			keyString.append(comboKey);
		}
		return keyString.toString();
	}
	
	public void setComboKeysString (String fullKeys) {
		if (fullKeys == null) return;
		for (String key : fullKeys.split(","))
			comboKeys.add(key);
	}

	/**
	 * @return the comboKeys
	 */
	@SolrField(name="comboKey")
	public List<String> getComboKeys() {
		return comboKeys;
	}

	/**
	 * @param comboKeys the comboKeys to set
	 */
	public void setComboKeys(List<String> comboKeys) {
		this.comboKeys = comboKeys;
	}

	@Column(name="filter_id")
	public String getFilterIdsString() {
		StringBuilder keyString = new StringBuilder(filterIds.size()*40);
		for (String comboKey : filterIds) {
			if (keyString.length() > 0) keyString.append(",");
			keyString.append(comboKey);
		}
		return keyString.toString();
	}
	
	public void setFilterIdsString (String allIds) {
		if (allIds == null) return;
		for (String id : allIds.split(","))
			filterIds.add(id);
	}

	/**
	 * @return the filterIds
	 */
	@SolrField(name="filterId")
	public List<String> getFilterIds() {
		return filterIds;
	}

	/**
	 * @param filterIds the filterIds to set
	 */
	public void setFilterIds(List<String> filterIds) {
		this.filterIds = filterIds;
	}
	
	@Override
	@Column(name="publish_dt")
	@SolrField(name=SearchDocumentHandler.PUBLISH_DATE)
	public Date getPublishDate() {
		return super.getPublishDate();
	}

	@Override
	@Column(name="create_dt")
	@SolrField(name=SearchDocumentHandler.UPDATE_DATE)
	public Date getUpdateDt() {
		return super.getUpdateDt();
	}
}