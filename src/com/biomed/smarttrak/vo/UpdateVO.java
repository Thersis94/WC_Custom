package com.biomed.smarttrak.vo;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.action.AdminControllerAction.Section;
import com.biomed.smarttrak.admin.UpdatesAction.UpdateType;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.util.UpdateIndexer;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.annotations.SolrField;
import com.siliconmtn.data.Node;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.user.HumanNameIntfc;
import com.smt.sitebuilder.changelog.ChangeLogIntfc;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.solr.SecureSolrDocumentVO;

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
public class UpdateVO extends SecureSolrDocumentVO implements HumanNameIntfc, ChangeLogIntfc, Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 5149725371008749427L;


	public enum UpdateStatusCd {
		N("New"), 
		R("Reviewed"), 
		A("Archived");

		private String statusName;
		UpdateStatusCd (String statusName) { this.statusName = statusName; }
		public String getStatusName() { return statusName; }
	}

	private String updateId;
	private String marketId;
	private String productId;
	private String companyId;
	private String marketNm;
	private String productNm;
	private String companyNm;
	private String titleTxt;
	private int typeCd;
	private int orderNo;
	private int emailFlg;
	private String messageTxt;
	private String twitterTxt;
	private String creatorProfileId;
	private String firstNm;
	private String lastNm;
	private String statusCd;
	private String historyId;
	private Date publishDt;
	private Date createDt;
	private Date updateDt;
	private List<UpdateXRVO> sections;

	public UpdateVO() {
		super(UpdateIndexer.INDEX_TYPE);
		sections = new ArrayList<>();
		super.addOrganization(AdminControllerAction.BIOMED_ORG_ID);
		super.addRole(SecurityController.PUBLIC_ROLE_LEVEL);
	}


	public UpdateVO(ResultSet rs) {
		this();
		setData(rs);
	}

	public UpdateVO(ActionRequest req) {
		this();
		setData(req);
	}

	protected void setData(ActionRequest req) {
		SMTSession ses = req.getSession();
		UserVO vo = (UserVO) ses.getAttribute(Constants.USER_DATA);
		if (vo != null) {
			this.creatorProfileId = StringUtil.checkVal(req.getParameter("creatorProfileId"), vo.getProfileId());
		}
		setUpdateId(StringUtil.checkVal(req.getParameter("updateId")));
		this.marketId = StringUtil.checkVal(req.getParameter("marketId"), null);
		this.productId = StringUtil.checkVal(req.getParameter("productId"), null);
		this.companyId = StringUtil.checkVal(req.getParameter("companyId"), null);
		this.titleTxt = req.getParameter("titleTxt");
		this.typeCd = Convert.formatInteger(req.getParameter("typeCd"));
		this.messageTxt = req.getParameter("messageTxt");
		this.twitterTxt = req.getParameter("twitterTxt");
		this.statusCd = req.getParameter("statusCd");
		this.publishDt = Convert.formatDate(req.getParameter("publishDt"));
		this.orderNo = Convert.formatInteger(req.getParameter("orderNo"));
		this.emailFlg = Convert.formatInteger(req.getParameter("emailFlg"), 1);
		if (req.hasParameter("sectionId")) {
			String [] s = req.getParameterValues("sectionId");
			for (String sec : s) {
				sections.add(new UpdateXRVO(updateId, sec));
			}
		}
	}

	@Override
	@SolrField(name=SearchDocumentHandler.DOCUMENT_URL)
	public String getDocumentUrl() {
		StringBuilder url = new StringBuilder(50);
		if (!StringUtil.isEmpty(marketId)) {
			url.append(Section.MARKET.getURLToken()).append("qs/").append(marketId);
		} else if (!StringUtil.isEmpty(productId)) {
			url.append(Section.PRODUCT.getURLToken()).append("qs/").append(productId);
		} else if (!StringUtil.isEmpty(companyId)) {
			url.append(Section.COMPANY.getURLToken()).append("qs/").append(companyId);
		}

		return url.toString();
	}

	@Override
	@SolrField(name=SearchDocumentHandler.CONTENT_TYPE)
	public String getContentType() {
		if (!StringUtil.isEmpty(marketId)) {
			super.setContentType(Section.MARKET.toString());
		} else if (!StringUtil.isEmpty(productId)) {
			super.setContentType(Section.PRODUCT.toString());
		} else if (!StringUtil.isEmpty(companyId)) {
			super.setContentType(Section.COMPANY.toString());
		}
		return super.getContentType();
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
	public String getCompanyNm() {
		return companyNm;
	}

	/**
	 * @return the titleTxt
	 */
	@Column(name="title_txt")
	@SolrField(name=SearchDocumentHandler.TITLE)
	public String getTitleTxt() {
		return titleTxt;
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
	@Column(name="creator_profile_id", isInsertOnly=true)
	public String getCreatorProfileId() {
		return creatorProfileId;
	}

	/**
	 * @return the statusCd
	 */
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
	@SolrField(name=SearchDocumentHandler.PUBLISH_DATE)
	@Column(name="publish_dt")
	public Date getPublishDt() {
		return publishDt;
	}

	@SolrField(name="publishDtNoTime_s")
	public String getPublishDtNoTime() {
		return Convert.formatDate(publishDt, Convert.DATE_DASH_PATTERN);
	}

	@SolrField(name="publishTime_s")
	public String getPublishTime() {
		return Convert.formatDate(publishDt, Convert.TIME_LONG_PATTERN);
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
	@SolrField(name=SearchDocumentHandler.UPDATE_DATE)
	@Column(name="update_dt", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDt() {
		return updateDt;
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
			docId.insert(0, "_").insert(0, UpdateIndexer.INDEX_TYPE);
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
	 * @param titleTxt the titleTxt to set.
	 */
	public void setTitleTxt(String titleTxt) {
		this.titleTxt = titleTxt;
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
	 * @param creatorProfileId the creatorProfileId to set.
	 */
	public void setCreatorProfileId(String creatorProfileId) {
		this.creatorProfileId = creatorProfileId;
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
		this.publishDt = publishDt;
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
					SectionVO sec = (SectionVO) n.getUserObject();
					super.addACLGroup(Permission.GRANT, sec.getSolrTokenTxt());
				}
			}
		}
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
		return titleTxt;
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
}