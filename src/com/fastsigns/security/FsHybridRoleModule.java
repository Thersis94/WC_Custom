package com.fastsigns.security;

import java.util.Map;

import com.siliconmtn.security.AbstractRoleModule;
import com.siliconmtn.security.AuthorizationException;
import com.siliconmtn.security.UserRoleContainer;
import com.siliconmtn.security.UserRoleVO;

public class FsHybridRoleModule extends AbstractRoleModule {
	
	/*
	 * enum constant for the types of RoleModules this object facades
	 */
	public enum RoleModule {
		Keystone, WebCrescendo
	}
	
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
	private AbstractRoleModule loadModule(RoleModule roleModule) {
			switch (roleModule) {
				case WebCrescendo:
					return new FsDBRoleModule(initVals);
				
				case Keystone:
				default:
					return new FsKeystoneRoleModule(initVals);
			}
	}
	
	
	@Override
	public UserRoleVO getUserRole(String profileId, String siteId)
			throws AuthorizationException {
		try {
			return loadModule(RoleModule.WebCrescendo).getUserRole(profileId, siteId);
		} catch (AuthorizationException ae) {
			log.warn("USER NOT FOUND IN WC, TRYING KEYSTONE");
			return loadModule(RoleModule.Keystone).getUserRole(profileId, siteId);
		}
	}

	@Override
	public UserRoleContainer getUserRoles(String profileId)
			throws AuthorizationException {
		try {
			return loadModule(RoleModule.WebCrescendo).getUserRoles(profileId);
		} catch (AuthorizationException ae) {
			log.warn("USER NOT FOUND IN WC, TRYING KEYSTONE");
			return loadModule(RoleModule.Keystone).getUserRoles(profileId);
		}
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
