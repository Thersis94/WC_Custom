package com.depuy.sitebuilder.compliance;

// JDK 1.5.0
import java.io.Serializable;
import java.sql.ResultSet;

// SMT Base Libs 2.0
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

/****************************************************************************
 * <b>Title</b>:ComplianceVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Oct 23, 2007
 ****************************************************************************/
public class ComplianceVO implements Serializable {
	private static final long serialVersionUID = 1l;
	private String complianceId = null;
	private String companyName = null;
	private String firstName = null;
	private String lastName = null;
	private String titleName = null;
	private String cityName = null;
	private String stateCode = null;
	private String zipCode = null;
	private String matchCode = null;
	
	private Integer grantNumber = 0;
	private Integer consultingNumber = 0;
	private Integer researchNumber = 0;
	private Integer royaltyNumber = 0;
	
	private Double latitude = 0.0;
	private Double longitude = 0.0;
	
	public ComplianceVO() {
		
	}
	
	public String toString() {
		StringBuffer s = new StringBuffer();
		s.append(complianceId).append("|");
		s.append(companyName).append("|");
		s.append(lastName).append("|");
		s.append(firstName).append("|");
		s.append(titleName).append("|");
		s.append(cityName).append("|");
		s.append(stateCode).append("|");
		s.append(consultingNumber).append("|");
		s.append(grantNumber).append("|");
		s.append(researchNumber).append("|");
		s.append(royaltyNumber).append("|");
		return s.toString();
	}
	
	public ComplianceVO(String rowData) {
		setData(rowData);
	}
	
	public ComplianceVO(ResultSet rs) {
		setData(rs);
	}
	
	public ComplianceVO(SMTServletRequest req) {
		setData(req);
	}
	
	/**
	 * Assigns request data to variables
	 * @param req
	 */
	public void setData(SMTServletRequest req) {
		complianceId = req.getParameter("complianceId");
		companyName = req.getParameter("companyName");
		lastName = req.getParameter("physLastName");
		firstName = req.getParameter("firstName");
		cityName = req.getParameter("cityName");
		stateCode = req.getParameter("stateCode");
		titleName = req.getParameter("titleName");
		zipCode = req.getParameter("zipCode");
		royaltyNumber = Convert.formatInteger(req.getParameter("royalty"));
		researchNumber = Convert.formatInteger(req.getParameter("research"));
		grantNumber = Convert.formatInteger(req.getParameter("grant"));
		consultingNumber = Convert.formatInteger(req.getParameter("consulting"));
	}
	
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		complianceId = db.getStringVal("compliance_id", rs);
		companyName = db.getStringVal("company_nm", rs);
		lastName = db.getStringVal("last_nm", rs);
		firstName = db.getStringVal("first_nm", rs);
		cityName = db.getStringVal("city_nm", rs);
		stateCode = db.getStringVal("state_cd", rs);
		titleName = db.getStringVal("titleName", rs);
		zipCode = db.getStringVal("zip_cd", rs);
		latitude = db.getDoubleVal("latitude_no", rs);
		longitude = db.getDoubleVal("longitude_no", rs);
		matchCode = db.getStringVal("match_cd", rs);
		royaltyNumber = db.getIntegerVal("royalty_no", rs);
		researchNumber = db.getIntegerVal("research_no", rs);
		grantNumber = db.getIntegerVal("grant_no", rs);
		consultingNumber = db.getIntegerVal("consulting_no", rs);
	}
	
	public void setData(String row) {
		if (StringUtil.checkVal(row).length() == 0) return;
		
		String newRow = row.replaceAll("\"", "");
		newRow = newRow.replaceAll("$", "");
		complianceId = new UUIDGenerator().getUUID();
		String[] st = newRow.split("\t");
		for(int i=0; i < st.length; i++) {
			switch(i) {
				case 0: companyName = st[i]; break;
				case 1: lastName = st[i]; break;
				case 2: firstName = st[i]; break;
				//case 3: titleName = st[i]; break;
				case 3: cityName = st[i]; break;
				case 4: stateCode = st[i]; break;
				case 5: consultingNumber = Convert.formatInteger(st[i]); break;
				case 6: grantNumber = Convert.formatInteger(st[i]); break;
				case 7: researchNumber = Convert.formatInteger(st[i]); break;
				case 8: royaltyNumber = Convert.formatInteger(st[i]); break;
			}
		}
	}

	public String getComplianceId() {
		return complianceId;
	}

	public void setComplianceId(String complianceId) {
		this.complianceId = complianceId;
	}

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getTitleName() {
		return titleName;
	}

	public void setTitleName(String titleName) {
		this.titleName = titleName;
	}

	public String getCityName() {
		return cityName;
	}

	public void setCityName(String cityName) {
		this.cityName = cityName;
	}

	public String getStateCode() {
		return stateCode;
	}

	public void setStateCode(String stateCode) {
		this.stateCode = stateCode;
	}

	public String getZipCode() {
		return zipCode;
	}

	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
	}

	public String getMatchCode() {
		return matchCode;
	}

	public void setMatchCode(String matchCode) {
		this.matchCode = matchCode;
	}

	public Integer getGrantNumber() {
		return grantNumber;
	}

	public void setGrantNumber(Integer grantNumber) {
		this.grantNumber = grantNumber;
	}

	public Integer getConsultingNumber() {
		return consultingNumber;
	}

	public void setConsultingNumber(Integer consultingNumber) {
		this.consultingNumber = consultingNumber;
	}

	public Integer getResearchNumber() {
		return researchNumber;
	}

	public void setResearchNumber(Integer researchNumber) {
		this.researchNumber = researchNumber;
	}

	public Integer getRoyaltyNumber() {
		return royaltyNumber;
	}

	public void setRoyaltyNumber(Integer royaltyNumber) {
		this.royaltyNumber = royaltyNumber;
	}

	public Double getLatitude() {
		return latitude;
	}

	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}

	public Double getLongitude() {
		return longitude;
	}

	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}
	
	/**
	 * returns the concatenated first name, last name and title
	 * @return
	 */
	public String getName() {
		StringBuffer sb = new StringBuffer();
		if (StringUtil.checkVal(firstName).length() > 0)
			sb.append(firstName).append("&nbsp;");
		
		if (StringUtil.checkVal(lastName).length() > 0)
			sb.append(lastName).append("&nbsp;");
		
		if (StringUtil.checkVal(titleName).length() > 0)
			sb.append(titleName).append(",&nbsp;");
		
		return sb.toString();
	}
}
