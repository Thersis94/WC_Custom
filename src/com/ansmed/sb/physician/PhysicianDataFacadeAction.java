package com.ansmed.sb.physician;

//SMT Base Libs
import com.ansmed.sb.action.TransactionLoggingAction;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// SB Libs
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteBuilderUtil;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: PhysicianDataFacadeAction.java<p/>
 * <b>Description: </b> Facade action for managing physician data in 
 * the Phys db.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 1.0
 * @since September 22, 2007
 ****************************************************************************/
public class PhysicianDataFacadeAction extends SimpleActionAdapter {
    SiteBuilderUtil util = null;
    public static final String ANS_PHYSICIAN_MESSAGE = "ansPhysicianMessage";
    
	/**
	 * 
	 */
	public PhysicianDataFacadeAction() {
        super();
        util = new SiteBuilderUtil();
	}

	/**
	 * @param actionInit
	 */
	public PhysicianDataFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
		util = new SiteBuilderUtil();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("building Phys info");
		SMTActionInterface aac = null;
		String surgeonId = StringUtil.checkVal(req.getParameter("surgeonId"));
		String transType = null;
		
		// Build the redir url
		StringBuffer url = new StringBuffer();
		url.append(req.getRequestURI()).append("?");
		url.append("page=").append(StringUtil.checkVal(req.getParameter("page")));
		url.append("&order=").append(StringUtil.checkVal(req.getParameter("order")));
		url.append("&tmName=").append(StringUtil.checkVal(req.getParameter("tmName")));
		url.append("&lastName=").append(StringUtil.checkVal(req.getParameter("lastName")));
		url.append("&state=").append(StringUtil.checkVal(req.getParameter("state")));
		url.append("&zipCode=").append(StringUtil.checkVal(req.getParameter("zipCode")));
		url.append("&fullName=").append(StringUtil.checkVal(req.getParameter("fullName")));
		url.append("&profileId=").append(req.getParameter("profileId"));
		
		if (Convert.formatBoolean(req.getParameter("updateEvent"))) {
			aac = new SurgeonEventAction(this.actionInit);
			url.append("&physEvent=true");
		} else if (Convert.formatBoolean(req.getParameter("businessPlan"))) {
			int planTypeId = (Convert.formatInteger(req.getParameter("planTypeId"))).intValue();
			log.debug("Build: business plan: type: " + planTypeId);
			switch (planTypeId) {
			case 1: // Resource Util tab
				aac = new ResourceAction(this.actionInit);
				break;
			case 2: // SCS Usage tab
				aac = new BusinessPlanAction(this.actionInit);
				break;
			case 3: // Business Assessment tab
				aac = new BusinessAssessmentAction(this.actionInit);
				break;
			case 4: // Fellows tab
				aac = new FellowsAction(this.actionInit);
				break;
			default: //
				break;
			}
			url.append("&businessPlan=true");
			url.append("&planTypeId=").append(StringUtil.checkVal(req.getParameter("planTypeId")));
			this.createTransaction(req, "Business Plan Update");
		} else if (Convert.formatBoolean(req.getParameter("updatePhysician"))) {
			aac = new SurgeonSearchAction(this.actionInit);
			url.append("&type=physician");
			transType = "Physician Update";
		} else if (Convert.formatBoolean(req.getParameter("updateStaff"))) {
			aac = new PhysicianStaffAction(this.actionInit);
			url.append("&type=staff");
			transType = "Staff Update";
		} else if (Convert.formatBoolean(req.getParameter("updateClinic"))) {
			aac = new PhysicianClinicAction(this.actionInit);
			url.append("&type=clinic");
			transType = "Clinic Update";
		} else if (Convert.formatBoolean(req.getParameter("updateDocs"))) {
			aac = new PhysicianDocumentAction(this.actionInit);
			url.append("&type=doc");
			transType = "Document Update";
		}
		
		// Run the action
		aac.setAttributes(this.attributes);
        aac.setDBConnection(dbConn);
        aac.build(req);
        
        // log the transaction
        this.createTransaction(req, transType);
        
        if (surgeonId.length() == 0)
        	surgeonId = StringUtil.checkVal(req.getAttribute(SurgeonSearchAction.SURGEON_ID));
        
        // Add the message to the redirect
		url.append("&msg=").append(req.getAttribute(ANS_PHYSICIAN_MESSAGE));
		url.append("&surgeonId=").append(surgeonId);
		log.debug("URL: " + url);
		
		// redirect back to the list of areas
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
        
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.info("ANS Phys Facade retrieve...");
		String surgeonId = StringUtil.checkVal(req.getParameter("surgeonId"));
		Boolean deactivate = Convert.formatBoolean(req.getParameter("deactivate"));
		Boolean physEvent = Convert.formatBoolean(req.getParameter("physEvent"));
		Boolean businessPlan = Convert.formatBoolean(req.getParameter("businessPlan"));
		Boolean viewDetail = Convert.formatBoolean(req.getParameter("viewDetail"));
		Boolean duplicate = Convert.formatBoolean(req.getParameter("duplicate"));
		
