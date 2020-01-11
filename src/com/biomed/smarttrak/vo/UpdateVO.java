package com.biomed.smarttrak.vo;

import java.io.Serializable;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.action.AdminControllerAction.Section;
import com.biomed.smarttrak.admin.UpdatesAction.UpdateType;
import com.biomed.smarttrak.security.SmarttrakRoleVO;
import com.biomed.smarttrak.util.BiomedLinkCheckerUtil;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.util.UpdateIndexer;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.annotations.SolrField;
import com.siliconmtn.data.Node;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.user.HumanNameIntfc;
import com.smt.sitebuilder.changelog.ChangeLogIntfc;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <b>Title</b>: UpdateVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> VO for managing Biomed Updates.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 1.0
 * @since Feb 14, 2017
 ****************************************************************************/
@Table(name="biomedgps_update")
public class UpdateVO extends AuthorVO implements HumanNameIntfc, ChangeLogIntfc, Serializable, Comparable<UpdateVO> {

	private static final long serialVersionUID = 5149725371008749427L;
	
	public static final String DOCUMENT_ID_PREFIX = UpdateIndexer.INDEX_TYPE + "_";

	public enum UpdateStatusCd {
		N("New"), 
		R("Reviewed"), 
		A("Archived");

		private String statusName;
		UpdateStatusCd (String statusName) { this.statusName = statusName; }
		public String getStatusName() { return statusName; }
	}
	
	public enum AnnouncementType {
		NON("Not an Announcement", 0),
		ANNOUNCEMENT("SmartTRAK Announcement", 1),
		POLICY("Healthcare Policy", 2),
		TREND("Healthcare Trend", 3),
		INDUSTRY("Industry News", 4);
		
		private String name;
		private int value;
		
		AnnouncementType(String name, int value) {
			this.name = name;
			this.value = value;
		}
		
		public String getName() {
			return name;
		}
		
		public int getValue() {
			return value;
		}
		
		public static AnnouncementType getFromValue(int value) {
			switch (value) {
				case 1:return AnnouncementType.ANNOUNCEMENT;
				case 2:return AnnouncementType.POLICY;
				case 3:return AnnouncementType.TREND;
				case 4:return AnnouncementType.INDUSTRY;
				default:return AnnouncementType.NON;
			}
		}
	}

	private String updateId;
	private String marketId;
	private String productId;
	private String companyId;
	private String marketNm;
	private String productNm;
	private String companyNm;
	private int typeCd;
	private int orderNo;
	private int emailFlg;
	private String messageTxt;
	private String twitterTxt;
	private int tweetFlg;
	private String firstNm;
	private String lastNm;
	private String statusCd;
	private String historyId;
	private Date createDt;
	private Date publishDtSort;
	private transient List<UpdateXRVO> sections; //UpdateXRVO is not serializable, so this List must be transient -JM- 7.03.2017
	private String qsPath;
	private int sslFlg;
	private String siteAliasUrl;
	private int announcementType;
	private List<String> sectionIds;
	private int archiveFlg;

	public UpdateVO() {
		super(UpdateIndexer.INDEX_TYPE);
		sections = new ArrayList<>();
		sectionIds = new ArrayList<>();
		addOrganization(AdminControllerAction.BIOMED_ORG_ID);
		addRole(SecurityController.PUBLIC_ROLE_LEVEL);
	}

	public UpdateVO(ResultSet rs) {
		this();
		setData(rs);
	}

	public UpdateVO(ActionRequest req) {
		this();
		setData(req);
	}

	@Override
	protected void setData(ActionRequest req) {
		super.setData(req); //set the creator_profile_id
		setUpdateId(StringUtil.checkVal(req.getParameter("updateId")));
		marketId = StringUtil.checkVal(req.getParameter("marketId"), null);
		productId = StringUtil.checkVal(req.getParameter("productId"), null);
		companyId = StringUtil.checkVal(req.getParameter("companyId"), null);
		setTitle(req.getParameter("titleTxt"));
		typeCd = Convert.formatInteger(req.getParameter("typeCd"));
		messageTxt = req.getParameter("messageTxt");
		twitterTxt = req.getParameter("twitterTxt");
		tweetFlg = Convert.formatInteger(Convert.formatBoolean(req.getParameter("tweetFlg")));
		statusCd = req.getParameter("statusCd");
		orderNo = Convert.formatInteger(req.getParameter("orderNo"));
		emailFlg = Convert.formatInteger(req.getParameter("emailFlg"), 1);
		announcementType = Convert.formatInteger(req.getParameter("announcementType"), 0);
		if (req.hasParameter("sectionId")) {
			String [] arr = req.getParameterValues("sectionId");
			for (String sec : arr)
				sections.add(new UpdateXRVO(updateId, sec));
		}
	}

