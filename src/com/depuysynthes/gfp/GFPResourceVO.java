package com.depuysynthes.gfp;

import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: GFPResourceVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Contains all information for a GFP resource that
 * corresponds to a media bin document.
 * <b>Copyright:</b> Copyright (c) 2015
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Eric Damschroder
 * @version 1.0
 * @since July 6, 2015
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/

public class GFPResourceVO {
	private String resourceId;
	private String parentId;
	private String name;
	private String desc;
	private String shortDesc;
	private String mediabinName;
	private String mediabinId;
	private String category;
	private String CategoryId;
	private int activeFlg;
	private Date completeDate;
	private int orderNo;
	
	public GFPResourceVO() {
	}
	
	public GFPResourceVO(SMTServletRequest req) {
		assignData(req);
	}
	
	public void assignData(SMTServletRequest req) {
		setResourceId(req.getParameter("resourceId"));
		System.out.println(req.getParameter("parentId"));
		setParentId(req.getParameter("parentId"));
		setCategoryId(req.getParameter("categoryId"));
		setName(req.getParameter("resourceName"));
		setDesc(req.getParameter("resourceDesc"));
		setShortDesc(req.getParameter("shortDesc"));
		setMediabinId(req.getParameter("mediabinId"));
		setMediabinName(req.getParameter("mediabinNm"));
		setCategory(req.getParameter("category"));
		setCategoryId(req.getParameter("categoryId"));
		setOrderNo(Convert.formatInteger(req.getParameter("orderNo")));
		setActiveFlg(Convert.formatInteger(req.getParameter("activeFlg")));
	}
	
	public GFPResourceVO(ResultSet rs) {
		assignData(rs);
	}
	
	public void assignData(ResultSet rs) {
		DBUtil db = new DBUtil();
		setResourceId(db.getStringVal("RESOURCE_ID", rs));
		setParentId(db.getStringVal("WORKSHOP_ID", rs));
		if (resourceId == null) setParentId(db.getStringVal("PROGRAM_ID", rs));
		setName(db.getStringVal("RESOURCE_NM", rs));
		setDesc(db.getStringVal("RESOURCE_DESC", rs));
		setShortDesc(db.getStringVal("SHORT_DESC", rs));
		setMediabinId(db.getStringVal("DPY_SYN_MEDIABIN_ID", rs));
		setMediabinName(db.getStringVal("FILE_NM", rs));
		setCategory(db.getStringVal("CATEGORY_NM", rs));
		setCategoryId(db.getStringVal("CATEGORY_ID", rs));
		setActiveFlg(db.getIntegerVal("ACTIVE_FLG", rs));
		setOrderNo(db.getIntegerVal("ORDER_NO", rs));
		setCompleteDate(db.getDateVal("COMPLETE_DT", rs));
		db = null;
	}

	public String getResourceId() {
		return resourceId;
	}

	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getShortDesc() {
		return shortDesc;
	}

	public void setShortDesc(String shortDesc) {
		this.shortDesc = shortDesc;
	}

	public String getMediabinName() {
		return mediabinName;
	}

	public void setMediabinName(String mediabinName) {
		this.mediabinName = mediabinName;
	}

	public String getMediabinId() {
		return mediabinId;
	}

	public void setMediabinId(String mediabinId) {
		this.mediabinId = mediabinId;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getCategoryId() {
		return CategoryId;
	}

	public void setCategoryId(String categoryId) {
		CategoryId = categoryId;
	}

	public int getActiveFlg() {
		return activeFlg;
	}

	public void setActiveFlg(int activeFlg) {
		this.activeFlg = activeFlg;
	}

	public Date getCompleteDate() {
		return completeDate;
	}

	public void setCompleteDate(Date completeDate) {
		this.completeDate = completeDate;
	}
	
	public boolean isComplete() {
		if (completeDate != null) return true;
		return false;
	}

	public int getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}

}
