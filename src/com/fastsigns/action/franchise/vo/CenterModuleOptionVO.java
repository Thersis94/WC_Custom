package com.fastsigns.action.franchise.vo;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.approval.Approvable;
import com.smt.sitebuilder.approval.ApprovalVO;

/****************************************************************************
 * <b>Title</b>: CenterModuleOptionVO.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Nov 22, 2010<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class CenterModuleOptionVO implements Serializable, Approvable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// Member Variables 
	private Map<String, OptionAttributeVO> attributes = new LinkedHashMap<String, OptionAttributeVO>();
	private Integer moduleOptionId = Integer.valueOf(0);
	private Integer moduleTypeId = Integer.valueOf(0);
	private String optionName = null;
	private String contentPath = null;
	private String optionDesc = null;
	private Integer rankNo = null;
	private String articleText = null;
	private String linkUrl = null;
	private String filePath = null;
	private String thumbPath = null;
	private String stillFramePath = null;
	private Date startDate = null;
	private Date endDate = null;
	private Integer approvalFlag = Integer.valueOf(0); //0=pending
	private Integer parentId = Integer.valueOf(0); //used to link modules with their predecessor prior to approval
	private Integer franchiseId = null;
	private String typeName = null;
	private Integer moduleLocationId = null;
	private Integer orderNo = null;
	private Date createDate = null;
	private String actionId = null;
	private String responseText = null;
	private String moduleFranXRId = null;
	private ApprovalVO approval;
	
	public CenterModuleOptionVO() {
		
	}

	public CenterModuleOptionVO(ResultSet rs) {
		this.assignVals(rs);
		try {
			this.franchiseId = rs.getInt("option_franchise_id");
		} catch (Exception e) {
			//let this fail silently.  Only the public-site query will pass it
		}
	}
	
	public CenterModuleOptionVO(SMTServletRequest req) {
		this.assignVals(req);
	}
	
	public String toString() {
		return StringUtil.getToString(this);
	}
	
	public void assignVals(ResultSet rs) {
		DBUtil db = new DBUtil();
		
		if (moduleOptionId == 0) {
			moduleOptionId = db.getIntegerVal("cp_module_option_id", rs);
			if (db.getIntegerVal("mod_opt_id", rs) > 0)
				moduleOptionId = db.getIntegerVal("mod_opt_id", rs);

			moduleTypeId = db.getIntegerVal("fts_cp_module_type_id", rs);
			optionName = db.getStringVal("option_nm", rs);
			contentPath = db.getStringVal("content_path_txt", rs);
			optionDesc = db.getStringVal("option_desc", rs);
			articleText = db.getStringVal("article_txt", rs);
			rankNo = db.getIntegerVal("rank_no", rs);
			linkUrl = db.getStringVal("link_url", rs);
			thumbPath = db.getStringVal("thumb_path_url", rs);
			filePath = db.getStringVal("file_path_url", rs);
			stillFramePath = db.getStringVal("video_stillframe_url", rs);
			startDate = db.getDateVal("start_dt", rs);
			endDate = db.getDateVal("end_dt", rs);
			approvalFlag = db.getIntegerVal("approval_flg", rs);
			parentId = db.getIntegerVal("parent_id", rs);
			franchiseId = db.getIntegerVal("franchise_id", rs);
			createDate = db.getDateVal("option_create_dt", rs);
			actionId = db.getStringVal("FTS_CP_MODULE_ACTION_ID", rs);
			responseText = db.getStringVal("RESPONSE_TXT", rs);
			moduleFranXRId = db.getStringVal("CP_MODULE_FRANCHISE_XR_ID", rs);

			//from related tables
			typeName = db.getStringVal("type_nm", rs);
			moduleLocationId = db.getIntegerVal("cp_location_module_xr_id",rs);
			setOrderNo(db.getIntVal("order_no", rs));
		}
		
		// Add attributes
		String id = db.getStringVal("cp_option_attr_id", rs);
		String key = db.getStringVal("attr_key_cd", rs);
		String value = db.getStringVal("attrib_value_txt", rs);
		int orderNo = db.getIntVal("attr_order_no", rs);
		this.addAttribute(id, key, value, orderNo);
	}
	
	/*
	 * added for Keystone interface...
	 */
	public void assignVals(SMTServletRequest req) {
		moduleOptionId = Convert.formatInteger(req.getParameter("moduleOptionId"));
		parentId = Convert.formatInteger(req.getParameter("parentId"), 0);
		moduleTypeId = Convert.formatInteger(req.getParameter("moduleTypeId"));
		if (moduleTypeId == null) 
			moduleTypeId = Convert.formatInteger(req.getParameter("modTypeId"));
		optionName = req.getParameter("optionName");
		optionDesc = req.getParameter("optionDesc");
		linkUrl = req.getParameter("linkUrl");
		contentPath = req.getParameter("contentPath");
		startDate = Convert.formatDate(req.getParameter("startDate"));
		endDate = Convert.formatDate(req.getParameter("endDate"));
		articleText = req.getParameter("articleText");
		rankNo = Convert.formatInteger(req.getParameter("rankNo"));
		filePath = req.getParameter("filePathUrl");
		thumbPath = req.getParameter("thumbPathUrl");
		stillFramePath = req.getParameter("videoStillframeUrl");
		actionId = req.getParameter("ftsCpModuleActionId");
		responseText = req.getParameter("responseText");
		//the 3 files are added to this VO by the action, since they need to be saved first
	}

	/**
	 * 
	 * @param key
	 * @param val
	 */
	public void addAttribute(String id, String key, String val, int orderNo) {
		OptionAttributeVO vo = new OptionAttributeVO(id, key, val);
		vo.setOrderNo(orderNo);
		attributes.put(id, vo);
	}
	
	public void addAttribute(OptionAttributeVO vo) {
		attributes.put(vo.getId(), vo);
	}
	
	public void addAttribute(List<OptionAttributeVO> attrs) {
		for (OptionAttributeVO vo : attrs) {
			attributes.put(vo.getId(), vo);
		}
	}

	/**
	 * @return the attributes
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<OptionAttributeVO> getAttributes() {
		return new ArrayList(attributes.values());
	}

	/**
	 * @param attributes the attributes to set
	 */
	public void setAttributes(Map<String, OptionAttributeVO> attributes) {
		this.attributes = attributes;
	}

	/**
	 * @return the moduleOptionId
	 */
	public Integer getModuleOptionId() {
		return moduleOptionId;
	}

	/**
	 * @param moduleOptionId the moduleOptionId to set
	 */
	public void setModuleOptionId(Integer moduleOptionId) {
		this.moduleOptionId = moduleOptionId;
	}

	/**
	 * @return the moduleTypeId
	 */
	public Integer getModuleTypeId() {
		return moduleTypeId;
	}

	/**
	 * @param moduleTypeId the moduleTypeId to set
	 */
	public void setModuleTypeId(Integer moduleTypeId) {
		this.moduleTypeId = moduleTypeId;
	}

	/**
	 * @return the optionName
	 */
	public String getOptionName() {
		return optionName;
	}

	/**
	 * @param optionName the optionName to set
	 */
	public void setOptionName(String optionName) {
		this.optionName = optionName;
	}

	/**
	 * @return the contentPath
	 */
	public String getContentPath() {
		return contentPath;
	}

	/**
	 * @param contentPath the contentPath to set
	 */
	public void setContentPath(String contentPath) {
		this.contentPath = contentPath;
	}

	/**
	 * @return the optionDesc
	 */
	public String getOptionDesc() {
		return optionDesc;
	}

	/**
	 * @param optionDesc the optionDesc to set
	 */
	public void setOptionDesc(String optionDesc) {
		this.optionDesc = optionDesc;
	}

	/**
	 * @return the articleText
	 */
	public String getArticleText() {
		return articleText;
	}

	/**
	 * @param articleText the articleText to set
	 */
	public void setArticleText(String articleText) {
		this.articleText = articleText;
	}

	/**
	 * @return the rankNo
	 */
	public Integer getRankNo() {
		return rankNo;
	}

	/**
	 * @param rankNo the rankNo to set
	 */
	public void setRankNo(Integer rankNo) {
		this.rankNo = rankNo;
	}

	
	/**
	 * @return the linkUrl
	 */
	public String getLinkUrl() {
		return linkUrl;
	}

	/**
	 * @param linkUrl the linkUrl to set
	 */
	public void setLinkUrl(String linkUrl) {
		this.linkUrl = linkUrl;
	}

	/**
	 * @return the filePath
	 */
	public String getFilePath() {
		return filePath;
	}

	/**
	 * @param filePath the filePath to set
	 */
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	/**
	 * @return the thumbPath
	 */
	public String getThumbPath() {
		return thumbPath;
	}

	/**
	 * @param thumbPath the thumbPath to set
	 */
	public void setThumbPath(String thumbPath) {
		this.thumbPath = thumbPath;
	}
	
	/**
	 * @return the stillFramePath
	 */
	public String getStillFramePath() {
		return stillFramePath;
	}

	/**
	 * @param stillFramePath the stillFramePath to set
	 */
	public void setStillFramePath(String stillFramePath) {
		this.stillFramePath = stillFramePath;
	}

	/**
	 * @return the startDate
	 */
	public Date getStartDate() {
		return startDate;
	}

	/**
	 * @param startDate the startDate to set
	 */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/**
	 * @return the endDate
	 */
	public Date getEndDate() {
		return endDate;
	}

	/**
	 * @param endDate the endDate to set
	 */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public void setApprovalFlag(Integer approvalFlag) {
		this.approvalFlag = approvalFlag;
	}

	public Integer getApprovalFlag() {
		return approvalFlag;
	}

	public void setParentId(Integer parentId) {
		this.parentId = parentId;
	}

	public Integer getParentId() {
		return parentId;
	}

	public void setFranchiseId(Integer franchiseId) {
		this.franchiseId = franchiseId;
	}

	public Integer getFranchiseId() {
		return franchiseId;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setModuleLocationId(Integer moduleLocationId) {
		this.moduleLocationId = moduleLocationId;
	}

	public Integer getModuleLocationId() {
		return moduleLocationId;
	}

	public void setOrderNo(Integer orderNo) {
		this.orderNo = orderNo;
	}

	public Integer getOrderNo() {
		return orderNo;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @param actionId the actionId to set
	 */
	public void setActionId(String actionId) {
		this.actionId = actionId;
	}

	/**
	 * @return the actionId
	 */
	public String getActionId() {
		return actionId;
	}

	public void setResponseText(String responseText) {
		this.responseText = responseText;
	}

	public String getResponseText() {
		return responseText;
	}

	public void setModuleFranXRId(String moduleFranXRId) {
		this.moduleFranXRId = moduleFranXRId;
	}

	public String getModuleFranXRId() {
		return moduleFranXRId;
	}

	@Override
	public ApprovalVO getSyncData() {
		return approval;
	}

	@Override
	public void setSyncData(ApprovalVO approval) {
		this.approval = approval;
	}

}
