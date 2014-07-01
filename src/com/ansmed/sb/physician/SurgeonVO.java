package com.ansmed.sb.physician;

// JDK 1.5.0
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

// SMT Base Libs
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSiteBuilderVO;

/****************************************************************************
 * <b>Title</b>: SurgeonVO.java<p/>
 * <b>Description: </b> Main value object.  Acts as the container for the 
 * physician data, clinic locations and phone numbers.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Apr 26, 2007
 ****************************************************************************/
public class SurgeonVO extends AbstractSiteBuilderVO {
	private static final long serialVersionUID = 1l;
	
	private String actionName =  null;
	private String headerText =  null;
	private String actionId =  null;	
	private String surgeonId = null;
	private String profileId = null;
	private String surgeonTypeId = null;
	private String surgeonTypeName = null;
	private Double distance = null;
	private String firstName = null;
	private String middleName = null;
	private String lastName = null;
	private String suffix = null;
	private String title = null;
	private String emailAddress = null;
	private String website = null;
	private String language = null;
	private String localeText = null;
	private Integer specialtyId = null;
	private String specialtyName = null;
	private Integer statusId = Integer.valueOf(0);
	private Integer allowMail = Integer.valueOf(0);
	private Integer spanishFlag = Integer.valueOf(0);
	private Integer boardCertifiedFlag = Integer.valueOf(0);
	private Integer fellowshipFlag = Integer.valueOf(0);
	private String statusName = null;
	private String salesRepId = null;
	private String salesRepName = null;
	private String salesRegionName = null;
	private String spouseName = null;
	private String childrenName = null;
	private String cellPhone = null;
	private String pager = null;
	private Integer rank = null;
	private String scsStartDate = null;
	private Integer productApprovalFlag = Integer.valueOf(0);
	private Integer productGroupNumber = Integer.valueOf(0);
	private String licenseNumber = null;
	private String nationalProviderIdentifier = null;
	private Map<String,String> clinicDays = null;
	private Map<String,String> procedureDays = null;
	private List<ClinicVO> clinics = null;
	private List<StaffVO> staff = null;
	private List<DocumentVO> documents = null;
	
	/**
	 * 
	 */
	public SurgeonVO() {
		clinics = new ArrayList<ClinicVO>();
		staff = new ArrayList<StaffVO>();
		clinicDays = new HashMap<String,String>();
		procedureDays = new HashMap<String,String>();
	}
	
	public SurgeonVO(ResultSet rs) {
		this();
		setData(rs);
	}
	
	public SurgeonVO(SMTServletRequest req) {
		this();
		setData(req);
	}

	
	/**
	 * Takes the request object params and parses them into the appropriate vars
	 * @param req
	 */
	public void setData(SMTServletRequest req) {
		StringEncoder se = new StringEncoder();
		surgeonId = req.getParameter("surgeonId");
		surgeonTypeId = req.getParameter("surgeonTypeId");
		profileId = req.getParameter("profileId");
		firstName = se.decodeValue(req.getParameter("firstName"));
		middleName = se.decodeValue(req.getParameter("middleName"));
		lastName = se.decodeValue(req.getParameter("lastName"));
		suffix = req.getParameter("suffix");
		title = req.getParameter("title");
		emailAddress = req.getParameter("emailAddress");
		website = req.getParameter("website");
		language = req.getParameter("language");
		localeText = req.getParameter("localeText");
		specialtyName = req.getParameter("specialtyName");
		salesRepId = req.getParameter("salesRepId");
		spouseName = se.decodeValue(req.getParameter("spouseName"));
		childrenName = se.decodeValue(req.getParameter("childrenName"));
		cellPhone = req.getParameter("cellPhone");
		pager = req.getParameter("pager");
		rank = Convert.formatInteger(req.getParameter("rank"));
		statusId = Convert.formatInteger(req.getParameter("statusId"));
		specialtyId = Convert.formatInteger(req.getParameter("specialtyId"));
		allowMail = Convert.formatInteger(req.getParameter("allowMail"));
		spanishFlag = Convert.formatInteger(req.getParameter("spanishFlag"));
		boardCertifiedFlag = Convert.formatInteger(req.getParameter("boardCertifiedFlag"));
		fellowshipFlag = Convert.formatInteger(req.getParameter("fellowshipFlag"));
		rank = Convert.formatInteger(req.getParameter("rank"));
		scsStartDate = req.getParameter("scsStartDate");
		clinicDays = this.parseVals(req.getParameterValues("clinicDays"));
		procedureDays = this.parseVals(req.getParameterValues("procedureDays"));
		productApprovalFlag = Convert.formatInteger(req.getParameter("productApprovalFlag"));
		productGroupNumber = Convert.formatInteger(req.getParameter("productGroupNumber"));
		licenseNumber = req.getParameter("licenseNumber");
		nationalProviderIdentifier = req.getParameter("nationalProviderIdentifier");
		this.addClinic(new ClinicVO(req));
		
		//Some forms pass different vals for the name
		if (StringUtil.checkVal(firstName).length() == 0) firstName = se.decodeValue(req.getParameter("physFirstName"));
		if (StringUtil.checkVal(lastName).length() == 0) lastName = se.decodeValue(req.getParameter("physLastName"));
	}
	
