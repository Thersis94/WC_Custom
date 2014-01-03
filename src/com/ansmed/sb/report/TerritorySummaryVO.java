package com.ansmed.sb.report;

import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSiteBuilderVO;

/*****************************************************************************
 <p><b>Title</b>: TerritorySummaryVO.java</p>
 <p>Description: <b/></p>
 <p>Copyright: Copyright (c) 2000 - 2009 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author David Bargerhuff
 @version 1.0
 @since May 07, 2009
 Last Updated:
 ***************************************************************************/

public class TerritorySummaryVO extends AbstractSiteBuilderVO {
	private static final long serialVersionUID = 1l;
	private String repFirstNm = null;
	private String repLastNm = null;
	private String surgeonFirstNm = null;
	private String surgeonLastNm = null;
	private String titleNm = null;
	private String practiceNm = null;
	private String addressTxt = null;
	private String address2Txt = null;
	private String address3Txt = null;
	private String cityNm = null;
	private String stateCd = null;
	private String zipCd = null;
	private String phoneNo = null;
	private String regionNm = null;
	private String surgeonEmailAddress = null;
	
	/**
	 * 
	 */
	public TerritorySummaryVO() {
	}
	
	/**
	 * Initializes the VO to the params provided in the row object
	 * @param rs
	 */
	public TerritorySummaryVO(ResultSet rs) {
		super();
		setData(rs);
	}
	
	/**
	 * Initializes the VO to the params provided in the request object
	 * @param rs
	 */
	public TerritorySummaryVO(SMTServletRequest req) {
		super();
		setData(req);
	}
	
	/**
	 * Sets the VO to the params provided in the row object
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		StringEncoder se = new StringEncoder();
		DBUtil db = new DBUtil();
		repFirstNm = se.decodeValue(db.getStringVal("rep_first_nm", rs));
		repLastNm = se.decodeValue(db.getStringVal("rep_last_nm", rs));
		surgeonFirstNm = se.decodeValue(db.getStringVal("phys_first_nm", rs));
		surgeonLastNm = se.decodeValue(db.getStringVal("phys_last_nm", rs));
		titleNm = se.decodeValue(db.getStringVal("title_nm", rs));
		practiceNm = se.decodeValue(db.getStringVal("clinic_nm", rs));
		addressTxt = se.decodeValue(db.getStringVal("address_txt", rs));
		address2Txt = se.decodeValue(db.getStringVal("address2_txt", rs));
		address3Txt = se.decodeValue(db.getStringVal("address3_txt", rs));
		cityNm = se.decodeValue(db.getStringVal("city_nm", rs));
		stateCd = db.getStringVal("state_cd", rs);
		zipCd = db.getStringVal("zip_cd", rs);
		phoneNo = db.getStringVal("phone_no", rs);
		regionNm = se.decodeValue(db.getStringVal("region_nm", rs));
		surgeonEmailAddress = se.decodeValue(db.getStringVal("email_address_txt", rs));

	}
	
	/**
	 * Sets the VO to the params provided in the request object
	 * @param req
	 */
	public void setData(SMTServletRequest req) {
		StringEncoder se = new StringEncoder();
		repFirstNm = se.decodeValue(req.getParameter("repFirstNm"));
		repLastNm = se.decodeValue(req.getParameter("repLastNm"));
		surgeonFirstNm = se.decodeValue(req.getParameter("physFirstNm"));
		surgeonLastNm = se.decodeValue(req.getParameter("physLastNm"));
		titleNm = se.decodeValue(req.getParameter("title_nm"));
		practiceNm = se.decodeValue(req.getParameter("clinic_nm"));
		addressTxt = se.decodeValue(req.getParameter("address_txt"));
		address2Txt = se.decodeValue(req.getParameter("address2_txt"));
		address3Txt = se.decodeValue(req.getParameter("address3_txt"));
		cityNm = se.decodeValue(req.getParameter("city_nm"));
		stateCd = req.getParameter("state_cd");
		zipCd = req.getParameter("zip_cd");
		phoneNo = req.getParameter("phone_no");
		regionNm = se.decodeValue(req.getParameter("region_nm"));
		surgeonEmailAddress = se.decodeValue(req.getParameter("surgeonEmailAddress"));

	}
	

	/**
	 * @return the repFirstNm
	 */
	public String getRepFirstNm() {
		return repFirstNm;
	}

