package com.biomed.smarttrak.admin.report;

// Java 8
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WebCrescendo
import com.smt.sitebuilder.action.AbstractSBReportVO;

/*****************************************************************************
 <p><b>Title</b>: AccountReportVO.java</p>
 <p><b>Description: </b></p>
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
	private static final String REPORT_TITLE = "Account Report";
	protected static final String KEY_ACCOUNTS = "accounts";
	protected static final String KEY_FIELD_OPTIONS = "fieldOptions";
	private static final String ROW = "ROW";
	private List<AccountUsersVO> accounts;
	private Map<String,Map<String,String>> fieldOptions;
	
	/**
	* Constructor
	*/
	public AccountReportVO() {
        super();
        setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
        setFileName(REPORT_TITLE+".xls");
        accounts = new ArrayList<>();
        fieldOptions = new HashMap<>();
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
		Map<String,Object> dataMap = (Map<String,Object>)o;
		this.accounts = (List<AccountUsersVO>) dataMap.get(KEY_ACCOUNTS);
		this.fieldOptions = (Map<String,Map<String,String>>) dataMap.get(KEY_FIELD_OPTIONS);
	}
	
	/**
	 * this method is used to generate the data rows of the excel sheet.
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateDataRows(
			List<Map<String, Object>> rows) {
		// loop accounts and process
		int activeAccounts = accounts.size();
		int totalSubscribers = 0;
		int totalAdded = 0;
		int totalComplementary = 0;
		for (AccountUsersVO acct : accounts) {
			addAccountRow(rows, acct);
			addAccountDatesRows(rows,acct);
			addAccountSegmentRows(rows,acct);
			addDivisions(rows,acct,fieldOptions.get(RegistrationMap.DIVISIONS.getFieldId()));
			totalSubscribers += acct.getTotalUsers();
			totalAdded += acct.getAddedCount();
			totalComplementary += acct.getComplementaryCount();
			// add blank separator row
			rows.add(addRow(ROW,""));
		}
		
		addSummaryRows(rows,activeAccounts,totalSubscribers,totalAdded,totalComplementary);
		return rows;
	}
	
	/**
	 * Adds the account row to the report
	 * @param rows
	 * @param acct
	 */
	protected void addAccountRow(List<Map<String,Object>> rows, AccountUsersVO acct) {
		StringBuilder sb = new StringBuilder(75);
		sb.append(acct.getAccountName().toUpperCase());
		int totUsers = acct.getTotalUsers() - 
				(acct.getAddedCount() + 
						acct.getComplementaryCount() + 
						acct.getUpdatesOnlyCount());
		if (totUsers > 0) 	sb.append(" (").append(totUsers).append(")");
		if (acct.getAddedCount() > 0) {
			sb.append(" ");
			sb.append(acct.getAddedCount());
			sb.append(UserVO.Status.EXTRA.getCode()); 
		}
		if (acct.getComplementaryCount() > 0) {
			sb.append(" ");
			sb.append(acct.getComplementaryCount());
			sb.append(UserVO.Status.COMPLIMENTARY.getCode()); 
		}
		rows.add(addRow(ROW, sb.toString()));
		
	}

	/**
	 * Adds the account starting/expiration date rows to the report
	 * @param rows
	 * @param acct
	 */
	protected void addAccountDatesRows(List<Map<String,Object>> rows, AccountUsersVO acct) {
		StringBuilder sb = new StringBuilder(100);
		sb.append("Start Date: ");
		sb.append(Convert.formatDate(acct.getCreateDate(),Convert.DATE_LONG));
		rows.add(addRow(ROW,sb.toString()));
		
		sb = new StringBuilder(100);
		sb.append("Expiration Date: ");
		sb.append(Convert.formatDate(acct.getExpirationDate(),Convert.DATE_LONG));
		rows.add(addRow(ROW,sb.toString()));
	}

	/**
	 * Adds the account segment rows to the report
	 * @param rows
	 * @param acct
	 */
	protected void addAccountSegmentRows(List<Map<String,Object>> rows, AccountUsersVO acct) {
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
					appendSegmentRow(lvl3,lvl4,rows);
					lvl3 = seg.getNodeName().toUpperCase();
					lvl4 = new StringBuilder(100);
					cnt = 1;
					break;
				case 4:
					PermissionVO perms = (PermissionVO)seg.getUserObject();
					appendSegment(perms,lvl4,seg.getNodeName(),cnt);
					cnt++;
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
	 * @param rows
	 */
	protected void appendSegmentRow(String parent, StringBuilder children, List<Map<String,Object>> rows) {
		if (parent != null && children.length() > 0) rows.add(addRow(ROW,parent + children.toString()));
	}
	
	/**
	 * Adds the level 4 segment name to the StringBuilder passed as an argument.
	 * Used by the method that builds the account segment rows.
	 * @param sb
	 * @param segName
	 * @param cnt
	 */
	protected void appendSegment(PermissionVO perms, StringBuilder sb, String segName, int cnt) {
		if (perms.isBrowseAuth()) {
			if (cnt == 1) sb.append(": ");
			else sb.append(",");
			sb.append(" ").append(segName);
		}
	}
	
	/**
	 * Adds the summary rows to the end of the report.
	 * @param rows
	 * @param totalAccounts
	 * @param totalSubscribers
	 * @param totalAdded
	 * @param totalComplimentary
	 */
	protected void addSummaryRows(List<Map<String,Object>> rows, int totalAccounts, 
			int totalSubscribers, int totalAdded, int totalComplimentary) {
		rows.add(addRow(ROW,"Account Summary"));
		
		StringBuilder sb = new StringBuilder(75);
		sb.append("Active Accounts ");
		sb.append(totalAccounts);
		rows.add(addRow(ROW,sb.toString()));
		
		sb = new StringBuilder(75);
		sb.append("Subscribers ");
		sb.append(totalSubscribers);
		rows.add(addRow(ROW,sb.toString()));
		
		sb = new StringBuilder(75);
		sb.append("Added Seats ");
		sb.append(totalAdded);
		rows.add(addRow(ROW,sb.toString()));
		
		sb = new StringBuilder(75);
		sb.append("Complimentary Seats ");
		sb.append(totalComplimentary);
		rows.add(addRow(ROW,sb.toString()));
	}

	/**
	 * Adds division rows to the report.
	 * @param rows
	 * @param acct
	 * @param divMap
	 */
	protected void addDivisions(List<Map<String,Object>> rows, AccountUsersVO acct, Map<String,String> divMap) {
		String divName;
		for (Map.Entry<String,List<UserVO>> division : acct.getDivisions().entrySet()) {
			divName = divMap.get(division.getKey());
			rows.add(addRow(ROW, StringUtil.checkVal(divName)));
			addDivisionUsers(rows,division.getValue());
		}
	}

	/**
	 * Adds division users to the division that is being added to the report
	 * @param rows
	 * @param users
	 */
	protected void addDivisionUsers(List<Map<String,Object>> rows, List<UserVO> users) {
		for (UserVO user : users) {
			StringBuilder sb = new StringBuilder(50);
			sb.append(user.getFullName());
			addUserIdentifier(sb,user);
			rows.add(addRow(ROW,sb.toString()));
		}
	}

	/**
	 * Adds user identifier for certain users based on country code or 
	 * job category/job level or simply job level and/or user status code
	 * @param sb
	 * @param jobCat
	 */
	protected void addUserIdentifier(StringBuilder sb, UserVO user) {
		// look at country code first.
		String suffix = null;
		if ("UK".equals(user.getCountryCode())) {
			suffix = " [UK]";
		} else {
			// not UK, so look at job category/level
			int jobCat = Convert.formatInteger(user.getJobCategory());
			int jobLvl = Convert.formatInteger(user.getJobLevel());

			switch(jobCat) {
				case 2:
					if (jobLvl == 10) suffix = " [PM]";
					break;
				case 5:
					if (jobLvl == 4) suffix = " [SA]";
					break;
				case 8:
					suffix = " [BD]";
					break;
				case 9:
					suffix = " [Ex]";
					break;
				default:
					break;
			}
			// if we haven't already added a suffix, check job level exclusively
			if (suffix == null && 
					jobLvl == 10) suffix = " [M]";
		}
		// add suffix if we calculated one
		if (suffix != null) sb.append(suffix);
		
		// now append status code if appropriate.
		addUserStatusCode(sb,user.getStatusCode());
	}

	/**
	 * Adds user status code identifier for certain user status codes.
	 * @param sb
	 * @param statCd
	 */
	protected void addUserStatusCode(StringBuilder sb, String statCd) {
		if (statCd.equalsIgnoreCase(Status.COMPLIMENTARY.getCode()) ||
				statCd.equalsIgnoreCase(Status.UPDATES.getCode())) {
			sb.append(" ").append(statCd);
		} else if (statCd.equalsIgnoreCase(Status.EXTRA.getCode())) {
			sb.append(" ").append(Status.ACTIVE.getCode());
		}
	}

	/**
	 * Helper method for adding a row.
	 * @param key
	 * @param value
	 * @return
	 */
	protected Map<String,Object> addRow(String key, Object value) {
		Map<String,Object> row = new HashMap<>();
		row.put(key, value);
		return row;
	}
	
	/**
	 * builds the header map for the excel report
	 * @return
	 */
	protected HashMap<String, String> getHeader() {
		HashMap<String, String> headerMap = new LinkedHashMap<>();
		headerMap.put(ROW,"");
		return headerMap;
	}

}
