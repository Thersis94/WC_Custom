package com.biomed.smarttrak.vo;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.admin.InsightAction;
import com.biomed.smarttrak.util.BiomedInsightIndexer;
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
import com.siliconmtn.util.user.HumanNameIntfc;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.util.solr.SecureSolrDocumentVO;


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
@Table(name="biomedgps_insight")
public class InsightVO extends SecureSolrDocumentVO implements HumanNameIntfc {


	public enum InsightStatusCd {P("Published"), D("Deleted"), E("Edit");

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
	private String profileImg;
	private String statusCd;
	private int orderNo;
	private Date publishDt;
	private Date createDt;
	private Date updateDt;

	public enum InsightType {
		CLINICAL(12, "Clinical"),
		COMPLIANCE(11, "Compliance"),
		HEALTHCARE_REFORM(13,"Healthcare Reform"),
		INSIGHTS(4, "Insights"),
		INTELLECTUAL_PROPERTY(14,"Intellectual Property"),
		MARKET_ANALYSIS(5, "Market Analysis"),
		MARKET_OUTLOOK(9, "Market Outlook"),
		MARKET_RECAP(10, "Market Recap"),
		PERSPECTIVE(1, "Perspective"),
		REGULATORY(8, "Regulatory"),
		REIMBURSEMENT(7, "Reimbursement"),
		SMARTTRAK_VIDEO_TIPS(15, "SmartTRAK Video Tips"),
		STARTUP_SPOTLIGHT(2, "Start-up Spotlight"),
		UC_VIEWPOINT(3, "U.C. Viewpoints"),
		UNCATIGORIZED(6, "Uncatigorized");

		private int val;
		private String text;

		InsightType(int val, String text) {
			this.val = val;
			this.text = text;
		}

		public int getVal() {
			return this.val;
		}
		public String getText() {
			return this.text;
		}
	}

	private List<InsightXRVO> sections;

