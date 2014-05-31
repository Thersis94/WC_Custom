package com.ram.action.user;

// JDK 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataComparator;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WebCrescendo 2.0
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

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
		SMTActionInterface sai = new RamUserAction(actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		sai.retrieve(req);
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
		String srchRole = StringUtil.checkVal(req.getParameter("srchRole"));
		String srchCustomerId = StringUtil.checkVal(req.getParameter("srchCustomerId"));
		String srchDeactivated = StringUtil.checkVal(req.getParameter("srchDeactivated"));
		StringBuilder sql = new StringBuilder();
		sql.append("select a.*, b.first_nm, b.last_nm ");
		sql.append("from PROFILE_ROLE a ");
		sql.append("inner join PROFILE b on a.PROFILE_ID = b.PROFILE_ID ");
		sql.append("where SITE_ID = ? ");
		sql.append("and a.STATUS_ID = ? ");
		if (srchRole.length() > 0) sql.append("and a.ROLE_ID = ? ");
		if (srchCustomerId.length() > 0) sql.append("and a.ATTRIB_TXT_1 = ? ");
		
		log.debug("User search SQL: " + sql.toString());
		
		PreparedStatement ps = null;
		int index = 1;
		List<UserDataVO> data = new ArrayList<>();
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(index++, srchSiteId);
			
			// set status
			if (srchDeactivated.length() > 0) {
				log.debug("srchDeactivated: " + srchDeactivated);
				try {
					ps.setInt(index++, Convert.formatInteger(srchDeactivated));
					log.debug("searching status ID of: " + srchDeactivated);
				} catch (NumberFormatException nfe) {
					log.error("Error converting search value to Integer, ", nfe);
					ps.setInt(index++, PROFILE_STATUS_DISABLED);
					log.debug("searching status ID of: " + PROFILE_STATUS_DISABLED);
				}
			} else {
				ps.setInt(index++, PROFILE_STATUS_ACTIVE);
				log.debug("searching status ID of: " + PROFILE_STATUS_ACTIVE);
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
				UserDataVO user = new UserDataVO();
				user.setProfileId(rs.getString("PROFILE_ID"));
				user.setFirstName(rs.getString("FIRST_NM"));
				user.setLastName(rs.getString("LAST_NM"));
				
				SBUserRole su = new SBUserRole();
				su.setProfileRoleId(rs.getString("PROFILE_ROLE_ID"));
				su.setSiteId(rs.getString("SITE_ID"));
				su.setRoleId(rs.getString("ROLE_ID"));
				su.setStatusId(rs.getInt("STATUS_ID"));
				// attrib1Txt contains the customer ID to whom this user is associated.
				su.setAttrib1Txt(rs.getString("ATTRIB_TXT_1"));
				
				// set the role data on the user vo as extended data
				user.setUserExtendedInfo(su);
				data.add(user);
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
		List<UserDataVO> filteredData = filterByUser(data, srchFN, srchLN);
		
		// sort by user name
		Collections.sort(filteredData, new UserDataComparator());
		
		putModuleData(filteredData, filteredData.size(), false, null);
		
	}
	
	/**
	 * Helper method for decrypting user first/last names.
	 * @param users
	 * @param fn
	 * @param ln
	 * @return
	 */
	private List<UserDataVO> filterByUser(List<UserDataVO> users, String fn, String ln) {
		StringEncrypter se = null;
		try {
			se = new StringEncrypter((String)attributes.get(Constants.ENCRYPT_KEY));
		} catch (EncryptionException ee) {
			log.error("Error instantiating StringEncrypter, ", ee);
			return users;
		}
		
		List<UserDataVO> filteredData = new ArrayList<>();
		for (UserDataVO user : users) {
			try {
				user.setFirstName(se.decrypt(user.getFirstName()));
			} catch (Exception e) {log.error("Error decrypting first name, " + e.getMessage());}
			try {
				user.setLastName(se.decrypt(user.getLastName()));
			} catch (Exception e) {log.error("Error decrypting last name, " + e.getMessage());}
			
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
