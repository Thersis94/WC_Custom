package com.wsla.action.ticket.transaction;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: UserTransaction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Micro changes for the user information
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 23, 2018
 * @updates:
 ****************************************************************************/

public class UserTransaction extends SBActionAdapter {

	/**
	 * Transaction key for the facade
	 */
	public static final String AJAX_KEY = "user";
	
	/**
	 * 
	 */
	public UserTransaction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public UserTransaction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		try {
			putModuleData(this.saveUser(req));
		} catch (Exception e) {
			log.error("Unable to save asset", e);
			putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}

	/**
	 * Stores the user information
	 * @param req
	 * @return
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 * @throws com.siliconmtn.exception.DatabaseException
	 */
	public UserVO saveUser(ActionRequest req) 
	throws InvalidDataException, DatabaseException, com.siliconmtn.exception.DatabaseException {
		UserVO user = new UserVO(req);
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.update(user);
		
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		pm.updateProfile(user.getProfile(), getDBConnection());
		
		return user;
	}
}

