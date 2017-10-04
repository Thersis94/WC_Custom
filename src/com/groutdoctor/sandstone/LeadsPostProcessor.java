package com.groutdoctor.sandstone;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: LeadsPostProcessor.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Post processor for Grout Doctor's "Free Estimate" form.
 * Forwards the contact data so it can be automatically processed in Sandstone.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 1.0
 * @since September 27, 2017
 ****************************************************************************/
public class LeadsPostProcessor extends SandstoneAbstractPostProcessor {

	public static final String CFG_ESTIMATE_SERVICE_ID = "groutDoctorEstimateServiceId";
	public static final String SANDSTONE_MODULE = "accounts";
	public static final String SANDSTONE_ACTION = "customerIntakeFromAPI";

	public LeadsPostProcessor() {
		super();
	}
	
	/**
	 * @param actionInit
	 */
	public LeadsPostProcessor(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		// Map the standard form parameters to those expected by Sandstone
		Map<String, String> params = new HashMap<>();
		params.put("first_name", StringUtil.checkVal(req.getParameter("pfl_FIRST_NM")));
		params.put("last_name", StringUtil.checkVal(req.getParameter("pfl_LAST_NM")));
		params.put("email_address", StringUtil.checkVal(req.getParameter("pfl_EMAIL_ADDRESS_TXT")));
		params.put("primary_phone", StringUtil.removeNonNumeric(StringUtil.checkVal(req.getParameter("pfl_MAIN_PHONE_TXT"))));
		params.put("mobile_phone", StringUtil.removeNonNumeric(StringUtil.checkVal(req.getParameter("pfl_MOBILE_PHONE_TXT"))));
		params.put("zipcode", StringUtil.checkVal(req.getParameter("pfl_ZIP_CD")));
		
		// Add the selected services to populate into the Sandstone call/to-do
		String[] services = req.getParameterValues((String) getAttribute(CFG_ESTIMATE_SERVICE_ID));
		params.put("services", Arrays.toString(services).replaceAll("[\\[\\]]", "").replace("_", " "));
		
		// Submit the data to Sandstone for processing
		submitToSandstone(params);
	}

	@Override
	public String getSandstoneModule() {
		return SANDSTONE_MODULE;
	}

	@Override
	public String getSandstoneAction() {
		return SANDSTONE_ACTION;
	}
}