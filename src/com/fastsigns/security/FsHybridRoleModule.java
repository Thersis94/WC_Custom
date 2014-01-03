package com.fastsigns.security;

import java.util.Map;

import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.AbstractRoleModule;
import com.siliconmtn.security.AuthorizationException;
import com.siliconmtn.security.UserRoleContainer;
import com.siliconmtn.security.UserRoleVO;

public class FsHybridRoleModule extends AbstractRoleModule {

	private AbstractRoleModule arm = null;
	
	public FsHybridRoleModule() {
		super();
	}
	
	public FsHybridRoleModule(Map<String, Object> init) {
		super(init);
		SMTServletRequest req = (SMTServletRequest)this.initVals.get(AbstractRoleModule.HTTP_REQUEST);
		if(req.hasParameter("type") && req.getParameter("type").equals("ecomm")){
			arm = new FsKeystoneRoleModule(init);
		} else {
			arm = new FsDBRoleModule(init);
		}
	}
	
	@Override
	public UserRoleVO getUserRole(String profileId, String siteId)
			throws AuthorizationException {
		return arm.getUserRole(profileId, siteId);
	}

	@Override
	public UserRoleContainer getUserRoles(String profileId)
			throws AuthorizationException {
		return arm.getUserRoles(profileId);
	}
	
	@Override
	public void setInitVals(Map<String, Object> init) {
		super.setInitVals(init);
		SMTServletRequest req = (SMTServletRequest)this.initVals.get(AbstractRoleModule.HTTP_REQUEST);
		if(req.hasParameter("type") && req.getParameter("type").equals("ecomm")){
			arm = new FsKeystoneRoleModule(init);
		} else {
			arm = new FsDBRoleModule(init);
		}
		arm.init(init);
	}
	
	@Override
	public void init(Map<String, Object> initVals) {
		super.init(initVals);
		SMTServletRequest req = (SMTServletRequest)this.initVals.get(AbstractRoleModule.HTTP_REQUEST);
		if(req.hasParameter("type") && req.getParameter("type").equals("ecomm")){
			arm = new FsKeystoneRoleModule(initVals);
		} else {
			arm = new FsDBRoleModule(initVals);
		}
		arm.init(initVals);
	}

}
