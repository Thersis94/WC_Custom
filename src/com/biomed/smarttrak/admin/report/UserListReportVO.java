package com.biomed.smarttrak.admin.report;

// Java 8
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//WC custom
import com.biomed.smarttrak.vo.UserVO;
import com.biomed.smarttrak.vo.UserVO.RegistrationMap;
//SMTBaseLibs
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;
// WebCrescendo
import com.smt.sitebuilder.action.AbstractSBReportVO;

/*****************************************************************************
 <p><b>Title</b>: UserListReportVO.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Mar 02, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class UserListReportVO extends AbstractSBReportVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7975115731097780002L;
	
	private List<AccountUsersVO> accounts;
	private static final String REPORT_TITLE = "User List Export";
	private static final String DATE_DASH_PATTERN = "MM-dd-yyyy";
	// account fields
	private static final String ACCT_EXPIRE = "ACCT_EXPIRE";
	private static final String ACCT_NM = "ACCT_NM";
	private static final String ACCT_STATUS = "ACCT_STATUS";
	private static final String ACCT_ID = "ACCT_ID";
	private static final String ACCT_START_DT = "ACCT_START_DT";
	private static final String ACCT_TYPE = "ACCT_TYPE";
	private static final String ACCT_CLASSIFICATION = "ACCT_CLASSIFICATION";

	// profile fields
	private static final String USER_STATUS = "USER_STATUS";
	private static final String FULL_NM = "FULL_NM";
	private static final String EMAIL = "EMAIL";
	private static final String ADDRESS1 = "ADDRESS1";
	private static final String ADDRESS2 = "ADDRESS2";
	private static final String CITY_NM = "CITY_NM";
	private static final String STATE_CD = "STATE_CD";
	private static final String POSTAL_CD = "POSTAL_CD";
	private static final String COUNTRY_CD = "COUNTRY_CD";
	private static final String MAIN_PHONE = "PHONE";
	private static final String MOBILE_PHONE = "MOBILE_PHONE";

	// SmartTRAK user fields
	private static final String LICENSE_TYPE = "LICENSE_TYPE";
	private static final String USER_EXPIRE = "USER_EXPIRE";
	private static final String HAS_FD = "HAS_FD";
	private static final String USER_ID = "USER_ID";
	private static final String ACCT_OWNER_FLAG = "ACCT_OWNER_FLAG";

	// other fields
	private static final String DATE_JOINED = "DATE_JOINED";
	protected static final String OS = "OS";
	protected static final String BROWSER = "BROWSER";
	protected static final String DEVICE_TYPE = "DEVICE_TYPE";
	protected static final String PAGEVIEWS = "PAGEVIEWS";

	private static final String EMPTY_STRING = "";
	private static final String LIST_DELIMITER = ",";
	private static final String USER_FD_VAL = "FD";
	private static final String DEFAULT_COUNTRY = "US";
	
	/**
	* Constructor
	*/
	public UserListReportVO() {
        super();
        setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
        setFileName(REPORT_TITLE+".xls");
        accounts = new ArrayList<>();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("generateReport...");

		ExcelReport rpt = new SmarttrakExcelReport(getHeader());

		List<Map<String, Object>> rows = new ArrayList<>(accounts.size() * 5);
		generateDataRows(rows);

		rpt.setData(rows);
		return rpt.generateReport();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		this.accounts =  (List<AccountUsersVO>) o;
	}
	
	/**
	 * this method is used to generate the data rows of the excel sheet.
	 * @param rows
	 * @return
	 */
	private void generateDataRows(List<Map<String, Object>> rows) {
		PhoneNumberFormat pnf = new PhoneNumberFormat();
		
		// loop the account map
		for (AccountUsersVO acct : accounts) {

			// user vals
			Map<String,Object> row;
			
			// loop account users
			for (UserVO user : acct.getUsers()) {
				row = new HashMap<>();
				row.put(ACCT_ID, acct.getAccountId());
				row.put(ACCT_NM, acct.getAccountName());
				row.put(ACCT_START_DT, formatDate(acct.getStartDate(), false));
				row.put(ACCT_EXPIRE, formatDate(acct.getExpirationDate(),false));
				row.put(ACCT_STATUS, acct.getStatusName());
				row.put(ACCT_TYPE, acct.getTypeName());
				row.put(ACCT_CLASSIFICATION, acct.getClassificationName());
				row.put(USER_STATUS, user.getStatusName());
				row.put(LICENSE_TYPE, user.getLicenseName());
				row.put(USER_ID, user.getUserId());
				row.put(USER_EXPIRE, formatDate(user.getExpirationDate(),false));
				row.put(RegistrationMap.COMPANY.name(),user.getCompany());
				row.put(RegistrationMap.TITLE.name(),user.getTitle());
				row.put(FULL_NM, user.getFullName());
				row.put(EMAIL,user.getEmailAddress());
				row.put(ACCT_OWNER_FLAG, user.getAcctOwnerFlg() == 1 ? "Yes" : "No");
				row.put(SmarttrakExcelReport.LAST_LOGIN_DT, formatDate(user.getLoginDate(),true));
				row.put(PAGEVIEWS, user.getAttribute(PAGEVIEWS));
				row.put(MAIN_PHONE,formatPhoneNumber(pnf,user.getMainPhone(),user.getCountryCode()));
				row.put(MOBILE_PHONE,formatPhoneNumber(pnf,user.getMobilePhone(),user.getCountryCode()));
				row.put(ADDRESS1,user.getAddress());
				row.put(ADDRESS2,user.getAddress2());
				row.put(CITY_NM,user.getCity());
				row.put(STATE_CD,user.getState());
				row.put(POSTAL_CD,user.getZipCode());
				row.put(COUNTRY_CD,user.getCountryCode());
				row.put(RegistrationMap.SOURCE.name(), user.getSource());
				row.put(RegistrationMap.UPDATES.name(), StringUtil.capitalize(user.getUpdates()));
				row.put(RegistrationMap.FAVORITEUPDATES.name(), StringUtil.capitalize(user.getFavoriteUpdates()));
				row.put(DATE_JOINED, formatDate(user.getCreateDate(),false));
				row.put(RegistrationMap.DIVISIONS.name(), formatUserDivisions(user.getDivisions()));
				row.put(OS, user.getAttribute(OS));
				row.put(BROWSER, user.getAttribute(BROWSER));
				row.put(DEVICE_TYPE, user.getAttribute(DEVICE_TYPE));
				row.put(HAS_FD, formatUserFDFlag(user.getFdAuthFlg()));
				row.put(RegistrationMap.NOTES.name(), user.getNotes());
				row.put(RegistrationMap.COMPANYURL.name(), user.getCompanyUrl());
				row.put(RegistrationMap.JOBCATEGORY.name(), user.getJobCategory());
				row.put(RegistrationMap.JOBLEVEL.name(), user.getJobLevel());
				row.put(RegistrationMap.INDUSTRY.name(), user.getIndustry());
				/* Add the login age to data map for reporting formatting. This particular field is utilized for styling 
				 * and not meant for actual display, hence no matching header column entry*/
				row.put(SmarttrakExcelReport.LAST_LOGIN_AGE, user.getLoginAge());
				
				rows.add(row);
			}
		}

	}
	
	/**
	 * Formats a phone number based on the country
	 * @param pnf
	 * @param phone
	 * @param country
	 * @return
	 */
	protected String formatPhoneNumber(PhoneNumberFormat pnf, String phone, String country) {
		if (StringUtil.checkVal(phone).isEmpty()) return EMPTY_STRING;
		// set the country based on the user's country code.
		pnf.setCountryCode(StringUtil.isEmpty(country) ? DEFAULT_COUNTRY : country);
		// if the country code that has been set is US, use dash formatting, else international formatting
		if (DEFAULT_COUNTRY.equalsIgnoreCase(pnf.getCountryCode())) {
			pnf.setFormatType(PhoneNumberFormat.DASH_FORMATTING);
		} else {
			pnf.setFormatType(PhoneNumberFormat.INTERNATIONAL_FORMAT);
		}
		pnf.setPhoneNumber(phone);
		return pnf.getFormattedNumber();
	}

	/**
	 * Return formatted date.
	 * @param date
	 * @param isLoginDate
	 * @return
	 */
	protected String formatDate(Date date, boolean isLoginDate) {
		if (isLoginDate && date == null) {
			return SmarttrakExcelReport.NO_ACTIVITY;
		}                                       
		return Convert.formatDate(date, DATE_DASH_PATTERN);
	}
	
	/**
	 * Parses user divisions list into a delimited String.
	 * @param divisions
	 * @return
	 */
	protected String formatUserDivisions(List<String> divisions) {
		String divs = EMPTY_STRING;
		if (divisions != null && ! divisions.isEmpty()) {
			divs = StringUtil.getDelimitedList(divisions.toArray(new String[]{}), false, LIST_DELIMITER);
		}
		return divs;
	}

	/**
	 * Formats the value displayed for the user's FD field.
	 * @param fd
	 * @return
	 */
	protected String formatUserFDFlag(int fd) {
		return fd == 1 ? USER_FD_VAL : EMPTY_STRING;
	}

	/**
	 * builds the header map for the excel report
	 * @return
	 */
	protected HashMap<String, String> getHeader() {
		HashMap<String, String> headerMap = new LinkedHashMap<>();
		headerMap.put(ACCT_ID, "Account Id");
		headerMap.put(ACCT_NM,"Account Name");
		headerMap.put(ACCT_START_DT, "Account Start Date");
		headerMap.put(ACCT_EXPIRE,"Account Expiration");
		headerMap.put(ACCT_STATUS,"Account Status");
		headerMap.put(ACCT_TYPE, "Account Type");
		headerMap.put(ACCT_CLASSIFICATION ,"Account Classification");
		headerMap.put(USER_STATUS,"User Status");
		headerMap.put(LICENSE_TYPE,"License Type");
		headerMap.put(USER_ID, "User Id");
		headerMap.put(USER_EXPIRE,"User Expiration");
		headerMap.put(RegistrationMap.COMPANY.name(),"Company");
		headerMap.put(RegistrationMap.TITLE.name(),"Title");
		headerMap.put(FULL_NM, "Full Name");
		headerMap.put(EMAIL,"Email Address");
		headerMap.put(ACCT_OWNER_FLAG, "Account Lead");
		headerMap.put(SmarttrakExcelReport.LAST_LOGIN_DT,"Last Login");
		headerMap.put(PAGEVIEWS, "Page Views");
		headerMap.put(MAIN_PHONE,"Phone");
		headerMap.put(MOBILE_PHONE,"Mobile Phone");
		headerMap.put(ADDRESS1,"Address 1");
		headerMap.put(ADDRESS2,"Address 2");
		headerMap.put(CITY_NM,"City");
		headerMap.put(STATE_CD,"State");
		headerMap.put(POSTAL_CD,"Zip Code");
		headerMap.put(COUNTRY_CD,"Country");
		headerMap.put(RegistrationMap.SOURCE.name(),"Source");
		headerMap.put(RegistrationMap.UPDATES.name(),"Updates");
		headerMap.put(RegistrationMap.FAVORITEUPDATES.name(),"Watchlist Updates");
		headerMap.put(DATE_JOINED,"Date Joined");
		headerMap.put(RegistrationMap.DIVISIONS.name(),"Division");
		headerMap.put(OS, OS);
		headerMap.put(BROWSER, "Browser");
		headerMap.put(DEVICE_TYPE, "Device Type");
		headerMap.put(HAS_FD, USER_FD_VAL);
		headerMap.put(RegistrationMap.NOTES.name(),"Notes");
		headerMap.put(RegistrationMap.COMPANYURL.name(),"Company URL");
		headerMap.put(RegistrationMap.JOBCATEGORY.name(),"Job Category");
		headerMap.put(RegistrationMap.JOBLEVEL.name(),"Job Level");
		headerMap.put(RegistrationMap.INDUSTRY.name(),"Industry");
		return headerMap;
	}

}
