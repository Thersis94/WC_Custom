package com.ram.action.user;

// JDK 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import java.util.Map;

// RAMDataFeed Libs
import com.ram.datafeed.data.RAMUserVO;

//SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WebCrescendo 2.0
import com.smt.sitebuilder.action.SBActionAdapter;
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
		SMTActionInterface sai = null;
		sai = new RamUserAction(actionInit);
		sai.setAttributes(attributes);
		sai.setDBConnection(dbConn);
		sai.build(req);		
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
		
		String ramSchema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		StringBuilder sql = new StringBuilder();
		sql.append("select a.*, b.FIRST_NM, b.LAST_NM, b.EMAIL_ADDRESS_TXT, ");
		sql.append("c.ROLE_ORDER_NO, c.ROLE_NM, d.PHONE_NUMBER_TXT, ");
		sql.append("e.CUSTOMER_ID, e.CUSTOMER_NM, f.AUDITOR_ID ");
		sql.append("from PROFILE_ROLE a ");
		sql.append("inner join PROFILE b on a.PROFILE_ID = b.PROFILE_ID ");
		sql.append("inner join ROLE c on a.ROLE_ID = c.ROLE_ID ");
		sql.append("left outer join PHONE_NUMBER d on a.PROFILE_ID = d.PROFILE_ID and d.PHONE_TYPE_CD = 'HOME' ");
		sql.append("left outer join ").append(ramSchema).append("RAM_CUSTOMER e ");
		sql.append("on a.ATTRIB_TXT_1 = e.CUSTOMER_ID ");
		sql.append("left outer join ").append(ramSchema).append("RAM_AUDITOR f ");
		sql.append("on a.PROFILE_ID = f.PROFILE_ID ");
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
		log.debug("original data size: " + data.size());
		// decrypt first/last name and filter by user if applicable
		List<RAMUserVO> filteredData = filterByUser(req, data, srchFN, srchLN);
		log.debug("filtered data size: " + filteredData.size());
		// sort by user name
		Collections.sort(filteredData, new RAMUserComparator());
		
		// paginate
		List<RAMUserVO> paginatedData = paginateData(req, filteredData);
		log.debug("paginated data size: " + paginatedData.size());
		
		Map<String, Object> rData = new HashMap<>();
		rData.put("count", filteredData.size());
		rData.put("actionData", paginatedData);
		rData.put(GlobalConfig.SUCCESS_KEY, Boolean.TRUE);
		this.putModuleData(rData, 3, false);
		
		//putModuleData(paginatedData, filteredData.size(), false, null);
		
	}
	
	/**
	 * Helper method for decrypting user first/last names.
	 * @param users
	 * @param fn
	 * @param ln
	 * @return
	 */
	private List<RAMUserVO> filterByUser(SMTServletRequest req, List<RAMUserVO> users, String fn, String ln) {
		StringEncrypter se = null;
		try {
			se = new StringEncrypter((String)attributes.get(Constants.ENCRYPT_KEY));
		} catch (EncryptionException ee) {
			log.error("Error instantiating StringEncrypter, ", ee);
			return users;
		}
		
		List<RAMUserVO> filteredData = new ArrayList<>();
		for (RAMUserVO user : users) {
			// decrypt certain fields
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
			
			String tmp = null;
			if (fn.length() > 0) {
				tmp = user.getFirstName().toLowerCase();
				if (tmp.contains(fn.toLowerCase())) {
					// first name matches, check last name
					if (ln.length() > 0) {
						tmp = user.getLastName().toLowerCase();
						// test last name
						if (tmp.contains(ln.toLowerCase())) {
							// last name matches also, add to final list.
							filteredData.add(user);
						}
					} else {
						// add to final list.
						filteredData.add(user);
					}
				}
			} else if (ln.length() > 0) {
				tmp = user.getLastName().toLowerCase();
				// test last name
				if (tmp.contains(ln.toLowerCase())) {
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
	
	/**
	 * Loops the sorted list and returns a list containing the records for the page number that was requested.
	 * @param req
	 * @param sortedList
	 * @return
	 */
	private List<RAMUserVO> paginateData(SMTServletRequest req, List<RAMUserVO> sortedList) {
		int navStart = Convert.formatInteger(req.getParameter("start"), 0);
		int navLimit = Convert.formatInteger(req.getParameter("limit"), 25);
		int navEnd = navStart + navLimit;
		int ctr = -1;
		List<RAMUserVO> paginatedList = new ArrayList<>();
		for (int i = 0; i < sortedList.size(); i++) {
			ctr++;
			// determine which records to add to the paginated list.
			if (ctr >= navStart) {
				if (ctr < navEnd) {
					paginatedList.add(sortedList.get(i));
				} else {
					break;
				}
			} else {
				continue;
			}
		}
		return paginatedList;
	}
	
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
        super.retrieve(req);
	}

}
