package com.arvadachamber.action;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.Date;

import com.siliconmtn.db.DBUtil;

/****************************************************************************
 * <b>Title</b>: HotDealVO.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Mar 22, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class HotDealVO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// Member Variables
	private Integer hotDealId = null;
	private Integer memberId = null;
	private String desc = null;
	private Date startDate = null;
	private Date endDate = null;
	private String hotDealUrl = null;
	
	/**
	 * 
	 */
	public HotDealVO() {
		
	}
	
	/**
	 * 
	 * @param hotDealId
	 * @param memberId
	 * @param desc
	 */
	public HotDealVO(Integer hotDealId, Integer memberId, String desc) {
		this.hotDealId = hotDealId;
		this.memberId = memberId;
		this.desc = desc;
	}
	
	/**
	 * 
	 * @param rs
	 */
	public HotDealVO(ResultSet rs) {
		this.assignData(rs);
	}
	
	/**
	 * Assigns the DB data o the params
	 * @param rs
	 */
	public void assignData(ResultSet rs) {
		DBUtil db = new DBUtil();
		memberId = db.getIntegerVal("member_id", rs);
		hotDealId = db.getIntegerVal("hot_deal_id", rs);
		desc = db.getStringVal("hot_deal_desc", rs);
		startDate = db.getDateVal("start_dt", rs);
		endDate = db.getDateVal("end_dt", rs);
	}

	/**
	 * @return the hotDealId
	 */
	public Integer getHotDealId() {
		return hotDealId;
	}

	/**
	 * @param hotDealId the hotDealId to set
	 */
	public void setHotDealId(Integer hotDealId) {
		this.hotDealId = hotDealId;
	}

	/**
	 * @return the memberId
	 */
	public Integer getMemberId() {
		return memberId;
	}

	/**
	 * @param memberId the memberId to set
	 */
	public void setMemberId(Integer memberId) {
		this.memberId = memberId;
	}

	/**
	 * @return the desc
	 */
	public String getDesc() {
		return desc;
	}

	/**
	 * @param desc the desc to set
	 */
	public void setDesc(String desc) {
		this.desc = desc;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public String getHotDealUrl() {
		return hotDealUrl;
	}

	public void setHotDealUrl(String hotDealUrl) {
		this.hotDealUrl = hotDealUrl;
	}

}
