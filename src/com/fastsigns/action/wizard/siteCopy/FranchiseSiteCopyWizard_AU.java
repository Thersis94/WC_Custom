package com.fastsigns.action.wizard.siteCopy;

import com.siliconmtn.action.ActionInitVO;

public class FranchiseSiteCopyWizard_AU extends FranchiseSiteCopyWizard {
	
public FranchiseSiteCopyWizard_AU(){
	FS_PREFIX = "FTS_AU_";
	FS_GROUP = "FAST_SIGNS_AU";
	emailSuffix = "@signwave.com.au";
	siteWizardPath = "com.fastsigns.action.wizard.SiteWizardAction_AU";
	}
	
	public FranchiseSiteCopyWizard_AU(ActionInitVO actionInit){
		super(actionInit);
	}

}
