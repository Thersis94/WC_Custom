 package com.ansmed.sb.report;

// JDK 1.5.0
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;

// SB Libs
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

// SB ANS Libs
import com.ansmed.sb.physician.ResourceAction;
import com.ansmed.sb.physician.ResourceVO;

/****************************************************************************
 * <b>Title</b>: ResourceReportAction.java<p/>
 * <b>Description: </b> Returns resource report data for surgeon.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since May 04, 2009
 ****************************************************************************/
public class ResourceReportAction extends SBActionAdapter {
	
	/**
	 * 
	 */
	public ResourceReportAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public ResourceReportAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	public void retrieve(SMTServletRequest req) throws ActionException {
		
		String report = StringUtil.checkVal(req.getParameter("report"));
		String surgeonId = StringUtil.checkVal(req.getParameter("surgeonId"));
		log.debug("report: " + report);
		log.debug("surgeon_id: " + surgeonId);
		String fileName = "ResourceSummaryReport.";
		if (report.equalsIgnoreCase("individual")) {
			fileName = "ResourceIndividualDetailReport.";
		}
		
		SMTActionInterface sai = new ResourceAction(this.actionInit);
		sai.setAttributes(this.attributes);
		sai.setDBConnection(dbConn);
		sai.retrieve(req);
		
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		Map<String,List<ResourceVO>> rvo = new HashMap<String,List<ResourceVO>>();
		if (report.equalsIgnoreCase("individual")) {
			rvo.put("individual",(List<ResourceVO>)mod.getActionData());
		} else {
			rvo.put("summary",(List<ResourceVO>)mod.getActionData());
		}
		
		mod.setActionData(rvo);
		AbstractSBReportVO rpt = new ResourceReportVO();
		rpt.setData(mod.getActionData());
		rpt.setFileName(fileName + req.getParameter("reportType", "html"));
		log.debug("Mime Type: " + rpt.getContentType());
		
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
		req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
	}
	
}
