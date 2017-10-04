package com.groutdoctor.sandstone;

import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: FranchisesPostProcessor.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Post processor for Grout Doctor's "Info on Franchise
 * Opportunities" contact form. Forwards the contact data to be automatically
 * processed in SandStone.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 1.0
 * @since September 27, 2017
 ****************************************************************************/
public class FranchisesPostProcessor extends SandstoneAbstractPostProcessor {

	public static final String SANDSTONE_MODULE = "franchises";
	public static final String SANDSTONE_ACTION = "createNewFromAPI";

	public FranchisesPostProcessor() {
		super();
	}
	
	/**
	 * @param actionInit
	 */
	public FranchisesPostProcessor(ActionInitVO actionInit) {
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
		params.put("mobile_phone", StringUtil.removeNonNumeric(StringUtil.checkVal(req.getParameter("pfl_MOBILE_PHONE_TXT"))));
		params.put("zipcode", StringUtil.checkVal(req.getParameter("pfl_ZIP_CD")));
		
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