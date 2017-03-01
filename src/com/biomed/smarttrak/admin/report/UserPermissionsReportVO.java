package com.biomed.smarttrak.admin.report;

// Java 8
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.util.SmarttrakTree;
// WC custom
import com.biomed.smarttrak.vo.AccountVO;
import com.biomed.smarttrak.vo.PermissionVO;
import com.biomed.smarttrak.vo.UserVO;

//SMTBaseLibs
import com.siliconmtn.data.Node;
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.util.Convert;

// WebCrescendo
import com.smt.sitebuilder.action.AbstractSBReportVO;

/*****************************************************************************
 <p><b>Title</b>: UserPermissionsReportVO.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Mar 01, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class UserPermissionsReportVO extends AbstractSBReportVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1335600957611516160L;
	
	private List<AccountPermissionsVO> accounts;
	private static final String REPORT_TITLE = "User Permissions Report";
	private static final String ACCT_ID = "ACCT_ID";
	private static final String ACCT_NM = "ACCT_NM";
	private static final String USER_ID = "USER_ID";
	private static final String EMAIL = "EMAIL";
	private static final String FULL_NM = "FULL_NM";
	private static final String HAS_FD = "HAS_FD";
	private static final String HAS_GA = "HAS_GA";

	/**
	* Constructor
	*/
	public UserPermissionsReportVO() {
        super();
        setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
        setFileName(REPORT_TITLE.replace(' ', '-')+".xls");
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
		this.accounts =  (List<AccountPermissionsVO>) o;
	}
	
	/**
	 * this method is used to generate the data rows of the excel sheet.
	 * @param rows
	 * @return
	 */
	private List<Map<String, Object>> generateDataRows(
			List<Map<String, Object>> rows) {
				
		// loop the account map
		for (AccountPermissionsVO acct : accounts) {

			AccountVO a = acct.getAccount();

			// user vals
			Map<String,Object> row;
			
			// loop account users
			for (UserVO user : acct.getUsers()) {
				row = new HashMap<>();
				row.put(ACCT_ID, a.getAccountId());
				row.put(ACCT_NM, a.getAccountName());
				row.put(USER_ID, user.getUserId());
				row.put(EMAIL,user.getEmailAddress());
				row.put(FULL_NM, user.getFullName());
				row.put(HAS_FD, checkFlag(a.getFdAuthFlg(),user.getFdAuthFlg()));
				row.put(HAS_GA, checkFlag(a.getGaAuthFlg(),user.getGaAuthFlg()));
				// loop hierarchy.
				SmarttrakTree sTree = acct.getPermissions();
				for (Node n : sTree.getPreorderList()) {
					PermissionVO acctPerm = (PermissionVO)n.getUserObject();
					row.put(n.getNodeId(), acctPerm.isSelected());
				}
				rows.add(row);
			}
		}
		
		return rows;
	}

	/**
	 * Compares account's flag value to user's flag value.
	 * @param acctFlag
	 * @param userFlag
	 * @return
	 */
	protected Boolean checkFlag(int acctFlag, int userFlag) {
		if (userFlag == 0) {
			return Convert.formatBoolean(acctFlag);
		} else {
			return Convert.formatBoolean(userFlag);
		}
	}
	
	/**
	 * builds the header map for the excel report
	 * @return
	 */
	protected HashMap<String, String> getHeader() {
		HashMap<String, String> headerMap = new LinkedHashMap<>();
		headerMap.put(ACCT_ID,"Account ID");
		headerMap.put(ACCT_NM,"Account Name");
		headerMap.put(USER_ID,"User ID");
		headerMap.put(EMAIL,"Username");
		headerMap.put(FULL_NM,"User Full Name");
		headerMap.put(HAS_FD,"Has FD");
		headerMap.put(HAS_GA,"Has GA");
		// loop the first account's SmarttrakTree to get the hierarchy
		if (! accounts.isEmpty()) {
			SmarttrakTree tree = accounts.get(0).getPermissions();
			for (Node n : tree.getPreorderList()) {
				headerMap.put(n.getNodeId(), n.getNodeName());
			}
		}

		return headerMap;
	} 
	
}