	/**
	 * Sets the base info for the surgeon from the db row
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		surgeonId = db.getStringVal("surgeon_id", rs);
		profileId = db.getStringVal("profile_id", rs);
		surgeonTypeId = db.getStringVal("surgeon_type_id", rs);
		surgeonTypeName = db.getStringVal("type_nm", rs);
		firstName = db.getStringVal("first_nm", rs);
		middleName = db.getStringVal("middle_nm", rs);
		lastName = db.getStringVal("last_nm", rs);
		suffix = db.getStringVal("suffix_nm", rs);
		title = db.getStringVal("title_nm", rs);
		emailAddress = db.getStringVal("email_address_txt", rs);
		website = db.getStringVal("website_url", rs);
		language = db.getStringVal("language_txt", rs);
		localeText = db.getStringVal("locale_txt", rs);
		specialtyId = db.getIntegerVal("specialty_id", rs);
		specialtyName = db.getStringVal("specialty_nm", rs);
		salesRepId = db.getStringVal("sales_rep_id", rs);
		distance = db.getDoubleVal("distance", rs);
		statusId = db.getIntegerVal("status_id", rs);
		statusName = db.getStringVal("status_nm", rs);
		spouseName = db.getStringVal("spouse_nm", rs);
		salesRepName = db.getStringVal("rep_nm", rs);
		salesRegionName = db.getStringVal("region_nm", rs);
		childrenName = db.getStringVal("children_nm", rs);
		cellPhone = db.getStringVal("cell_phone_no", rs);
		pager = db.getStringVal("pager_no", rs);
		rank = db.getIntegerVal("rank_no", rs);
		allowMail = db.getIntegerVal("allow_mail_flg", rs);
		spanishFlag = db.getIntegerVal("spanish_flg", rs);
		boardCertifiedFlag = db.getIntegerVal("board_cert_flg", rs);
		fellowshipFlag = db.getIntegerVal("fellowship_flg", rs);
		rank = db.getIntegerVal("rank_no", rs);
		setScsStartDate(db.getDateVal("scs_start_dt", rs));
		clinicDays = this.parseVals(db.getStringVal("clinic_days", rs),",");
		procedureDays = this.parseVals(db.getStringVal("procedure_days", rs),",");
		productApprovalFlag = db.getIntegerVal("prod_approval_flg", rs);
		productGroupNumber = db.getIntegerVal("prod_group_no", rs);
		licenseNumber = db.getStringVal("medical_license_no", rs);
		nationalProviderIdentifier = db.getStringVal("national_provider_id", rs);
		this.addClinic(new ClinicVO(rs));
	}

	public void addClinic(ClinicVO cvo) {
		clinics.add(cvo);
	}
	
	/**
	 * Converts the SurgeonVO into a User Data VO
	 * @return
	 */
	public UserDataVO getUserInfo() {
		UserDataVO user = new UserDataVO();
		user.setFirstName(this.getFirstName());
		user.setLastName(this.getLastName());
		user.setEmailAddress(emailAddress);
		user.setLocation(this.getClinic().getLocation());
		
		return user;
	}
	
	/**
	 * Parse String array into a Map.
	 * @param vals
	 * @return
	 */
	public Map<String,String> parseVals(String[] vals) {
		Map<String,String> days = new HashMap<String,String>();
		if (vals != null && vals.length > 0) {
			for (int i = 0; i < vals.length; i++) {
				if (vals[i] != null && vals[i].length() > 0) {
					days.put(vals[i],"true");
				}
			}
		}
		return days;
	}
	
