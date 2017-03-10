package com.biomed.smarttrak.admin;

//WC custom
import com.biomed.smarttrak.admin.report.AccountReportVO;
import com.biomed.smarttrak.admin.report.AccountsReportAction;
import com.biomed.smarttrak.admin.report.CompanySegmentsReportAction;
import com.biomed.smarttrak.admin.report.CompanySegmentsReportVO;
import com.biomed.smarttrak.admin.report.UserActivityAction;
import com.biomed.smarttrak.admin.report.UserActivityReportVO;
import com.biomed.smarttrak.admin.report.UserListReportAction;
import com.biomed.smarttrak.admin.report.UserListReportVO;
import com.biomed.smarttrak.admin.report.UserPermissionsReportAction;
import com.biomed.smarttrak.admin.report.UserPermissionsReportVO;
import com.biomed.smarttrak.admin.report.UserUtilizationDailyRollupReportVO;
import com.biomed.smarttrak.admin.report.UserUtilizationMonthlyRollupReportVO;
import com.biomed.smarttrak.admin.report.UserUtilizationReportAction;
import com.biomed.smarttrak.admin.report.UserUtilizationReportAction.UtilizationReportType;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;

// WebCrescendo
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: ReportFacadeAction.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Feb 22, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class ReportFacadeAction extends SBActionAdapter {

	public enum ReportType {
		ACCOUNT_REPORT,
		ACTIVITY_LOG,
		COMPANY_SEGMENTS,
		USER_LIST,
		USER_PERMISSIONS,
		UTILIZATION,
	}
	
	/**
	* Constructor
	*/
	public ReportFacadeAction() {
		super();
	}

	/**
	* Constructor
	*/
	public ReportFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (! req.hasParameter("reportType")) return;

		ReportType rType = checkReportType(req.getParameter("reportType"));
		AbstractSBReportVO rpt = null;

		switch (rType) {
			case ACCOUNT_REPORT:
				rpt = generateAccountReport(req);
				break;
			case ACTIVITY_LOG:
				rpt = generateActivityLogReport(req);
				break;
			case COMPANY_SEGMENTS:
				rpt = generateCompanySegmentsReport(req);
				break;
			case USER_LIST:
				rpt = generateUserListReport(req);
				break;
			case USER_PERMISSIONS:
				rpt = generateUserPermissionsReport(req);
				break;
			case UTILIZATION:
				rpt = generateUserUtilizationReport(req);
				break;
			default:
				break;
		}

		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
		req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
	}
	
	/**
	 * Generates the Account report.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	protected AbstractSBReportVO generateAccountReport(ActionRequest req) 
			throws ActionException {
		log.debug("generateAccountReport...");
		AccountsReportAction ara = new AccountsReportAction();
		ara.setDBConnection(dbConn);
		ara.setAttributes(getAttributes());
		AbstractSBReportVO rpt = new AccountReportVO();
		rpt.setData(ara.retrieveAccountsList(req));
		return rpt;
	}
	
	/**
	 * Generates the activity log report report.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	protected AbstractSBReportVO generateActivityLogReport(ActionRequest req) 
			throws ActionException {
		log.debug("generateActivityLogReport...");
		UserActivityAction uaa = new UserActivityAction();
		uaa.setDBConnection(dbConn);
		uaa.setAttributes(getAttributes());
		AbstractSBReportVO rpt = new UserActivityReportVO();
		rpt.setData(uaa.retrieveUserActivity(req));
		return rpt;
	}
	
	/**
	 * Generates the company segment report.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	protected AbstractSBReportVO generateCompanySegmentsReport(ActionRequest req) 
			throws ActionException {
		log.debug("generateCompanySegmentsReport...");
		CompanySegmentsReportAction csra = new CompanySegmentsReportAction(); 
		csra.setDBConnection(dbConn);
		csra.setAttributes(getAttributes());
		AbstractSBReportVO rpt = new CompanySegmentsReportVO();
		rpt.setData(csra.retrieveCompanySegments(req));
		return rpt;
	}

	/**
	 * Generates the user list report
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	protected AbstractSBReportVO generateUserListReport(ActionRequest req) 
			throws ActionException {
		UserListReportAction ul = new UserListReportAction();
		ul.setDBConnection(dbConn);
		ul.setAttributes(getAttributes());
		
		AbstractSBReportVO rpt = new UserListReportVO();
		rpt.setData(ul.retrieveUserList(req));
		return rpt;
		
	}
	
	/**
	 * Generates the user permissions report
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	protected AbstractSBReportVO generateUserPermissionsReport(ActionRequest req) 
			throws ActionException {
		UserPermissionsReportAction upra = new UserPermissionsReportAction();
		upra.setDBConnection(dbConn);
		upra.setAttributes(getAttributes());
		
		AbstractSBReportVO rpt = new UserPermissionsReportVO();
		rpt.setData(upra.retrieveUserPermissions(req));
		return rpt;

	}
	
	/**
	 * Generates the user utilization roll-up report.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	protected AbstractSBReportVO generateUserUtilizationReport(ActionRequest req) 
			throws ActionException {
		UserUtilizationReportAction uu = new UserUtilizationReportAction();
		uu.setDBConnection(dbConn);
		uu.setAttributes(getAttributes());
		
		String uReportType = StringUtil.checkVal(req.getParameter("utilizationReportType")).toUpperCase();
		UtilizationReportType urt = null;
		try {
			urt = UtilizationReportType.valueOf(uReportType);
		} catch (Exception e) {
			urt = UtilizationReportType.DAYS_365;
		}
		
		AbstractSBReportVO rpt;
		
		switch(urt){
			case DAYS_14:
			case DAYS_90:
				rpt = new UserUtilizationDailyRollupReportVO();
				break;
			default:
				rpt = new UserUtilizationMonthlyRollupReportVO();
				break;
		}
		
		rpt.addAttributes(UserUtilizationReportAction.ATTRIB_REPORT_SUFFIX, urt.getReportSuffix());
		rpt.setData(uu.retrieveUserUtilization(req));
		return rpt;
	}
	
	/**
	 * Converts a String to the equivalent ReportType.
	 * @param reportType
	 * @return
	 * @throws ActionException
	 */
	protected ReportType checkReportType(String reportType) throws ActionException {
		try {
			return ReportType.valueOf(StringUtil.checkVal(reportType).toUpperCase());
		} catch (Exception e) {
			throw new ActionException("Unknown report type, " + reportType);
		}
	}
	
	
}
