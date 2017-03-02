package com.biomed.smarttrak.admin.report;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/*****************************************************************************
 <p><b>Title</b>: UserListReportAction.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author groot
 @version 1.0
 @since Mar 1, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class UserListReportAction extends SimpleActionAdapter {

	/**
	* Constructor
	*/
	public UserListReportAction() {
		super();
	}

	/**
	* Constructor
	*/
	public UserListReportAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	public void retrieveUserList(ActionRequest req) {
		
		// 1. retrieve account/users
		List<AccountUsersVO> accounts = retrieveAccountUsers();
		
		// 2. retrieve profiles, profile addresses, phones
		
		// 3. retrieve registration data
		
		// 4. Mix together and bake @ 350 for 30ms.
		
		// 5. return data.
		
	}
	
	protected List<AccountUsersVO> retrieveAccountUsers() {
		// 1. build query
		StringBuilder sql = buildAccountsUsersQuery();
		
		// 2. build PS
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			return parseAccountUsers(rs);
		} catch (SQLException sqle) {
			log.error("Error retrieving accounts and users, ",sqle);
			return new ArrayList<>();
		}
		
	}
	
	protected List<AccountUsersVO> parseAccountUsers(ResultSet rs) {
		List<AccountUsersVO> accounts = new ArrayList<>();
		
		return accounts;
	}
	
	/**
	 * Builds the base accounts/users query.
	 * @return
	 */
	protected StringBuilder buildAccountsUsersQuery() {
		StringBuilder sql = new StringBuilder(800);
		sql.append("select ac.account_id, ac.account_nm, ");
		sql.append("c.expiration_dt as acct_expire_dt, ");
		sql.append("us.profile_id, us.user_id, us.status_cd, us.fd_auth_flg, ");
		sql.append("us.expiration_dt as user_expire_dt, us.create_dt as join_dt, ");
		sql.append("rd.register_field_id, rd.value_txt ");
		sql.append("from custom.biomedgps_account ac ");
		sql.append("inner join custom.biomedgps_user us on ac.account_id = us.account_id ");
		sql.append("inner join register_submittal rs on us.profile_id = rs.profile_id ");
		sql.append("inner join register_data rd on rs.register_submittal_id = rd.register_submittal_id ");
		sql.append("order by account_nm, profile_id ");
		log.debug("accounts users SQL: " + sql.toString());
		return sql;
	}

}
