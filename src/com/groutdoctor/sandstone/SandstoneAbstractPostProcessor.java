package com.groutdoctor.sandstone;

import java.util.Map;

import com.siliconmtn.action.ActionInitVO;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: SandstonePostProcessor.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Abstract post processor for submitting Grout Doctor
 * data to be processed in Sandstone.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 1.0
 * @since September 27, 2017
 ****************************************************************************/
public abstract class SandstoneAbstractPostProcessor extends SBActionAdapter {

	public static final String CORPORATE_FRANCHISE_ID = "0";

	public SandstoneAbstractPostProcessor() {
		super();
	}
	
	/**
	 * @param actionInit
	 */
	public SandstoneAbstractPostProcessor(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * Returns the Sandstone module to be called
	 * 
	 * @return
	 */
	public abstract String getSandstoneModule();
	
	/**
	 * Returns the Sandstone module's action to be called
	 * 
	 * @return
	 */
	public abstract String getSandstoneAction();
	
	/**
	 * Handles submission of parameters to Sandstone
	 * 
	 * @param params
	 */
	public void submitToSandstone(Map<String, String> params) {
		// Create the Sandstone proxy and submit the data for processing
		SandstoneProxy proxy = new SandstoneProxy(getAttributes());
		proxy.setModule(getSandstoneModule());
		proxy.setAction(getSandstoneAction());
		proxy.setFranchiseId(CORPORATE_FRANCHISE_ID);
		proxy.setPostData(params);
		
		try {
			proxy.callSandstone();
		} catch (Exception e) {
			log.error("Problem with sending contact data to Sandstone in post processor", e);
		}
	}
}