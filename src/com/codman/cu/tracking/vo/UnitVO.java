package com.codman.cu.tracking.vo;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.Comparator;
import java.util.Date;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: UnitVO<p/>
 * <b>Description: Data bean for Unit data</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Nov 17, 2010
 * updated: Feb. 13, 2012 - Added accountCountry and accountCity for Report
 ****************************************************************************/
public class UnitVO implements Serializable {

	private static final long serialVersionUID = -1251378830897509843L;
	private String unitId = null;
	private String softwareRevNo = null;
	private String hardwareRevNo = null;
	private String serialNo = null;
	private Integer statusId = Integer.valueOf(0);
	private String statusName = "";
	private Date deployedDate = null;
	private Date createDate = null;
	private String organizationId = null;
	private String commentsText = null;
	private String parentId = null; //used on the history report to group Units together with their historical data
	//JSTL helpers
	private String physicianId = null;
	private String physicianName = "";
	private String repId = null;
	private String repName = "";
	private String accountId = null;
	private String accountName = "";
	private String accountCity = null;
	private String accountCountry = null;
	//Added Feb. 14, 2012
	private String ifuArticleNo = null;
	private String ifuRevNo = null;
	private String programArticleNo = null;
	private String programRevNo = null;
	private String batteryType = null;
	private String batterySerNo = null;
	private String lotNo = null; 
	private String serviceRefNo = null;
	private Date serviceDate = null;
	private String modifyingUserId = null;
	private String modifyingUserName = "";
	private String productionCommentsText = null;
	//For new product types
	private ProdType productType = null;
	
	//this is for UnitHistoryReportVO
	private PhysicianVO phys = null;
	private Integer transactionType = null;
	
	/**
	 * Possible unit types
	 */
	public enum ProdType {
		MEDSTREAM, //The original CU type
		ICP
	}
	
	public UnitVO() {
	}

	public UnitVO(SMTServletRequest req) {
		unitId = req.getParameter("unitId");
		softwareRevNo = req.getParameter("softwareRevNo");
		hardwareRevNo = req.getParameter("hardwareRevNo");
		serialNo = req.getParameter("serialNo");
		statusId = Convert.formatInteger(req.getParameter("unitStatusId"));
		commentsText = req.getParameter("commentsText");
		parentId = StringUtil.checkVal(req.getParameter("unitParentId"), null);
		
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		organizationId = site.getOrganizationId();
		
		ifuArticleNo = req.getParameter("ifuArtNo");
		ifuRevNo = req.getParameter("ifuRevNo");
		programArticleNo = req.getParameter("progArtNo");
		programRevNo = req.getParameter("progRevNo");
		batteryType = req.getParameter("battPackType");
		batterySerNo = req.getParameter("battSerNo");
		lotNo = req.getParameter("lotNo");
		serviceRefNo = req.getParameter("servRefNo");
		serviceDate = Convert.formatDate(req.getParameter("servDt"));
		modifyingUserId = req.getParameter("modifyingUserId");
		productionCommentsText = req.getParameter("productionCommentsText");
		
		String prodType = StringUtil.checkVal(req.getParameter("prodCd"));
		if (!prodType.isEmpty())
			productType = ProdType.valueOf(prodType);
	}
	
	/**
	 * @param rs
	 */
	public UnitVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		unitId = db.getStringVal("unit_id", rs);
		softwareRevNo = db.getStringVal("software_rev_no", rs);
		hardwareRevNo = db.getStringVal("hardware_rev_no", rs);
		serialNo = db.getStringVal("serial_no_txt", rs);
		statusId = db.getIntegerVal("unit_status_id", rs);
		statusName = db.getStringVal("status_nm", rs);
		deployedDate = db.getDateVal("deployed_dt", rs);
		physicianId = db.getStringVal("phys_profile_id", rs);
		repId = db.getStringVal("rep_person_id", rs); //actually is rep's profile_id
		accountName = db.getStringVal("account_nm", rs);
		accountCity = db.getStringVal("city_nm", rs);
		accountCountry = db.getStringVal("country_cd", rs);
		commentsText = db.getStringVal("comments_txt", rs);
		organizationId = db.getStringVal("organization_id", rs);
		parentId = db.getStringVal("parent_id", rs);
		createDate = db.getDateVal("update_dt", rs);
		if (createDate == null) createDate = db.getDateVal("create_dt", rs);
		
		ifuArticleNo = db.getStringVal("ifu_art_no", rs);
		ifuRevNo = db.getStringVal("ifu_rev_no", rs);
		programArticleNo = db.getStringVal("prog_guide_art_no", rs);
		programRevNo = db.getStringVal("prog_guide_rev_no", rs);
		batteryType = db.getStringVal("battery_type", rs);
		batterySerNo = db.getStringVal("battery_serial_no", rs);
		lotNo = db.getStringVal("lot_number", rs);
		serviceRefNo = db.getStringVal("service_ref", rs);
		serviceDate = db.getDateVal("service_dt", rs);
		modifyingUserId = db.getStringVal("modifying_user_id", rs);
		productionCommentsText = db.getStringVal("production_comments_txt", rs);
		
		String prodType = StringUtil.checkVal(db.getStringVal("product_cd",rs));
		if (!prodType.isEmpty())
			productType = ProdType.valueOf(prodType);
		
