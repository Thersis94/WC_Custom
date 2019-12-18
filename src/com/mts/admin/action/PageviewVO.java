package com.mts.admin.action;

import java.io.Serializable;
import java.util.Date;

import com.mts.admin.action.PageviewReportAction.Publication;
import com.siliconmtn.db.orm.Column;

/****************************************************************************
 * <p><b>Title:</b> PageviewVO.java</p>
 * <p><b>Description:</b> Simple POJO to hold the report data, which gets serialized to Gson.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Nov 4, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class PageviewVO implements Serializable {

	private static final long serialVersionUID = -8040671191655945598L;
	String pkId;
	String page;
	Publication pub;
	String pubNm;
	String issue;
	String documentPath; 
	String item;
	String url;
	String qs;
	Date dt;

	public PageviewVO() {
		super();
	}
	
	/***********************************************************
	 * NOTE: getters & setters here are for DBProcessor to populate the query results.
	 * Not all variables are represented.  Default access modifiers expose the rest to the action.
	 ***********************************************************/

	@Column(name="pageview_user_id", isPrimaryKey=true)
	public String getPageviewId() {
		return pkId;
	}
	@Column(name="page_display_nm")
	public String getPage() {
		return page;
	}
	@Column(name="request_uri_txt")
	public String getUrl() {
		return url;
	}
	@Column(name="query_str_txt")
	public String getQs() {
		return qs;
	}
	@Column(name="visit_dt")
	public Date getDt() {
		return dt;
	}

	public void setPageviewId(String pkId) {
		this.pkId = pkId;
	}
	public void setPage(String page) {
		this.page = page;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public void setQs(String qs) {
		this.qs = qs;
	}
	public void setDt(Date dt) {
		this.dt = dt;
	}
}
