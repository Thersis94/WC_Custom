package com.biomed.smarttrak.vo;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: PermissionVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Feb 21, 2017
 ****************************************************************************/
public class PermissionVO extends SectionVO {

	private static final long serialVersionUID = 3136795243351165911L;

	private String accountId;
	private boolean browseAuth;
	private boolean updatesAuth;
	private boolean fdAuth;
	private boolean gaAuth;

	/**
	 * no-args constructor
	 */
	public PermissionVO() {
		super();
	}

	/**
	 * @param accountId
	 * @param sectionId
	 */
	public PermissionVO(String accountId, String sectionId) {
		this();
		setAccountId(accountId);
		setSectionId(sectionId);
	}

	/**
	 * @param rs
	 * @throws SQLException 
	 */
	public PermissionVO(ResultSet rs) throws SQLException {
		super(rs);
	}

	/**
	 * @param req
	 */
	public PermissionVO(ActionRequest req) {
		super(req);
	}

	public void setData(ResultSet rs) throws SQLException {
		super.setData(rs);
		setBrowseAuth(1 == rs.getInt("browse_no"));
		setUpdatesAuth( 1 == rs.getInt("updates_no"));
		setFdAuth(1 == rs.getInt("fd_no"));
		setGaAuth(1 == rs.getInt("ga_no"));
		setAccountId(rs.getString("account_id"));
	}

	public boolean isBrowseAuth() {
		return browseAuth;
	}

	public boolean isUpdatesAuth() {
		return updatesAuth;
	}

	public boolean isFdAuth() {
		return fdAuth;
	}

	public boolean isGaAuth() {
		return gaAuth;
	}

	private void setBrowseAuth(boolean browseAuth) {
		this.browseAuth = browseAuth;
	}

	private void setUpdatesAuth(boolean updatesAuth) {
		this.updatesAuth = updatesAuth;
	}

	private void setFdAuth(boolean fdAuth) {
		this.fdAuth = fdAuth;
	}

	private void setGaAuth(boolean gaAuth) {
		this.gaAuth = gaAuth;
	}

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	/**
	 * Used from the /manage interface to save account permissions
	 * @param value
	 */
	public void addSelection(String value) {
		if (StringUtil.isEmpty(value)) return;
		//depending on the value prefix, set the proper authentication component
		if (value.startsWith("fd~")) setFdAuth(true);
		else if (value.startsWith("ga~")) setGaAuth(true);
		else if (value.startsWith("updates~")) setUpdatesAuth(true);
		else if (value.startsWith("browse~")) setBrowseAuth(true);

	}

	/**
	 * a helper used during login (RoleModule) to "toss out" this VO if there are no permissions set in it.
	 * @return true if permissions exist, false otherwise.
	 */
	public boolean isUnauthorized() {
		return !isBrowseAuth() && !isUpdatesAuth() && !isFdAuth() && !isGaAuth();
	}
}