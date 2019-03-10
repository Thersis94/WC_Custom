package com.biomed.smarttrak.admin.report;

// Java 8
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

//WC custom
import com.biomed.smarttrak.util.SmarttrakTree;
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
	private static final int MAX_DEPTH_LEVEL = 4;
	private static final String COLUMN_NAME_SPACER = " - ";

	private List<AccountUsersVO> accounts;
	private boolean showUsers;
	private String reportTitle;
	private static final String ACCT_ID = "ACCT_ID";
	private static final String ACCT_NM = "ACCT_NM";
	private static final String USER_ID = "USER_ID";
	private static final String EMAIL = "EMAIL";
	private static final String FULL_NM = "FULL_NM";
	private static final String HAS_FD = "HAS_FD";
	private static final String HAS_GA = "HAS_GA";
	private static final String HUBSPOT = "HUBSPOT";
	private static final String ACCT_START = "ACCT_START";
	private static final String ACCT_EXP = "ACCT_EXP";
	private static final String ACCT_STAT = " ACCT_STA";
	private static final String ACCT_TYPE = "ACCT_TYPE";
	private static final String ACCT_CLASS = "ACCT_CLASS";
	private static final String USER_LIC = "USER_LIC";
	

	/**
	* Constructor
	*/
	public UserPermissionsReportVO() {
        super();
        setContentType("application/vnd.ms-excel");
        isHeaderAttachment(Boolean.TRUE);
        accounts = new ArrayList<>();
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
		this.showUsers = (boolean) data.get("showUsers");
		if (this.showUsers) {
			this.reportTitle = "User Permissions Report";
		} else {
			this.reportTitle = "Account Permissions Report";
		}
		
	}
	
	/**
	 * this method is used to generate the data rows of the excel sheet.
	 * @param rows
	 * @return
	 */
	private void generateDataRows(List<Map<String, Object>> rows) {
				
		// loop the account map
		for (AccountUsersVO acct : accounts) {

			// user vals
			Map<String,Object> row;
			
			// loop account users
			for (UserVO user : acct.getUsers()) {
				row = new HashMap<>();
				row.put(ACCT_ID, acct.getAccountId());
				row.put(ACCT_NM, acct.getAccountName());
				if (showUsers) {
					row.put(USER_ID, user.getUserId());
					row.put(EMAIL,user.getEmailAddress());
					row.put(FULL_NM, user.getFullName());
					row.put(USER_LIC, user.getLicenseName());
				}
				row.put(HAS_FD, checkFlag(acct.getFdAuthFlg(),user.getFdAuthFlg())? "True":"");
				row.put(HAS_GA, checkFlag(acct.getGaAuthFlg(),user.getGaAuthFlg())? "True":"");
				// loop hierarchy.
				addPermissions(row, acct.getPermissions());
				row.put(HUBSPOT, buildHubSpot(user, acct));
				row.put(ACCT_START, acct.getStartDate());
				row.put(ACCT_EXP, acct.getExpirationDate());
				row.put(ACCT_STAT, acct.getStatusName());
				row.put(ACCT_TYPE, acct.getTypeName());
				row.put(ACCT_CLASS, acct.getClassificationName());
				rows.add(row);
				
				// Only build one row if we aren't showing user data
				if (!showUsers) break;
			}
		}

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
		headerMap.put(ACCT_ID,"Account ID");
		headerMap.put(ACCT_NM,"Account Name");
		if (showUsers) {
			headerMap.put(USER_ID,"User ID");
			headerMap.put(EMAIL,"Username");
			headerMap.put(FULL_NM,"User Full Name");
			headerMap.put(USER_LIC,"User License Type");
		}
		headerMap.put(HAS_FD,"Has FD");
		headerMap.put(HAS_GA,"Has GA");
		headerMap.put(HAS_GA,"Has GA");
		// loop the first account's SmarttrakTree to get the hierarchy
		if (! accounts.isEmpty()) {
			addSectionColumnHeaders(headerMap,accounts.get(0).getPermissions());
		}
		headerMap.put(HUBSPOT, "HubSpot Load");
		headerMap.put(ACCT_START,"Account Start Date");
		headerMap.put(ACCT_EXP,"Account Expiration Date");
		headerMap.put(ACCT_STAT,"Account Status");
		headerMap.put(ACCT_TYPE,"Account Type");
		headerMap.put(ACCT_CLASS,"Account Classification");
		return headerMap;
	}
	
	/**
	 * Formats the section column headers in the main header.  Section column header
	 * names are "Level2 Name - Level3 Name - Level4 Name".
	 * @param headerMap
	 * @param sTree
	 */
	protected void addSectionColumnHeaders(Map<String,String> headerMap, SmarttrakTree sTree) {
		String colStub2 = "";
		String colStub3 = "";
		for (Node n : sTree.getPreorderList()) {
			if (n.getDepthLevel() > MAX_DEPTH_LEVEL) continue;
			switch(n.getDepthLevel()) {
				case 2:
					colStub2 = n.getNodeName();
					break;
				case 3:
					colStub3 = colStub2 + COLUMN_NAME_SPACER + n.getNodeName();
					break;
				case 4:
					headerMap.put(n.getNodeId(), colStub3 + COLUMN_NAME_SPACER + n.getNodeName());
					break;
				default:
					break;
			}
		}
	}

	
	/**
	 * Create the hubspot field ased on the top level groups, gap analysis, and financial dashboard
	 * @param user
	 * @param acct
	 * @return
	 */
	private String buildHubSpot(UserVO user, AccountUsersVO acct) {
		Set<String> load = new HashSet<>();
		String group = "";
		for (Node n : acct.getPermissions().getPreorderList()) {
			if (n.getDepthLevel() == 2) group = n.getNodeName();
			if (n.getDepthLevel() != MAX_DEPTH_LEVEL) continue;
			// Permissions are authoritative at level 4 so we use the level 4 perm
			PermissionVO perm = (PermissionVO)n.getUserObject();
			if (perm.isBrowseAuth())
				load.add(group);
		}
		

		if (checkFlag(acct.getFdAuthFlg(),user.getFdAuthFlg()))
			load.add("FD");
		
		if (checkFlag(acct.getGaAuthFlg(),user.getGaAuthFlg()))
			load.add("GA");
		
		if (load.isEmpty()) return "";
		
		return StringUtils.join(load, ", ");
	}
	
	/**
	 * Adds section permission values to each section column.
	 * @param row
	 * @param sTree
	 */
	protected void addPermissions(Map<String,Object>row, SmarttrakTree sTree) {
		for (Node n : sTree.getPreorderList()) {
			if (n.getDepthLevel() != MAX_DEPTH_LEVEL) continue;
			// Permissions are authoritative at level 4 so we use the level 4 perm
			PermissionVO perm = (PermissionVO)n.getUserObject();
			row.put(n.getNodeId(), perm.isBrowseAuth()? "True": "");
		}
	}
	
}
