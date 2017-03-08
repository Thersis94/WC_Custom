package com.biomed.smarttrak.admin.report;

// Java 8
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// WC custom
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.AccountVO;
import com.biomed.smarttrak.vo.UserVO;
import com.biomed.smarttrak.vo.UserVO.Status;

/*****************************************************************************
 <p><b>Title</b>: AccountPermissionsVO.java</p>
 <p><b>Description: </b>Value object primarily used for building reports.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Feb 28, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class AccountUsersVO extends AccountVO {

	/**
	 * List of UserVO: The list of users belonging to this account.
	 */
	private List<UserVO> users;
	/**
	 * Map of List of UserVO: List of users grouped by division
	 */
	private Map<String,List<UserVO>> divisions;
	private SmarttrakTree permissions;
	
	// counter for 'Added' seats
	private int addedSeatsCnt;
	// counter for 'Complementary' seats
	private int compSeatsCnt;
	// counter for 'Updates-only' seats
	private int updatesOnlyCnt;
	
	/**
	* Constructor
	*/
	public AccountUsersVO() {
		// contructor
		users = new ArrayList<>();
		divisions = new LinkedHashMap<>();
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
		users.add(user);
	}

	/**
	 * @return the divisions
	 */
	public Map<String, List<UserVO>> getDivisions() {
		return divisions;
	}

	/**
	 * @param divisions the divisions to set
	 */
	public void setDivisions(Map<String, List<UserVO>> divisions) {
		this.divisions = divisions;
	}
	
	public void addDivision(String division, List<UserVO> users) {
		if (division == null) return;
		divisions.put(division, users);
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
	
	public int getTotalUsers() {
		return users == null ? 0 : users.size();
	}

	/**
	 * Increments the count for certain user status values.
	 * @param statusCode
	 */
	public void countUserStatus(String statusCode) {
		Status status;
		try {
			status = Status.valueOf(statusCode);
		} catch (Exception e) {
			return;
		}
		switch(status) {
			case COMPLIMENTARY:
				compSeatsCnt++;
				break;
			case UPDATES:
				updatesOnlyCnt++;
				break;
			default:
				break;
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public int getAddedCount() {
		return addedSeatsCnt;
	}

	/**
	 * 
	 * @param seatCnt
	 */
	public void setAddedCount(int seatCnt) {
		addedSeatsCnt = seatCnt;
	}

	/**
	 * 
	 * @return
	 */
	public int getComplementaryCount() {
		return compSeatsCnt;
	}

	/**
	 * 
	 * @param seats
	 */
	public void setComplementaryCount(int seats) {
		compSeatsCnt = seats;
	}

	/**
	 * 
	 * @return
	 */
	public int getUpdatesOnlyCount() {
		return updatesOnlyCnt;
	}

	/**
	 * 
	 * @param seatsCnt
	 */
	public void setUpdatesOnlyCount(int seatsCnt) {
		updatesOnlyCnt = seatsCnt;
	}

}
