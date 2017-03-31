package com.biomed.smarttrak.fd;

import java.io.Serializable;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;

import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: FinancialDashFootnoteVO.java<p/>
 * <b>Description: Bean for financial dashboard footnotes.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Mar 22, 2017
 ****************************************************************************/

@Table(name="biomedgps_fd_revenue_footnote")
public class FinancialDashFootnoteVO implements Serializable {

	private static final long serialVersionUID = -8861767513761326537L;
	private String footnoteId;
	private String regionCd;
	private String sectionId;
	private String companyId;
	private String footnoteTxt;
	private Date expirationDt;
	private Date createDt;
	private Date updateDt;
	
	public FinancialDashFootnoteVO() {
		super();
	}

	/**
	 * @param req
	 */
	public FinancialDashFootnoteVO(ActionRequest req) {
		this();
		setData(req);
	}
	
	/**
	 * Sets data from the ActionRequest
	 * 
	 * @param req
	 */
	public void setData(ActionRequest req) {
		setFootnoteId(req.getParameter("footnoteId"));
		setRegionCd(req.getParameter("regionCd"));
		setSectionId(req.getParameter("sectionId"));
		setCompanyId(req.getParameter("companyId"));
		setFootnoteTxt(req.getParameter("footnoteTxt"));
		setExpirationDt(Convert.parseDateUnknownPattern(req.getParameter("expirationDt")));
	}

	/**
	 * @return the footnoteId
	 */
	@Column(name="footnote_id", isPrimaryKey=true)
	public String getFootnoteId() {
		return footnoteId;
	}

	/**
	 * @return the regionCd
	 */
	@Column(name="region_cd")
	public String getRegionCd() {
		return regionCd;
	}

	/**
	 * @return the sectionId
	 */
	@Column(name="section_id")
	public String getSectionId() {
		return sectionId;
	}

	/**
	 * @return the companyId
	 */
	@Column(name="company_id")
	public String getCompanyId() {
		return companyId;
	}

	/**
	 * @return the footnoteTxt
	 */
	@Column(name="footnote_txt")
	public String getFootnoteTxt() {
		return footnoteTxt;
	}

	/**
	 * @return the expirationDt
	 */
	@Column(name="expiration_dt")
	public Date getExpirationDt() {
		return expirationDt;
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
	 * @param footnoteId the footnoteId to set
	 */
	public void setFootnoteId(String footnoteId) {
		this.footnoteId = footnoteId;
	}

	/**
	 * @param regionCd the regionCd to set
	 */
	public void setRegionCd(String regionCd) {
		this.regionCd = regionCd;
	}

	/**
	 * @param sectionId the sectionId to set
	 */
	public void setSectionId(String sectionId) {
		this.sectionId = sectionId;
	}

	/**
	 * @param companyId the companyId to set
	 */
	public void setCompanyId(String companyId) {
		if (StringUtil.isEmpty(companyId)) {
			companyId = null;
		}
		
		this.companyId = companyId;
	}

	/**
	 * @param footnoteTxt the footnoteTxt to set
	 */
	public void setFootnoteTxt(String footnoteTxt) {
		this.footnoteTxt = footnoteTxt;
	}

	/**
	 * @param expirationDt the expirationDt to set
	 */
	public void setExpirationDt(Date expirationDt) {
		this.expirationDt = expirationDt;
	}

	/**
	 * @param createDt the createDt to set
	 */
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}

	/**
	 * @param updateDt the updateDt to set
	 */
	public void setUpdateDt(Date updateDt) {
		this.updateDt = updateDt;
	}
	
}
