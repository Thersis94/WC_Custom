package com.ram.action.user;

// JDK 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// RAM Libs
import com.ram.datafeed.data.RAMUserVO;

//SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WebCrescendo 2.0
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title: </b>RamUserFacadeAction.java <p/>
 * <b>Project: </b>WC_Custom <p/>
 * <b>Description: </b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2014<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0<p/>
 * @since May 27, 2014<p/>
 *<b>Changes: </b>
 * May 27, 2014: David Bargerhuff: Created class.
 ****************************************************************************/
public class RamUserFacadeAction extends SBActionAdapter {
	
	public static final int PROFILE_STATUS_DISABLED = 5;
	public static final int PROFILE_STATUS_ACTIVE = 20;
	
	/**
	 * 
	 */
	public RamUserFacadeAction() {
		super(new ActionInitVO());
	}

	/**
	 * @param actionInit
	 */
	public RamUserFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("RamUserFacadeAction retrieve...");
		boolean searchSubmitted = Convert.formatBoolean(req.getParameter("searchSubmitted"));
		if (searchSubmitted) {
			performSearch(req);
		} else {
			SMTActionInterface sai = new RamUserAction(actionInit);
			sai.setAttributes(attributes);
			sai.setDBConnection(dbConn);
			sai.retrieve(req);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("RamUserFacadeAction build...");
		
		boolean searchSubmitted = Convert.formatBoolean(req.getParameter("searchSubmitted"));
		if (searchSubmitted) {
			
			performSearch(req);
			StringBuilder retUrl = new StringBuilder();
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			retUrl.append(page.getRequestURI());
			retUrl.append("?srchFirstName=").append(req.getParameter("srchFirstName"));
			retUrl.append("&srchLastName=").append(req.getParameter("srchLastName"));
			retUrl.append("&srchRole=").append(req.getParameter("srchRole"));
			retUrl.append("&srchCustomerId=").append(req.getParameter("srchCustomerId"));
			
			log.debug("performSearch redir: " + retUrl);
			req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.FALSE);
			req.setAttribute(Constants.REDIRECT_URL, retUrl.toString());
			
		} else {
			SMTActionInterface sai = null;
			sai = new RamUserAction(actionInit);
			sai.setAttributes(attributes);
			sai.setDBConnection(dbConn);
			sai.build(req);
		}
				
	}
	
	/**
	 * Performs search against customer table(s)
	 * @param req
	 */
	private void performSearch(SMTServletRequest req) {
		log.debug("RamUserFacadeAction performSearch...");
		// determine the site ID we need to use for the search
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		String srchSiteId = StringUtil.checkVal(site.getAliasPathParentId());
		if (srchSiteId.length() == 0) {
			srchSiteId = site.getSiteId();
		}
		log.debug("using site ID: " + srchSiteId);
		String srchFN = StringUtil.checkVal(req.getParameter("srchFirstName"));
		String srchLN = StringUtil.checkVal(req.getParameter("srchLastName"));
		String srchRole = StringUtil.checkVal(req.getParameter("srchRoleId"));
		String srchCustomerId = StringUtil.checkVal(req.getParameter("srchCustomerId"));
		String srchStatusId = StringUtil.checkVal(req.getParameter("srchStatusId"));
		
		StringBuilder sql = new StringBuilder();
		sql.append("select a.*, b.FIRST_NM, b.LAST_NM, b.EMAIL_ADDRESS_TXT, ");
		sql.append("c.ROLE_ORDER_NO, c.ROLE_NM, d.PHONE_NUMBER_TXT ");
		sql.append("from PROFILE_ROLE a ");
		sql.append("inner join PROFILE b on a.PROFILE_ID = b.PROFILE_ID ");
		sql.append("left outer join PHONE_NUMBER d on a.PROFILE_ID = d.PROFILE_ID and d.PHONE_TYPE_CD = 'HOME' ");
		sql.append("inner join ROLE c on a.ROLE_ID = c.ROLE_ID ");
		sql.append("where SITE_ID = ? ");
		
		int statusId = Convert.formatInteger(srchStatusId, -1, false);
		log.debug("srchStatusId | statusId: " + srchStatusId + " | " + statusId);
		if (statusId > -1) sql.append("and a.STATUS_ID = ? ");
		
		if (srchRole.length() > 0) sql.append("and a.ROLE_ID = ? ");
		if (srchCustomerId.length() > 0) sql.append("and a.ATTRIB_TXT_1 = ? ");
		sql.append("order by a.PROFILE_ID");
		log.debug("User search SQL: " + sql.toString());
		
		PreparedStatement ps = null;
		int index = 1;
		List<RAMUserVO> data = new ArrayList<>();
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(index++, srchSiteId);
			
			// set status
			if (statusId > -1) {
				ps.setInt(index++, statusId);
				log.debug("srchStatusId: " + srchStatusId);
			}
			
			// set role
			if (srchRole.length() > 0) {
				ps.setString(index++, srchRole);
				log.debug("srchRole: " + srchRole);
			}

			// set customer ID
			if (srchCustomerId.length() > 0) {
				ps.setString(index++, srchCustomerId);
				log.debug("srchCustomerId: " + srchCustomerId);
			}
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				data.add(new RAMUserVO(rs));
			}
			
			log.debug("initial data size: " + data.size());
			
		} catch (SQLException sqle) {
			log.error("Error performing customer search, ", sqle);
			
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) { log.error("Error closing PreparedStatement, ", e);}
			}
		}
		
		// decrypt first/last name and filter by user if applicable
		List<RAMUserVO> filteredData = filterByUser(data, srchFN, srchLN);
		
		// sort by user name
		Collections.sort(filteredData, new RAMUserComparator());
		
		putModuleData(filteredData, filteredData.size(), false, null);
		
	}
	
	/**
	 * Helper method for decrypting user first/last names.
	 * @param users
	 * @param fn
	 * @param ln
	 * @return
	 */
	private List<RAMUserVO> filterByUser(List<RAMUserVO> users, String fn, String ln) {
		StringEncrypter se = null;
		try {
			se = new StringEncrypter((String)attributes.get(Constants.ENCRYPT_KEY));
		} catch (EncryptionException ee) {
			log.error("Error instantiating StringEncrypter, ", ee);
			return users;
		}
		
		List<RAMUserVO> filteredData = new ArrayList<>();
		for (RAMUserVO user : users) {
			try {
				user.setFirstName(se.decrypt(user.getFirstName()));
			} catch (Exception e) {log.error("Error decrypting first name, " + e.getMessage());}
			try {
				user.setLastName(se.decrypt(user.getLastName()));
			} catch (Exception e) {log.error("Error decrypting last name, " + e.getMessage());}
			try {
				user.setEmailAddress(se.decrypt(user.getEmailAddress()));
			} catch (Exception e) {log.error("Error decrypting email address, " + e.getMessage());}
			try {
				user.setPhoneNumber(se.decrypt(user.getPhoneNumber()));
			} catch (Exception e) {log.error("Error decrypting telephone, " + e.getMessage());}
			
			if (fn.length() > 0) {
				if (user.getFirstName().contains(fn)) {
					// first name matches, check last name
					if (ln.length() > 0) {
						// test last name
						if (user.getLastName().contains(ln)) {
							// last name matches also, add to final list.
							filteredData.add(user);
						}
					} else {
						// add to final list.
						filteredData.add(user);
					}
				}
			} else if (ln.length() > 0) {
				// test last name
				if (user.getLastName().contains(ln)) {
					// last name matches, add to final list.
					filteredData.add(user);
				}
			} else {
				// add final list.
				filteredData.add(user);
			}
			
		}
		return filteredData;
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
        super.retrieve(req);
	}

}
