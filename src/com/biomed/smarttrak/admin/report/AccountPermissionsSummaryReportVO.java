package com.biomed.smarttrak.admin.report;

// Java 8
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.vo.AccountVO.Type;
//WC custom
import com.biomed.smarttrak.vo.PermissionVO;
//SMTBaseLibs
import com.siliconmtn.data.Node;
import com.siliconmtn.data.report.ExcelReport;
import com.siliconmtn.util.Convert;

// WebCrescendo
import com.smt.sitebuilder.action.AbstractSBReportVO;

/*****************************************************************************
 <p><b>Title</b>: AccountPermissionsSummaryReportVO.java</p>
 <p><b>Description: Gets the total number of full accounts for each permission type.</b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2019 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Eric Damschroder
 @version 1.0
 @since Aug 30, 2019
 <b>Changes:</b> 
 ***************************************************************************/
public class AccountPermissionsSummaryReportVO extends AbstractSBReportVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1335600957611516160L;
	private static final int MAX_DEPTH_LEVEL = 4;
	private static final String COLUMN_NAME_SPACER = " - ";

	private List<AccountUsersVO> accounts;
	private String reportTitle;
	
	private static final String NAME = "NAME";
	private static final String COUNT = "COUNT";
	private static final String PERCENTAGE = "PERCENTAGE";
	

	/**
	* Constructor
	*/
	public AccountPermissionsSummaryReportVO() {
        super();
        setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
        accounts = new ArrayList<>();
		this.reportTitle = "Account Permissions Summary Report";
	}
	

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		log.debug("generateReport...");
        setFileName(reportTitle.replace(' ', '-')+".xls");

		ExcelReport rpt = new ExcelReport(getHeader());
		rpt.setTitleCell(reportTitle);

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
		Map<String, Object> data = (Map<String, Object>) o;
		this.accounts = (List<AccountUsersVO>) data.get("accounts");
	}
	
	
	/**
	 * this method is used to generate the data rows of the excel sheet.
	 * @param rows
	 * @return
	 */
	private void generateDataRows(List<Map<String, Object>> rows) {
		int accountCount = getAccountCount();
		generateTopRows(rows, accountCount);
		generateSectionRows(rows, accountCount);
	}
	
	
	/**
	 * Generate non section rows
	 * @param rows
	 * @param accountCount
	 */
	private void generateTopRows(List<Map<String, Object>> rows, int accountCount) {
		Map<String,Object> row = new HashMap<>();
		row.put(NAME, "Count of Accounts");
		row.put(PERCENTAGE, accountCount);
		rows.add(row);

		int fdCount = 0;
		int gaCount = 0;
		for (AccountUsersVO account : accounts) {
			if (!Type.FULL.getId().equals(account.getTypeId()) || !"A".equals(account.getStatusNo())) continue;
			fdCount += account.getFdAuthFlg();
			gaCount += account.getGaAuthFlg();
				
		}
		
		row = new HashMap<>();
		row.put(NAME, "Has FD");
		row.put(COUNT, fdCount);
		row.put(PERCENTAGE, fdCount * 100/accountCount + "%");
		rows.add(row);
		
		row = new HashMap<>();
		row.put(NAME, "Has GA");
		row.put(COUNT, gaCount);
		row.put(PERCENTAGE, gaCount * 100/accountCount + "%");
		rows.add(row);
		
	}
	
	
	/**
	 * Build the section counts rows
	 * @param rows
	 * @param accountCount
	 */
	private void generateSectionRows(List<Map<String, Object>> rows, int accountCount) {
		Map<String,Object> row = new HashMap<>();
		String sec1 = "";
		String sec2 = "";
		for (Node n : accounts.get(0).getPermissions().getPreorderList()) {
			if (n.getDepthLevel() > MAX_DEPTH_LEVEL) continue;
			
			if (n.getDepthLevel() == 2) {
				sec1 = n.getNodeName() + COLUMN_NAME_SPACER; 
			} else if (n.getDepthLevel() == 3) {
				sec2 = n.getNodeName() + COLUMN_NAME_SPACER;
			} else {
				row.put(NAME,  sec1 + sec2 + n.getNodeName());
				int count = getSectionCount(n.getNodeId());
				row.put(COUNT, count);
				row.put(PERCENTAGE, count * 100/accountCount + "%");
				rows.add(row);
				row = new HashMap<>();
			}
		}
	}
	
	
	/**
	 * Get total number of valid accounts
	 * @return
	 */
	private int getAccountCount() {
		int count = 0;
		for (AccountUsersVO account : accounts) {
			if (!Type.FULL.getId().equals(account.getTypeId()) || !"A".equals(account.getStatusNo())) continue;
			
			count++;
		}
		return count;
	}
	
	
	/**
	 * Get the number of accounts with the supplied permission
	 * @param nodeId
	 * @return
	 */
	private int getSectionCount(String nodeId) {
		int count = 0;
		for (AccountUsersVO account : accounts) {
			if (!Type.FULL.getId().equals(account.getTypeId()) || !"A".equals(account.getStatusNo())) continue;
			
			PermissionVO perm = (PermissionVO)account.getPermissions().findNode(nodeId).getUserObject();
			if (perm.isBrowseAuth()) count++;
		}
		return count;
	}
	

	/**
	 * Compares account's flag value to user's flag value.
	 * @param acctFlag
	 * @param userFlag
	 * @return
	 */
	protected boolean checkFlag(int acctFlag, int userFlag) {
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
		headerMap.put(NAME, "");
		headerMap.put(COUNT, "");
		headerMap.put(PERCENTAGE, "");
		
		return headerMap;
	}
	
}
