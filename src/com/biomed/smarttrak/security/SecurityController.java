package com.biomed.smarttrak.security;


import org.apache.log4j.Logger;

/****************************************************************************
 * <b>Title</b>: SecurityController.java<p/>
 * <b>Description: overload of Smarttrak permissions.  Makes the decisions about who gets to see what, 
 * based on the Roles of the user and the asset they're trying to view.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Feb 22, 2017
 ****************************************************************************/
public class SecurityController {

	protected static final Logger log = Logger.getLogger(SecurityController.class);
	
	/**
	 * how far down the hierarchy tree are permissions applied.  
	 * Put in a constant in-case Smarttrak ever changes their hierarchy structure.
	 */
	public static final int PERMISSION_DEPTH_LVL = 4;

	private SmarttrakRoleVO role;

	private SecurityController(SmarttrakRoleVO role) {
		super();
		this.role = role;
	}

	/**
	 * static factory method.  useful for inline applications: SecurityController.getInstance(roleData).getSolrACL();
	 * @param role
	 * @return
	 */
	public static SecurityController getInstance(SmarttrakRoleVO role) {
		return new SecurityController(role);
	}

	/**
	 * is the user authorized to see the Financial Dashboard tool (period)
	 * @return
	 */
	public boolean isFdAuthorized() {
		return role.isFdAuthorized();
	}

	/**
	 * is the user authorized to see the Gap Analysis tool (period)
	 * @return
	 */
	public boolean isGaAuthorized() {
		return role.isGaAuthorized();
	}

	/**
	 * is the user authorized to see the Market Reports (period)
	 * @return
	 */
	public boolean isMktAuthorized() {
		return role.isMktAuthorized();
	}
}