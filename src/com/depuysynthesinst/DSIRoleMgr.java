package com.depuysynthesinst;

import com.siliconmtn.security.UserDataVO;

/****************************************************************************
 * <b>Title</b>: DSIRoleMgr.java<p/>
 * <b>Description: Decouples the business rule decisions for "Role", which is really a reflection
 * of how the user answered the "Profession" question during registration. </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jun 29, 2015
 ****************************************************************************/
public class DSIRoleMgr {

	public DSIRoleMgr() {
	}

	
	/**
	 * returns true if the user's profession is Resident (RESIDENT)
	 * @param user
	 * @return
	 */
	public static boolean isResident(UserDataVO user) {
		return true;
	}
	
	/**
	 * returns true if the user's profession is Fellow (FELLOW)
	 * @param user
	 * @return
	 */
	public static boolean isFellow(UserDataVO user) {
		return true;
	}
	
	/**
	 * returns true if the user's profession is Chief Resident (CHIEF)
	 * @param user
	 * @return
	 */
	public static boolean isChiefResident(UserDataVO user) {
		return true;
	}
	
	/**
	 * returns true if the user's profession is Resident Director/ (DIRECTOR)
	 * @param user
	 * @return
	 */
	public static boolean isDirector(UserDataVO user) {
		return false;
	}
	
	/**
	 * returns true if the user's profession is any of the above; meaning they interact with the
	 * Assignments functionality in any way.  Used for displaying menus, etc.
	 * Fellows DO NOT partake in My Assignments, at all.
	 * @param user
	 * @return
	 */
	public static boolean isAssgUser(UserDataVO user) {
		return isResident(user) || isChiefResident(user) || isDirector(user);
	}
	
	/**
	 * returns true if the user has administrative access for managing Assignments
	 * @param user
	 * @return
	 */
	public static boolean isAssgAdmin(UserDataVO user) {
		return isChiefResident(user) || isDirector(user);
	}
	
}
