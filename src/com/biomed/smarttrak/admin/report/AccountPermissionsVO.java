package com.biomed.smarttrak.admin.report;

// Java 8
import java.util.ArrayList;
import java.util.List;

// WC custom
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.AccountVO;
import com.biomed.smarttrak.vo.UserVO;

/*****************************************************************************
 <p><b>Title</b>: AccountPermissionsVO.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author groot
 @version 1.0
 @since Feb 28, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class AccountPermissionsVO {

	private AccountVO account;
	private List<UserVO> users;
	private SmarttrakTree permissions;
	
	/**
	* Constructor
	*/
	public AccountPermissionsVO() {
		// contructor
	}

	/**
	 * @return the account
	 */
	public AccountVO getAccount() {
		return account;
	}

	/**
	 * @param account the account to set
	 */
	public void setAccount(AccountVO account) {
		this.account = account;
	}

	/**
	 * @return the users
	 */
	public List<UserVO> getUsers() {
		return users;
	}

	/**
	 * @param users the users to set
	 */
	public void setUsers(List<UserVO> users) {
		this.users = users;
	}
	
	/**
	 * Convenience method for adding a user to the
	 * users list.
	 * @param user
	 */
	public void addUser(UserVO user) {
		if (user == null) return;
		if (users == null) users = new ArrayList<>();
		users.add(user);
	}

	/**
	 * @return the permissions
	 */
	public SmarttrakTree getPermissions() {
		return permissions;
	}

	/**
	 * @param permissions the permissions to set
	 */
	public void setPermissions(SmarttrakTree permissions) {
		this.permissions = permissions;
	}

}
