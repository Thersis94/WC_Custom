package com.ansmed.sb.report;

//SMT Base Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// SB Libs
import com.smt.sitebuilder.common.SiteBuilderUtil;

// SB ANS Libs
import com.ansmed.sb.report.ImplanterReportAction;
import com.ansmed.sb.patient.MDJournalAction;
import com.ansmed.sb.physician.SurgeonSearchAction;
import com.ansmed.sb.report.PhysicianReport;
import com.ansmed.sb.report.SummaryReportAction;


public class ReportFacadeAction extends SimpleActionAdapter {
    SiteBuilderUtil util = null;
    public static final String ANS_REPORT_MESSAGE = "ansReportMessage";
    
	/**
	 * 
	 */
	public ReportFacadeAction() {
        super();
        util = new SiteBuilderUtil();
	}

	/**
	 * @param actionInit
	 */
	public ReportFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
		util = new SiteBuilderUtil();
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.info("ANS Report Facade retrieve...");
		
		int reportId = 0;
		if (StringUtil.checkVal(req.getParameter("reportId")).length() > 0) {
			reportId = Convert.formatInteger(req.getParameter("reportId")).intValue();
		}
		boolean submitted = Convert.formatBoolean(req.getParameter("searchSubmitted")).booleanValue();
		log.debug("reportId: " + reportId);
		log.debug("searchSubmitted: " + submitted);
		SMTActionInterface ai = null;
		
		// If form is submitted, process.
		if (submitted && reportId > 0) {
			switch(reportId) {
			case 1: // Implanter report
				ai = new ImplanterReportAction(this.actionInit);
				break;
			case 2: // Physician Summary Report
				ai = new PhysicianReport(this.actionInit);
				break;	
			case 3: // Summary report
				ai = new SummaryReportAction(this.actionInit);
				break;
			case 4: //MD Journal report
				ai = new MDJournalAction(this.actionInit);
				break;
			case 5: //Physician Individual(SWOT) report
				ai = new PhysicianIndividualReportAction(this.actionInit);
				break;
			case 6: //Resource Utilization report
				ai = new ResourceReportAction(this.actionInit);
				break;
			case 7:
				ai = new FellowsReportAction(this.actionInit);
				break;
			case 8:
				ai = new TerritorySummaryReportAction(this.actionInit);
				break;
			case 9:
				ai = new RankCompetitorReportAction(this.actionInit);
				break;
			case 10:
				ai = new RankReportAction(this.actionInit);
				break;
			case 11:
				ai = new RevenueReportAction(this.actionInit);
				break;
			default:
				//Default behavior is to return a surgeon list.
				ai = new SurgeonSearchAction(this.actionInit);
				break;
			}
			
			ai.setAttributes(this.attributes);
			ai.setDBConnection(dbConn);
			ai.retrieve(req); 
		}
		
		// Set the reportId value to drive the facade view.
		req.setParameter("reportId", StringUtil.checkVal(req.getParameter("reportId")));
		log.debug("Setting reportID:" + StringUtil.checkVal(req.getParameter("reportId")));
		
	}
}
