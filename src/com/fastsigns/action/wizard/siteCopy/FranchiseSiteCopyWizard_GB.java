package com.fastsigns.action.wizard.siteCopy;

import com.siliconmtn.action.ActionInitVO;

public class FranchiseSiteCopyWizard_GB extends FranchiseSiteCopyWizard {

	public FranchiseSiteCopyWizard_GB(){
		FS_PREFIX = "FTS_UK_";
		FS_GROUP = "FAST_SIGNS_GB";
		siteWizardPath = "com.fastsigns.action.wizard.SiteWizardAction_GB";
	}
	
	public FranchiseSiteCopyWizard_GB(ActionInitVO actionInit){
		super(actionInit);
	}

	
}
