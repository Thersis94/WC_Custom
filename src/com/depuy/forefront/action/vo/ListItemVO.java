package com.depuy.forefront.action.vo;

import java.io.Serializable;
import java.sql.ResultSet;

import com.depuy.forefront.action.ProgramAction;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

public class ListItemVO implements Serializable {
	private static final long serialVersionUID = 1134645724331L;
	private String listItemId = null;
	private String programId = null;
	private String hospitalId = null;
	private String hospitalName = null;
	private Integer itemTypeId = 0;
	private String listName = null;
	private String summaryText = null;
	private String articleText = null;
	private String thumbImageUrl = null;
	private String detailImageUrl = null;
	private String videoUrl = null;
	private String externalUrl = null;
	
	/**
	 * Variable for use in the Action Plan XR
	 */
	private String actionPlanId = null;
	private Integer orderNo = 0;
	private Integer caregiverFlg = 0;
	private Integer featuredFlg = 0;
	
	/*
	 * not used, but use as reference for itemTypeNo's
	public static enum ItemType { 
		Image=0, Video=1, Article=2, ExternalPage=3
	}
	*/
	
	public ListItemVO() {
	}
	
	public ListItemVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		listItemId = db.getStringVal("list_item_id", rs);
		programId = db.getStringVal("program_id", rs);
		hospitalId = db.getStringVal("hospital_id", rs);
		hospitalName = db.getStringVal("hospital_nm", rs);
		itemTypeId = db.getIntegerVal("item_type_no", rs);
		listName = db.getStringVal("list_nm", rs);
		summaryText = db.getStringVal("summary_txt", rs);
		articleText = db.getStringVal("article_txt", rs);
		thumbImageUrl = db.getStringVal("thumb_img_url", rs);
		detailImageUrl = db.getStringVal("detail_img_url", rs);
		videoUrl = db.getStringVal("video_url", rs);
		setExternalUrl(db.getStringVal("external_url", rs));
		
		actionPlanId = db.getStringVal("action_plan_xr_id", rs);
		orderNo = db.getIntegerVal("order_no", rs);
		setCaregiverFlg(db.getIntegerVal("caregiver_flg", rs));
		setFeaturedFlg(db.getIntegerVal("featured_flg", rs));

		db = null;
	}
	
	public ListItemVO(ActionRequest req) {
		listItemId = req.getParameter("listItemId");
		programId = (String) req.getSession().getAttribute(ProgramAction.PROGRAM_ID);
		hospitalId = StringUtil.checkVal(req.getParameter("hospitalId"), null);
		listName = req.getParameter("listName");
		summaryText = req.getParameter("summaryText");
		articleText = req.getParameter("articleText");
		thumbImageUrl = req.getParameter("thumbImageUrl");
		detailImageUrl = req.getParameter("detailImageUrl");
		externalUrl = req.getParameter("externalUrl");
		videoUrl = req.getParameter("videoUrl");
		itemTypeId = Convert.formatInteger(req.getParameter("typeName"));
				
		actionPlanId = StringUtil.checkVal(req.getParameter("actionPlanId"), null);
		orderNo = Convert.formatInteger(req.getParameter("orderNo"), 0);
		setCaregiverFlg(Convert.formatInteger(req.getParameter("caregiverFlg"), 0));
		setFeaturedFlg(Convert.formatInteger(req.getParameter("featuredFlg"), 0));
	}

	/**
	 * @return the listItemId
	 */
	public String getListItemId() {
		return listItemId;
	}

	/**
	 * @param listItemId the listItemId to set
	 */
	public void setListItemId(String listItemId) {
		this.listItemId = listItemId;
	}

	/**
	 * @return the programId
	 */
	public String getProgramId() {
		return programId;
	}

	/**
	 * @param programId the programId to set
	 */
	public void setProgramId(String programId) {
		this.programId = programId;
	}

	/**
	 * @return the hospitalId
	 */
	public String getHospitalId() {
		return hospitalId;
	}

	/**
	 * @param hospitalId the hospitalId to set
	 */
	public void setHospitalId(String hospitalId) {
		this.hospitalId = hospitalId;
	}

	/**
	 * @return the itemTypeId
	 */
	public Integer getItemTypeId() {
		return itemTypeId;
	}

	/**
	 * @param itemTypeId the itemTypeId to set
	 */
	public void setItemTypeId(Integer itemTypeId) {
		this.itemTypeId = itemTypeId;
	}

	/**
	 * @return the listName
	 */
	public String getListName() {
		return listName;
	}

	/**
	 * @param listName the listName to set
	 */
	public void setListName(String listName) {
		this.listName = listName;
	}

	/**
	 * @return the summaryText
	 */
	public String getSummaryText() {
		return summaryText;
	}

	/**
	 * @param summaryText the summaryText to set
	 */
	public void setSummaryText(String summaryText) {
		this.summaryText = summaryText;
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
	 * @return the thumbImageUrl
	 */
	public String getThumbImageUrl() {
		return thumbImageUrl;
	}

	/**
	 * @param thumbImageUrl the thumbImageUrl to set
	 */
	public void setThumbImageUrl(String thumbImageUrl) {
		this.thumbImageUrl = thumbImageUrl;
	}

	/**
	 * @return the detailImageUrl
	 */
	public String getDetailImageUrl() {
		return detailImageUrl;
	}
	
	/**
	 * @param detailImageUrl the detailImageUrl to set
	 */
	public void setDetailImageUrl(String detailImageUrl) {
		this.detailImageUrl = detailImageUrl;
	}
	
	/**
	 * @return the videoUrl
	 */
	public String getVideoUrl() {
		return videoUrl;
	}

	/**
	 * @param videoUrl the videoUrl to set
	 */
	public void setVideoUrl(String videoUrl) {
		this.videoUrl = videoUrl;
	}
	
	/**
	 * @return the actionPlanId
	 */
	public String getActionPlanId() {
		return actionPlanId;
	}

	/**
	 * @param actionPlanId the actionPlanId to set
	 */
	public void setActionPlanId(String actionPlanId) {
		this.actionPlanId = actionPlanId;
	}

	/**
	 * @return the orderNo
	 */
	public Integer getOrderNo() {
		return orderNo;
	}

	/**
	 * @param orderNo the orderNo to set
	 */
	public void setOrderNo(Integer orderNo) {
		this.orderNo = orderNo;
	}

	public String getExternalUrl() {
		return externalUrl;
	}

	public void setExternalUrl(String externalUrl) {
		this.externalUrl = externalUrl;
	}

	public Integer getCaregiverFlg() {
		return caregiverFlg;
	}

	public void setCaregiverFlg(Integer caregiverFlg) {
		this.caregiverFlg = caregiverFlg;
	}

	public Integer getFeaturedFlg() {
		return featuredFlg;
	}

	public void setFeaturedFlg(Integer featuredFlg) {
		this.featuredFlg = featuredFlg;
	}

	public String getHospitalName() {
		return hospitalName;
	}

	public void setHospitalName(String hospitalName) {
		this.hospitalName = hospitalName;
	}
}
