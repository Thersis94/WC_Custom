package com.fastsigns.security;

//JDK
import java.sql.Connection;
import java.util.Map;

//SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.AbstractRoleModule;
import com.siliconmtn.security.AuthorizationException;
import com.siliconmtn.security.UserRoleVO;
import com.siliconmtn.security.UserRoleContainer;

//WC libs
import com.smt.sitebuilder.security.DBRoleModule;
import com.smt.sitebuilder.security.SBUserRoleContainer;

//Log4J API
import org.apache.log4j.Logger;

/****************************************************************************
 * <b>Title</b>: FsDBRoleModule.java<p/>
 * <b>Description: Implements the AbstractRoleModule to retrieve the user
 * roles in the database.  A database connection is required to be added
 * to the initVals Map using the DBRoleModule.DB_CONN parameter as the key</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Dec 14, 2010
 ****************************************************************************/
public class FsDBRoleModule extends AbstractRoleModule {
	/**
	 * Requires a database connection be asigned to the initialization Collection
	 * using the DB_CONN string value as the Map key
	 */
	public static final String DB_CONN = GlobalConfig.KEY_DB_CONN;
	protected static final Logger log = Logger.getLogger(FsDBRoleModule.class);
	
	public FsDBRoleModule() {
		super();
	}
	
	/**
	 * @param init Map of init objects used by the class.
	 */
	public FsDBRoleModule(Map<String, Object> init) {
		super(init);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractRoleModule#getUserRole(java.lang.String,java.lang.String)
	 */
	public UserRoleVO getUserRole(String profileId, String siteId)
	throws AuthorizationException {
		DBRoleModule drm = new DBRoleModule(initVals);
		UserRoleVO vo = null;
		
		try {
			vo = drm.getUserRole(profileId, siteId);
			
		} catch (AuthorizationException ae) {
			throw new AuthorizationException(ae);
		}
		
		// set the user's franchiseId on the session if available
		this.setUserFranchiseData(profileId);
		
		return vo;
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.security.AbstractRoleModule#getUserRoles(java.lang.String)
	 */
	public UserRoleContainer getUserRoles(String authId)
	throws AuthorizationException {
		
		DBRoleModule drm = new DBRoleModule(initVals);
		UserRoleContainer sites = new UserRoleContainer();
		
		try {
			sites = drm.getUserRoles(authId);
		} catch (AuthorizationException ae) {
			throw new AuthorizationException(ae);
		}

		return sites;
	}
	
	/**
	 * 
	 * @param profileId
	 * @return
	 * @throws AuthorizationException
	 */
	public SBUserRoleContainer adminAuthorized(String profileId) 
	throws AuthorizationException {
		log.debug("Starting admin Authorized ..." + profileId);
		
		DBRoleModule drm = new DBRoleModule(initVals);
		SBUserRoleContainer roles = new SBUserRoleContainer();
		
		try {
			roles = drm.adminAuthorized(profileId);
		} catch (AuthorizationException ae) {
			throw new AuthorizationException(ae);
		}

		return roles;
	}
	
	/**
	 * Retrieves user's franchise role/id data and sets it on the session if 
	 * it is not there already
	 * @param authId
	 */
	@SuppressWarnings("unchecked")
	public void setUserFranchiseData(String profileId) {
		SMTServletRequest req = (SMTServletRequest)this.initVals.get(HTTP_REQUEST);
		Connection dbConn = (Connection)initVals.get(DB_CONN);
		
		try {
			SMTActionInterface sai = new FsFranchiseRoleAction(new ActionInitVO());
			sai.setAttributes(initVals);
			sai.setAttribute("profileId", profileId);
			sai.setDBConnection(new SMTDBConnection(dbConn));
			sai.build(req);
		} catch (ActionException ae) {
			log.error("Error setting user franchise role XR record, ", ae);
		}
		
		// set the user attributes using the franchise map from the action
		this.setUserAttributes((Map<String, Object>)req.getAttribute(FsFranchiseRoleAction.FRANCHISE_MAP));
		req.removeAttribute(FsFranchiseRoleAction.FRANCHISE_MAP);
	}
}