	/**
	 * @param solrIndex
	 */
	public InsightVO() {
		super(BiomedInsightIndexer.INDEX_TYPE);
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

	/**
	 * sets the vo off of the req object
	 * @param req
	 */
	protected void setData(ActionRequest req) {
		SMTSession ses = req.getSession();
		UserVO vo = (UserVO) ses.getAttribute(Constants.USER_DATA);
		if(vo != null) {
			this.setCreatorProfileId(StringUtil.checkVal(req.getParameter("creatorProfileId"), vo.getProfileId()));
		}
		setInsightId(req.getParameter("insightsId"));

		if (StringUtil.isEmpty(insightId)) setInsightId(req.getParameter("pkId"));

		setTitleTxt(req.getParameter("titleTxt"));
		setTypeCd(Convert.formatInteger(req.getParameter("typeCd")));
		setAbstractTxt(req.getParameter("abstractTxt"));
		setBylineTxt(req.getParameter("bylineTxt"));
		setContentTxt(req.getParameter("contentTxt"));
		setSideContentTxt(req.getParameter("sideContentTxt"));
		setFeaturedFlg(Convert.formatInteger(req.getParameter("featuredFlg")));
		setFeaturedImageTxt(req.getParameter("featuredImageTxt"));
		setProfileImg(req.getParameter("profileImg"));
		setStatusCd(req.getParameter("statusCd"));
		setOrderNo(Convert.formatInteger(req.getParameter("orderNo")));
		setPublishDt(Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("publishDt")));	

		//only want to see the publish date to today if the status is publish and the 
		//date feild is null
		if(InsightStatusCd.P.toString().equals(statusCd) && publishDt == null) {
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

	/**
	 * adds an insight section to the list
	 * @param u
	 */
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
	public void configureSolrHierarchies(Tree t) {
		for(InsightXRVO uxr : sections) {
			
			if (uxr.getSectionId() == null){
				uxr.setSectionId(InsightAction.ROOT_NODE_ID);
			}
			
			Node n = t.findNode(uxr.getSectionId());
			
			if(n != null && !StringUtil.isEmpty(n.getFullPath())) {
				super.addHierarchies(n.getFullPath());
				SectionVO sec = (SectionVO) n.getUserObject();
				super.addACLGroup(Permission.GRANT, sec.getSolrTokenTxt());
			}
		}

	}

	/* (non-Javadoc)
	 * @see com.biomed.smarttrak.admin.user.HumanNameIntfc#getFirstName()
	 */
	@Override
	@SolrField(name="firstNm_s")
	@Column(name="first_nm", isReadOnly=true)
	public String getFirstName() {
		return firstNm;
	}

	/**
	 * @return the lastNm
	 */
	@SolrField(name="lastNm_s")
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
	 * @return the insightId
	 */
	@Column(name="insight_id", isPrimaryKey=true)
	public String getInsightId() {
		//doesnt have a solr tag as it is set to doc 
		return insightId;
	}

	/**
	 * @return the creatorProfileId
	 */
	@Column(name="creator_profile_id", isInsertOnly=true)
	public String getCreatorProfileId() {
		return creatorProfileId;
	}

	/**
	 * @return the titleTxt
	 */
	@SolrField(name=SearchDocumentHandler.TITLE)
	@Column(name="title_txt")
	public String getTitleTxt() {
		return titleTxt;
	}

	/**
	 * gets the numeric type code
	 * @return the typeNm
	 */
	@SolrField(name=SearchDocumentHandler.MODULE_TYPE)
	@Column(name="type_cd")
	public int getTypeCd() {
		return typeCd;
	}

	/**
	 * looks at the type code and returns the string name
	 * @return
	 */
	public String getTypeNm() {
		InsightType t = getInsightType();
		if(t != null){
			return t.getText();
		}
		else
			return "";
	}

	/**
	 * @return the abstractTxt
	 */
	@SolrField(name=SearchDocumentHandler.SUMMARY)
	@Column(name="abstract_txt")
	public String getAbstractTxt() {
		return abstractTxt;
	}

	/**
	 * @return the bylineTxt
	 */
	@Column(name="byline_txt")
	public String getBylineTxt() {
		return bylineTxt;
	}

	/**
	 * @return the contentTxt
	 */
	@Column(name="content_txt")
	public String getContentTxt() {
		return contentTxt;
	}

	/**
	 * @return the sideContentTxt
	 */
	@Column(name="side_content_txt")
	public String getSideContentTxt() {
		return sideContentTxt;
	}

	/**
	 * @return the featuredFlg
	 */
	@SolrField(name="featuredFlg_i")
	@Column(name="featured_flg")
	public int getFeaturedFlg() {
		return featuredFlg;
	}

	/**
	 * @return the featuredImageTxt
	 */
	@SolrField(name="featuredImageTxt_s")
	@Column(name="featured_image_txt")
	public String getFeaturedImageTxt() {
		return featuredImageTxt;
	}

	/**
	 * @return the statusCd
	 */
	@Column(name="status_cd")
	public String getStatusCd() {
		return statusCd;
	}
	/**
	 * @return the orderNo
	 */
	@SolrField(name="orderNo_i")
	@Column(name="order_no")
	public int getOrderNo() {
		return orderNo;
	}

	/**
	 * @return the publishDt
	 */
	@SolrField(name="publishDt_s")
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
	@SolrField(name=SearchDocumentHandler.UPDATE_DATE)
	@Column(name="update_dt", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDt() {
		return updateDt;
	}


	/**
	 * used on the list view to trigger a retrieve of one particular insight
	 */
	@Override
	@SolrField(name=SearchDocumentHandler.DOCUMENT_URL)
	public String getDocumentUrl() {
		StringBuilder url = new StringBuilder(50);
		url.append(AdminControllerAction.Section.INSIGHTS.getURLToken()).append("qs/").append(this.insightId);
		return url.toString();
	}

	/**
	 * @return the profileImg
	 */
	@SolrField(name="profileImg_s")
	@Column(name="profile_img", isReadOnly=true)
	public String getProfileImg() {
		return profileImg;
	}


	/**
	 * @param authorImageTxt the authorImageTxt to set
	 */
	public void setProfileImg(String profileImg) {
		this.profileImg = profileImg;
	}
	
	/**
	 * @param insightId the insightId to set
	 */
	public void setInsightId(String insightId) {
		super.setDocumentId("ins_"+insightId);
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
	 * Helper method gets the InsightSection for the internal typeCd.
	 * @return
	 */
	public InsightType getInsightType() {

		for(InsightType type : InsightType.values()){
			if (type.getVal() == typeCd){
				return type;
			}
		}
		return null;
	}
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return StringUtil.getToString(this);
	}
}

