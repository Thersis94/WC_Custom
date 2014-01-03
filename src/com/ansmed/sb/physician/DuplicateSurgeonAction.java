package com.ansmed.sb.physician;

// SMT Base Libs 2.0
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

// SMT Base Libs 2.0
import com.ansmed.sb.action.TransactionLoggingAction;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// SB Libs
import com.smt.sitebuilder.common.SiteBuilderUtil;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: DuplicateSurgeonAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since September 15, 2008
 ****************************************************************************/
public class DuplicateSurgeonAction extends SimpleActionAdapter {
    
	public static final String PHYSICIAN_UPDATE = "Physician Update";
	public static final String MSG_SUCCESS = "The physician, clinic, and staff data were successfully duplicated.";
	public static final String MSG_FAILURE = "Error: Unable to duplicate the physician, clinic, phone, and staff data.";
	SiteBuilderUtil util = null;
    
	/**
	 * 
	 */
	public DuplicateSurgeonAction() {
        util = new SiteBuilderUtil();
	}

	/**
	 * @param actionInit
	 */
	public DuplicateSurgeonAction(ActionInitVO actionInit) {
		super(actionInit);
        util = new SiteBuilderUtil();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		String message = "";
		String surgeonId = StringUtil.checkVal(req.getParameter("surgeonId"));
		String newSurgeonId = "";
		String transType = null;
		
		newSurgeonId = new UUIDGenerator().getUUID();
		
        // Prepare the message for the redirect
		StringBuffer url = new StringBuffer();
		url.append(req.getRequestURI()).append("?");
		url.append("&msg=").append(message);
		
		// Set auto commit off so you can roll back
		try {
			dbConn.setAutoCommit(false);
		} catch (SQLException e) {}
				
		try {
			log.debug("Starting physician duplication...");
			duplicatePhysician(req, newSurgeonId);
			duplicateClinics(req, newSurgeonId);
			duplicateStaff(req, newSurgeonId);
			dbConn.commit();
			url.append("&surgeonId=").append(newSurgeonId);
			req.removeAttribute("duplicate");
			
			// set transaction type
			transType = PHYSICIAN_UPDATE;
			log.debug("Successful duplication of physician, clinic, phone, and/or staff data.");
			
		} catch (Exception e) {
			try {
				dbConn.rollback();
				url.append("&surgeonId=").append(surgeonId);
				url.append("&duplicate=true");
				message = MSG_FAILURE;
				log.error("Error: Unable to duplicate the physician information.  Rolling back transaction.");
			} catch(Exception e1) {}
		} finally {
			// Set auto commit back on
			try {
				dbConn.setAutoCommit(true);
			} catch (SQLException e) {}
		}
		
		// log the initial duplication as a physician update. If transType is
		// null, , nothing will be logged.
		this.createTransaction(req, transType, newSurgeonId);
		
		log.debug("Redirect URL: " + url);
		
		// redirect back to the list of areas
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
	
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		String surgeon = StringUtil.checkVal(req.getParameter("surgeonId"));
		Boolean dupePhys = Convert.formatBoolean(req.getParameter("dupePhys"));
		
		if (dupePhys) {
			this.build(req);
		} else {
			log.debug("Initial retrieve...Nothing to duplicate.");
			req.setAttribute("surgeonId",surgeon);
			req.setAttribute("duplicate",true);	
		}
	}
	
	/**
	 * 
	 * @param req
	 * @param newSurgeonId
	 * @throws DatabaseException
	 */
	private void duplicatePhysician(SMTServletRequest req, String newSurgeonId) throws DatabaseException {
		log.debug("Retrieving/duplicating physician data...");
		StringBuffer sql = new StringBuffer();
		String schema = (String) this.getAttribute("customDbSchema");
		String surgeonId = StringUtil.checkVal(req.getParameter("surgeonId"));
		String firstName = StringUtil.checkVal(req.getParameter("firstName"));
		String lastName = StringUtil.checkVal(req.getParameter("lastName"));
		String titleName = StringUtil.checkVal(req.getParameter("title"));
		
		sql.append("insert into ").append(schema).append("ans_surgeon (");
		sql.append("surgeon_type_id, status_id, specialty_id, sales_rep_id, ");
		sql.append("first_nm, last_nm, title_nm, website_url, ");
		sql.append("allow_mail_flg, spanish_flg, board_cert_flg, "); 
		sql.append("fellowship_flg, create_dt, surgeon_id) ");
		sql.append("select surgeon_type_id, status_id, specialty_id, sales_rep_id, ");
		sql.append("?, ?, ?, website_url, allow_mail_flg, spanish_flg, ");
		sql.append("board_cert_flg, fellowship_flg, ").append("?, ? ");
		sql.append("from ").append(schema).append("ans_surgeon ");
		sql.append("where surgeon_id = ?");
		
		log.debug("Duplicate Physician Insert sql: " + sql);
		log.debug("Params: " + firstName + "|" + lastName + "|" + titleName + "|" + newSurgeonId + "|" + surgeonId);
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1,firstName);
			ps.setString(2,lastName);
			ps.setString(3,titleName);
			ps.setTimestamp(4,Convert.getCurrentTimestamp());
			ps.setString(5,newSurgeonId);
			ps.setString(6,surgeonId);
		