	@Override
	@SolrField(name=SearchDocumentHandler.DOCUMENT_URL)
	public String getDocumentUrl() {
		StringBuilder url = new StringBuilder(50);
		if (!StringUtil.isEmpty(productId)) {
			url.append(Section.PRODUCT.getPageURL()).append(qsPath).append(productId);
		} else if (!StringUtil.isEmpty(companyId)) {
			url.append(Section.COMPANY.getPageURL()).append(qsPath).append(companyId);
		} else if (!StringUtil.isEmpty(marketId)) {
			url.append(Section.MARKET.getPageURL()).append(qsPath).append(marketId);
		}
		return url.toString();
	}

	@Override
	@SolrField(name=SearchDocumentHandler.CONTENT_TYPE)
	public String getContentType() {
		if (!StringUtil.isEmpty(productId)) {
			super.setContentType(Section.PRODUCT.toString()); 
		} else if (!StringUtil.isEmpty(companyId)) {
			super.setContentType(Section.COMPANY.toString());
		} else if (!StringUtil.isEmpty(marketId)) {
			super.setContentType(Section.MARKET.toString());
		}
		return super.getContentType();
	}

	/**
	 * display title encapsulates the logic for which label we put on displayed URLs in the html.
	 * If the update is for a market, URL will be /markets and display text will be market short name.
	 * if the update is for a company, URL will be /companies and display text will be company short name.
	 * if the update is for a product, URL will be /products and display text will be "product short name - company short name" (w/two URLs)
	 */
	@SolrField(name="display_title_s")
	public String getDisplayTitle() {
		if (!StringUtil.isEmpty(productId)) {
			return this.getProductNm();
		} else if (!StringUtil.isEmpty(companyId)) {
			return this.getCompanyNm();
		} else if (!StringUtil.isEmpty(marketId)) {
			return this.getMarketNm();
		} else {
			return getTitle();
		}
	}

	public String getRelativeAdminDisplayLink() {
		return buildDisplayLink("", true);
	}
	/**
	 * Getter builds the display Link String without a concrete Domain.  This is
	 * used on public and manage facing views.
	 * @return
	 */
	public String getRelativeDisplayLink() {
		return buildDisplayLink("", false);
	}

	/**
	 * Getter builds the display Link String with a Concrete Domain.  This is
	 * used by Email Campaigns.
	 * @return
	 */
	public String getDisplayLink() {
		String domain = "http://";
		if(sslFlg == 1) {
			domain = "https://";
		}
		return buildDisplayLink(domain + siteAliasUrl, false);
	}

	/**
	 * Helper method that accepts a domain string and builds the Update Link
	 * Title Text for display.
	 * @param domain
	 * @return
	 */
	protected String buildDisplayLink(String domain, boolean isAdmin) {
		String aTxt = "<a href=\"" + StringUtil.checkVal(domain);
		String targetClassTxt = "\" target=\"_blank\" style=\"color:#008ec9;\">";
		StringBuilder displayLink = new StringBuilder(200);
		String url = getDocumentUrl();
		if(!StringUtil.isEmpty(productId)) {
			displayLink.append(aTxt).append(url).append(targetClassTxt);
			displayLink.append(!StringUtil.isEmpty(productNm) ? productNm : getTitle()).append("</a>");
			if(!StringUtil.isEmpty(companyNm)) {
				displayLink.append("- ").append(aTxt).append(Section.COMPANY.getPageURL()).append(qsPath).append(companyId).append("\" target=\"_blank\" class=\"title\">").append(companyNm).append("</a>");
			}
		} else if(!StringUtil.isEmpty(companyId)) {
			displayLink.append(aTxt).append(url).append(targetClassTxt);
			displayLink.append(!StringUtil.isEmpty(companyNm) ? companyNm : getTitle()).append("</a>");
		} else if(!StringUtil.isEmpty(marketId)) {
			displayLink.append(aTxt).append(url).append(targetClassTxt);
			displayLink.append(!StringUtil.isEmpty(marketNm) ? marketNm : getTitle()).append("</a>");
		} else {
			displayLink.append(getTitle());
		}
		SimpleDateFormat sdf = new SimpleDateFormat("MMM. dd, yyyy");
		displayLink.append("&mdash; ").append(sdf.format(getPublishDt()));
		if(isAdmin) {
			return new BiomedLinkCheckerUtil().modifyRelativeLinks(displayLink.toString());
		} else {
			return displayLink.toString();
		}
	}
	/**
	 * @return the updateId
	 */
	@Column(name="update_id", isPrimaryKey=true)
	public String getUpdateId() {
		return updateId;
	}

