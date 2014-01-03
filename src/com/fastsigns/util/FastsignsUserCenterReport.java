package com.fastsigns.util;

// JDK 1.5.0
import java.sql.PreparedStatement;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

// SMT Base Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;

// SB Libs
import com.smt.sitebuilder.action.registration.RegistrationDataContainer;
import com.smt.sitebuilder.action.registration.RegistrationDataModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: FastsignsUserCenterReport<p/>
 * <b>Description: Generate are report of all users who have registered with a 
 * Franchise.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Aug 2013
 * @update Billy Larsen - Removed ProfileManager middle man.  To many profiles
 * are being returned to adequately handle in the request timeframe.  Moved 
 * decryption to inline.
 ****************************************************************************/
public class FastsignsUserCenterReport extends SBActionAdapter {

	public FastsignsUserCenterReport() {
		super();
	}

	public FastsignsUserCenterReport(ActionInitVO arg0) {
		super(arg0);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#delete(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void delete(SMTServletRequest arg0) throws ActionException {

	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		
		//Create String Encryptor to decrypt profile Data
		StringEncrypter se = null;
		try {
			se = new StringEncrypter((String)getAttribute(Constants.ENCRYPT_KEY));
		} catch (EncryptionException e1) {
			log.error("Could not decrypt information", e1);
		}

		log.info("Starting Registration Data Retrieve");
		RegistrationDataContainer cdc = new RegistrationDataContainer();
		
		String start = req.getParameter("startDate");
		String end = req.getParameter("endDate");
		boolean useDates = start != null || end != null;
		
		// Build the SQL Statement
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT distinct p.PROFILE_ID, p.EMAIL_ADDRESS_TXT, p.FIRST_NM, ");
		sql.append("p.LAST_NM, FRANCHISE_ID, PHONE_NUMBER_TXT ");
		sql.append("FROM PROFILE p left join REGISTER_SUBMITTAL s on p.PROFILE_ID = s.PROFILE_ID ");
		sql.append("left join ").append((String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("FTS_FRANCHISE_ROLE_XR f on p.PROFILE_ID = f.PROFILE_ID "); 
		sql.append("left join PHONE_NUMBER n on n.PROFILE_ID = p.PROFILE_ID ");
		sql.append("WHERE SITE_ID=? "); 
		if (useDates) sql.append("and f.create_dt between ? and ? ");
		sql.append("order by FRANCHISE_ID");
		log.debug(sql + " orgId=" + req.getParameter("organizationId") + " start=" + start + " end=" + end);
		
		int i = 0;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(++i, req.getParameter("organizationId") + "_7");
			if (useDates) {
				ps.setDate(++i, Convert.formatSQLDate(Convert.formatStartDate(start, "1/1/2000")));
				ps.setDate(++i, Convert.formatSQLDate(Convert.formatEndDate(end)));
			}
			
			ResultSet rs = ps.executeQuery();
			String csId = "";
			RegistrationDataModuleVO vo = null;
			while(rs.next()) {
				
				//Build new UserDataVo and set data with decrypted values.
				String newCsId = rs.getString("profile_id"); 
				UserDataVO u = new UserDataVO();
				u.setFirstName(se.decrypt(rs.getString("FIRST_NM")));
				u.setLastName(se.decrypt(rs.getString("LAST_NM")));
				u.setEmailAddress(se.decrypt(rs.getString("EMAIL_ADDRESS_TXT")));
				PhoneVO p = new PhoneVO();
				p.setPhoneNumber(se.decrypt(rs.getString("PHONE_NUMBER_TXT")));
				List<PhoneVO> l = new ArrayList<PhoneVO>();
				l.add(p);
				u.setPhoneNumbers(l);				
				
				
				//set vo if it exsts and/or Instantiate the RegistrationDataModuleVO. 
				if (!csId.equals(newCsId)) {
					if (vo != null)	cdc.addResponse(vo);

					vo = new RegistrationDataModuleVO();
					vo.setData(u);
				}		
				
				// Add the franchise number to the vo
				vo.addExtData("Franchise Number", rs.getString("FRANCHISE_ID"));
				
				// Reset the ids for comparison and increment the counter
				csId = newCsId;
			}

			// add the dangling record
			if (vo != null) cdc.addResponse(vo);

			log.debug("size=" + cdc.getData().size());
			// load UserDataVO's for the users
			//loadProfiles(cdc, profileIds, req.getParameter("organizationId"));
			
			
		} catch (Exception e) {
			log.error("Error getting registrationData for action: ", e);
			throw new ActionException("Error Gettting RegistrationDataAction: ", e);
		} finally {
	        try { ps.close(); } catch(Exception e) {}
		}
		
		log.debug("Finished Retrieving Data: " + cdc.toString());
		req.setAttribute(Constants.REDIRECT_DATATOOL, Boolean.TRUE);
		
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		//test for null SiteVO because the admintool does not populate this consistently (w/public sites)
		super.putModuleData(cdc, cdc.getData().size(), (site == null || site.getAdminFlag() == 1));
	}

}
