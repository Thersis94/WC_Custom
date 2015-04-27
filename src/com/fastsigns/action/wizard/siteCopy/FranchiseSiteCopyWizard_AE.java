package com.fastsigns.action.wizard.siteCopy;

import com.siliconmtn.action.ActionInitVO;

public class FranchiseSiteCopyWizard_AE extends FranchiseSiteCopyWizard{

	public FranchiseSiteCopyWizard_AE(){
		FS_PREFIX = "FTS_AE_";
		FS_GROUP = "FAST_SIGNS_AE";
		siteWizardPath = "com.fastsigns.action.wizard.SiteWizardAction_AE";
	}
	
	public FranchiseSiteCopyWizard_AE(ActionInitVO actionInit){
		super(actionInit);
	}
}