	/**
	 * Parse delimited String into a Map.
	 * @param vals
	 * @return
	 */
	public Map<String,String> parseVals(String vals, String delim) {
		Map<String,String> valueMap = new HashMap<String,String>();
		int index = -1;
		if (vals != null && vals.length() > 0) {
			while (vals.length() > 0) {
				index = vals.indexOf(delim);
				if (index > 0) {
					valueMap.put(vals.substring(0,index), "true");
					vals = vals.substring(index + 1);
				} else {
					valueMap.put(vals,"true");
					vals = "";
				}
			}
		}
		return valueMap;
	}
	
	/**
	 * Parse a Map's keys into a delimited String.
	 * @param days
	 * @return
	 */
	public String parseMap(Map<String,String> days, String delim) {
		StringBuffer buf = new StringBuffer("");
		int count = 0;
		int size = 0;
		if (! days.isEmpty()) {
			Set<String> keys = days.keySet();
			size = keys.size();
			Iterator<String> iter = keys.iterator();
			while (iter.hasNext()) {
				count++;
				String val = iter.next();
				buf.append(val);
				if (count < size) {
					buf.append(delim);
				}
			}
		}
		return buf.toString();
	}
	
	
	/**
	 * this method checks mandatory fields for data
	 * call this immediately before running the SQL insert/update to determine if 
	 * this query should be successful or fail by integrity rules.
	 * If this method returns false we should be returning the user to the input form (for more input) 
	 * @return
	 */
	public boolean isValidRecord() {
		if (this.getFirstName() == null || this.getFirstName().length() == 0) return false;
		if (this.getMiddleName() == null || this.getMiddleName().length() == 0) return false;
		if (this.getLastName() == null || this.getLastName().length() == 0) return false;
		if (this.clinics.size() == 0 || !this.clinics.get(1).isCompleteAddress()) return false;
		if (this.clinics.size() == 0 || !this.clinics.get(1).getPhones().get(1).isValidPhone())
			return false;
		
		return true;
	}
	
	/**
	 * @return the emailAddress
	 */
	public String getEmailAddress() {
		return emailAddress;
	}


	/**
	 * @return the firstName
	 */
	public String getFirstName() {
		return firstName;
	}


	/**
	 * @return the language
	 */
	public String getLanguage() {
		return language;
	}


	/**
	 * @return the lastName
	 */
	public String getLastName() {
		return lastName;
	}


	/**
	 * @return the localeText
	 */
	public String getLocaleText() {
		return localeText;
	}


	/**
	 * @return the specialty
	 */
	public Integer getSpecialty() {
		return specialtyId;
	}


	/**
	 * @return the staff
	 */
	public List<StaffVO> getStaff() {
		return staff;
	}


