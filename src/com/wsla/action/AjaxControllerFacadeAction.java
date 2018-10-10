package com.wsla.action;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;

// WC Libs
import com.smt.sitebuilder.action.FacadeActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.wsla.action.admin.BatchImport;
import com.wsla.action.admin.DefectAction;
import com.wsla.action.admin.DiagnosticAction;
import com.wsla.action.admin.ProductCategoryAction;
import com.wsla.action.admin.ProductCategoryXRAction;
import com.wsla.action.admin.ProductMasterAction;
import com.wsla.action.admin.ProductSerialAction;
import com.wsla.action.admin.ProductSetAction;
import com.wsla.action.admin.ProductWarrantyAction;
import com.wsla.action.admin.ProviderAction;
import com.wsla.action.admin.ProviderLocationAction;
import com.wsla.action.ticket.TicketAttributeAction;
import com.wsla.action.admin.ProviderLocationUserAction;
import com.wsla.action.admin.WarrantyAction;

/****************************************************************************
 * <b>Title</b>: AjaxControllerFacadeAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manager for ajax actions.  This class will act as a 
 * single interface / facade to multiple ajax actions.  These actions will typically
 * be simple ajax request / response and will replace having to register each one as
 * a widget.  More complex ajax actions should be registered with its own amid
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 11, 2018
 * @updates:
 ****************************************************************************/

public class AjaxControllerFacadeAction extends FacadeActionAdapter {

	/**
	 * Default type if not passed
	 */
	public static final String DEFAULT_TYPE = "lookup";
	/**
	 * Request key utilized top determine widget to call
	 */
	public static final String SELECTOR_KEY = "type";

	/**
	 * 
	 */
	public AjaxControllerFacadeAction() {
		super();
		loadTypes();
	}

	/**
	 * @param actionInit
	 */
	public AjaxControllerFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
		loadTypes();
	}

	/**
	 * 
	 */
	private void loadTypes() {
		actionMap.put(DEFAULT_TYPE, SelectLookupAction.class);
		actionMap.put("provider", ProviderAction.class);
		actionMap.put("providerLocation", ProviderLocationAction.class);
		actionMap.put(TicketAttributeAction.TICKET_ATTRRIBUTE_TYPE, TicketAttributeAction.class);
		actionMap.put(DefectAction.DEFECTS_TYPE, DefectAction.class);
		actionMap.put("productMaster", ProductMasterAction.class);
		actionMap.put("productSet", ProductSetAction.class);
		actionMap.put("productSerial", ProductSerialAction.class);
		actionMap.put("productCategoryXR", ProductCategoryXRAction.class);
		actionMap.put("productCategory", ProductCategoryAction.class);
		actionMap.put("productWarranty", ProductWarrantyAction.class);
		actionMap.put("providerLocationUser", ProviderLocationUserAction.class);
		actionMap.put("diagnostics", DiagnosticAction.class);
		actionMap.put("warranty", WarrantyAction.class);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		ModuleVO module = (ModuleVO) getAttribute(AdminConstants.ADMIN_MODULE_DATA);
		module.setSimpleAction(true);
		loadActionByType(req.getParameter(SELECTOR_KEY	, DEFAULT_TYPE)).list(req);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (! req.hasParameter("json")) return;
		loadActionByType(req.getParameter(SELECTOR_KEY, DEFAULT_TYPE)).retrieve(req);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ActionInterface action = loadActionByType(req.getParameter(SELECTOR_KEY, DEFAULT_TYPE));
		
		if (req.hasParameter("isBatch") && BatchImport.class.isAssignableFrom(action.getClass())) {
			log.debug("### BATCH TRANSACTION ###");
			BatchImport ba = (BatchImport) action;
			if (!req.getFiles().isEmpty()) { //if files were passed, ingest them.
				ba.processImport(req);
			} else { //return a template for user to populate
				ba.getBatchTemplate(req, req.getStringParameter("fileName", "batch-file"));
			}
		} else {
			action.build(req);
		}
	}
}
