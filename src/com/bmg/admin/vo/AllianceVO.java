package com.bmg.admin.vo;

import java.util.Date;

import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: AllianceVO.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> DBProcessor enabled VO that stores information regarding
 * alliances a company can enter into.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Jan 16, 2017<p/>
 * <b>Changes: </b>
 ****************************************************************************/

@Table(name="BIOMEDGPS_COMPANY_ALLIANCE_XR")
public class AllianceVO {
	private String allianceId;
	private String companyId;
	private String allianceTypeId;
	private String allianceTypeName;
	private String allyId;
	private String allyName;
	private String referenceText;
	private int orderNo;
	
	public AllianceVO(){}
	
	public AllianceVO(SMTServletRequest req) {
		setData(req);
	}
	
	private void setData(SMTServletRequest req) {
		allianceId = req.getParameter("allianceId");
		companyId = req.getParameter("companyId");
		allianceTypeId = req.getParameter("allianceTypeId");
		allianceTypeName = req.getParameter("allianceTypeName");
		allyId = req.getParameter("allyId");
		allyName = req.getParameter("allyName");
		referenceText = req.getParameter("referenceText");
		orderNo = Convert.formatInteger(req.getParameter("orderNo"));
	}

	@Column(name="company_alliance_xr_id", isPrimaryKey=true)
	public String getAllianceId() {
		return allianceId;
	}
	public void setAllianceId(String allianceId) {
		this.allianceId = allianceId;
	}
	@Column(name="company_id")
	public String getCompanyId() {
		return companyId;
	}
	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}
	@Column(name="alliance_type_id")
	public String getAllianceTypeId() {
		return allianceTypeId;
	}
	public void setAllianceTypeId(String allianceTypeId) {
		this.allianceTypeId = allianceTypeId;
	}
	@Column(name="type_nm", isReadOnly=true)
	public String getAllianceTypeName() {
		return allianceTypeName;
	}
	public void setAllianceTypeName(String allianceTypeName) {
		this.allianceTypeName = allianceTypeName;
	}
	@Column(name="rel_company_id")
	public String getAllyId() {
		return allyId;
	}
	public void setAllyId(String allyId) {
		this.allyId = allyId;
	}
	@Column(name="company_nm", isReadOnly=true)
	public String getAllyName() {
		return allyName;
	}
	public void setAllyName(String allyName) {
		this.allyName = allyName;
	}
	@Column(name="reference_txt")
	public String getReferenceText() {
		return referenceText;
	}
	public void setReferenceText(String referenceText) {
		this.referenceText = referenceText;
	}
	@Column(name="order_no")
	public int getOrderNo() {
		return orderNo;
	}
	public void setOrderNo(int orderNo) {
		this.orderNo = orderNo;
	}
	// These functions exists only to give the DBProcessor a hook to autogenerate dates on
	@Column(name="UPDATE_DT", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDate() {return null;}
	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {return null;}

}
