package com.biomed.smarttrak.admin;

// WC custom
import com.biomed.smarttrak.admin.report.UserUtilizationReportAction;
import com.biomed.smarttrak.admin.report.UserUtilizationReportVO;

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
		USER_UTILIZATION
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
		
		String reportType = StringUtil.checkVal(req.getParameter("reportType"));
		ReportType rType = checkReportType(reportType);
		
		AbstractSBReportVO rpt = null;
		
		switch (rType) {
			case USER_UTILIZATION:
				//do utilization report.
				rpt = generateUserUtilizationReport(req);
			break;
		}

		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
		req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
	}
	
	/**
	 * Generates the user utilization roll-up report.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	protected AbstractSBReportVO generateUserUtilizationReport(ActionRequest req) 
			throws ActionException {
		UserUtilizationReportAction uu = new UserUtilizationReportAction(actionInit);
		uu.setDBConnection(dbConn);
		AbstractSBReportVO rpt = new UserUtilizationReportVO();
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
			return ReportType.valueOf(reportType);
		} catch (Exception e) {
			throw new ActionException("Unknown report type, " + reportType);
		}
	}
	
	
}