	/**
	 * @return the marketId
	 */
	@Column(name="market_id")
	public String getMarketId() {
		return marketId;
	}

	/**
	 * @return the marketNm
	 */
	@Column(name="market_nm", isReadOnly=true)
	@SolrField(name="market_nm_t")
	public String getMarketNm() {
		return marketNm;
	}

	/**
	 * @return the productId
	 */
	@Column(name="product_id")
	public String getProductId() {
		return productId;
	}

	/**
	 * @return the productNm
	 */
	@Column(name="product_nm", isReadOnly=true)
	@SolrField(name="product_nm_t")
	public String getProductNm() {
		return productNm;
	}

	/**
	 * @return the companyId
	 */
	@Column(name="company_id")
	public String getCompanyId() {
		return companyId;
	}

	/**
	 * @return the companyNm
	 */
	@Column(name="company_nm", isReadOnly=true)
	@SolrField(name="company_nm_t")
	public String getCompanyNm() {
		return companyNm;
	}

	/**
	 * @override so we could add the DB annotation
	 * @return the titleTxt
	 */
	@Override
	@Column(name="title_txt")
	@SolrField(name=SearchDocumentHandler.TITLE)
	public String getTitle() {
		return super.getTitle();
	}

	/**
	 * @return the typeNm
	 */
	@SolrField(name=SearchDocumentHandler.MODULE_TYPE)
	@Column(name="type_cd")
	public int getTypeCd() {
		return typeCd;
	}

	public String getTypeNm() {
		UpdateType t = getType();
		if(t != null)
			return t.getText();
		else
			return "";
	}
	/**
	 * @return the messageTxt
	 */
	@Column(name="message_txt")
	@SolrField(name=SearchDocumentHandler.SUMMARY)
	public String getMessageTxt() {
		return messageTxt;
	}

	/**
	 * @return the twitterTxt
	 */
	@Column(name="twitter_txt")
	public String getTwitterTxt() {
		return twitterTxt;
	}

	/**
	 * @return the creatorProfileId
	 */
	@Override
	@SolrField(name=SearchDocumentHandler.AUTHOR)
	@Column(name="creator_profile_id")
	public String getCreatorProfileId() {
		return creatorProfileId;
	}

	/**
	 * @return the statusCd
	 */
	@SolrField(name="status_cd_s")
	@Column(name="status_cd")
	public String getStatusCd() {
		return statusCd;
	}

	/**
	 * Get Full Status Nm.
	 * @return
	 */
	public String getStatusNm() {
		String nm = "";
		try {
			nm = UpdateStatusCd.valueOf(statusCd).getStatusName();
		} catch(Exception e) {
			//If fail, no problem.  Means no status.
		}

		return nm;
	}

	/**
	 * @return the publishDt
	 */
	@Column(name="publish_dt")
	public Date getPublishDt() {
		return getPublishDate();
	}

	@SolrField(name="companyLink_s")
	public String getCompanyLink() {
		if (StringUtil.isEmpty(companyId)) return "";

		return AdminControllerAction.Section.COMPANY.getPageURL() + qsPath + getCompanyId();
	}

	/**
	 * @deprecated this should not be done, URL is built dynamically, just set companyId
	 * @return
	 */
	@Deprecated
	public void setCompanyLink(String link){
		//does nothing
	}

	/**
	 * @deprecated use overlapping getCompanyNm()
	 * @return
	 */
	@Deprecated
	public String getCompanyShortName() {
		return getCompanyNm();
	}

	/**
	 * @deprecated use overlapping setCompanyNm()
	 * @return
	 */
	@Deprecated
	public void setCompanyShortName(String shortNm){
		setCompanyNm(shortNm);
	}

