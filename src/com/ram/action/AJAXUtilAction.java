package com.ram.action;

// JDK 1.7.x
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ram.action.customer.CustomerAction;
// RAM Data Feed Libs
import com.ram.action.customer.CustomerLocationAction;
import com.ram.action.customer.CustomerTypesAction;
import com.ram.action.event.InventoryEventGroupAction;
import com.ram.action.products.ProductAction;
import com.ram.datafeed.data.CustomerLocationVO;
//SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.http.SMTServletRequest;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.report.ReportDataAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: AJAXUtilAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Action to process various ajax calls (usually lookups)
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since May 28, 2014<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class AJAXUtilAction extends SBActionAdapter {

	/**
	 * Codes for the customer search
	 */
	public static final String CUSTOMER_TYPE = "customer";

	/**
	 * Codes for customer type search
	 */
	public static final String CUSTOMER_TYPES_TYPE = "customer_types";

	/**
	 * Codes for the customer location search
	 */
	public static final String CUSTOMER_LOCATION_TYPE = "customer_location";

	/**
	 * Code to process the event group data
	 */
	public static final String EVENT_GROUP_TYPE = "event_group";

	/**
	 * 
	 */
	public static final String SAVE_EVENT_GROUP_TYPE = "save_event_group";

	/**
	 * 
	 */
	public static final String REGIONS_TYPE = "regions";

	/**
	 * Code to handle exporting a Report to browser from JasperServer.
	 */
	public static final String REPORT_EXPORT = "report_export";

	/**
	 * 
	 */
	public static final String PRODUCT_CLONE = "productClone";
	/**
	 *
	 */
	public AJAXUtilAction() {
	}

	/**
	 * @param actionInit
	 */
	public AJAXUtilAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		SMTActionInterface sai = null;
		String type = req.getParameter("type");
		
		// Call the appropriate action
		switch(type) {
			case CUSTOMER_TYPE:
				sai = new CustomerAction(getActionInit());
				sai.setDBConnection(getDBConnection());
				sai.setAttributes(getAttributes());
				sai.retrieve(req);
				break;
			case CUSTOMER_TYPES_TYPE:
				sai = new CustomerTypesAction(getActionInit());
				sai.setDBConnection(getDBConnection());
				sai.setAttributes(getAttributes());
				sai.retrieve(req);
				break;
			case CUSTOMER_LOCATION_TYPE:
				sai = new CustomerLocationAction(getActionInit());
				sai.setDBConnection(getDBConnection());
				sai.setAttributes(getAttributes());
				sai.retrieve(req);
				this.processLocations(sai);
				break;
			case EVENT_GROUP_TYPE:
				sai = new InventoryEventGroupAction(getActionInit());
				sai.setDBConnection(getDBConnection());
				sai.setAttributes(getAttributes());
				sai.retrieve(req);
				log.info("****** Getting Group Info");
				break;
			case SAVE_EVENT_GROUP_TYPE:
				this.saveEventGroup(req);
				log.info("****** Saving Group Info");
				break;
			case REGIONS_TYPE:
				sai = new RegionAction(getActionInit());
				sai.setDBConnection(getDBConnection());
				sai.setAttributes(getAttributes());
				sai.retrieve(req);
				break;
			case REPORT_EXPORT:
				sai = new ReportDataAction(getActionInit());
				sai.setDBConnection(getDBConnection());
				sai.setAttributes(getAttributes());
				sai.build(req);
				break;
			case PRODUCT_CLONE:
				ProductAction pa = new ProductAction(getActionInit());
				pa.setDBConnection(getDBConnection());
				pa.setAttributes(getAttributes());
				pa.copy(req);
				break;
		}
	}
	
	/**
	 * Saves and process changes for the event groups and recurrence
	 * @param req
	 * @throws ActionException
	 */
	protected void saveEventGroup(SMTServletRequest req) throws ActionException {
		// Save the Event group data
		SMTActionInterface sai = new InventoryEventGroupAction(getActionInit());
		sai.setDBConnection(getDBConnection());
		sai.setAttributes(getAttributes());
		sai.update(req);
		
		//send a success response, since nothing above has thrown an ActionException
		Map<String, Object> res = new HashMap<>(); 
		res.put("success", true);
		putModuleData(res);
	}
	
	/**
	 * Need to re-process the customer locations to reduce the amount of data transmitted
	 * @param sai
	 */
	@SuppressWarnings({"unchecked" })
	protected void processLocations(SMTActionInterface sai) {
		ModuleVO modVo = (ModuleVO)sai.getAttribute(Constants.MODULE_DATA);
		List<CustomerLocationVO> data = (List<CustomerLocationVO>) modVo.getActionData();
		
		List<GenericVO> locs = new ArrayList<>();
		locs.add(new GenericVO("", "Please Select ..."));
		for (CustomerLocationVO loc : data) {
			locs.add(new GenericVO(loc.getCustomerLocationId(), loc.getLocationName()));
		}
		
		modVo.setActionData(locs);
		sai.setAttribute(Constants.MODULE_DATA, modVo) ;
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}

}
