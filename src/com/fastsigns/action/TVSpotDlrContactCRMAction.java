package com.fastsigns.action;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.fastsigns.action.RequestAQuoteSTF.TransactionStep;
import com.fastsigns.action.franchise.CenterPageAction;
import com.fastsigns.action.saf.SAFConfig;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.contact.ContactDataAction;
import com.smt.sitebuilder.action.contact.ContactDataContainer;
import com.smt.sitebuilder.action.contact.ContactDataModuleVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.common.SiteVO;

/****************************************************************************
 * <b>Title</b>: TVSpotDlrContactCRMAction.java<p/>
 * <b>Description: Handles reporting and user-contact (CRM) for the TV Spot ads run in Q2 2014.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Feb 12, 2014
 ****************************************************************************/
public class TVSpotDlrContactCRMAction extends SimpleActionAdapter {
	
	private static final String TRANS_STEP_FIELD_ID = "";
		
	public TVSpotDlrContactCRMAction() {
	}

	public TVSpotDlrContactCRMAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		String franchiseId = CenterPageAction.getFranchiseId(req);
		if (franchiseId == null || franchiseId.length() == 0) return;
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		
		
		//setup data filters needed for data retrieval
		setupDataFilters(franchiseId, mod, req);

		//load the report data
		ContactDataAction cda = new ContactDataAction(this.actionInit);
		cda.setAttributes(attributes);
		cda.setDBConnection(dbConn);
		cda.update(req);
		mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		ContactDataContainer cdc = (ContactDataContainer) mod.getActionData();
		
		//if we're not getting a single record, apply some post-data-loading filters.
		if (!req.hasParameter("contactSubmittalId"))
			cdc = filterData(cdc, req);
		

		//drop to Excel report if desired
		if (cdc.getData().size() == 1 && "excel".equalsIgnoreCase(req.getParameter("type"))) {
			AbstractSBReportVO rpt = new SAFReportVO();
			rpt.setData(cdc.getData().get(0));
			req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
			req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
		}
		
	}
	
	
	/**
	 * add req params before we call ContactDataAction, to load the data set. 
	 * @param mod
	 * @param req
	 */
	private void setupDataFilters(String franId, ModuleVO mod, SMTServletRequest req) {
		//shift today's date by a negative integer to find the starting point for this report.
		//default is "past 24 hours"
		if (!req.hasParameter("contactSubmittalId")) {
			int dayShift = -1;
			if (req.hasParameter("range"))
				dayShift = Integer.valueOf(req.getParameter("range"));
			
			log.debug("shift=" + dayShift);
			Calendar c = Calendar.getInstance();
			c.add(Calendar.DAY_OF_YEAR, dayShift);
			req.setParameter("startDate", Convert.formatDate(c.getTime(), Convert.DATE_SLASH_PATTERN));
		}
		
		req.setParameter("dealerLocationId", franId);
		req.setParameter("orderBy", "dateDesc");
		req.setParameter("contactId", mod.getAttribute(ModuleVO.ATTRIBUTE_1) + "|TV Spot CRM Report");
	}
	
	
	/**
	 * post-filter the data after it's been loaded.  This allows us to filter the data
	 * against the form-fields.  (not supported by ContactDataAction)
	 * @param cdc
	 * @param filterType
	 * @return
	 */
	private ContactDataContainer filterData(ContactDataContainer cdc, SMTServletRequest req) {
		List<ContactDataModuleVO> newData = new ArrayList<ContactDataModuleVO>();
		
		//loop the submissions and remove any that don't meet our criteria
		for (ContactDataModuleVO vo : cdc.getData()) {
//			String stage = StringUtil.checkVal(vo.getExtData().get(safConfig.getTransactionStageFieldId()));
//			if (filterType.equals(complete) && stage.equalsIgnoreCase(complete)) {
//				newData.add(vo);
//			} else if (status.equals("inprogress") && !stage.equalsIgnoreCase(complete)) {
				newData.add(vo);
//			}
		}
		
		//replace the old list of submissions with the filtered ones
		cdc.setData(newData);
		
		return cdc;
	}
}
