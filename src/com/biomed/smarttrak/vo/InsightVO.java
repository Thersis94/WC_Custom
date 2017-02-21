package com.biomed.smarttrak.vo;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.admin.InsightAction.InsightCategory;
import com.biomed.smarttrak.admin.UpdatesAction.UpdateType;
import com.biomed.smarttrak.admin.user.HumanNameIntfc;
import com.biomed.smarttrak.solr.BiomedUpdateIndexer;
import com.biomed.smarttrak.vo.UpdatesVO.UpdateStatusCd;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.annotations.SolrField;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
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
 * <b>Title</b>: InsightVO.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> Holds all the data related to one Insight entry
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Feb 20, 2017<p/>
 * @updates:
 ****************************************************************************/
@Table(name="BIOMEDGPS_INSIGHT")
public class InsightVO extends SolrDocumentVO implements HumanNameIntfc {


	public enum InsightStatusCd {N("New"), R("Reviewed"), A("Archived");

	private String statusName;
	InsightStatusCd(String statusName) {
		this.statusName = statusName;
	}

	public String getStatusName() {
		return statusName;
	}
	}

	private String insightId;
	private String creatorProfileId;
	private String firstNm;
	private String lastNm;
	private String titleTxt;
	private int typeCd;
	private String abstractTxt;
	private String bylineTxt;
	private String contentTxt;
	private String sideContentTxt;
	private int featuredFlg;
	private String featuredImageTxt;
	private String statusCd;
	private int orderNo;
	private Date publishDt;
	private Date createDt;
	private Date updateDt;
	
	
	//TODO looks like i might need a second VO
	private List<InsightXRVO> sections;

	/**
	 * @param solrIndex
	 */
	public InsightVO() {
		//TODO replace this with a insite indexer when it exists
		super(BiomedUpdateIndexer.INDEX_TYPE);
		sections = new ArrayList<>();
		super.addOrganization(AdminControllerAction.BIOMED_ORG_ID);
		super.addRole(AdminControllerAction.DEFAULT_ROLE_LEVEL);
	}

	
	public InsightVO(ResultSet rs) {
		this();
		setData(rs);
	}

	public InsightVO(ActionRequest req) {
		this();
		setData(req);
	}
	
	protected void setData(ResultSet rs) {
		
	}

	protected void setData(ActionRequest req) {
		SMTSession ses = req.getSession();
		UserVO vo = (UserVO) ses.getAttribute(Constants.USER_DATA);
		if(vo != null) {
			this.setCreatorProfileId(StringUtil.checkVal(req.getParameter("creatorProfileId"), vo.getProfileId()));
		}
		setInsightId(req.getParameter("insightId"));
		setTitleTxt(req.getParameter("titleTxt"));
		setTypeCd(Convert.formatInteger(req.getParameter("typeCd")));
		setAbstractTxt(req.getParameter("abstractTxt"));
		setBylineTxt(req.getParameter("bylineTxt"));
		setContentTxt(req.getParameter("contentTxt"));
		setFeaturedFlg(Convert.formatInteger(req.getParameter("featuredFlg")));
		setFeaturedImageTxt(req.getParameter("featuredImageTxt"));
		setStatusCd(req.getParameter("statusCd"));
		setOrderNo(Convert.formatInteger(req.getParameter("orderNo")));
		if(InsightStatusCd.R.toString().equals(statusCd)) {
			setPublishDt(new Date());
		}
		if(req.hasParameter("sectionId")) {
			String [] s = req.getParameterValues("sectionId");
			for(String sec : s) {
				sections.add(new InsightXRVO(insightId, sec));
			}
		}
	}
	
	/**
	 * @return the sections
	 */
	public List<InsightXRVO> getInsightSections() {
		return sections;
	}

	@BeanSubElement()
	public void addInsightXrVO(InsightXRVO u) {
		if(u != null)
			this.sections.add(u);
	}
	
	/**
	 * @param sections the sections to set.
	 */
	public void setSections(List<InsightXRVO> sections) {
		this.sections = sections;
	}
	
