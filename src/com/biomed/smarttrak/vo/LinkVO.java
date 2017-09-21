package com.biomed.smarttrak.vo;

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
	private String objectId;
	private String html;
	private Date lastChecked;
	private int outcome;
	private String adminUrl;
	private String publicUrl;
	private int reviewFlag;

	public LinkVO(String section, String id, String html) {
		this.setSection(section);
		this.setObjectId(id);
		this.html = html;
	}

	public static LinkVO makeForUrl(String section, String id, String url) {
		LinkVO vo = new LinkVO(section, id, null);
		vo.setUrl(url);
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
}