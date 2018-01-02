package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.vo.AccountVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.user.HumanNameIntfc;
import com.siliconmtn.util.user.NameComparator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: UserAccountSearchAction.java</p>
 <p><b>Description: Takes in search parameters and does a search for accounts
 and users that meet those criterion.</b></p>
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Eric Damschroder
 @version 1.0
 @since Jun 9, 2017
 <b>Changes:</b> 
 ***************************************************************************/

public class UserAccountSearchAction extends SBActionAdapter {
	
	
	public UserAccountSearchAction() {
		super();
	}
	
	
	public UserAccountSearchAction(ActionInitVO init) {
		super(init);
	}
	
	
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		// This action exists to return a search.
		// If no search data is provided return.
		if (!req.hasParameter("searchData")) return;
		
		String[] splitSearchData = req.getParameter("searchData").toUpperCase().split(" ");
		
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		
		List<AccountVO> accounts = new ArrayList<>();
		
		try (PreparedStatement ps = dbConn.prepareStatement(buildSQL(splitSearchData.length))) {
			StringEncrypter se = new StringEncrypter((String) attributes.get(Constants.ENCRYPT_KEY));
			int pos = 1;
			for (String searchData : splitSearchData) {
				ps.setString(pos++, "%" + searchData + "%");
				ps.setString(pos++, pm.getEncValue("SEARCH_FIRST_NM", searchData));
				ps.setString(pos++, pm.getEncValue("SEARCH_LAST_NM", searchData));
				ps.setString(pos++, pm.getEncValue("SEARCH_EMAIL_TXT", searchData));
			}
			
			ResultSet rs = ps.executeQuery();
			
			while (rs.next())
				buildAccount(rs, se, accounts);
		} catch (Exception e) {
			throw new ActionException(e);
		}
		
		super.putModuleData(accounts);
		
	}

	
	/**
	 * Create a user account from the result set's current row.
	 * @param rs
	 * @param se
	 * @param accounts
	 */
	private void buildAccount(ResultSet rs, StringEncrypter se, List<AccountVO> accounts) {
		try {
			AccountVO account = new AccountVO();
			account.setAccountId(rs.getString("ACCOUNT_ID"));
			account.setAccountName(rs.getString("ACCOUNT_NM"));
			account.setFirstName(se.decrypt(rs.getString("FIRST_NM")));
			account.setLastName(se.decrypt(rs.getString("LAST_NM")));
			account.setOwnerEmailAddr(se.decrypt(rs.getString("EMAIL_ADDRESS_TXT")));
			account.setOwnerProfileId(rs.getString("USER_ID"));
			
			accounts.add(account);
		} catch (Exception e) {
			log.error("failed to get user from resultset ", e);
		}
	}


	/**
	 * loop and decrypt owner names, which came from the profile table
	 * @param accounts
	 */
	@SuppressWarnings("unchecked")
	protected void decryptNames(List<Object> data) {
		new NameComparator().decryptNames((List<? extends HumanNameIntfc>)(List<?>)data, (String)getAttribute(Constants.ENCRYPT_KEY));
	}


	/**
	 * Create an sql query to search both the accounts and the users in those accounts
	 * @return
	 */
	protected String buildSQL(int searchParams) {
		StringBuilder sql = new StringBuilder(750);
		String customDb = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		
		sql.append("SELECT a.ACCOUNT_ID, a.ACCOUNT_NM, p.FIRST_NM, p.LAST_NM, ");
		sql.append("p.EMAIL_ADDRESS_TXT, u.USER_ID ");
		sql.append("FROM ").append(customDb).append("biomedgps_account a ");
		sql.append("LEFT JOIN ").append(customDb).append("biomedgps_user u ");
		sql.append("ON u.ACCOUNT_ID = a.ACCOUNT_ID ");
		sql.append("LEFT JOIN PROFILE p on p.PROFILE_ID = u.PROFILE_ID ");
		
		sql.append("WHERE 1=2 ");
		for (int i = 0; i < searchParams; i++) {
			sql.append("OR UPPER(a.account_nm)  like ? ");
			sql.append("OR p.search_first_nm = ? ");
			sql.append("OR p.search_last_nm = ? ");
			sql.append("OR p.search_email_txt = ? ");
		}
		
		sql.append("GROUP BY a.ACCOUNT_ID, a.ACCOUNT_NM, p.FIRST_NM, p.LAST_NM, p.EMAIL_ADDRESS_TXT, u.USER_ID ");
		
		sql.append("ORDER BY a.account_id");
		log.debug(sql);
		return sql.toString();
	}
	

}
