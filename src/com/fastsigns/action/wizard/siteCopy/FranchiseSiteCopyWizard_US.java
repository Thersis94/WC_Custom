package com.fastsigns.action.wizard.siteCopy;

import com.siliconmtn.action.ActionInitVO;

public class FranchiseSiteCopyWizard_US extends FranchiseSiteCopyWizard {

	public FranchiseSiteCopyWizard_US(){
		FS_PREFIX = "FTS_";
		FS_GROUP = "FAST_SIGNS";
		siteWizardPath = "com.fastsigns.action.wizard.SiteWizardAction_US";
	}
	
	public FranchiseSiteCopyWizard_US(ActionInitVO actionInit){
		super(actionInit);
	}

}