			ps.execute();
		} catch (SQLException sqle) {
			throw new DatabaseException("Could not insert new physician data.", sqle);
		} finally {
			if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
		}
	}
	
	/**
	 * 
	 * @param req
	 * @param newSurgeon
	 * @throws DatabaseException
	 */
	private void duplicateClinics(SMTServletRequest req, String newSurgeon) throws DatabaseException {
		log.debug("Retrieving/duplicating clinic data...");
		StringBuffer sql = new StringBuffer();
		List<String> clinics = new ArrayList<String>();
		// Map: key = old clinic ID, value = new clinic ID.
		Map<String,String> mClinics = new HashMap<String,String>();
		String schema = (String) this.getAttribute("customDbSchema");
		String surgeonId = StringUtil.checkVal(req.getParameter("surgeonId"));
		String clinicId = "";
		
		sql.append("select clinic_id from ").append(schema).append("ans_clinic ");
		sql.append("where surgeon_id = ?");
		
		PreparedStatement ps = null;

		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, surgeonId);
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				clinics.add(rs.getString("clinic_id"));
			}
		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to retrieve clinic information for surgeon.", sqle);
		}

		if (clinics.isEmpty()) return;
		
		ps = null;
		sql = new StringBuffer();

		sql.append("insert into ").append(schema).append("ans_clinic (");
		sql.append("surgeon_id, clinic_nm, address_txt, address2_txt, ");
		sql.append("address3_txt, city_nm, state_cd, zip_cd, latitude_no, ");
		sql.append("longitude_no, geo_match_cd, location_type_id, ");
		sql.append("locator_display_flg, create_dt, clinic_id) ");
		sql.append("select ?, clinic_nm, address_txt, address2_txt, ");
		sql.append("address3_txt, city_nm, state_cd, zip_cd, latitude_no, ");
		sql.append("longitude_no, geo_match_cd, location_type_id, ");
		sql.append("locator_display_flg, ?, ? from ").append(schema);
		sql.append("ans_clinic where surgeon_id = ? and clinic_id = ?");
		
		log.debug("Duplicate Clinic Insert sql: " + sql + "|" + newSurgeon);

		try {
			ps = dbConn.prepareStatement(sql.toString());
			
			for(int i = 0; i < clinics.size(); i++) {
				clinicId = new UUIDGenerator().getUUID();
				mClinics.put(clinics.get(i),clinicId);
				log.debug("New clinic ID for insert: " + clinicId);
				ps.setString(1,newSurgeon);
				ps.setTimestamp(2, Convert.getCurrentTimestamp());
				ps.setString(3,clinicId);
				ps.setString(4,surgeonId);
				ps.setString(5,clinics.get(i));
				ps.addBatch();
			}
			
			ps.executeBatch();

		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to insert clinic info for duplicated physician.", sqle);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e1) {}
			}
		}
		
		// After clinic duplication, we need to duplicate phone records.
		duplicatePhones(req, mClinics);
	}
	
	/**
	 * 
	 * @param req
	 * @param mClinics
	 * @throws DatabaseException
	 */
	private void duplicatePhones(SMTServletRequest req, Map<String,String> mClinics) throws DatabaseException {
		log.debug("Retrieving/duplicating phone data...");
		StringBuffer sql = new StringBuffer();
		String schema = (String) this.getAttribute("customDbSchema");
		
		String phoneId = "";
		String newPhoneId = "";
		String oldClinicId = "";
		Set<String> keys = mClinics.keySet();
		Iterator<String> cIter = keys.iterator();
		
		Map<String,String> phones = new HashMap<String,String>();
		
		PreparedStatement ps = null;
		// Get the phone data for each clinic.
		sql.append("select phone_id, clinic_id from ").append(schema);
		sql.append("ans_phone where clinic_id = ?");
		
		try {
			
			ps = dbConn.prepareStatement(sql.toString());
			
			while (cIter.hasNext()) {
				ps.setString(1, cIter.next());
				ResultSet rs = ps.executeQuery();
				while(rs.next()) {
					phones.put(rs.getString(1), rs.getString(2));
				}
			}
			
		} catch (SQLException sqle) {
			throw new DatabaseException("Could not retrieve phone data for each clinic.", sqle);
		}
		
		if (phones.isEmpty()) {
			log.debug("No phone information exists for clinics.");
			return;
		}
		
		// Insert phone data based on the phone/clinic map.
		Set<String> pKeys = phones.keySet();
		Iterator<String> pIter = pKeys.iterator();
		
		ps = null;
		sql = new StringBuffer();
		
		sql.append("insert into ").append(schema).append("ans_phone (");
		sql.append("phone_id, phone_type_id, clinic_id, area_cd, exchange_no, ");
		sql.append("line_no, extension_no, phone_number_txt, phone_country_cd, create_dt) ");
		sql.append("select ?, phone_type_id, ?, area_cd, exchange_no, line_no, ");
		sql.append("extension_no, phone_number_txt, phone_country_cd, ? from ").append(schema).append("ans_phone ");
		sql.append("where phone_id = ?");
		
		log.debug("Duplicate phone insert sql: " + sql.toString());
		
		try {
			ps = dbConn.prepareStatement(sql.toString());
			
			while (pIter.hasNext()) {
				phoneId = pIter.next();
				oldClinicId = phones.get(phoneId);
				newPhoneId = new UUIDGenerator().getUUID();

				ps.setString(1,newPhoneId);
				ps.setString(2,mClinics.get(oldClinicId));
				ps.setTimestamp(3, Convert.getCurrentTimestamp());
				ps.setString(4,phoneId);
				ps.addBatch();
				log.debug("New phone ID for insert: " + newPhoneId);
				log.debug("oldphoneid, oldclinicid, newphoneid, newclinicid: " + phoneId + "," + oldClinicId + "," + newPhoneId + "," + mClinics.get(oldClinicId));
			}
			
			ps.executeBatch();

		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to insert phone info for duplicated physician.", sqle);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {}
			}
		}
	}
	
	/**
	 * 
	 * @param req
	 * @param newSurgeon
	 * @throws DatabaseException
	 */
	private void duplicateStaff(SMTServletRequest req, String newSurgeon) throws DatabaseException {
		log.debug("Retrieving/duplicating staff data...");
		StringBuffer sql = new StringBuffer();
		List<String> staff = new ArrayList<String>();
		String schema = (String) this.getAttribute("customDbSchema");
		String surgeonId = StringUtil.checkVal(req.getParameter("surgeonId"));
		String staffId = "";
		
		// Retrieve staff associated with surgeon being duplicated.
		sql.append("select staff_id from ").append(schema).append("ans_staff ");
		sql.append("where surgeon_id = ?");
		
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, surgeonId);
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				staff.add(rs.getString("staff_id"));
			}
		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to retrieve staff info for physician.", sqle);
		}

		ps = null;
		sql = new StringBuffer();
		
		if(staff.isEmpty()) {
			log.debug("No staff to duplicate.");
			return;
		}
		
		// Create and insert duplicated staff records.
		sql.append("insert into ").append(schema).append("ans_staff (");
		sql.append("surgeon_id, staff_type_id, member_nm, email_txt, ");
		sql.append("phone_no, comments_txt, create_dt, staff_id) ");
		sql.append("select ?, staff_type_id, member_nm, email_txt, ");
		sql.append("phone_no, comments_txt, ?, ? from ").append(schema);
		sql.append("ans_staff where surgeon_id = ? and staff_id = ?");
		
		try {
			ps = dbConn.prepareStatement(sql.toString());
			
			log.debug("Duplicate Staff SQL: " + sql);
			log.debug("Staff size = " + staff.size());
			
			for(int i = 0; i < staff.size(); i++) {
				staffId = new UUIDGenerator().getUUID();
				log.debug("New staff ID for insert: " + staffId + " | index: " + i);
				ps.setString(1,newSurgeon);
				ps.setTimestamp(2, Convert.getCurrentTimestamp());
				ps.setString(3,staffId);
				ps.setString(4,surgeonId);
				ps.setString(5,staff.get(i));
				ps.addBatch();
			}
			
			ps.executeBatch();
			
		} catch (SQLException sqle) {
			throw new DatabaseException("Unable to insert staff data for duplicated physician.", sqle);
		} finally {
			if (ps != null) {
	        	try {
	        		ps.close();
	        	} catch(Exception e) {}
        	}
		}
	}
	
	/**
	 * Calls the TransactionLoggingAction to log the Phys db transaction type.
	 * @param req
	 * @param type
	 */
	public void createTransaction(SMTServletRequest req, String type, String newSurgeonId) throws ActionException {
		
		// if transaction type is specified, log it.
		if (type != null) {
			// set the transaction type on the request
       		SMTActionInterface sai = new TransactionLoggingAction(this.actionInit);
       		sai.setAttribute(TransactionLoggingAction.TRANSACTION_TYPE, type);
       		sai.setAttribute(TransactionLoggingAction.SURGEON_ID, newSurgeonId);
       		sai.setAttributes(this.attributes);
       		sai.setDBConnection(dbConn);
       		sai.build(req);
		}
		
		return;	
	}
}