	/**
	 * @return the surgeonId
	 */
	public String getSurgeonId() {
		return surgeonId;
	}


	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}


	/**
	 * @return the website
	 */
	public String getWebsite() {
		if (website != null) return website.replace("http://", "");
		else return website;
	}

	/**
	 * @param emailAddress the emailAddress to set
	 */
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}


	/**
	 * @param firstName the firstName to set
	 */
	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}


	/**
	 * @param language the language to set
	 */
	public void setLanguage(String language) {
		this.language = language;
	}


	/**
	 * @param lastName the lastName to set
	 */
	public void setLastName(String lastName) {
		this.lastName = lastName;
	}


	/**
	 * @param localeText the localeText to set
	 */
	public void setLocaleText(String localeText) {
		this.localeText = localeText;
	}

	/**
	 * @param specialty the specialty to set
	 */
	public void setSpecialty(Integer specialty) {
		this.specialtyId = specialty;
	}


	/**
	 * @param staff the staff to set
	 */
	public void setStaff(List<StaffVO> staff) {
		this.staff = staff;
	}
	
	/**
	 * Adds a single element to the list of staff members
	 * @param ele
	 */
	public void addStaff(StaffVO ele) {
		staff.add(ele);
	}

	/**
	 * @param surgeonId the surgeonId to set
	 */
	public void setSurgeonId(String surgeonId) {
		this.surgeonId = surgeonId;
	}


	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}


	/**
	 * @param website the website to set
	 */
	public void setWebsite(String website) {
		this.website = website;
		if (website != null) this.website = website.replace("http://", "");
	}

	/**
	 * @return the primary clinic
	 */
	public ClinicVO getClinic() {

		ClinicVO clinic = new ClinicVO();
		if (clinics.size() > 0) clinic = clinics.get(0);
		
		return clinic;
	}
	
	/**
	 * @return the middleName
	 */
	public String getMiddleName() {
		return middleName;
	}

	/**
	 * @return the suffix
	 */
	public String getSuffix() {
		return suffix;
	}

	/**
	 * @param middleName the middleName to set
	 */
	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}
	
	/**
	 * @param suffix the suffix to set
	 */
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	/**
	 * @return the distance
	 */
	public Double getDistance() {
		return distance;
	}

	/**
	 * @param distance the distance to set
	 */
	public void setDistance(Double distance) {
		this.distance = distance;
	}

	/**
	 * @return the actionId
	 */
	public String getActionId() {
		return actionId;
	}

	/**
	 * @return the actionName
	 */
	public String getActionName() {
		return actionName;
	}

	/**
	 * @param actionId the actionId to set
	 */
	public void setActionId(String actionId) {
		this.actionId = actionId;
	}

	/**
	 * @param actionName the actionName to set
	 */
	public void setActionName(String actionName) {
		this.actionName = actionName;
	}

	/**
	 * @return the headerText
	 */
	public String getHeaderText() {
		return headerText;
	}

	/**
	 * @param headerText the headerText to set
	 */
	public void setHeaderText(String headerText) {
		this.headerText = headerText;
	}

	public List<ClinicVO> getClinics() {
		return clinics;
	}

	public void setClinics(List<ClinicVO> clinics) {
		this.clinics = clinics;
	}

	/**
	 * @return the statusId
	 */
	public Integer getStatusId() {
		return statusId;
	}

	/**
	 * @param statusId the statusId to set
	 */
	public void setStatusId(Integer statusId) {
		this.statusId = statusId;
	}

	/**
	 * @return the statusName
	 */
	public String getStatusName() {
		return statusName;
	}

	/**
	 * @param statusName the statusName to set
	 */
	public void setStatusName(String statusName) {
		this.statusName = statusName;
	}

	/**
	 * @return the specialtyId
	 */
	public Integer getSpecialtyId() {
		return specialtyId;
	}

	/**
	 * @param specialtyId the specialtyId to set
	 */
	public void setSpecialtyId(Integer specialtyId) {
		this.specialtyId = specialtyId;
	}

	/**
	 * @return the specialtyName
	 */
	public String getSpecialtyName() {
		return specialtyName;
	}

	/**
	 * @param specialtyName the specialtyName to set
	 */
	public void setSpecialtyName(String specialtyName) {
		this.specialtyName = specialtyName;
	}

	/**
	 * @return the surgeonTypeId
	 */
	public String getSurgeonTypeId() {
		return surgeonTypeId;
	}

	/**
	 * @param surgeonTypeId the surgeonTypeId to set
	 */
	public void setSurgeonTypeId(String surgeonTypeId) {
		this.surgeonTypeId = surgeonTypeId;
	}

	/**
	 * @return the salesRepId
	 */
	public String getSalesRepId() {
		return salesRepId;
	}

	/**
	 * @param salesRepId the salesRepId to set
	 */
	public void setSalesRepId(String salesRepId) {
		this.salesRepId = salesRepId;
	}

	/**
	 * @return the spouseName
	 */
	public String getSpouseName() {
		return spouseName;
	}

	/**
	 * @param spouseName the spouseName to set
	 */
	public void setSpouseName(String spouseName) {
		this.spouseName = spouseName;
	}

	/**
	 * @return the childrenName
	 */
	public String getChildrenName() {
		return childrenName;
	}

	/**
	 * @param childrenName the childrenName to set
	 */
	public void setChildrenName(String childrenName) {
		this.childrenName = childrenName;
	}

	/**
	 * @return the surgeonTypeName
	 */
	public String getSurgeonTypeName() {
		return surgeonTypeName;
	}

	/**
	 * @param surgeonTypeName the surgeonTypeName to set
	 */
	public void setSurgeonTypeName(String surgeonTypeName) {
		this.surgeonTypeName = surgeonTypeName;
	}

	/**
	 * @return the allowMail
	 */
	public Integer getAllowMail() {
		return allowMail;
	}

	/**
	 * @param allowMail the allowMail to set
	 */
	public void setAllowMail(Integer allowMail) {
		this.allowMail = allowMail;
	}

	/**
	 * @return the cellPhone
	 */
	public String getCellPhone() {
		return cellPhone;
	}

	/**
	 * @param cellPhone the cellPhone to set
	 */
	public void setCellPhone(String cellPhone) {
		this.cellPhone = cellPhone;
	}

	/**
	 * @return the pager
	 */
	public String getPager() {
		return pager;
	}

	/**
	 * @param pager the pager to set
	 */
	public void setPager(String pager) {
		this.pager = pager;
	}

	public List<DocumentVO> getDocuments() {
		return documents;
	}

	public void setDocuments(List<DocumentVO> documents) {
		this.documents = documents;
	}
	
	/**
	 * Adds a single element to the list of staff members
	 * @param ele
	 */
	public void addDocument(DocumentVO ele) {
		documents.add(ele);
	}

	public Integer getSpanishFlag() {
		return spanishFlag;
	}

	public void setSpanishFlag(Integer spanishFlag) {
		this.spanishFlag = spanishFlag;
	}
	
	public Integer getBoardCertifiedFlag() {
		return boardCertifiedFlag;
	}
	
	public String getSpanishText() {
		if (spanishFlag == 1) return "Yes";
		return "No";
	}

	public void setBoardCertifiedFlag(Integer bordCertifiedFlag) {
		this.boardCertifiedFlag = bordCertifiedFlag;
	}

	public Integer getFellowshipFlag() {
		return fellowshipFlag;
	}

	public void setFellowshipFlag(Integer fellowshipFlag) {
		this.fellowshipFlag = fellowshipFlag;
	}

	/**
	 * @return the salesRepName
	 */
	public String getSalesRepName() {
		return salesRepName;
	}

	/**
	 * @param salesRepName the salesRepName to set
	 */
	public void setSalesRepName(String salesRepName) {
		this.salesRepName = salesRepName;
	}

	/**
	 * @return the salesRegionName
	 */
	public String getSalesRegionName() {
		return salesRegionName;
	}

	/**
	 * @param salesRegionName the salesRegionName to set
	 */
	public void setSalesRegionName(String salesRegionName) {
		this.salesRegionName = salesRegionName;
	}

	/**
	 * @return the rank
	 */
	public Integer getRank() {
		return rank;
	}

	/**
	 * @param rank the rank to set
	 */
	public void setRank(Integer rank) {
		this.rank = rank;
	}

	/**
	 * @return the clinicDays
	 */
	public Map<String, String> getClinicDays() {
		return clinicDays;
	}

	/**
	 * @param clinicDays the clinicDays to set
	 */
	public void setClinicDays(Map<String, String> clinicDays) {
		this.clinicDays = clinicDays;
	}

	/**
	 * @return the procedureDays
	 */
	public Map<String, String> getProcedureDays() {
		return procedureDays;
	}

	/**
	 * @param procedureDays the procedureDays to set
	 */
	public void setProcedureDays(Map<String, String> procedureDays) {
		this.procedureDays = procedureDays;
	}

	public String getProfileId() {
		return profileId;
	}

	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	/**
	 * @return the scsStartDate
	 */
	public String getScsStartDate() {
		return scsStartDate;
	}
	
	public void setScsStartDate(Date scsDate) {
		if (scsDate != null) {
			
			Calendar cal = GregorianCalendar.getInstance();
			cal.setTime(scsDate);

			StringBuffer dateAsString = new StringBuffer();
			dateAsString.append(cal.get(Calendar.MONTH) + 1);
			dateAsString.append("/").append(cal.get(Calendar.DAY_OF_MONTH));
			dateAsString.append("/").append(cal.get(Calendar.YEAR));
			
			this.setScsStartDate(dateAsString.toString());
			
		} else {
			this.setScsStartDate("");
		}
	}

	/**
	 * @param scsStartDate the scsStartDate to set
	 */
	public void setScsStartDate(String scsStartDate) {
		this.scsStartDate = scsStartDate;
	}

	public Integer getProductApprovalFlag() {
		return productApprovalFlag;
	}

	public void setProductApprovalFlag(Integer productApprovalFlag) {
		this.productApprovalFlag = productApprovalFlag;
	}

	public Integer getProductGroupNumber() {
		return productGroupNumber;
	}

	public void setProductGroupNumber(Integer productGroupNumber) {
		this.productGroupNumber = productGroupNumber;
	}

	public String getLicenseNumber() {
		return licenseNumber;
	}

	public void setLicenseNumber(String licenseNumber) {
		this.licenseNumber = licenseNumber;
	}

	public String getNationalProviderIdentifier() {
		return nationalProviderIdentifier;
	}

	public void setNationalProviderIdentifier(String nationalProviderIdentifier) {
		this.nationalProviderIdentifier = nationalProviderIdentifier;
	}

}
