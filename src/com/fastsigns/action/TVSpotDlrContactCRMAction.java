package com.fastsigns.action;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.servlet.http.HttpSession;

import com.fastsigns.security.FastsignsSessVO;
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
import com.smt.sitebuilder.action.contact.SubmittalDataAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;
import com.smt.sitebuilder.security.SecurityController;

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
	
	public TVSpotDlrContactCRMAction() {
	}

	public TVSpotDlrContactCRMAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	/**
	 * retrieves the contact submissions to display as interactive report data in WebEdit.
	 * Similar to SAF/RAQ Reporting.
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		HttpSession ses = req.getSession();
		SBUserRole role = (SBUserRole) ses.getAttribute(Constants.ROLE_DATA);
		String franchiseId = (String) ses.getAttribute(FastsignsSessVO.WEBEDIT_FRANCHISE_ID);
		
		//security checkpoint, only admins can get data without a franchiseId.
		if (franchiseId == null && (role == null || role.getRoleLevel() < SecurityController.ADMIN_ROLE_LEVEL)) 
			return;
				
		//allow admins a way to remove the franchise restriction
		if (req.hasParameter("showAll")) 
			ses.removeAttribute(FastsignsSessVO.WEBEDIT_FRANCHISE_ID);
				
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
		if ("excel".equalsIgnoreCase(req.getParameter("type"))) {
			AbstractSBReportVO rpt = new TVSpotReportVO();
			rpt.setData(cdc);
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
		req.setParameter("dealerLocationId", franId);
		req.setParameter("orderBy", "dateDesc");
		req.setParameter("contactId", mod.getAttribute(ModuleVO.ATTRIBUTE_1) + "|TV Spot CRM Report");

		if (req.hasParameter("contactSubmittalId")) return;
		
		//shift today's date by a negative integer to find the starting point for this report.
		//default is "past 24 hours"
		int dayShift = -1;
		if (req.hasParameter("range"))
			dayShift = Integer.valueOf(req.getParameter("range"));
		
		log.debug("shift=" + dayShift);
		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_YEAR, dayShift);
		req.setParameter("startDate", Convert.formatDate(c.getTime(), Convert.DATE_SLASH_PATTERN));
		
		//default status filter when no other filters are passed
		if (req.getQueryString() == null) 
			req.setParameter("status", TVSpotUtil.Status.initiated.toString());
		
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
		boolean useStatus = req.hasParameter("status") && !(req.getParameter("status").equals("all"));
		boolean useCustNm = req.hasParameter("customerName");
//		boolean useCompNm = req.hasParameter("companyName");
		
		//loop the submissions and remove any that don't meet our criteria
		for (ContactDataModuleVO vo : cdc.getData()) {
			if (useStatus) {
				String stage = StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.status.id()));
				if (! stage.equalsIgnoreCase(req.getParameter("status"))) continue;
			}
			
			if (useCustNm) {
				String custNm = StringUtil.checkVal(vo.getFullName()).toLowerCase();
				if (! custNm.contains(req.getParameter("customerName").toLowerCase())) continue;
			}
			
//			if (useCompNm) {
//				String compNm = StringUtil.checkVal(vo.getExtData().get(TVSpotUtil.ContactField.companyNm.id())).toLowerCase();
//				if (! compNm.contains(req.getParameter("companyName").toLowerCase())) continue;
//			}
			
			//passed all tests, add this record to the new List
			newData.add(vo);
		}
		
		//replace the old list of submissions with the filtered ones
		cdc.setData(newData);
		
		return cdc;
	}

	
	/**
	 * updates a single contactSubmittal's values for status and notes (contact fields)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		String csId = req.getParameter("contactSubmittalId");
		SubmittalDataAction sda = new SubmittalDataAction(actionInit);
		sda.setDBConnection(dbConn);
		
		try {
			//save the status
			sda.updateField(req.getParameter("transactionStatus"), csId, TVSpotUtil.ContactField.status.id());
			
			//save the notes
			sda.updateField(req.getParameter("transactionNotes"), csId, TVSpotUtil.ContactField.transactionNotes.id());
			
			//save saleAmount
			sda.updateField(req.getParameter("saleAmount"), csId, TVSpotUtil.ContactField.saleAmount.id());
			
		} catch (SQLException sqle) {
			log.error("could not update contact fields", sqle);
		}
		
		//redirect the browser
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		StringBuilder url = new StringBuilder(page.getRequestURI());
		//replace the search status with the one we just changed 'this' record to, so it's still visible to the user and they can see it was updated.
		url.append("?status=").append(req.getParameter("transactionStatus"));
		if (req.hasParameter("searchRange")) url.append("&range=").append(req.getParameter("searchRange"));
		if (req.hasParameter("searchCustomerName")) url.append("&customerName=").append(req.getParameter("searchCustomerName"));
//		if (req.hasParameter("searchCompanyName")) url.append("&companyName=").append(req.getParameter("searchCompanyName"));
		
		super.sendRedirect(url.toString(), null, req);
	}
}
