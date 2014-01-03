package com.ansmed.sb.physician;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.smt.sitebuilder.action.SBActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.address.AbstractAddressFormatter;
import com.siliconmtn.gis.AbstractGeocoder;
import com.siliconmtn.gis.GeocodeFactory;
import com.siliconmtn.gis.GeocodeLocation;
import com.siliconmtn.gis.Location;
import com.siliconmtn.gis.MatchCode;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

/*****************************************************************************
 <p><b>Title</b>: PhysicianClinicAction.java</p>
 <p>Description: <b/>Manages the physician Info for a clinic</p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 1.0
 @since Sep 22, 2007
 Last Updated:
 ***************************************************************************/

public class PhysicianClinicAction extends SBActionAdapter {
	public static final String CLINIC_DATA = "clinicData";
	
	/**
	 * 
	 */
	public PhysicianClinicAction() {
	}

	/**
	 * @param actionInit
	 */
	public PhysicianClinicAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("Deleting Clinic ... " + req.getParameter("deleteEle"));
		if(Convert.formatBoolean(req.getParameter("deleteEle"))) {
			delete(req);
		} else {
			update(req);
		}

	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#delete(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void delete(SMTServletRequest req) throws ActionException {
		log.debug("****************** Deleting clinic");
		String message = "You have successfully deleted the clinic information";
		String schema = (String)getAttribute("customDbSchema");
		StringBuffer sql = new StringBuffer();
		sql.append("delete from ").append(schema).append("ans_clinic where clinic_id = ?");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter("clinicId"));
			int count = ps.executeUpdate();
			if (count == 0) message = "Unable to delete clinic information";
		} catch(SQLException sqle) {
			log.error("Error deleting ans physician clinic", sqle);
			message = "Unable to delete clinic information";
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		req.setAttribute(PhysicianDataFacadeAction.ANS_PHYSICIAN_MESSAGE, message);

	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		String schema = (String)getAttribute("customDbSchema");
		String clinicId = StringUtil.checkVal(req.getParameter("clinicId"));
		StringBuffer sql = new StringBuffer();
		sql.append("select a.*, type_nm from ").append(schema).append("ans_clinic a ");
		sql.append("inner join ").append(schema).append("ans_lu_location_type b ");
		sql.append("on a.location_type_id = b.location_type_id ");
		sql.append("where surgeon_id = ? ");
		if (clinicId.length() > 0) sql.append("and clinic_id = ? ");
		sql.append("order by type_nm desc ");
		log.debug("ANS Clinic SQL: " + sql + "|" + req.getParameter("surgeonId"));
		
		PreparedStatement ps = null;
		List<ClinicVO> data = new ArrayList<ClinicVO>();
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter("surgeonId"));
			if (clinicId.length() > 0) ps.setString(2, req.getParameter("clinicId"));
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				// Load the clinic data
				ClinicVO cl = new ClinicVO(rs);
				req.setAttribute(SurgeonSearchAction.CLINIC_ID, cl.getClinicId());
				
				// Get the phone data
				SMTActionInterface ca = new PhysicianPhoneAction(this.actionInit);
				ca.setAttributes(this.attributes);
				ca.setDBConnection(dbConn);
				ca.retrieve(req);
				List<PhoneVO> phones = (List<PhoneVO>)req.getAttribute(PhysicianPhoneAction.PHONE_DATA);
				cl.setPhones(phones);
				
				// add to the collection
				data.add(cl);
			}
		} catch(SQLException sqle) {
			log.error("Error retrieving clinics", sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		// Add the data to the request object
		req.setAttribute(CLINIC_DATA, data);

	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		log.debug("Updating clinic");
		String message = "You have successfully updated the clinic information";
		ClinicVO vo = new ClinicVO(req);
		String surgeonId = StringUtil.checkVal(req.getParameter("surgeonId"));
		if (surgeonId.length() == 0) 
			surgeonId = (String)req.getAttribute(SurgeonSearchAction.SURGEON_ID);
		
		/*
		 * If this location is being set to the 'primary' location, reset the other
		 * locations to 'not primary'. If this location is being set to 'not primary',
		 * make sure that it is not already the 'primary' location.  Otherwise the
		 * surgeon will end up with no 'primary' location and will be filtered out 
		 * of the 'physician list' because we filter the SQL query using 
		 * a 'location_type_id = 1' condition in the query.
		*/
		if(vo.getLocationTypeId() == 1) {
			this.resetPrimaryLocation(surgeonId);
		} else {
			// get the primary location
			String primaryClinic = this.retrievePrimaryClinic(surgeonId);
			// make sure we're not trying to set the 'primary' to 'not primary'
			if (primaryClinic.equalsIgnoreCase(vo.getClinicId())) {
				vo.setLocationTypeId(1);
			}
		}
		
		// CASS Validate the location
		Location loc = vo.getLocation();
		String cName = "com.siliconmtn.address.DotsAddressFormatter";
		AbstractAddressFormatter aaf = AbstractAddressFormatter.getInstance(cName);
		try {
			Location cassLoc = aaf.checkAddress(loc);
			cassLoc.setAddress2(loc.getAddress2());
			if (cassLoc.isValidAddress()) vo.setLocation(cassLoc);
		} catch(Exception e) {}
		
		// Geocode the address 
		log.debug("geocoding");
		Integer manualGeocodeFlag = Convert.formatInteger(req.getParameter("manualGeocodeFlag"));
		
		if (manualGeocodeFlag == 0) {
		AbstractGeocoder geo = GeocodeFactory.getInstance((String)getAttribute(GlobalConfig.GEOCODER_CLASS));
			geo.addAttribute(AbstractGeocoder.CONNECT_URL, (String) getAttribute(GlobalConfig.GEOCODER_URL));
			GeocodeLocation geocodeLocation = geo.geocodeLocation(vo.getLocation()).get(0);
			
			// If standard geocoding fails, use alternate lookup against static zipcode table
			if ((geocodeLocation.getLatitude() == 0 && geocodeLocation.getLongitude() == 0) 
				|| geocodeLocation.getMatchCode() == MatchCode.noMatch) {
				log.debug("Using alternate geocode lookup for clinic for zip: " + vo.getLocation().getZipCode());
				geocodeLocation = this.getGeocodeByZip(vo.getLocation().getZipCode());
				geocodeLocation.setMatchCode(MatchCode.zipCode);
			}
			
			vo.setLatitude(geocodeLocation.getLatitude());
			vo.setLongitude(geocodeLocation.getLongitude());
			vo.setMatchCode(geocodeLocation.getMatchCode());
			
		} else {
			vo.setLatitude(Convert.formatDouble(req.getParameter("latitude")));
			vo.setLongitude(Convert.formatDouble(req.getParameter("longitude")));
			vo.setMatchCode(MatchCode.manual);
		}
		
		// Prepare the query info
		Integer success = 100;
		String schema = (String) this.getAttribute("customDbSchema");
		StringBuffer sql = new StringBuffer();
		String clinicId = StringUtil.checkVal(req.getParameter("clinicId"));
		
		// Build the sql statement
		if(clinicId.length() > 0) {
			sql.append("update ").append(schema).append("ans_clinic set ");
			sql.append("surgeon_id = ?, clinic_nm = ?, address_txt = ?, ");
			sql.append("address2_txt = ?, address3_txt = ?, city_nm = ?, ");
			sql.append("state_cd = ?, zip_cd = ?, latitude_no = ?, longitude_no = ?, ");
			sql.append("geo_match_cd = ?, location_type_id = ?, locator_display_flg = ?, ");
			sql.append("cass_validate_flg = ?, manual_geocode_flg = ?, ");
			sql.append("update_dt = ? where clinic_id = ?");
		} else {
			clinicId = new UUIDGenerator().getUUID();
			sql.append("insert into ").append(schema).append("ans_clinic (");
			sql.append("surgeon_id, clinic_nm, address_txt, address2_txt,");
			sql.append(" address3_txt, city_nm, state_cd, zip_cd, latitude_no, ");
			sql.append("longitude_no, geo_match_cd, location_type_id, ");
			sql.append("locator_display_flg, cass_validate_flg, manual_geocode_flg, ");
			sql.append("create_dt, clinic_id) ");
			sql.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		}
		
		// Update the db
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, surgeonId);
			ps.setString(2, vo.getClinicName());
			ps.setString(3, vo.getAddress());
			ps.setString(4, vo.getAddress2());
			ps.setString(5, vo.getAddress3());
			ps.setString(6, req.getParameter("city"));
			ps.setString(7,  req.getParameter("physState"));
			ps.setString(8, req.getParameter("physZipCode"));
			ps.setDouble(9, vo.getLatitude());
			ps.setDouble(10, vo.getLongitude());
			ps.setString(11, vo.getMatchCode().toString());
			ps.setInt(12, vo.getLocationTypeId());
			ps.setInt(13, vo.getLocatorDisplay());
			ps.setInt(14, vo.getCassValidated());
			ps.setInt(15, manualGeocodeFlag);
			ps.setTimestamp(16, Convert.getCurrentTimestamp());
			ps.setString(17, clinicId);
			
			// Update the db
			int count = ps.executeUpdate();
			if (count == 0) message = "Unable to update clinic information";
		} catch(SQLException sqle) {
			message = "Unable to update clinic information";
			success = -1;
			log.error("Unable to add/update physician", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		
		// Update the phones for the clinic
		req.setAttribute(SurgeonSearchAction.CLINIC_ID, clinicId);
		SMTActionInterface ca = new PhysicianPhoneAction(this.actionInit);
		ca.setAttributes(this.attributes);
		ca.setDBConnection(dbConn);
		ca.update(req);
		
		// Append the surgeon Id to the req obj
		req.setAttribute(PhysicianDataFacadeAction.ANS_PHYSICIAN_MESSAGE, message);
		req.setAttribute(SurgeonSearchAction.FAILED_PHYSICIAN_UPDATE, success);

	}
	
	/**
	 * Sets the primary_location_flg to 0 for all locations
	 * @param surgeonId
	 */
	private void resetPrimaryLocation(String surgeonId) {
		log.debug("resetting primary Location for " + surgeonId);
		String schema = (String) this.getAttribute("customDbSchema");
		StringBuffer sql = new StringBuffer();
		sql.append("update ").append(schema).append("ans_clinic ");
		sql.append("set location_type_id = 3 where surgeon_id = ?");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, surgeonId);
			ps.executeUpdate();
		} catch(SQLException sqle) {
			log.error("Unable to reset primary location", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
	}
	
	/**
	 * Returns list of 'primary' clinic ids for surgeon.
	 * @param surgeonId
	 * @return
	 */
	private String retrievePrimaryClinic(String surgeonId) {
		log.debug("retrieving 'primary' clinic id for surgeon: " + surgeonId);
		
		String schema = (String) this.getAttribute("customDbSchema");
		StringBuffer sql = new StringBuffer();
		sql.append("select clinic_id from ").append(schema).append("ans_clinic ");
		sql.append("where surgeon_id = ? and location_type_id = 1");
		
		String clinic = null;
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, surgeonId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				clinic = StringUtil.checkVal(rs.getString("clinic_id"));
			}
		} catch(SQLException sqle) {
			log.error("Unable to retrieve 'primary' clinic id for surgeon. ", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		return clinic;
	}
	
	/**
	 * Performs geocode lookup from static table if Google geocoding fails.
	 * @param zipCode
	 * @return
	 */
	private GeocodeLocation getGeocodeByZip(String zipCode) {
		
		StringBuffer sql = new StringBuffer();
		sql.append("select * from zip_code where zip_cd = ?");
        
		log.debug("geocodeByZip SQL: " + sql.toString() + "|" + zipCode);
		
        PreparedStatement ps = null;
        GeocodeLocation gl = new GeocodeLocation();
        try {
              ps = dbConn.prepareStatement(sql.toString());
              ps.setString(1, zipCode);
              ResultSet rs = ps.executeQuery();
              if (rs.next()) {
                    //gl.setZipCode(rs.getString("zip_cd"));
                    //gl.setState(rs.getString("state_cd"));
                    //gl.setCity(rs.getString("city_nm"));
                    //gl.setCounty(rs.getString("county_nm"));
                    gl.setLatitude(rs.getDouble("latitude_no"));
                    gl.setLongitude(rs.getDouble("longitude_no"));
              }
              
        } catch(SQLException sqle) {
        	log.error("Error retrieving geocode-by-zip from zip_code table. ", sqle);
        } finally {
        	try {
        		ps.close();
        	} catch(Exception e) {}
        }
        
        return gl;
	}

}
