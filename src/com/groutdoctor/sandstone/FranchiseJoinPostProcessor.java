package com.groutdoctor.sandstone;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;

/****************************************************************************
 * <b>Title</b>: FranchiseJoinPostProcessor.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Post processor for Grout Doctor's "Join the Team"
 * contact form. Forwards the contact data to be automatically processed in
 * SandStone.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 1.0
 * @since July 5, 2018
 ****************************************************************************/
public class FranchiseJoinPostProcessor extends FranchisesPostProcessor {

	public static final String FORM_TYPE = "join";

	public FranchiseJoinPostProcessor() {
		super();
	}
	
	/**
	 * @param actionInit
	 */
	public FranchiseJoinPostProcessor(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.groutdoctor.sandstone.FranchisesPostProcessor#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		setFormType(FORM_TYPE);
		super.build(req);
	}
}