	/**
	 * Helper method that builds hierarchy path.
	 * 
	 * Replace spaces with _ and replace & and and
	 * @param loadSections
	 */
	public void setHierarchies(Tree t) {
		for(InsightXRVO uxr : sections) {
			Node n = t.findNode(uxr.getSectionId());
			if(n != null && !StringUtil.isEmpty(n.getFullPath())) {
				super.addHierarchies(n.getFullPath().replaceAll(" ", "_").replaceAll("&", "and"));
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.biomed.smarttrak.admin.user.HumanNameIntfc#getFirstName()
	 */
	@Override
	@Column(name="FIRST_NM", isReadOnly=true)
	public String getFirstName() {
		//TODO ask about read only
		return firstNm;
	}

	/**
	 * @return the lastNm
	 */
	@Column(name="LAST_NM", isReadOnly=true)
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
	 * @return the insightId
	 */
	@Column(name="INSIGHT_ID", isPrimaryKey=true)
	public String getInsightId() {
		return insightId;
	}

	/**
	 * @return the creatorProfileId
	 */
	@Column(name="CREATOR_PROFILE_ID", isInsertOnly=true)
	public String getCreatorProfileId() {
		return creatorProfileId;
	}

	/**
	 * @return the titleTxt
	 */
	@Column(name="TITLE_TXT")
	public String getTitleTxt() {
		return titleTxt;
	}
	
	/**
	 * @return the typeNm
	 */
	@SolrField(name=SearchDocumentHandler.MODULE_TYPE)
	@Column(name="TYPE_CD")
	public int getTypeCd() {
		return typeCd;
	}
	
	/**
	 * @return the abstractTxt
	 */
	@Column(name="ABSTRACT_TXT")
	public String getAbstractTxt() {
		return abstractTxt;
	}

	/**
	 * @return the bylineTxt
	 */
	@Column(name="BYLINE_TXT")
	public String getBylineTxt() {
		return bylineTxt;
	}

	/**
	 * @return the contentTxt
	 */
	@Column(name="CONTENT_TXT")
	public String getContentTxt() {
		return contentTxt;
	}

	/**
	 * @return the sideContentTxt
	 */
	@Column(name="SIDE_CONTENT_TXT")
	public String getSideContentTxt() {
		return sideContentTxt;
	}
	
	/**
	 * @return the featuredFlg
	 */
	@Column(name="FEATURED_FLG")
	public int getFeaturedFlg() {
		return featuredFlg;
	}
	
	/**
	 * @return the featuredImageTxt
	 */
	@Column(name="FEATURED_IMAGE_TXT")
	public String getFeaturedImageTxt() {
		return featuredImageTxt;
	}
	
	/**
	 * @return the statusCd
	 */
	@Column(name="STATUS_CD")
	public String getStatusCd() {
		return statusCd;
	}
	/**
	 * @return the orderNo
	 */
	@Column(name="ORDER_NO")
	public int getOrderNo() {
		return orderNo;
	}
	
	/**
	 * @return the publishDt
	 */
	@Column(name="PUBLISH_DT")
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
	 * @param insightId the insightId to set
	 */
	public void setInsightId(String insightId) {
		this.insightId = insightId;
	}

	/**
	 * @param creatorProfileId the creatorProfileId to set
	 */
	public void setCreatorProfileId(String creatorProfileId) {
		this.creatorProfileId = creatorProfileId;
	}

	/**
	 * @param titleTxt the titleTxt to set
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
	 * @param abstractTxt the abstractTxt to set
	 */
	public void setAbstractTxt(String abstractTxt) {
		this.abstractTxt = abstractTxt;
	}
	
	/**
	 * @param bylineTxt the bylineTxt to set
	 */
	public void setBylineTxt(String bylineTxt) {
		this.bylineTxt = bylineTxt;
	}
	
	/**
	 * @param contentTxt the contentTxt to set
	 */
	public void setContentTxt(String contentTxt) {
		this.contentTxt = contentTxt;
	}
	
	/**
	 * @param sideContentTxt the sideContentTxt to set
	 */
	public void setSideContentTxt(String sideContentTxt) {
		this.sideContentTxt = sideContentTxt;
	}
	
	/**
	 * @param featuredFlg the featuredFlg to set
	 */
	public void setFeaturedFlg(int featuredFlg) {
		this.featuredFlg = featuredFlg;
	}
	
	/**
	 * @param featuredImageTxt the featuredImageTxt to set
	 */
	public void setFeaturedImageTxt(String featuredImageTxt) {
		this.featuredImageTxt = featuredImageTxt;
	}
	
	/**
	 * @param statusCd the statusCd to set
	 */
	public void setStatusCd(String statusCd) {
		this.statusCd = statusCd;
	}
	
	/**
	 * @param orderNo the orderNo to set
	 */
	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}
	
	/**
	 * @param publishDt the publishDt to set
	 */
	public void setPublishDt(Date publishDt) {
		this.publishDt = publishDt;
	}
	
	/**
	 * @param createDt the createDt to set
	 */
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}
	
	/**
	 * @param createDt the createDt to set
	 */
	public void setUpdateDt(Date updateDt) {
		this.updateDt = updateDt;
	}

	/**
	 * Helper method gets the UpdateType for the internal typeCd.
	 * @return
	 */
	public InsightCategory getType() {
		switch(typeCd) {
			case 12 :
				return InsightCategory.EXTREMITIES;
			case 15 :
				return InsightCategory.TOTAL_JOINT;
			case 17 :
				return InsightCategory.TRAUMA;
			case 20 :
				return InsightCategory.EU_TRAUMA;
			case 30 :
				return InsightCategory.SPINE;
			case 35 :
				return InsightCategory.ORTHOBIO;
			case 37 :
				return InsightCategory.SOFT_TISSUE;
			case 38 :
				return InsightCategory.ADV_WOUND_CARE;
			case 40 :
				return InsightCategory.EU_ADV_WOUND_CARE;
			case 45 :
				return InsightCategory.SURGICAL_MATRICIES;
			case 50 :
				return InsightCategory.INF_PREV;
			case 55 :
				return InsightCategory.GLUES_AND_SEALANTS;
			case 60 :
				return InsightCategory.WND_MGMT_STD_OF_CARE;
			case 65 :
				return InsightCategory.REGEN_MED;
			case 70	:
				return InsightCategory.NEUROVASCULAR;
			case 75 :
				return InsightCategory.NEUROMODULATION;
			default :
				return null;
		}
	}
}
