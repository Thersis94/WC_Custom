package com.biomed.smarttrak.admin.report;

// Java 8
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//WC custom
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.PermissionVO;
import com.biomed.smarttrak.vo.UserVO;
import com.biomed.smarttrak.vo.UserVO.RegistrationMap;
import com.biomed.smarttrak.vo.UserVO.Status;

//SMTBaseLibs
import com.siliconmtn.data.Node;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WebCrescendo
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.common.SiteVO;

/*****************************************************************************
 <p><b>Title</b>: AccountReportVO.java</p>
 <p><b>Description: </b>Creates the account(s) report as HTML.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Mar 7, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class AccountReportVO extends AbstractSBReportVO {

	private static final long serialVersionUID = -4695811549286840882L;
	private static final String REPORT_TITLE = "Accounts Report";
	protected static final String KEY_ACCOUNTS = "accounts";
	protected static final String KEY_FIELD_OPTIONS = "fieldOptions";
	private static final int ACCT_OWNER_FLAG_TRUE = 1;

	// CSS style constants
	private static final String CSS_ACCT_REPORT_WRAPPER = "acctReportWrapper";
	private static final String CSS_ACCT_ITEM_WRAPPER = "acctItem";
	private static final String CSS_ACCT_HEADER = "acctHeader";
	private static final String CSS_ACCT_DATE = "acctDate";
	private static final String CSS_ACCT_SEGMENT = "acctSegment";
	private static final String CSS_DIVISION_WRAPPER = "divisionWrapper";
	private static final String CSS_DIVISION_NAME = "divisionName";
	private static final String CSS_DIVISION_USER = "divisionUser";
	private static final String CSS_DIVISION_ACCT_OWNER = "acctOwner";
	private static final String CSS_ACCT_SUMMARY_HEADER = "acctSummaryHeader";
	private static final String CSS_ACCT_SUMMARY_ITEM = "acctSummaryItem";
	private static final String CSS_USER_STATUS_CD = "userStatusCode";

	private List<AccountUsersVO> accounts;
	private Map<String,Map<String,String>> fieldOptions;
	private SiteVO site;
	
	/**
	* Constructor
	*/
	public AccountReportVO() {
        super();
        setContentType("text/html");
        isHeaderAttachment(Boolean.FALSE);
        setFileName(REPORT_TITLE);
        accounts = new ArrayList<>();
        fieldOptions = new HashMap<>();
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("generateReport...");
		return generateReportAsString().getBytes();
	}

	/**
	 * Generates the report and returns it as a String.  Used to render the report
	 * in HTML in a JSTL view.
	 * @return
	 */
	public String generateReportAsString() {
		log.debug("generateReportAsString...");
		return buildReport().toString();
	}

	/**
	 * Builds the report.
	 * @return
	 */
	protected StringBuilder buildReport() {
		StringBuilder rows = new StringBuilder(accounts.size() * 1200);
		getHeader(rows);
		generateBody(rows);
		getFooter(rows);
		return rows;
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object o) {
		Map<String,Object> dataMap = (Map<String,Object>)o;
		this.accounts = (List<AccountUsersVO>) dataMap.get(KEY_ACCOUNTS);
		this.fieldOptions = (Map<String,Map<String,String>>) dataMap.get(KEY_FIELD_OPTIONS);
	}
	
	/**
	 * this method is used to generate the data rows of the  sheet.
	 * @param sb
	 */
	private void generateBody(StringBuilder sb) {
		// loop accounts and process
		int activeAccounts = accounts.size();
		int totalSubscribers = 0;
		int totalAdded = 0;
		int totalComplementary = 0;

		sb.append("<body>");
		startDiv(sb, CSS_ACCT_REPORT_WRAPPER);

		// build the report body
		for (AccountUsersVO acct : accounts) {
			startDiv(sb,CSS_ACCT_ITEM_WRAPPER);
			addAccountRow(sb,acct);
			addAccountDatesRows(sb,acct);
			addAccountSegmentRows(sb,acct);
			addDivisions(sb,acct,fieldOptions.get(RegistrationMap.DIVISIONS.getFieldId()));
			closeDiv(sb);
			totalSubscribers += acct.getTotalDivisionUsers();
			totalAdded += acct.getAddedCount();
			totalComplementary += acct.getComplementaryCount();
		}
		addSummaryRows(sb,activeAccounts,totalSubscribers,totalAdded,totalComplementary);

		// close the report wrapper
		closeDiv(sb);
		sb.append("</body>");

	}

	/**
	 * Adds the account row to the report
	 * @param sb
	 * @param acct
	 */
	protected void addAccountRow(StringBuilder sb, AccountUsersVO acct) {
		startDiv(sb,CSS_ACCT_HEADER);
		sb.append(acct.getAccountName().toUpperCase());
		int totUsers = acct.getTotalDivisionUsers() - 
				(acct.getAddedCount() + 
						acct.getComplementaryCount() + 
						acct.getUpdatesOnlyCount());
		if (totUsers > 0) 	sb.append(" (").append(totUsers).append(")");
		if (acct.getAddedCount() > 0) {
			sb.append(" ");
			sb.append(acct.getAddedCount());
			sb.append(UserVO.Status.ACTIVE.getCode()); 
		}
		if (acct.getComplementaryCount() > 0) {
			sb.append(" ");
			sb.append(acct.getComplementaryCount());
			sb.append(UserVO.Status.COMPLIMENTARY.getCode()); 
		}
		closeDiv(sb);
		
	}

	/**
	 * Adds the account starting/expiration date rows to the report
	 * @param sb
	 * @param acct
	 */
	protected void addAccountDatesRows(StringBuilder sb, AccountUsersVO acct) {
		startDiv(sb,CSS_ACCT_DATE);
		sb.append("Start Date:");
		appendSpace(sb);
		sb.append(Convert.formatDate(acct.getCreateDate(),Convert.DATE_SHORT_MONTH));
		closeDiv(sb);
		
		startDiv(sb,CSS_ACCT_DATE);
		sb.append("Expiration Date:");
		appendSpace(sb);
		sb.append(Convert.formatDate(acct.getExpirationDate(),Convert.DATE_SHORT_MONTH));
		closeDiv(sb);
	}

	/**
	 * Adds the account segment rows to the report
	 * @param sb
	 * @param acct
	 */
	protected void addAccountSegmentRows(StringBuilder sb, AccountUsersVO acct) {
		SmarttrakTree tree = acct.getPermissions();
		String lvl3 = null;
		StringBuilder lvl4 = null;
		int level;
		int cnt = 0;
		
		for (Node seg : tree.getPreorderList()) {
			level = seg.getDepthLevel();
			if (level < 3 || level > 4) continue;
			switch (level) {
				case 3:
					appendSegmentRow(sb,lvl3,lvl4);
					lvl3 = seg.getNodeName().toUpperCase() + ":";
					lvl4 = new StringBuilder(100);
					cnt = 1;
					break;
				case 4:
					PermissionVO perms = (PermissionVO)seg.getUserObject();
					appendSegment(lvl4,seg.getNodeName(),perms.isBrowseAuth(),cnt);
					if (perms.isBrowseAuth()) cnt++;
					break;
				default:
					break;
			}
		}
	}
	
	/**
	 * Adds the level 3 segment name to the StringBuilder passed in as an argument.
	 * Used by the method that builds the account segment rows
	 * @param sb
	 * @param parent
	 * @param children
	 */
	protected void appendSegmentRow(StringBuilder sb, String parent, StringBuilder children) {
		if (parent != null && children.length() > 0) {
			startDiv(sb,null);
			startSpan(sb,CSS_ACCT_SEGMENT);
			sb.append(parent);
			closeSpan(sb);
			sb.append(children);
			closeDiv(sb);
		}
	}
	
	/**
	 * Adds the level 4 segment name to the StringBuilder passed as an argument.
	 * Used by the method that builds the account segment rows.
	 * @param sb
	 * @param segName
	 * @param addSegment
	 * @param cnt
	 */
	protected void appendSegment(StringBuilder sb, String segName, boolean addSegment, int cnt) {
		if (addSegment) {
			if (cnt > 1) sb.append(",");
			appendSpace(sb);
			sb.append(segName);
		}
	}
	
	/**
	 * Adds the summary rows to the end of the report.
	 * @param sb
	 * @param totalAccounts
	 * @param totalSubscribers
	 * @param totalAdded
	 * @param totalComplimentary
	 */
	protected void addSummaryRows(StringBuilder sb, int totalAccounts, 
			int totalSubscribers, int totalAdded, int totalComplimentary) {

		startDiv(sb,CSS_ACCT_SUMMARY_HEADER);
		sb.append("Account Summary ");
		closeDiv(sb);

		startDiv(sb,CSS_ACCT_SUMMARY_ITEM);
		sb.append("Active Accounts ");
		sb.append(totalAccounts);
		closeDiv(sb);

		startDiv(sb,CSS_ACCT_SUMMARY_ITEM);
		sb.append("Subscribers ");
		sb.append(totalSubscribers);
		closeDiv(sb);

		startDiv(sb,CSS_ACCT_SUMMARY_ITEM);
		sb.append("Added Seats ");
		sb.append(totalAdded);
		closeDiv(sb);

		startDiv(sb,CSS_ACCT_SUMMARY_ITEM);
		sb.append("Complimentary Seats ");
		sb.append(totalComplimentary);
		closeDiv(sb);
	}

	/**
	 * Adds division rows to the report.
	 * @param sb
	 * @param acct
	 * @param divMap
	 */
	protected void addDivisions(StringBuilder sb, 
			AccountUsersVO acct, Map<String,String> divMap) {
		String divName;
		for (Map.Entry<String,List<UserVO>> division : acct.getDivisions().entrySet()) {
			divName = divMap.get(division.getKey());
			startDiv(sb,CSS_DIVISION_WRAPPER);
			startSpan(sb,CSS_DIVISION_NAME);
			sb.append(StringUtil.checkVal(divName));
			closeSpan(sb);
			closeDiv(sb);
			addDivisionUsers(sb,division.getValue());
		}
	}

	/**
	 * Adds division users to the division that is being added to the report
	 * @param sb
	 * @param users
	 */
	protected void addDivisionUsers(StringBuilder sb, List<UserVO> users) {
		for (UserVO user : users) {
			if (user.getAcctOwnerFlg() == ACCT_OWNER_FLAG_TRUE) {
				startDiv(sb,CSS_DIVISION_ACCT_OWNER);
			} else {
				startDiv(sb,CSS_DIVISION_USER);
			}
			sb.append(user.getFullName());
			addUserIdentifier(sb,user);
			closeDiv(sb);
		}
	}

	/**
	 * Adds user identifier for certain users based on country code or 
	 * job category/job level or simply job level and/or user status code
	 * @param sb
	 * @param user
	 */
	protected void addUserIdentifier(StringBuilder sb, UserVO user) {
		// look at job category/level
		String sfx = findSuffix(Convert.formatInteger(user.getJobCategory()),
					Convert.formatInteger(user.getJobLevel()));

		if (sfx != null) {
			sb.append(" [").append(sfx).append("]");
		}

		// now append status code if appropriate.
		addUserStatusCode(sb,user.getStatusCode());
	}

	/**
	 * Checks job category and job level to determine suffix.
	 * @param cat
	 * @param lvl
	 * @return
	 */
	protected String findSuffix(int cat, int lvl) {
		String sfx = null;
		// first, check for match against both vals
		if (cat == 2 && lvl == 10) {
			sfx =  "PM";
		} else if (cat == 5 && lvl == 4) {
			sfx =  "SA";
		}

		// if no match, consider job category
		if (sfx == null) {
			sfx = findSuffixViaJobCategory(cat);
			// if still no match, consider job level only.
			if (sfx == null && lvl == 10) {
				sfx = "M";
			}
		}
		return sfx;
	}

	/**
	 * Determine suffix using job category only.
	 * @param cat
	 * @return
	 */
	protected String findSuffixViaJobCategory(int cat) {
		String sfx = null;
		switch(cat) {
			case 8: sfx =  "BD"; break;
			case 9: sfx =  "Ex"; break;
			case 10: sfx =  "NA"; break;
			case 11: sfx =  "UK"; break;
			case 15: sfx =  "RA";	 break;
			default: break;
		}
		return sfx;
	}

	/**
	 * Adds user status code identifier for certain user status codes.
	 * @param sb
	 * @param statCd
	 */
	protected void addUserStatusCode(StringBuilder sb, String statCd) {
		if (statCd.equalsIgnoreCase(Status.COMPLIMENTARY.getCode()) ||
				statCd.equalsIgnoreCase(Status.UPDATES.getCode()) ||
				statCd.equalsIgnoreCase(Status.EXTRA.getCode()) ||
				statCd.equalsIgnoreCase(Status.COMPUPDATES.getCode())) {

			startSpan(sb,CSS_USER_STATUS_CD);
			appendSpace(sb);

			if (statCd.equalsIgnoreCase(Status.EXTRA.getCode())) {
				sb.append(Status.ACTIVE.getCode());
			} else {
				sb.append(statCd);
			}

			closeSpan(sb);
		}
	}

	/**
	 * builds the header map for the report
	 * @param sb
	 */
	protected void getHeader(StringBuilder sb) {
		sb.append("<!DOCTYPE html>");
		sb.append("<head>");
		sb.append("<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\">");
		sb.append("<title>").append(REPORT_TITLE).append("</title>");
		sb.append("<link href=\"").append(site.getFullSiteAlias()).append("/binary/themes/");
		sb.append(site.getTheme().getPageLocationName()).append("/scripts/modules_admin.css\" type='text/css' rel='stylesheet' />");
		sb.append("</head>");
	}

	/**
	 * Adds a closing html tag.
	 * @param sb
	 */
	protected void getFooter(StringBuilder sb) {
		sb.append("</html>");
	}

	/**
	 * Adds a starting div tag with a class attribute of classNm if
	 * classNm is not empty or null.
	 * @param sb
	 * @param classNm
	 */
	protected void startDiv(StringBuilder sb, String classNm) {
		sb.append("<div");
		if (! StringUtil.isEmpty(classNm)) {
			sb.append(" class=\"");
			sb.append(classNm);
			sb.append("\"");
		}
		sb.append(">");
	}

	/**
	 * Adds a closing div tag
	 * @param sb
	 */
	protected void closeDiv(StringBuilder sb) {
		sb.append("</div>");
	}

	/**
	 * Adds a starting span tag with a class attribute of classNm if
	 * classNm is not empty or null.
	 * @param sb
	 * @param classNm
	 */
	protected void startSpan(StringBuilder sb, String classNm) {
		sb.append("<span");
		if (! StringUtil.isEmpty(classNm)) {
			sb.append(" class=\"");
			sb.append(classNm);
			sb.append("\"");
		}
		sb.append(">");
	}
	
	/**
	 * Adds a closing span tag
	 * @param sb
	 */
	protected void closeSpan(StringBuilder sb) {
		sb.append("</span>");
	}

	/**
	 * Adds a non-breaking space
	 * @param sb
	 */
	protected void appendSpace(StringBuilder sb) {
		sb.append("&nbsp;");
	}

	/**
	 * @param site the site to set
	 */
	public void setSite(SiteVO site) {
		this.site = site;
	}

}
