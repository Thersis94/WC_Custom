/**
 * 
 */
package com.codman.cu.tracking;

import java.util.List;

import com.codman.cu.tracking.vo.AccountVO;
import com.codman.cu.tracking.vo.TransactionVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SecurityController;

/****************************************************************************
 * <b>Title</b>: AbstractTransactionAction.java<p/>
 * <b>Description: Action that manages a unit transaction. 
 * Some of the original CU transaction methods can be used in
 * other unit types. Those were pulled out and put here to avoid repetition
 * in those other classes. </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Erik Wingo
 * @since Oct 29, 2014
 ****************************************************************************/
public abstract class AbstractTransAction extends SBActionAdapter {

	/**
	 * Default Constructor
	 */
	public AbstractTransAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public AbstractTransAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * Retrieves admin 
	 * @param req
	 * @throws ActionException
	 */
	protected List<UserDataVO> retrieveAdministrators(SMTServletRequest req) throws ActionException {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		UserAction ua = new UserAction(this.actionInit);
		ua.setAttributes(attributes);
		ua.setDBConnection(dbConn);
		List<UserDataVO> admins = ua.loadUserList(SecurityController.ADMIN_ROLE_LEVEL, site.getOrganizationId());
		ua = null;
		return admins;
	}
	
	/**
	 * Retrieves all the users with the given roleLevel
	 * @param req
	 * @param roleLevel The role level requested.
	 * @return
	 * @throws ActionException
	 */
	protected List<UserDataVO> retrieveUsers(SMTServletRequest req, Integer roleLevel) throws ActionException {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		UserAction ua = new UserAction(this.actionInit);
		ua.setAttributes(attributes);
		ua.setDBConnection(dbConn);
		List<UserDataVO> users = ua.loadUserList(roleLevel, site.getOrganizationId());
		ua = null;
		return users;
	}
	
	/**
	 * retrieves the entire account record, filtered to this transaction, rep, and physician
	 * @param req
	 * @param trans
	 * @return
	 * @throws ActionException
	 */
	@SuppressWarnings("unchecked")
	protected AccountVO retrieveRecord(SMTServletRequest req, TransactionVO trans)
	 throws ActionException {
		
		log.debug("trans=" + trans.toString());
		req.setParameter("transactionId", trans.getTransactionId());
		req.setParameter("physicianId", trans.getPhysician().getPhysicianId());
		req.setParameter("accountId", trans.getAccountId());
		req.setParameter("unfiltered", "true"); //this bypasses all search filtering that normally would apply from the SearchVO stored on session
		
		SMTActionInterface ai = new AccountFacadeAction(actionInit);
		ai.setAttributes(attributes);
		ai.setDBConnection(dbConn);
		ai.retrieve(req);
		
		ModuleVO modVo = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		List<AccountVO> accounts = (List<AccountVO>) modVo.getActionData();
		AccountVO vo = null;
		try {
			vo = accounts.get(0);
		} catch (Exception e) {
			log.error("could not retrieve transaction for emailing", e);
			vo = new AccountVO();
		}
		
		return vo;
	}

}
