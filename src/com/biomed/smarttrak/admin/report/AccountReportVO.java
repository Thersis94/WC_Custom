package com.biomed.smarttrak.admin.report;

// Java 8
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//WC custom
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.UserVO;
import com.biomed.smarttrak.vo.UserVO.RegistrationMap;
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
 @author groot
 @version 1.0
 @since Mar 7, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class AccountReportVO extends AbstractSBReportVO {

	/**
	 * 
	 */
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
        //setContentType("text/htm");
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
			addDivisions(rows,acct);
			totalSubscribers += acct.getTotalUsers();
			totalAdded += acct.getAddedCount();
			totalComplementary += acct.getComplementaryCount();
		}
		
		addSummaryRows(rows,activeAccounts,totalSubscribers,totalAdded,totalComplementary);
		return rows;
	}
	
	protected void addAccountRow(List<Map<String,Object>> rows, AccountUsersVO acct) {
		StringBuilder sb = new StringBuilder(75);
		sb.append(acct.getAccountName());
		int totUsers = acct.getTotalUsers() - 
				(acct.getAddedCount() + 
						acct.getComplementaryCount() + 
						acct.getUpdatesOnlyCount());
		if (totUsers > 0) 	sb.append(" (").append(totUsers).append(")");
		if (acct.getAddedCount() > 0) {
			sb.append(" ");
			sb.append(acct.getAddedCount());
			sb.append("A"); 
		}
		if (acct.getComplementaryCount() > 0) {
			sb.append(" ");
			sb.append(acct.getComplementaryCount());
			sb.append("C"); 
		}
		rows.add(addRow(ROW, sb.toString()));
		
	}

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

	protected void addAccountSegmentRows(List<Map<String,Object>> rows, AccountUsersVO acct) {
		SmarttrakTree tree = acct.getPermissions();
		StringBuilder sb = null;
		int level;
		int cnt = 0;
		for (Node seg : tree.getPreorderList()) {
			level = seg.getDepthLevel();
			if (level < 3 || level > 4) continue;
			switch (level) {
				case 3:
					appendSegmentRow(sb,rows);
					cnt = 1;
					sb = new StringBuilder(100);
					sb.append(seg.getNodeName());
				case 4:
					appendSegment(sb,seg.getNodeName(),cnt);
					cnt++;
					break;
			}
		}
	}
	
	protected void addSummaryRows(List<Map<String,Object>> rows, int totalAccounts, 
			int totalSubscribers, int totalAdded, int totalComplementary) {
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
		sb.append("Complementary Seats ");
		sb.append(totalComplementary);
		rows.add(addRow(ROW,sb.toString()));
	}
	
	protected void appendSegmentRow(StringBuilder sb, List<Map<String,Object>> rows) {
		if (sb != null) rows.add(addRow(ROW,sb.toString()));
	}
	
	protected void appendSegment(StringBuilder sb, String segName, int cnt) {
		if (cnt > 1) sb.append(",");
		sb.append(" ").append(segName);
	}
	
	protected StringBuilder checkSegment(List<Map<String,Object>> rows, StringBuilder sb,
			String prevId,String currId,String segName) {
		if (! currId.equals(prevId)) {
			// changed parent
			rows.add(addRow(ROW, sb.toString()));
			return new StringBuilder(100);
		} else {
			return sb;
		}
		
	}

	protected void addDivisions(List<Map<String,Object>> rows, AccountUsersVO acct, Map<String,String> divMap) {
		String divName;
		for (Map.Entry<String,List<UserVO>> division : acct.getDivisions().entrySet()) {
			divName = divMap.get(division.getKey());
			rows.add(addRow(ROW, StringUtil.checkVal(divName)));
			addDivisionUsers(rows,division.getValue());
		}

	}

	protected void addDivisionUsers(List<Map<String,Object>> rows, List<UserVO> users) {
		for (UserVO user : users) {
			StringBuilder sb = new StringBuilder(50);
			sb.append(user.getFullName());
			switch (user.getStatusCode()) {
				case "A":
				case "C":
				case "U":
					sb.append(user.getStatusCode());
					break;
			}
			rows.add(addRow(ROW,sb.toString()));
		}
	}

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
