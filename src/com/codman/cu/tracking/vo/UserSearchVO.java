/**
 * 
 */
package com.codman.cu.tracking.vo;

import java.io.Serializable;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: SearchVO.java<p/>
 * <b>Description: simple container for holding the search parameters so we don't
 *    have to worry about passing them around on the request URLs. </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Aug 10, 2010
 ****************************************************************************/
public class UserSearchVO implements Serializable {
	private static final long serialVersionUID = 5358729392488250434L;
	
	private String lastName = null;
	private String emailAddress = null;
	private String roleId = null;
	private String orderBy = null;
	public static final String SESSION_VAR = "CodmanCUSearchVO";
	
	public UserSearchVO(ActionRequest req) {
		if (req.getParameter("sBtn") != null) { //indicates a search was performed
			lastName = StringUtil.checkVal(req.getParameter("sLastName"), null);
			emailAddress = StringUtil.checkVal(req.getParameter("sEmailAddress"), null);
			roleId = StringUtil.checkVal(req.getParameter("sRoleId"), null);
			orderBy = StringUtil.checkVal(req.getParameter("sOrderBy"), "a"); //a=alphabetically
			
			req.getSession().setAttribute(SESSION_VAR, this);
			
		} else if (req.getSession().getAttribute(SESSION_VAR) != null) {
			UserSearchVO s = (UserSearchVO) req.getSession().getAttribute(SESSION_VAR);
			lastName = s.getLastName();
			emailAddress = s.getEmailAddress();
			roleId = s.getRoleId();
			orderBy = s.getOrderBy();
		}
	}

	public String getLastName() {
		return lastName;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public String getRoleId() {
		return roleId;
	}

	public String getOrderBy() {
		return orderBy;
	}
}
