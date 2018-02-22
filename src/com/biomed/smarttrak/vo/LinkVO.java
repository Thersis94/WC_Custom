package com.biomed.smarttrak.vo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;

/****************************************************************************
 * <b>Title</b>: LinkVO.java<p/>
 * <b>Description: a POJO for holding row-level data used by the link checker script and it's report.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Apr 2, 2017
 ****************************************************************************/
public class LinkVO {
	private String url;
	private String section;
	private String linkId;
	private String objectId; //used for the associated section's id(company, product, update, etc.)
	private String html;
	private Date lastChecked;
	private int outcome;
	private String adminUrl;
	private String publicUrl;
	private String redirectUrl;
	private int reviewFlag;

	public LinkVO(String section, String id, String html) {
		this.setSection(section);
		this.setObjectId(id);
		this.html = html;
	}
	
	public LinkVO() {
		//no-arg constructor for simple instantiation
	}

	public static LinkVO makeForUrl(String section, String id, String url) {
		LinkVO vo = new LinkVO(section, id, null);
		vo.setUrl(url);
		return vo;
	}
	
	/**
	 * Generates a new LinkVO and sets relevant values via ResultSet
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public static LinkVO makeForUrl(ResultSet rs) throws SQLException{
		LinkVO vo = new LinkVO();
		vo.setLinkId(rs.getString("link_id"));
		vo.setObjectId(rs.getString("id"));
		vo.setSection(rs.getString("section"));
		vo.setUrl(rs.getString("url_txt"));
		vo.setLastChecked(rs.getDate("check_dt"));
		vo.setOutcomeNo(rs.getInt("status_no"));
		vo.setReviewFlag(rs.getInt("review_flg"));
		
		return vo;
	}

	public String getHtml() {
		return html;
	}

	public void setHtml(String html) {
		this.html = html;
	}

	public int getOutcome() {
		return this.outcome;
	}

	public void setOutcome(int i) {
		this.outcome = i;
		this.setLastChecked(Calendar.getInstance().getTime());
	}

	public void setOutcomeNo(int i) {
		this.outcome = i;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getSection() {
		return section;
	}

	public void setSection(String section) {
		this.section = section;
	}

	public String getObjectId() {
		return objectId;
	}

	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}

	public Date getLastChecked() {
		return lastChecked;
	}

	public void setLastChecked(Date lastChecked) {
		this.lastChecked = lastChecked;
	}

	public String getAdminUrl() {
		return adminUrl;
	}

	public void setAdminUrl(String adminUrl) {
		this.adminUrl = adminUrl;
	}

	public String getPublicUrl() {
		return publicUrl;
	}

	public void setPublicUrl(String publicUrl) {
		this.publicUrl = publicUrl;
	}

	/**
	 * @return the reviewFlag
	 */
	public int getReviewFlag() {
		return reviewFlag;
	}

	/**
	 * @param reviewFlag the reviewFlag to set
	 */
	public void setReviewFlag(int reviewFlag) {
		this.reviewFlag = reviewFlag;
	}

	/**
	 * @return the linkId
	 */
	public String getLinkId() {
		return linkId;
	}

	/**
	 * @param linkId the linkId to set
	 */
	public void setLinkId(String linkId) {
		this.linkId = linkId;
	}

	/**
	 * redirectUrl is used by the LinkChecker script - when a server response 301 or 302 and we need to follow it.
	 * @return
	 */
	public String getRedirectUrl() {
		return redirectUrl;
	}

	public void setRedirectUrl(String redirectUrl) {
		this.redirectUrl = redirectUrl;
	}
}