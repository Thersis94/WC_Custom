package com.depuy.events_v2.vo;

import com.siliconmtn.security.UserDataVO;

/****************************************************************************
 * <b>Title</b>: PersonVO.java<p/>
 * <b>Description: Adds Role support to a normal WC user; used for TGM's and Sales Reps.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 15, 2014
 ****************************************************************************/
public class PersonVO extends UserDataVO {
	
	private static final long serialVersionUID = 388704499390927423L;

	public enum Role { TGM, REP }
	
	private Role roleCode = null;
	
	public PersonVO() {
		super();
	}
	
	public PersonVO(Role r) {
		this();
		setRoleCode(r);
	}
	
	public PersonVO(String roleStr, String profileId) {
		this();
		setRoleCode(roleStr);
		setProfileId(profileId);
	}
	
	/**
	 * For our purposes, this is "the same person" if their role (in this Seminar) and profileId are a match.
	 * This prevents duplicate persons when iterating large resultsets.
	 * @param person
	 * @return
	 */
	@Override
	public boolean equals(Object p) {
		if (p == null) return false;
		if (p == this) return true;
		if (! (p instanceof PersonVO)) return false;
		
		PersonVO person = (PersonVO) p;
		return toString().equals(person.toString());
	}
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	@Override
	public String toString() {
		return roleCode + profileId;
	}
	
	public Role getRoleCode() {
		return roleCode;
	}

	public void setRoleCode(Role roleCode) {
		this.roleCode = roleCode;
	}
	public void setRoleCode(String roleCode) {
		try {
			this.roleCode = Role.valueOf(roleCode);
		} catch (Exception e) {}
	}
	
	/**
	 * returns the email's username portion.
	 * @return
	 */
	public String getEmailAddressUsername() {
		if (emailAddress == null) return null;
		return emailAddress.substring(0, emailAddress.indexOf("@"));
	}
}
