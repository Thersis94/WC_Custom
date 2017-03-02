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
	// account fields
	private static final String ACCT_EXPIRE = "ACCT_EXPIRE";
	private static final String ACCT_NM = "ACCT_NM";
	
	// profile fields
	private static final String FIRST_NM = "FIRST_NM";
	private static final String LAST_NM = "LAST_NM";
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
	private static final String USER_STATUS = "USER_STATUS";
	private static final String USER_EXPIRE = "USER_EXPIRE";
	private static final String HAS_FD = "HAS_FD";
	
	// SmartTRAK user reg fields
	private static final String COMPANY = "COMPANY";
	private static final String COMPANY_URL = "COMPANY_URL";
	private static final String DATE_DEMOED = "DATE_DEMOED";
	private static final String DATE_JOINED = "DATE_JOINED";
	private static final String DATE_TRAINED = "DATE_TRAINED";
	private static final String DIVISION = "DIVISION";
	private static final String FAV_UPDATES = "FAV_UPDATES";
	private static final String INDUSTRY = "INDUSTRY";
	private static final String JOB_CATEGORY = "JOB_CATEGORY";
	private static final String JOB_LEVEL = "JOB_LEVEL";
	private static final String NOTES = "NOTES";
	private static final String SOURCE = "SOURCE";
	private static final String TITLE = "TITLE";
	private static final String UPDATES = "UPDATES";

	// other fields
	private static final String LAST_LOGIN = "LAST_LOGIN";
	protected static final String OS = "OS";
	protected static final String BROWSER = "BROWSER";
	protected static final String HITS = "HITS";

	private static final String EMPTY_STRING = "";
	private static final String LIST_DELIMITER = ",";
	private static final String USER_FD_VAL = "FD";
	private static final String LOGIN_DATE_NULL_VAL = "Never";
	
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

		ExcelReport rpt = new ExcelReport(getHeader());
		rpt.setTitleCell(REPORT_TITLE);

		List<Map<String, Object>> rows = new ArrayList<>(accounts.size() * 5);
		rows = generateDataRows(rows);

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
	private List<Map<String, Object>> generateDataRows(
			List<Map<String, Object>> rows) {
		PhoneNumberFormat pnf = new PhoneNumberFormat();
		
		// loop the account map
		for (AccountUsersVO acct : accounts) {

			// user vals
			Map<String,Object> row;
			
			// loop account users
			for (UserVO user : acct.getUsers()) {
				row = new HashMap<>();
				row.put(ACCT_NM, acct.getAccountName());
				row.put(ACCT_EXPIRE, formatDate(acct.getExpirationDate()));
				row.put(USER_EXPIRE, formatDate(user.getExpirationDate()));
				row.put(COMPANY,user.getCompany());
				row.put(TITLE,user.getTitle());
				row.put(FIRST_NM,user.getFirstName());
				row.put(LAST_NM,user.getLastName());
				row.put(EMAIL,user.getEmailAddress());
				row.put(LAST_LOGIN, formatDate(user.getLoginDate(),true));
				/* row.put(HITS,user.getHits()); */
				row.put(MAIN_PHONE,formatPhoneNumber(pnf,user.getMainPhone(),user.getCountryCode()));
				row.put(MOBILE_PHONE,formatPhoneNumber(pnf,user.getMobilePhone(),user.getCountryCode()));
				row.put(ADDRESS1,user.getAddress());
				row.put(ADDRESS2,user.getAddress2());
				row.put(CITY_NM,user.getCity());
				row.put(STATE_CD,user.getState());
				row.put(POSTAL_CD,user.getZipCode());
				row.put(COUNTRY_CD,user.getCountryCode());
				row.put(USER_STATUS, user.getStatusCode());
				row.put(SOURCE, user.getSource());
				row.put(UPDATES, StringUtil.capitalize(user.getUpdates()));
				row.put(FAV_UPDATES, StringUtil.capitalize(user.getFavoriteUpdates()));
				row.put(DATE_JOINED, formatDate(user.getCreateDate()));
				row.put(DIVISION, formatUserDivisions(user.getDivisions()));
				/* row.put(OS, user.getOs()); */
				/* row.put(BROWSER, user.getBrowser()); */
				row.put(HAS_FD, formatUserFDFlag(user.getFdAuthFlg()));
				row.put(NOTES, user.getNotes());
				row.put(COMPANY_URL, user.getCompanyUrl());
				row.put(JOB_CATEGORY, user.getJobCategory());
				row.put(JOB_LEVEL, user.getJobLevel());
				row.put(INDUSTRY, user.getIndustry());
				row.put(DATE_DEMOED, user.getDemoDate());
				row.put(DATE_TRAINED, user.getTrainingDate());
				rows.add(row);
			}
		}
		
		return rows;
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
		pnf.setCountryCode(country);
		pnf.setPhoneNumber(phone);
		return pnf.getFormattedNumber();
		
	}
	
	/**
	 * Formats the date using Convert.DATE_SLASH_ABBREV_PATTERN (i.e. MMM/dd/yyyy)
	 * @param date
	 * @return
	 */
	protected String formatDate(Date date) {
		return formatDate(date,false);
	}
	
	/**
	 * Formats the date using Convert.DATE_SLASH_ABBREV_PATTERN (i.e. MMM/dd/yyyy).
	 * If isLastLogin is true and the date argument is null, the constant LOGIN_DATE_NULL_VAL
	 * is returned.
	 * @param date
	 * @param isLastLogin
	 * @return
	 */
	protected String formatDate(Date date, boolean isLastLogin) {
		String dStr = null;
		if (date == null) { 
			if (isLastLogin) {
				dStr = LOGIN_DATE_NULL_VAL;
			}
		} else {
			dStr = Convert.formatDate(date,Convert.DATE_SLASH_ABBREV_PATTERN);
		}
		return dStr;
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
		headerMap.put(ACCT_NM,"Account Name");
		headerMap.put(ACCT_EXPIRE,"Account Expiration");
		headerMap.put(USER_EXPIRE,"User Expiration");
		headerMap.put(COMPANY,"Company");
		headerMap.put(TITLE,"Title");
		headerMap.put(FIRST_NM,"First");
		headerMap.put(LAST_NM,"Last");
		headerMap.put(EMAIL,"Email Address");
		headerMap.put(LAST_LOGIN,"Last Login");
		/* headerMap.put(HITS,"Hits"); */
		headerMap.put(MAIN_PHONE,"Phone");
		headerMap.put(MOBILE_PHONE,"Mobile Phone");
		headerMap.put(ADDRESS1,"Address 1");
		headerMap.put(ADDRESS2,"Address 2");
		headerMap.put(CITY_NM,"City");
		headerMap.put(STATE_CD,"State");
		headerMap.put(POSTAL_CD,"Zip Code");
		headerMap.put(COUNTRY_CD,"Country");
		headerMap.put(USER_STATUS,"Status");
		headerMap.put(SOURCE,"Source");
		headerMap.put(UPDATES,"General Notifications");
		headerMap.put(FAV_UPDATES,"Favorite Notifications");
		headerMap.put(DATE_JOINED,"Date Joined");
		headerMap.put(DIVISION,"Division");
		/* headerMap.put(OS,"OS"); */
		/* headerMap.put(BROWSER,"Browser"); */
		headerMap.put(HAS_FD, USER_FD_VAL);
		headerMap.put(NOTES,"Notes");
		headerMap.put(COMPANY_URL,"Company URL");
		headerMap.put(JOB_CATEGORY,"Job Category");
		headerMap.put(JOB_LEVEL,"Job Level");
		headerMap.put(INDUSTRY,"Industry");
		headerMap.put(DATE_DEMOED,"Date Demoed");
		headerMap.put(DATE_TRAINED,"Date Trained");
		return headerMap;
	}

}
