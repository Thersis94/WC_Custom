package com.fastsigns.security;

import java.util.Map;

import com.fastsigns.security.FsHybridLoginModule.LoginModule;
import com.siliconmtn.security.AbstractRoleModule;
import com.siliconmtn.security.AuthorizationException;
import com.siliconmtn.security.UserRoleContainer;
import com.siliconmtn.security.UserRoleVO;

public class FsHybridRoleModule extends AbstractRoleModule {
	
	public FsHybridRoleModule() {
		super();
	}
	
	public FsHybridRoleModule(Map<String, Object> init) {
		super(init);
	}
	
	/**
	 * Static class loader method, allows methods to easily switch between them. 
	 * The map value was set by the KeystoneLoginModule, if login was succesfully done there.
	 * (If login failed, this method is never called.)
	 * WC roles and Keystone roles.
	 * @param lm
	 * @return
	 */
	private AbstractRoleModule loadModule() {
			if (initVals.containsKey("LoginModule") && LoginModule.Keystone == initVals.get("LoginModule")) {
				log.debug("discovered Keystone login module was used");
				return new FsKeystoneRoleModule(initVals);
			} else { 
				return new FsDBRoleModule(initVals);
			}
	}
	
	
	@Override
	public UserRoleVO getUserRole(String profileId, String siteId)
			throws AuthorizationException {
		return loadModule().getUserRole(profileId, siteId);
	}

	@Override
	public UserRoleContainer getUserRoles(String profileId)
			throws AuthorizationException {
		return loadModule().getUserRoles(profileId);
	}
	
	@Override
	public void setInitVals(Map<String, Object> init) {
		super.setInitVals(init);
	}
	
	@Override
	public void init(Map<String, Object> initVals) {
		super.init(initVals);
	}

}