	/**
	 * The Solr annotation here is used in order to ensure that the public side solr search
	 * can return items in the same order as they are presented on the manage side.
	 * @return the createDt
	 */
	@SolrField(name=SearchDocumentHandler.UPDATE_DATE)
	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @return the updateDt
	 */
	@Column(name="update_dt", isAutoGen=true, isUpdateOnly=true)
	@Override
	public Date getUpdateDt() {
		return super.getUpdateDt();
	}

	@Column(name="publish_dt_sort")
	@SolrField(name="sortDate_dt")
	public Date getPublishDtSort() {
		return publishDtSort;
	}

	@Column(name="email_flg")
	public int getEmailFlg() {
		return emailFlg;
	}

	@SolrField(name="order_i")
	@Column(name="order_no")
	public int getOrderNo() {
		return orderNo;
	}

	@Column(name="archive_flg")
	public int getArchiveFlg() {
		return archiveFlg;
	}

	/**
	 * @return the sections
	 */
	public List<UpdateXRVO> getUpdateSections() {
		return sections;
	}

	@BeanSubElement()
	public void addUpdateXrVO(UpdateXRVO u) {
		if (u == null || u.getUpdateSectionXrId() == null) return;
		sections.add(u);
	}

	/**
	 * @param updateId the updateId to set.
	 */
	public void setUpdateId(String updateId) {
		this.updateId = updateId;
		StringBuilder docId = new StringBuilder(updateId);
		if (docId.length() < AdminControllerAction.DOC_ID_MIN_LEN) {

			//Insert separator and then insert Index Type
			docId.insert(0, DOCUMENT_ID_PREFIX);
		}
		setDocumentId(docId.toString());
	}

	/**
	 * @param marketId the marketId to set.
	 */
	public void setMarketId(String marketId) {
		this.marketId = marketId;
	}

	public void setMarketNm(String marketNm) {
		this.marketNm = marketNm;
	}
	/**
	 * @param productId the productId to set.
	 */
	public void setProductId(String productId) {
		this.productId = productId;
	}

