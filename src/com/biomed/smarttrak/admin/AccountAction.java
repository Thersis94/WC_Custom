package com.biomed.smarttrak.admin;

//Java 7
import java.util.ArrayList;
import java.util.List;

// WC_Custom
import com.biomed.smarttrak.vo.AccountVO;

// SMTBaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
// WebCrescendo
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
 <p><b>Title</b>: AccountAction.java</p>
 <p><b>Description: Manages the Account records for Smartrak.</b></p>
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Feb 2, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class AccountAction extends SBActionAdapter {
	
	private static final String ACCOUNT_ID = "accountId"; //req param

	public AccountAction() {
		super();
	}

	public AccountAction(ActionInitVO actionInit) {
		super(actionInit);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		//loadData gets passed on the ajax call.  If we're not loading data simply go to view to render the bootstrap 
		//table into the view (which will come back for the data).
		if (!req.hasParameter("loadData") && !req.hasParameter(ACCOUNT_ID)) return;

		String schema = (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA);
		String accountId = req.hasParameter(ACCOUNT_ID) ? req.getParameter(ACCOUNT_ID) : null;
		String sql = formatRetrieveQuery(accountId, schema);

		List<Object> params = new ArrayList<>();
		if (accountId != null) params.add(accountId);

		DBProcessor db = new DBProcessor(dbConn, schema);
		List<Object>  accounts = db.executeSelect(sql, params, new AccountVO());
		log.debug("loaded " + accounts.size() + " accounts");

		//decrypt the owner profiles
		decryptOwnerNames(accounts);

		putModuleData(accounts, accounts.size(), false);
	}


	/**
	 * loop and decrypt owner names, which came from the profile table
	 * @param accounts
	 */
	private void decryptOwnerNames(List<Object>  accounts) {
		StringEncrypter se;
		try {
			se = new StringEncrypter((String)getAttribute(Constants.ENCRYPT_KEY));
		} catch (EncryptionException e1) {
			return; //cannot use the decrypter, fail fast
		}

		for (Object o : accounts) {
			try {
				AccountVO acct = (AccountVO) o;
				acct.setFirstName(se.decrypt(acct.getFirstName()));
				acct.setLastName(se.decrypt(acct.getLastName()));
			} catch (Exception e) {
				//ignoreable
			}
		}
	}


	/**
	 * Formats the account retrieval query.
	 * @return
	 */
	public String formatRetrieveQuery(String accountId, String schema) {
		StringBuilder sql = new StringBuilder(300);
		sql.append("select a.account_id, a.company_id, a.account_nm, a.type_id, ");
		sql.append("a.start_dt, a.expiration_dt, a.owner_profile_id, a.address_txt, ");
		sql.append("a.address2_txt, a.city_nm, a.state_cd, a.zip_cd, a.country_cd, ");
		sql.append("a.status_no, a.create_dt, a.update_dt, p.first_nm, p.last_nm ");
		sql.append("from ").append(schema).append("biomedgps_account a ");
		sql.append("left outer join profile p on a.owner_profile_id=p.profile_id ");		
		if (accountId != null) sql.append("where a.account_id=? ");
		sql.append("order by a.account_nm");

		log.debug(sql);
		return sql.toString();
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		DBProcessor db = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		try {
			db.save(new AccountVO(req));
		} catch (InvalidDataException | DatabaseException e) {
			throw new ActionException(e);
		}
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		DBProcessor db = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		try {
			db.delete(new AccountVO(req));
		} catch (InvalidDataException | DatabaseException e) {
			throw new ActionException(e);
		}
	}
}