		SMTActionInterface aac = null;
		if (surgeonId.length() > 1 && deactivate) {
			log.info("passing to SSA");
			Boolean deleteEntries = Convert.formatBoolean(req.getParameter("deleteEntries"));
			if (!deleteEntries) this.createTransaction(req, "Deactivate Surgeon");
			aac = new SurgeonSearchAction(this.actionInit);
			aac.setAttributes(this.attributes);
			aac.setDBConnection(dbConn);
			aac.delete(req);
		} else if (surgeonId.length() > 1 && physEvent) {
			aac = new SurgeonEventAction(this.actionInit);
			aac.setAttributes(this.attributes);
			aac.setDBConnection(dbConn);
			aac.retrieve(req);
		} else if (surgeonId.length() > 1 && duplicate) {
			aac = new DuplicateSurgeonAction(this.actionInit);
			aac.setAttributes(this.attributes);
			aac.setDBConnection(dbConn);
			aac.retrieve(req);
		} else if (surgeonId.length() > 1 && businessPlan) {
			log.debug("processing Business Plan.");
			int planTypeId = (Convert.formatInteger(req.getParameter("planTypeId"))).intValue();
			log.debug("processing business plan planTypeId: " + planTypeId);
			switch (planTypeId) {
			case 1: // Resource Util tab
				aac = new ResourceAction(this.actionInit);
				aac.setAttributes(this.attributes);
				aac.setDBConnection(dbConn);
				aac.retrieve(req);
				break;
			case 2: // SCS Usage tab
				aac = new BusinessPlanAction(this.actionInit);
				aac.setAttributes(this.attributes);
				aac.setDBConnection(dbConn);
				aac.retrieve(req);
				break;
			case 3: // Business Assessment tab
				log.debug("passing to BusinessAssessmentAction...");
				aac = new BusinessAssessmentAction(this.actionInit);
				aac.setAttributes(this.attributes);
				aac.setDBConnection(dbConn);
				aac.retrieve(req);
				break;
			case 4: // Fellows tab
				aac = new FellowsAction(this.actionInit);
				aac.setAttributes(this.attributes);
				aac.setDBConnection(dbConn);
				aac.retrieve(req);
				break;
			default: // default action is to list all surgeons.
				aac = new SurgeonSearchAction(this.actionInit);
				aac.setAttributes(this.attributes);
				aac.setDBConnection(dbConn);
				aac.list(req);
				break;
			}
		} else if (surgeonId.length() > 1 && ! deactivate && !physEvent && !duplicate && !viewDetail) {
			aac = new SurgeonSearchAction(this.actionInit);
			aac.setAttributes(this.attributes);
			aac.setDBConnection(dbConn);
			aac.list(req);
		} else if (viewDetail) {
			// Get the Event Data
			aac = new SurgeonEventAction(this.actionInit);
			aac.setAttributes(this.attributes);
			aac.setDBConnection(dbConn);
			aac.list(req);
			Object event = ((ModuleVO)attributes.get(Constants.MODULE_DATA)).getActionData();
			req.setAttribute("physicianEvent", event);
			
			// Get the Business Plan Data
			aac = new BusinessPlanAction(this.actionInit);
			aac.setAttributes(this.attributes);
			aac.setDBConnection(dbConn);
			aac.retrieve(req);
			Object bp = ((ModuleVO)attributes.get(Constants.MODULE_DATA)).getActionData();
			req.setAttribute("physicianPlan", bp);
			
			// Get the physician Data
			aac = new SurgeonSearchAction(this.actionInit);
			aac.setAttributes(this.attributes);
			aac.setDBConnection(dbConn);
			aac.list(req);
			
			// If no physician is found (security check, clear out the other data)
			log.debug("Num Phys Found: " + ((ModuleVO)attributes.get(Constants.MODULE_DATA)).getDataSize());
			if (((ModuleVO)attributes.get(Constants.MODULE_DATA)).getDataSize() == 0) {
				req.setAttribute("physicianPlan", null);
				req.setAttribute("physicianEvent", null);
			}
		} else {
			aac = new SurgeonSearchAction(this.actionInit);
			aac.setAttributes(this.attributes);
			aac.setDBConnection(dbConn);
			aac.retrieve(req);
		}
	}
	
	/**
	 * Calls the TransactionLoggingAction to log the Phys db transaction type.
	 * @param req
	 * @param type
	 */
	public void createTransaction(SMTServletRequest req, String type) throws ActionException {
		
		// if transaction type is specified, log it.
		if (type != null) {
       		SMTActionInterface sai = new TransactionLoggingAction(this.actionInit);
       		sai.setAttributes(this.attributes);
       		sai.setAttribute(TransactionLoggingAction.TRANSACTION_TYPE, type);
       		sai.setDBConnection(dbConn);
       		sai.build(req);
		}
		
		return;
		
	}
	
}
