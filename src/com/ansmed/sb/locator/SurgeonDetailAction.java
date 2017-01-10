package com.ansmed.sb.locator;

//JDK 1.5
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

//ANS SB Libs
import com.ansmed.sb.physician.ClinicVO;
import com.ansmed.sb.physician.DocumentVO;
import com.ansmed.sb.physician.PhysicianClinicAction;
import com.ansmed.sb.physician.PhysicianDocumentAction;
import com.ansmed.sb.physician.PhysicianStaffAction;
import com.ansmed.sb.physician.StaffVO;
import com.ansmed.sb.physician.SurgeonVO;

// SMT Base Libs 2.0
import com.smt.sitebuilder.action.SBActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.action.ActionRequest;

// SB Libs
import com.smt.sitebuilder.action.gis.MapAction;
import com.smt.sitebuilder.action.gis.MapLocationVO;
import com.smt.sitebuilder.action.gis.MapVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: SurgeonDetailAction.java<p/>
 * <b>Description: </b> Manages the information for retrieving the detail view
 * of the surgeon location.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2007<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Apr 29, 2007
 ****************************************************************************/
public class SurgeonDetailAction extends SBActionAdapter {

	/**
	 * 
	 */
	public SurgeonDetailAction() {
		
	}

	/**
	 * @param arg0
	 */
	public SurgeonDetailAction(ActionInitVO arg0) {
		super(arg0);
		
	}


	public void retrieve(ActionRequest req) throws ActionException {
		// Setup the map info
		MapVO map = new MapVO();
		map.setMapZoomFlag(true);
		map.setBestFitFlag(true);
		map.setMapHeight(300);
		map.setMapWidth(450);
		
		// Retrieve the data for the Physician
		this.getData(req);
		
		// Get the clinics and add the Map Locations to the Map
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		SurgeonVO s = (SurgeonVO) mod.getActionData();
		List<ClinicVO> c = s.getClinics();
		
		for (int i=0; c != null && i < c.size(); i++) {
			map.addLocation(c.get(i).getMapLocation());
		}
		
		// add the map data to the page
		log.debug("MAP: " + map);
		req.setAttribute(MapAction.MAP_ALT_DATA, map);
	}
	
	
	@SuppressWarnings("unchecked")
	public void getData(ActionRequest req) 
	throws ActionException {
		log.debug("Getting the detail info for a surgeon");
		int count = 0;
		String schema = (String) this.getAttribute("customDbSchema");
		StringBuffer sql = new StringBuffer();
		
		sql.append("select a.*, status_nm, specialty_nm, type_nm from ");
		sql.append(schema).append("ans_surgeon a ");
		sql.append("inner join ").append(schema).append("ans_status b ");
		sql.append("on a.status_id = b.status_id ");
		sql.append("left outer join ").append(schema).append("ans_specialty c ");
		sql.append("on a.specialty_id = c.specialty_id ");
		sql.append("inner join ").append(schema).append("ans_surgeon_type f on a.surgeon_type_id = f.surgeon_type_id ");
		sql.append("where a.surgeon_id = ? ");
		log.debug("ANS Phys Detail Info SQL: " + sql + "|" + req.getParameter("surgeonId"));
		
		PreparedStatement ps = null;
		SurgeonVO vo = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter("surgeonId"));
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				count = 1;
				vo = new SurgeonVO(rs);
				
				// Get the Clinic Info
				SMTActionInterface ca = new PhysicianClinicAction(this.actionInit);
				ca.setAttributes(this.attributes);
				ca.setDBConnection(dbConn);
				ca.retrieve(req);
				List<ClinicVO> clinics = (List<ClinicVO>)req.getAttribute(PhysicianClinicAction.CLINIC_DATA);
				vo.setClinics(clinics);
				
				// Get the Staff Info
				SMTActionInterface sa = new PhysicianStaffAction(this.actionInit);
				sa.setAttributes(this.attributes);
				sa.setDBConnection(dbConn);
				sa.retrieve(req);
				List<StaffVO> staff = (List<StaffVO>)req.getAttribute(PhysicianStaffAction.STAFF_DATA);
				vo.setStaff(staff);
				
				// Get the Documents Info
				log.debug("Getting docs:");
				SMTActionInterface pd = new PhysicianDocumentAction(this.actionInit);
				pd.setAttributes(this.attributes);
				pd.setDBConnection(dbConn);
				pd.retrieve(req);
				List<DocumentVO> docs = (List<DocumentVO>)req.getAttribute(PhysicianDocumentAction.DOCUMENT_DATA);
				vo.setDocuments(docs);
			}

		} catch (SQLException sqle) {
			log.error("Error searching for surgeons", sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		// Return the data to the user
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		mod.setActionData(vo);
		mod.setDataSize(count);
		attributes.put(Constants.MODULE_DATA, mod);
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve1(ActionRequest req) throws ActionException {
		log.debug("Retrieving full surgeon information");
		String dbSch = (String)this.getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuffer sql = new StringBuffer();
		sql.append("select a.*, b.*, c.*, d.type_nm from ").append(dbSch);
		sql.append("ans_surgeon a inner join ");
		sql.append(dbSch).append("ans_clinic b on a.surgeon_id = b.surgeon_id ");
		sql.append("left outer join ").append(dbSch).append("ans_phone c ");
		sql.append("on b.clinic_id = c.clinic_id ");
		sql.append("inner join ").append(dbSch).append("ans_phone_type d ");
		sql.append("on c.phone_type_id = d.phone_type_id where a.surgeon_id = ? ");
		sql.append("and b.clinic_id = ? ");
		log.info("Surgeon Detail SQL: " + sql);
		
		// Setup the map info
		MapVO map = new MapVO();
		map.setMapZoomFlag(true);
		map.setBestFitFlag(true);
		map.setMapHeight(300);
		map.setMapWidth(450);
		
		PreparedStatement ps = null;
		SurgeonVO vo = new SurgeonVO();
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter("surgeonId"));
			ps.setString(2, req.getParameter("clinicId"));
			ResultSet rs = ps.executeQuery();
			
			// get the bulk of the data
			if (rs.next()) {
				vo.setData(rs);
				vo.getClinic().setData(rs);
			}
			
			// Add any additional phones
			while(rs.next()) {
				vo.getClinic().addPhone(rs);
			}
			
			// Add the data for the map
			MapLocationVO mLoc = new MapLocationVO();
			mLoc.setAddress(vo.getClinic().getAddress());
			mLoc.setCity(vo.getClinic().getCity());
			mLoc.setState(vo.getClinic().getState());
			mLoc.setZipCode(vo.getClinic().getZipCode());
			mLoc.setLatitude(vo.getClinic().getLatitude());
			mLoc.setLongitude(vo.getClinic().getLongitude());
			mLoc.setLocationDesc(vo.getClinic().getClinicName());
			map.addLocation(mLoc);
			
			// add the map data to the page
			log.debug("MAP: " + map);
			req.setAttribute(MapAction.MAP_ALT_DATA, map);
		} catch (SQLException sqle) {
			log.error("**********Error getting ANS Surgeon Data: " + req.getAttribute("surgeonId"), sqle);
			throw new ActionException("Error Gettting ANS Surgeon Action: " + sqle.getMessage());
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch(Exception e) {}
			}
		}
		
		// Store the retrieved data in the ModuleVO.actionData and replace into
		// the Map
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		mod.setActionData(vo);
		attributes.put(Constants.MODULE_DATA, mod);
		
	}


}
