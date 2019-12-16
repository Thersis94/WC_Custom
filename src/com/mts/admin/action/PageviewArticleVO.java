package com.mts.admin.action;

import com.siliconmtn.db.orm.Column;

/****************************************************************************
 * <p><b>Title:</b> PageviewArticleVO.java</p>
 * <p><b>Description:</b> Lookup VO used for marrying pageview data to MTS articles on the site.  (/qs/ paths)</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Nov 4, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class PageviewArticleVO {

	private String publicationId;
	private String publicationNm;
	private String issueId;
	private String issueNm;
	private String documentId;
	private String documentPath;
	private String documentNm;
	private int pageviews;

	public PageviewArticleVO() {
		super();
	}
	public static PageviewArticleVO from(PageviewVO view) {
		PageviewArticleVO vo = new PageviewArticleVO();
		vo.setDocumentPath(view.documentPath);
		return vo;
	}


	@Column(name="publication_id")
	public String getPublicationId() {
		return publicationId;
	}
	@Column(name="publication_nm")
	public String getPublicationNm() {
		return publicationNm;
	}
	@Column(name="issue_id")
	public String getIssueId() {
		return issueId;
	}
	@Column(name="issue_nm")
	public String getIssueNm() {
		return issueNm;
	}
	@Column(name="action_id", isPrimaryKey=true)
	public String getDocumentId() {
		return documentId;
	}
	@Column(name="direct_access_pth")
	public String getDocumentPath() {
		return documentPath;
	}
	@Column(name="action_nm")
	public String getDocumentNm() {
		return documentNm;
	}
	@Column(name="pageviews")
	public int getPageviews() {
		return pageviews;
	}


	public void setPublicationId(String publicationId) {
		this.publicationId = publicationId;
	}
	public void setPublicationNm(String publicationNm) {
		this.publicationNm = publicationNm;
	}
	public void setIssueId(String issueId) {
		this.issueId = issueId;
	}
	public void setIssueNm(String issueNm) {
		this.issueNm = issueNm;
	}
	public void setDocumentId(String documentId) {
		this.documentId = documentId;
	}
	public void setDocumentPath(String path) {
		this.documentPath = path;
	}
	public void setDocumentNm(String documentNm) {
		this.documentNm = documentNm;
	}
	public void setPageviews(int pageviews) {
		this.pageviews = pageviews;
	}
}
