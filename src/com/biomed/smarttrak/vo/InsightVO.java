package com.biomed.smarttrak.vo;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.admin.user.HumanNameIntfc;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.annotations.SolrField;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.DBUtil;
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
@Table(name="biomedgps_insight")
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
	
	public enum InsightType {
		PERSPECTIVE(10, "Perspective"),
		MARKET_RECAP(20, "Market Recap"),
		MARKET_OUTLOOK(25, "Market Outlook"),
		STARTUP_SPOTLIGHT(30, "Start-up Spotlight");

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
		//TODO replace this with a insight indexer when it exists
		super("");
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
		
		DBUtil util = new DBUtil();

		setCreatorProfileId(util.getStringVal("PROFILE_ID", rs));
		setInsightId(util.getStringVal("INSIGHT_ID", rs));
		setTitleTxt(util.getStringVal("TITLE_TXT", rs));
		setTypeCd(util.getIntVal("TYPE_CD", rs));
		setAbstractTxt(util.getStringVal("ABSTRACT_TXT", rs));
		setBylineTxt(util.getStringVal("BYLINE_TXT", rs));
		setContentTxt(util.getStringVal("CONTENT_TXT", rs));
		setFeaturedFlg(util.getIntVal("FEATURED_FLG", rs));
		setFeaturedImageTxt(util.getStringVal("FEATURED_IMAGE_TXT", rs));
		setStatusCd(util.getStringVal("STATUS_CD", rs));
		setOrderNo(util.getIntVal("ORDER_NO", rs));
		if(InsightStatusCd.R.toString().equals(statusCd)) {
			setPublishDt(new Date());
		}

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
		setSideContentTxt(req.getParameter("sideContentTxt"));
		setFeaturedFlg(Convert.formatInteger(req.getParameter("featuredFlg")));
		setFeaturedImageTxt(req.getParameter("featuredImageTxt"));
		setStatusCd(req.getParameter("statusCd"));
		setOrderNo(Convert.formatInteger(req.getParameter("orderNo")));
		setPublishDt(Convert.formatDate(Convert.DATE_SLASH_PATTERN, req.getParameter("publishDt")));	
		
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
	 * @return the insightId
	 */
	@Column(name="insight_id", isPrimaryKey=true)
	public String getInsightId() {
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
	@Column(name="featured_flg")
	public int getFeaturedFlg() {
		return featuredFlg;
	}
	
	/**
	 * @return the featuredImageTxt
	 */
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
	@Column(name="order_no")
	public int getOrderNo() {
		return orderNo;
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
