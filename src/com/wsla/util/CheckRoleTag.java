package com.wsla.util;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import com.wsla.common.WSLAConstants.WSLARole;

/****************************************************************************
 * <b>Title</b>: CheckRoleTag.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Custom JSTL tag to make a decision whether to output
 * (or not) based on the user's role and authorized roles for the given html.
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Jan 7, 2019
 * @updates:
 ****************************************************************************/

public class CheckRoleTag extends BodyTagSupport {

	private static final long serialVersionUID = 142581693252591270L;
	
	private String roleId;
	private List<WSLARole> authorizedRole;
	
	public CheckRoleTag() {
		// Nothing to do here
	}

	/**
	 * @return the roleId
	 */
	public String getRoleId() {
		return roleId;
	}

	/**
	 * @param roleId the roleId to set
	 */
	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	/**
	 * @return the authorizedRole
	 */
	public List<WSLARole> getAuthorizedRole() {
		return authorizedRole;
	}

	/**
	 * @param authorizedRole the authorizedRole to set
	 */
	public void setAuthorizedRole(List<WSLARole> authorizedRole) {
		this.authorizedRole = authorizedRole;
	}
	
	/**
	 * @return the prohibitedRole
	 */
	public List<WSLARole> getProhibitedRole() {
		List<WSLARole> excludedRoles = new ArrayList<>();
		for (WSLARole role : WSLARole.values()) {
			if (!authorizedRole.contains(role)) {
				excludedRoles.add(role);
			}
		}
		
		return excludedRoles;
	}

	/**
	 * @param prohibitedRole the prohibitedRole to set
	 */
	public void setProhibitedRole(List<WSLARole> prohibitedRole) {
		List<WSLARole> includedRoles = new ArrayList<>();
		for (WSLARole role : WSLARole.values()) {
			if (!prohibitedRole.contains(role)) {
				includedRoles.add(role);
			}
		}
		
		this.authorizedRole = includedRoles;
	}
	
	

	/* (non-Javadoc)
	 * @see javax.servlet.jsp.tagext.Tag#doEndTag()
	 */
	@Override
	public int doEndTag() throws JspException {
		// Determine if the user's role matches one of the authorized roles
		boolean display = false;
		for (WSLARole role : authorizedRole) {
			if (role.getRoleId().equals(roleId)) {
				display = true;
				break;
			}
		}
		
		// If the user is authorized, display the body content inside of the tag
		String bodyText = display ? getBodyContent().getString() : "";
		try {
			pageContext.getOut().print(bodyText);
		} catch (Exception e) {
			throw new JspTagException(e.getMessage());
		} 
		
		return EVAL_PAGE;
	}
}
