package com.biomed.smarttrak.admin;

import javax.servlet.http.HttpServletResponse;

//WC custom
import com.biomed.smarttrak.admin.report.AccountReportVO;
import com.biomed.smarttrak.admin.report.AccountsReportAction;
import com.biomed.smarttrak.admin.report.CompanySegmentsReportAction;
import com.biomed.smarttrak.admin.report.CompanySegmentsReportVO;
import com.biomed.smarttrak.admin.report.SupportReportAction;
import com.biomed.smarttrak.admin.report.SupportReportVO;
import com.biomed.smarttrak.admin.report.UserActivityAction;
import com.biomed.smarttrak.admin.report.UserActivityReportVO;
import com.biomed.smarttrak.admin.report.UserListReportAction;
import com.biomed.smarttrak.admin.report.UserListReportVO;
import com.biomed.smarttrak.admin.report.UserPermissionsReportAction;
import com.biomed.smarttrak.admin.report.UserPermissionsReportVO;
import com.biomed.smarttrak.admin.report.UserUtilizationDailyRollupReportVO;
import com.biomed.smarttrak.admin.report.UserUtilizationMonthlyRollupReportVO;
import com.biomed.smarttrak.admin.report.UserUtilizationReportAction;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.common.http.CookieUtil;
import com.siliconmtn.util.StringUtil;

// WebCrescendo
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
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
		USAGE_ROLLUP_DAILY,
		USAGE_ROLLUP_MONTHLY,
		SUPPORT
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
		boolean doRedirect = true;
		switch (rType) {
			case ACCOUNT_REPORT:
				rpt = generateAccountReport(req);
				doRedirect = false;
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
			case USAGE_ROLLUP_DAILY:
				rpt = generateUserUtilizationReport(req,true);
				break;
			case USAGE_ROLLUP_MONTHLY:
				rpt = generateUserUtilizationReport(req,false);
				break;
			case SUPPORT:
				rpt = generateSupportReport(req);
				break;
			default:
				break;
		}

		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, doRedirect);
		req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
		
		//delete the 'waiting' cookie on the response, so the loading icon disappears
		HttpServletResponse resp = (HttpServletResponse) req.getAttribute(GlobalConfig.HTTP_RESPONSE);
		CookieUtil.add(resp, "reportLoadingCookie", "", "/", 0);
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
		AccountReportVO rpt = new AccountReportVO();
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		rpt.setSite(site);
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
	 * @param isDaily
	 * @return
	 * @throws ActionException
	 */
	protected AbstractSBReportVO generateUserUtilizationReport(ActionRequest req, 
			boolean isDaily) throws ActionException {
		UserUtilizationReportAction uu = new UserUtilizationReportAction();
		uu.setDBConnection(dbConn);
		uu.setAttributes(getAttributes());
		req.setParameter(UserUtilizationReportAction.PARAM_IS_DAILY, Boolean.toString(isDaily));

		AbstractSBReportVO rpt;
		if (isDaily) {
			rpt = new UserUtilizationDailyRollupReportVO();
		} else {
			rpt = new UserUtilizationMonthlyRollupReportVO();
		}

		rpt.setData(uu.retrieveUserUtilization(req));
		return rpt;
	}

	/**
	 * Method calls out to get support report data and then builds a report.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	protected AbstractSBReportVO generateSupportReport(ActionRequest req) throws ActionException {
		SupportReportAction sra = new SupportReportAction();
		sra.setDBConnection(dbConn);
		sra.setAttributes(getAttributes());

		AbstractSBReportVO rpt = new SupportReportVO();
		rpt.setData(sra.retrieveSupportData(req));
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
