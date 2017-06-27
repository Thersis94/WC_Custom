package com.biomed.smarttrak.admin.vo;

import java.io.Serializable;
import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: UpdateTitleVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> used to store the data needed for biomed update title creation.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Jun 22, 2017
 * @updates:
 ****************************************************************************/
public class UpdateTitleVO implements Serializable {

	private static final long serialVersionUID = 7823314539047455764L;
	
	private String mainId;
	private String fullNm;
	private String shortNm;
	private String mainUrl;
	private String associatedCompanyShortNm;
	private String associatedCompanyId;
	private String associatedCompanyUrl;
	

	public UpdateTitleVO() {
		super();
	}
	public UpdateTitleVO(ResultSet rs) {
		this();
		setData(rs);
	}

	/**
	 * @param rs
	 */
	private void setData(ResultSet rs) {
		DBUtil util = new DBUtil();
		setMainId(util.getStringVal("MAIN_ID", rs));
		setFullNm(util.getStringVal("FULL_NM", rs));
		setShortNm(util.getStringVal("SHORT_NM", rs));
		setAssociatedCompanyShortNm(util.getStringVal("COMPANY_NM", rs));
		setAssociatedCompanyId(util.getStringVal("COMPANY_ID", rs));
	}
	/**
	 * @return the mainId
	 */
	public String getMainId() {
		return mainId;
	}
	/**
	 * @param mainId the mainId to set
	 */
	public void setMainId(String mainId) {
		this.mainId = mainId;
	}
	/**
	 * @return the fullNm
	 */
	public String getFullNm() {
		return fullNm;
	}
	/**
	 * @param fullNm the fullNm to set
	 */
	public void setFullNm(String fullNm) {
		this.fullNm = fullNm;
	}
	/**
	 * @return the shortNm
	 */
	public String getShortNm() {
		return shortNm;
	}
	/**
	 * @param shortNm the shortNm to set
	 */
	public void setShortNm(String shortNm) {
		this.shortNm = shortNm;
	}
	/**
	 * @return the associatedCompanyShortNm
	 */
	public String getAssociatedCompanyShortNm() {
		return associatedCompanyShortNm;
	}
	/**
	 * @param associatedCompanyShortNm the associatedCompanyShortNm to set
	 */
	public void setAssociatedCompanyShortNm(String associatedCompanyShortNm) {
		this.associatedCompanyShortNm = associatedCompanyShortNm;
	}
	/**
	 * @return the associatedCompanyId
	 */
	public String getAssociatedCompanyId() {
		return associatedCompanyId;
	}
	/**
	 * @param associatedCompanyId the associatedCompanyId to set
	 */
	public void setAssociatedCompanyId(String associatedCompanyId) {
		this.associatedCompanyId = associatedCompanyId;
	}
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString(){
		return StringUtil.getToString(this);
	}
	/**
	 * @return the mainUrl
	 */
	public String getMainUrl() {
		return mainUrl;
	}
	/**
	 * @param mainUrl the mainUrl to set
	 */
	public void setMainUrl(String mainUrl) {
		this.mainUrl = mainUrl;
	}
	/**
	 * @return the associatedCompanyUrk
	 */
	public String getAssociatedCompanyUrl() {
		return associatedCompanyUrl;
	}
	/**
	 * @param associatedCompanyUrk the associatedCompanyUrk to set
	 */
	public void setAssociatedCompanyUrl(String associatedCompanyUrk) {
		this.associatedCompanyUrl = associatedCompanyUrk;
	}
}
