 package com.ansmed.sb.report;

// JDK 1.5.0
import java.util.List;

// SMT Base Libs
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.action.ActionRequest;

// SB Libs
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

// SB ANS Libs
import com.ansmed.sb.physician.BusinessAssessmentAction;
import com.ansmed.sb.physician.BusAssessVO;
import com.ansmed.sb.physician.BusGoalVO;

/****************************************************************************
 * <b>Title</b>: PhysicianIndividualReportAction.java<p/>
 * <b>Description: </b> Returns rank and business plan data for surgeon.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Apr. 30, 2009
 ****************************************************************************/
public class PhysicianIndividualReportAction extends SBActionAdapter {
	
	/**
	 * 
	 */
	public PhysicianIndividualReportAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public PhysicianIndividualReportAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	public void retrieve(ActionRequest req) throws ActionException {
		
		SMTActionInterface sai = new BusinessAssessmentAction(this.actionInit);
		sai.setAttributes(this.attributes);
		sai.setDBConnection(dbConn);
		sai.retrieve(req);
		
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		List<BusAssessVO> bavo = (List<BusAssessVO>)mod.getActionData();
		List<BusGoalVO> bago = (List<BusGoalVO>)mod.getAttribute(BusinessAssessmentAction.BUSINESS_GOALS);
		log.debug("bago size: " + bago.size());
		
		PhysicianIndividualVO pivo = new PhysicianIndividualVO();
		pivo.setSwot(bavo);
		pivo.setObjectives(bago);
		
		mod.setActionData(pivo);
		
		AbstractSBReportVO rpt = new PhysicianIndividualReport();
		rpt.setData(mod.getActionData());
		rpt.setFileName("PhysicianSWOTReport." + req.getParameter("reportType", "html"));
		log.debug("Mime Type: " + rpt.getContentType());
		
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
		req.setAttribute(Constants.BINARY_DOCUMENT, rpt);

	}
	
	
}
