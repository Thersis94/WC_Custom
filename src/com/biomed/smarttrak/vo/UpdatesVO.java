/**
 *
 */
package com.biomed.smarttrak.vo;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.action.AdminControllerAction.Section;
import com.biomed.smarttrak.admin.UpdatesAction.UpdateType;
import com.biomed.smarttrak.admin.user.HumanNameIntfc;
import com.biomed.smarttrak.solr.BiomedUpdateIndexer;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.annotations.SolrField;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: UpdatesVO.java
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
public class UpdatesVO extends SolrDocumentVO implements HumanNameIntfc {

	public enum UpdateStatusCd {N("New"), R("Reviewed"), A("Archived");
		private String statusName;
		UpdateStatusCd(String statusName) {
			this.statusName = statusName;
		}

		public String getStatusName() {
			return statusName;
		}
	}

	private String updateId;
	//Create single point url or field for id/type
	private String marketId;
	private String productId;
	private String companyId;
	private String titleTxt;
	private int typeCd;
	private String messageTxt;
	private String twitterTxt;
	private String creatorProfileId;
	private String firstNm;
	private String lastNm;
	private String statusCd;
	private Date publishDt;
	private Date createDt;
	private Date updateDt;

	private List<UpdatesXRVO> sections;

	public UpdatesVO() {
		super(BiomedUpdateIndexer.INDEX_TYPE);
		sections = new ArrayList<>();
		super.addOrganization(AdminControllerAction.BIOMED_ORG_ID);
		super.addRole(AdminControllerAction.DEFAULT_ROLE_LEVEL);
	}


	public UpdatesVO(ResultSet rs) {
		this();
		setData(rs);
	}

	public UpdatesVO(ActionRequest req) {
		this();
		setData(req);
	}

	protected void setData(ResultSet rs) {
		
	}

	protected void setData(ActionRequest req) {
		SMTSession ses = req.getSession();
		UserVO vo = (UserVO) ses.getAttribute(Constants.USER_DATA);
		if(vo != null) {
			this.creatorProfileId = StringUtil.checkVal(req.getParameter("creatorProfileId"), vo.getProfileId());
		}
		setUpdateId(req.getParameter("updateId"));
		this.marketId = StringUtil.checkVal(req.getParameter("marketId"), null);
		this.productId = StringUtil.checkVal(req.getParameter("productId"), null);
		this.companyId = StringUtil.checkVal(req.getParameter("companyId"), null);
		this.titleTxt = req.getParameter("titleTxt");
		this.typeCd = Convert.formatInteger(req.getParameter("typeCd"));
		this.messageTxt = req.getParameter("messageTxt");
		this.twitterTxt = req.getParameter("twitterTxt");
		this.statusCd = req.getParameter("statusCd");
		if(UpdateStatusCd.R.toString().equals(statusCd)) {
			setPublishDt(new Date());
		}
		if(req.hasParameter("sectionId")) {
			String [] s = req.getParameterValues("sectionId");
			for(String sec : s) {
				sections.add(new UpdatesXRVO(updateId, sec));
			}
		}
	}

	@Override
	@SolrField(name=SearchDocumentHandler.DOCUMENT_URL)
	public String getDocumentUrl() {
		StringBuilder url = new StringBuilder(50);
		if(!StringUtil.isEmpty(marketId)) {
			url.append(Section.MARKET.getURLToken()).append("qs/").append(marketId);
		} else if(!StringUtil.isEmpty(productId)) {
			url.append(Section.PRODUCT.getURLToken()).append("qs/").append(productId);
		} else if(!StringUtil.isEmpty(companyId)) {
			url.append(Section.COMPANY.getURLToken()).append("qs/").append(companyId);
		}

		return url.toString();
	}

	@Override
	@SolrField(name=SearchDocumentHandler.CONTENT_TYPE)
	public String getContentType() {
		if(!StringUtil.isEmpty(marketId)) {
			super.setContentType(Section.MARKET.toString());
		} else if(!StringUtil.isEmpty(productId)) {
			super.setContentType(Section.PRODUCT.toString());
		} else if(!StringUtil.isEmpty(companyId)) {
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
	 * @return the productId
	 */
	@Column(name="product_id")
	public String getProductId() {
		return productId;
	}

	/**
	 * @return the companyId
	 */
	@Column(name="company_id")
	public String getCompanyId() {
		return companyId;
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
	 * @return the publishDt
	 */
	@SolrField(name=SearchDocumentHandler.UPDATE_DATE)
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
	 * @return the updateDt
	 */
	@Column(name="update_dt", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDt() {
		return updateDt;
	}

	/**
	 * @return the sections
	 */
	public List<UpdatesXRVO> getUpdateSections() {
		return sections;
	}

	@BeanSubElement()
	public void addUpdateXrVO(UpdatesXRVO u) {
		if(u != null)
			this.sections.add(u);
	}

	@Override
	@SolrField(name=SearchDocumentHandler.SECTION)
	public List<String> getSections() {
		List<String> secList = new ArrayList<>();
		for(UpdatesXRVO uxr : sections) {
			secList.add(uxr.getSectionId());
		}
		return secList;
	}
	/**
	 * @param updateId the updateId to set.
	 */
	public void setUpdateId(String updateId) {
		this.updateId = updateId;
		setDocumentId(updateId);
	}

	/**
	 * @param marketId the marketId to set.
	 */
	public void setMarketId(String marketId) {
		this.marketId = marketId;
	}

	/**
	 * @param productId the productId to set.
	 */
	public void setProductId(String productId) {
		this.productId = productId;
	}

	/**
	 * @param companyId the companyId to set.
	 */
	public void setCompanyId(String companyId) {
		this.companyId = companyId;
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
	public void setSections(List<UpdatesXRVO> sections) {
		this.sections = sections;
	}

	/**
	 * Helper method gets the UpdateType for the internal typeCd.
	 * @return
	 */
	public UpdateType getType() {
		switch(typeCd) {
			case 12 :
				return UpdateType.MARKET;
			case 15 :
				return UpdateType.REVENUES;
			case 17 :
				return UpdateType.NEW_PRODUCTS;
			case 20 :
				return UpdateType.DEALS_FINANCING;
			case 30 :
				return UpdateType.CLINICAL_REGULATORY;
			case 35 :
				return UpdateType.PATENTS;
			case 37 :
				return UpdateType.REIMBURSEMENT;
			case 38 :
				return UpdateType.ANNOUNCEMENTS;
			case 40 :
				return UpdateType.STUDIES;
			default :
				return null;
		}
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
}