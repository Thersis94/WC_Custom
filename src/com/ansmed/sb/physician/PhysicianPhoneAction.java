package com.ansmed.sb.physician;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import com.smt.sitebuilder.action.SBActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

/*****************************************************************************
 <p><b>Title</b>: PhysicianPhoneAction.java</p>
 <p>Description: <b/></p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 1.0
 @since Sep 25, 2007
 Last Updated:
 ***************************************************************************/

public class PhysicianPhoneAction extends SBActionAdapter {
	public static final String PHONE_DATA = "phoneData";
	
	/**
	 * 
	 */
	public PhysicianPhoneAction() {
	}

	/**
	 * @param actionInit
	 */
	public PhysicianPhoneAction(ActionInitVO actionInit) {
		super(actionInit);
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#delete(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void delete(SMTServletRequest req) throws ActionException {
		log.debug("Deleting clinic phone data...");
		String schema = (String)getAttribute("customDbSchema");
		StringBuffer sql = new StringBuffer();
		sql.append("delete from ").append(schema).append("ans_phone ");
		sql.append("where clinic_id = ? ");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter("clinicId"));
			ps.executeUpdate();
		} catch(SQLException sqle) {
			log.error("Error retrieving clinics", sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		String schema = (String)getAttribute("customDbSchema");
		StringBuffer sql = new StringBuffer();
		sql.append("select a.phone_number_txt, a.phone_type_id, type_nm from ");
		sql.append(schema).append("ans_phone a inner join ");
		sql.append(schema).append("ans_phone_type b ");
		sql.append("on a.phone_type_id = b.phone_type_id ");
		sql.append("where clinic_id = ? ");
		sql.append("union ");
		sql.append("select '', phone_type_id, type_nm ");
		sql.append("from ").append(schema).append("ans_phone_type b ");
		sql.append("where phone_type_id not in ( ");
		sql.append("select phone_type_id  ");
		sql.append("from ").append(schema).append("ans_phone "); 
		sql.append("where clinic_id = ?) order by type_nm desc ");
		log.debug("Phone retrv SQL: " + sql + "|" + req.getAttribute(SurgeonSearchAction.CLINIC_ID));
		
		PreparedStatement ps = null;
		List<PhoneVO> phones = new ArrayList<PhoneVO>();
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, (String)req.getAttribute(SurgeonSearchAction.CLINIC_ID));
			ps.setString(2, (String)req.getAttribute(SurgeonSearchAction.CLINIC_ID));
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				PhoneVO vo = new PhoneVO();
				vo.setPhoneNumber(rs.getString("phone_number_txt"));
				vo.setPhoneType(rs.getString("phone_type_id"));
				vo.setTypeName(rs.getString("type_nm"));
				
				// Add the data to the collection
				phones.add(vo);
			}
		} catch(SQLException sqle) {
			log.error("Error retrieving phone numbers", sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		// Add to the request object
		req.setAttribute(PHONE_DATA, phones);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		log.debug("Updating phone numbers for the clinic");
		
		// Updating phone 2 step process.  
		// 1 - Delete all entries for that user
		// 2 - Insert new record for all
		
		// Delete the existing if clinic id exists on the req object (Update)
		// Otherwise get the new clinic id and don't worry about deleting
		String clinicId = StringUtil.checkVal(req.getParameter("clinicId"));
		if (clinicId.length() > 0) {
			delete(req);
		} else {
			clinicId = (String) req.getAttribute(SurgeonSearchAction.CLINIC_ID);
		}
		
		// Insert new entries
		String schema = (String)getAttribute("customDbSchema");
		StringBuffer sql = new StringBuffer();
		sql.append("insert into ").append(schema).append("ans_phone (");
		sql.append("phone_id, phone_type_id, clinic_id, phone_number_txt, ");
		sql.append("phone_country_cd, create_dt) values (?,?,?,?,?,?)" );
		log.debug("Phone Update SQL: " + sql);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			
			// Loop the request object for phone numbers
			Enumeration<String> data = req.getParameterNames();
			while (data.hasMoreElements()) {
				String name = data.nextElement();
				if (name != null && name.indexOf("phone_") > -1) {
					PhoneVO ph = new PhoneVO(req.getParameter(name));
					log.debug("Name: " + name.substring(6) + "-" + ph.getPhoneNumber());
					
					// Make sure there is a phone number added
					if (StringUtil.checkVal(ph.getPhoneNumber()).length() == 0 || ph.getPhoneNumber().length() < 7) continue;
					
					// Store the Phone Number
					ps.setString(1, new UUIDGenerator().getUUID());
					ps.setString(2, name.substring(6));
					ps.setString(3, clinicId);
					ps.setString(4, ph.getPhoneNumber());
					ps.setString(5, ph.getCountry());
					ps.setTimestamp(6, Convert.getCurrentTimestamp());
					
					// add to the batch
					ps.executeUpdate();
				}
			}

		} catch(SQLException sqle) {
			sqle.printStackTrace();
			log.error("Error updating phone numbers", sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
	}

}