	public void setProductNm(String productNm) {
		this.productNm = productNm;
	}
	/**
	 * @param companyId the companyId to set.
	 */
	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}

	public void setCompanyNm(String companyNm) {
		this.companyNm = companyNm;
	}

	/**
	 * @param typeNm the typeNm to set.
	 */
	public void setTypeCd(int typeCd) {
		this.typeCd = typeCd;
	}

	/**
	 * @param messageTxt the messageTxt to set.
	 */
	public void setMessageTxt(String messageTxt) {
		this.messageTxt = messageTxt;
	}

	/**
	 * @param twitterTxt the twitterTxt to set.
	 */
	public void setTwitterTxt(String twitterTxt) {
		this.twitterTxt = twitterTxt;
	}

	/**
	 * @param statusCd the statusCd to set.
	 */
	public void setStatusCd(String statusCd) {
		this.statusCd = statusCd;
	}

	/**
	 * @param publishDt the publishDt to set.
	 */
	public void setPublishDt(Date publishDt) {
		setPublishDate(publishDt);
	}

	/**
	 * @param createDt the createDt to set.
	 */
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}

	/**
	 * @param publishDtSort the publishDtSort to set.
	 */
	public void setPublishDtSort(Date publishDtSort) {
		Calendar c = Calendar.getInstance();
		c.setTime(publishDtSort);
		c.set(Calendar.HOUR_OF_DAY, 0);
		this.publishDtSort = c.getTime();
	}

	/**
	 * @param updateDt the updateDt to set.
	 */
	@Override
	public void setUpdateDt(Date updateDt) {
		super.setUpdateDt(updateDt);
	}

	/**
	 * @param sections the sections to set.
	 */
	public void setSections(List<UpdateXRVO> sections) {
		this.sections = sections;
	}

	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}

	public void setEmailFlg(int emailFlg) {
		this.emailFlg = emailFlg;
	}

	/**
	 * Helper method gets the UpdateType for the internal typeCd.
	 * @return
	 */
	public UpdateType getType() {
		UpdateType t = null;
		for(UpdateType u : UpdateType.values()) {
			if(u.getVal() == typeCd) {
				t = u;
			}
		}

		return t;
	}

	/* (non-Javadoc)
	 * @see com.biomed.smarttrak.admin.user.HumanNameIntfc#getFirstName()
	 */
	@Override
	@Column(name="first_nm", isReadOnly=true)
	public String getFirstName() {
		return firstNm;
	}

	/**
	 * @return the lastNm
	 */
	@Column(name="last_nm", isReadOnly=true)
	@Override
	public String getLastName() {
		return lastNm;
	}

	/* (non-Javadoc)
	 * @see com.biomed.smarttrak.admin.user.HumanNameIntfc#setFirstName(java.lang.String)
	 */
	@Override
	public void setFirstName(String firstNm) {
		this.firstNm = firstNm;
	}

	/* (non-Javadoc)
	 * @see com.biomed.smarttrak.admin.user.HumanNameIntfc#setLastName(java.lang.String)
	 */
	@Override
	public void setLastName(String lastNm) {
		this.lastNm = lastNm;
	}


	/**
	 * Helper method that builds hierarchy path.
	 * 
	 * Replace spaces with _ and replace & and and
	 * @param loadSections
	 */
	public void configureSolrHierarchies(SmarttrakTree t) {
		//loop through the selected sections, and add them to the Solr record as 1) hierarchy. 2) ACL.
		for (UpdateXRVO uxr : sections) {
			if(uxr.getSectionId() != null) {
				Node n = t.findNode(uxr.getSectionId());
				if (n != null && !StringUtil.isEmpty(n.getFullPath())) {
					super.addHierarchies(n.getFullPath());
					sectionIds.add(n.getNodeId());
					SectionVO sec = (SectionVO) n.getUserObject();
					super.addACLGroup(Permission.GRANT, sec.getSolrTokenTxt());
				}
			}
		}
		// Add the public acl for announcements since they are visible to anyone
		if (announcementType > 0)
			super.addACLGroup(Permission.GRANT, SmarttrakRoleVO.PUBLIC_ACL);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.changelog.ChangeLogIntfc#getDiffText()
	 */
	@Override
	public String getDiffText() {
		return messageTxt;
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.changelog.ChangeLogIntfc#getItemName()
	 */
	@Override
	public String getItemName() {
		return getTitle();
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.changelog.ChangeLogIntfc#getItemDesc()
	 */
	@Override
	public String getItemDesc() {
		return "Modified Smarttrak Update Record.";
	}

	public void setHistory(String historyId) {
		this.historyId = StringUtil.checkVal(historyId);
	}

	@Column(name="wc_sync_id", isReadOnly=true)
	public String getHistory() {
		return historyId;
	}

	public boolean getHasHistory() {
		return !StringUtil.isEmpty(historyId);
	}

	/**
	 * @return the tweetFlg
	 */
	@Column(name="tweet_flg")
	public int getTweetFlg() {
		return tweetFlg;
	}

	/**
	 * @deprecated - use getTitle
	 * @return
	 */
	@Deprecated
	public String getTitleTxt() {
		return getTitle();
	}

	/**
	 * @param tweetFlg the tweetFlg to set
	 */
	public void setTweetFlg(int tweetFlg) {
		this.tweetFlg = tweetFlg;
	}

	@Column(name="qs_path", isReadOnly=true)
	public String getQsPath() {
		return qsPath;
	}

	public void setQsPath(String qsPath) {
		this.qsPath = qsPath;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(UpdateVO vo) {
		int typeComp = Convert.formatInteger(getTypeCd()).compareTo(vo.getTypeCd());
		if (typeComp == 0) {
			//look at publish date if type is the same.
			return getPublishDt().compareTo(vo.getPublishDt());
		}
		return typeComp;
	}

	/**
	 * @param sslFlg
	 */
	public void setSSLFlg(int sslFlg) {
		this.sslFlg = sslFlg;
	}
	@Column(name="ssl_flg", isReadOnly=true)
	public int getSSLFlg() {
		return this.sslFlg;
	}

	public void setSiteAliasUrl(String siteAliasUrl) {
		this.siteAliasUrl = siteAliasUrl;
	}
	@Column(name="site_alias_url", isReadOnly=true)
	public String getSiteAliasUrl() {
		return this.siteAliasUrl;
	}

	@SolrField(name="announcement_type_i")
	@Column(name="announcement_type")
	public int getAnnouncementType() {
		return announcementType;
	}

	public void setAnnouncementType(int announcementType) {
		this.announcementType = announcementType;
	}

	@SolrField(name="sectionid_ss")
	public List<String> getSectionIds() {
		return sectionIds;
	}

	public void setSectionIds(List<String> sectionIds) {
		this.sectionIds = sectionIds;
	}

	public void setArchiveFlg(int archiveFlg) {
		this.archiveFlg = archiveFlg;
	}
}