		setPhys(new PhysicianVO(rs));
		db = null;
	}

	public String getSoftwareRevNo() {
		return softwareRevNo;
	}

	public void setSoftwareRevNo(String softwareRevNo) {
		this.softwareRevNo = softwareRevNo;
	}

	public String getSerialNo() {
		return serialNo;
	}

	public void setSerialNo(String serialNo) {
		this.serialNo = serialNo;
	}

	public String getUnitId() {
		return unitId;
	}

	public void setUnitId(String unitId) {
		this.unitId = unitId;
	}

	/**
	 * @return the unitStatusId
	 */
	public Integer getStatusId() {
		return statusId;
	}

	/**
	 * @param unitStatusId the unitStatusId to set
	 */
	public void setStatusId(Integer ss) {
		this.statusId = ss;
	}

	/**
	 * @return the deployedDate
	 */
	public Date getDeployedDate() {
		return deployedDate;
	}

	/**
	 * @param deployedDate the deployedDate to set
	 */
	public void setDeployedDate(Date deployedDate) {
		this.deployedDate = deployedDate;
	}

	public String getPhysicianId() {
		return physicianId;
	}

	public void setPhysicianId(String physicianId) {
		this.physicianId = physicianId;
	}

	public String getPhysicianName() {
		return physicianName;
	}

	public void setPhysicianName(String physicianName) {
		this.physicianName = physicianName;
	}

	public String getRepId() {
		return repId;
	}

	public void setRepId(String repId) {
		this.repId = repId;
	}

	public String getRepName() {
		return repName;
	}

	public void setRepName(String repName) {
		this.repName = repName;
	}

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public void setStatusName(String statusNm) {
		this.statusName = statusNm;
	}

	public String getStatusName() {
		return statusName;
	}

	public void setOrganizationId(String organizationId) {
		this.organizationId = organizationId;
	}

	public String getOrganizationId() {
		return organizationId;
	}

	public void setCommentsText(String commentsText) {
		this.commentsText = commentsText;
	}

	public String getCommentsText() {
		return commentsText;
	}

	public void setHardwareRevNo(String hardwareRevNo) {
		this.hardwareRevNo = hardwareRevNo;
	}

	public String getHardwareRevNo() {
		return hardwareRevNo;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getParentId() {
		return parentId;
	}

	public String getAccountCity() {
		return accountCity;
	}

	public void setAccountCity(String accountCity) {
		this.accountCity = accountCity;
	}

	public String getAccountCountry() {
		return accountCountry;
	}

	public void setAccountCountry(String accountCountry) {
		this.accountCountry = accountCountry;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public String getIfuArticleNo() {
		return ifuArticleNo;
	}

	public void setIfuArticleNo(String ifuArticleNo) {
		this.ifuArticleNo = ifuArticleNo;
	}

	public String getIfuRevNo() {
		return ifuRevNo;
	}

	public void setIfuRevNo(String ifuRevNo) {
		this.ifuRevNo = ifuRevNo;
	}

	public String getProgramArticleNo() {
		return programArticleNo;
	}

	public void setProgramArticleNo(String programArticleNo) {
		this.programArticleNo = programArticleNo;
	}

	public String getProgramRevNo() {
		return programRevNo;
	}

	public void setProgramRevNo(String programRevNo) {
		this.programRevNo = programRevNo;
	}

	public String getBatteryType() {
		return batteryType;
	}

	public void setBatteryType(String batteryType) {
		this.batteryType = batteryType;
	}

	public String getBatterySerNo() {
		return batterySerNo;
	}

	public void setBatterySerNo(String batterySerNo) {
		this.batterySerNo = batterySerNo;
	}

	public String getLotNo() {
		return lotNo;
	}

	public void setLotNo(String lotNo) {
		this.lotNo = lotNo;
	}

	public String getServiceRefNo() {
		return serviceRefNo;
	}

	public void setServiceRefNo(String serviceRefNo) {
		this.serviceRefNo = serviceRefNo;
	}

	public Date getServiceDate() {
		return serviceDate;
	}

	public void setServiceDate(Date serviceDate) {
		this.serviceDate = serviceDate;
	}
	
	public String getModifyingUserId(){
		return modifyingUserId;
	}
	
	public void setModifyingUserId(String modifyingUserId){
		this.modifyingUserId = modifyingUserId;
	}
	
	public String getModifyingUserName(){
		return modifyingUserName;
	}
	
	public void setModifyingUserName(String modifyingUserName){
		this.modifyingUserName = modifyingUserName;
	}

	public String getProductionCommentsText() {
		return productionCommentsText;
	}

	public void setProductionCommentsText(String productionCommentsText) {
		this.productionCommentsText = productionCommentsText;
	}

	public PhysicianVO getPhysician() {
		return phys;
	}

	public void setPhys(PhysicianVO phys) {
		this.phys = phys;
	}

	public Integer getTransactionType() {
		return transactionType;
	}

	public void setTransactionType(Integer transactionType) {
		this.transactionType = transactionType;
	}

	/**
	 * @return the productType
	 */
	public ProdType getProductType() {
		return productType;
	}

	/**
	 * @param productType the productType to set
	 */
	public void setProductType(ProdType productType) {
		this.productType = productType;
	}
	
	public String getProductCode(){
		if ( productType == null)
			return ProdType.MEDSTREAM.name();
		else
			return productType.name();
	}
}


class UnitComparator implements Comparator<UnitVO> {
	public static final long serialVersionUID = 1l;
	
	/**
	 * Compares using the last name and then first name and then state
	 */
	public int compare(UnitVO o1, UnitVO o2) {
		// Check the objects for null
		if (o1 == null && o2 == null) return 0;
		if (o1 == null) return -1;
		else if (o2 == null) return 1;
		return o1.getSerialNo().compareToIgnoreCase(o2.getSerialNo());
		
	}
}
