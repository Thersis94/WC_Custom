package com.biomed.smarttrak.admin.user;

// Java 7
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

// Log4j
import org.apache.log4j.Logger;

// WC_Custom
import com.biomed.smarttrak.vo.AccountVO;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: AccountManager.java</p>
 <p><b>Description: </b></p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author DBargerhuff
 @version 1.0
 @since Feb 2, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class AccountManager extends AbstractManager {

	private static final String STATUS_NO_INACTIVE = "I";
	private Logger log = Logger.getLogger(AccountManager.class);
	private String accountId;
	private String teamId;
	private String userId;
	private boolean excludeInactiveAccounts;
	
	/**
	* Constructor
	*/
	public AccountManager() {
		// constructor stub
	}
	
	/**
	* Constructor
	*/
	public AccountManager(Connection dbConn) {
		setDbConn(dbConn);
		setAttributes(new HashMap<String,Object>());
	}

	/**
	* Constructor
	*/
	public AccountManager(Connection dbConn, Map<String,Object> attributes) {
		this(dbConn);
		setAttributes(attributes);
	}

	/**
	 * Retrieves account data.  Query format is based on 
	 * the values of the ID fields that are set on this object
	 * before the method is called.
	 * @return
	 * @throws SQLException
	 */
	public Map<String, AccountVO> retrieveAccounts() throws SQLException {
		StringBuilder sql = formatRetrieveQuery();
		log.debug("Smarttrak account(s) retrieve SQL: " + sql.toString());
		
		Map<String,AccountVO> accounts;
		try (PreparedStatement ps = getDbConn().prepareStatement(sql.toString())) {
			AccountVO account;
			accounts =  new LinkedHashMap<>();
			int idx = 1;
			if (accountId != null) ps.setString(idx++, accountId);
			if (excludeInactiveAccounts) ps.setString(idx, STATUS_NO_INACTIVE);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				account = new AccountVO(rs);
				accounts.put(account.getAccountId(), account);
			}
		} catch (SQLException sqle) {
			throw new SQLException(sqle.getMessage());
		}
		
		return accounts;
	}
	
	/**
	 * Formats the account retrieval query.
	 * @return
	 */
	public StringBuilder formatRetrieveQuery() {
		String schema = (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(300);
		sql.append("select a.account_id, a.company_id, a.account_nm, a.type_id, ");
		sql.append("a.start_dt, a.expiration_dt, a.owner_profile_id, a.address_txt, ");
		sql.append("a.address2_txt, a.city_nm, a.state_cd, a.zip_cd, a.country_cd, ");
		sql.append("a.status_no, a.create_dt, a.update_dt ");
		sql.append("from ").append(schema).append("biomedgps_account a where 1=1 ");		
		if (accountId != null) sql.append("and a.account_id = ? ");
		if (excludeInactiveAccounts) sql.append("and status_no != ? ");
		sql.append("order by a.account_nm");
		return sql;
	}

	/**
	 * @return the accountId
	 */
	public String getAccountId() {
		return accountId;
	}

	/**
	 * @param accountId the accountId to set
	 */
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	/**
	 * @return the teamId
	 */
	public String getTeamId() {
		return teamId;
	}

	/**
	 * @param teamId the teamId to set
	 */
	public void setTeamId(String teamId) {
		this.teamId = teamId;
	}

	/**
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/**
	 * @param includeInactiveAccounts the includeInactiveAccounts to set
	 */
	public void setExcludeInactiveAccounts(boolean excludeInactiveAccounts) {
		this.excludeInactiveAccounts = excludeInactiveAccounts;
	}
	
}