	/**
	 * @param repFirstNm the repFirstNm to set
	 */
	public void setRepFirstNm(String repFirstNm) {
		this.repFirstNm = repFirstNm;
	}

	/**
	 * @return the repLastNm
	 */
	public String getRepLastNm() {
		return repLastNm;
	}

	/**
	 * @param repLastNm the repLastNm to set
	 */
	public void setRepLastNm(String repLastNm) {
		this.repLastNm = repLastNm;
	}

	/**
	 * @return the surgeonFirstNm
	 */
	public String getSurgeonFirstNm() {
		return surgeonFirstNm;
	}

	/**
	 * @param surgeonFirstNm the surgeonFirstNm to set
	 */
	public void setSurgeonFirstNm(String surgeonFirstNm) {
		this.surgeonFirstNm = surgeonFirstNm;
	}

	/**
	 * @return the surgeonLastNm
	 */
	public String getSurgeonLastNm() {
		return surgeonLastNm;
	}

	/**
	 * @param surgeonLastNm the surgeonLastNm to set
	 */
	public void setSurgeonLastNm(String surgeonLastNm) {
		this.surgeonLastNm = surgeonLastNm;
	}

	/**
	 * @return the titleNm
	 */
	public String getTitleNm() {
		return titleNm;
	}

	/**
	 * @param titleNm the titleNm to set
	 */
	public void setTitleNm(String titleNm) {
		this.titleNm = titleNm;
	}

	/**
	 * @return the practiceNm
	 */
	public String getPracticeNm() {
		return practiceNm;
	}

	/**
	 * @param practiceNm the practiceNm to set
	 */
	public void setPracticeNm(String practiceNm) {
		this.practiceNm = practiceNm;
	}

	/**
	 * @return the addressTxt
	 */
	public String getAddressTxt() {
		return addressTxt;
	}

	/**
	 * @param addressTxt the addressTxt to set
	 */
	public void setAddressTxt(String addressTxt) {
		this.addressTxt = addressTxt;
	}

	/**
	 * @return the address2Txt
	 */
	public String getAddress2Txt() {
		return address2Txt;
	}

	/**
	 * @param address2Txt the address2Txt to set
	 */
	public void setAddress2Txt(String address2Txt) {
		this.address2Txt = address2Txt;
	}

	/**
	 * @return the address3Txt
	 */
	public String getAddress3Txt() {
		return address3Txt;
	}

	/**
	 * @param address3Txt the address3Txt to set
	 */
	public void setAddress3Txt(String address3Txt) {
		this.address3Txt = address3Txt;
	}
	
	/**
	 * @return the cityNm
	 */
	public String getCityNm() {
		return cityNm;
	}

	/**
	 * @param cityNm the cityNm to set
	 */
	public void setCityNm(String cityNm) {
		this.cityNm = cityNm;
	}

	/**
	 * @return the stateCd
	 */
	public String getStateCd() {
		return stateCd;
	}

	/**
	 * @param stateCd the stateCd to set
	 */
	public void setStateCd(String stateCd) {
		this.stateCd = stateCd;
	}

	/**
	 * @return the zipCd
	 */
	public String getZipCd() {
		return zipCd;
	}

	/**
	 * @param zipCd the zipCd to set
	 */
	public void setZipCd(String zipCd) {
		this.zipCd = zipCd;
	}

	/**
	 * @return the phoneNo
	 */
	public String getPhoneNo() {
		if (StringUtil.checkVal(phoneNo).length() > 0) {
			PhoneNumberFormat pnf = new PhoneNumberFormat(phoneNo, PhoneNumberFormat.DASH_FORMATTING);
			return pnf.getFormattedNumber();
		} else return phoneNo;
	}

	/**
	 * @param phoneNo the phoneNo to set
	 */
	public void setPhoneNo(String phoneNo) {
		this.phoneNo = phoneNo;
	}

	/**
	 * @return the regionNm
	 */
	public String getRegionNm() {
		return regionNm;
	}

	/**
	 * @param regionNm the regionNm to set
	 */
	public void setRegionNm(String regionNm) {
		this.regionNm = regionNm;
	}

	/**
	 * @return the surgeonEmailAddress
	 */
	public String getSurgeonEmailAddress() {
		return surgeonEmailAddress;
	}

	/**
	 * @param surgeonEmailAddress the surgeonEmailAddress to set
	 */
	public void setSurgeonEmailAddress(String surgeonEmailAddress) {
		this.surgeonEmailAddress = surgeonEmailAddress;
	}
		
}
