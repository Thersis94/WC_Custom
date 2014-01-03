package com.fastsigns.action.wizard.siteCopy;

import com.siliconmtn.action.ActionInitVO;

public class FranchiseSiteCopyWizard_SA extends FranchiseSiteCopyWizard{

	public FranchiseSiteCopyWizard_SA(){
		FS_PREFIX = "FTS_SA_";
		FS_GROUP = "FAST_SIGNS_SA";
		siteWizardPath = "com.fastsigns.action.wizard.SiteWizardAction_SA";
	}
	
	public FranchiseSiteCopyWizard_SA(ActionInitVO actionInit){
		super(actionInit);
	}
}
