package com.ansmed.sb.patient;

import java.sql.PreparedStatement;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.UUIDGenerator;

// SB Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>:PatientProcedureAction.java<p/>
 * <b>Description: </b> Manages the data in the  ANS_PATIENT_PROC table
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Feb 13, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class PatientProcedureAction extends SBActionAdapter {

	/**
	 * 
	 */
	public PatientProcedureAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public PatientProcedureAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}
	
	/**
	 * Inserts or updates a record in the database
	 */
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("Updating Stim Tracker");

		// Build the sql statement
		String schema = (String) this.getAttribute("customDbSchema");
		StringBuffer sql = new StringBuffer();

		String id = new UUIDGenerator().getUUID();
		sql.append("insert into ").append(schema).append("ans_patient_proc ");
		sql.append("(stim_tracker_id, entry_lead_txt, final_lead_txt, comments_txt,  trial_removal_dt, ");
		sql.append("perm_dt, trial_facility_txt, perm_facility_txt, education_dt, perm_consult_dt, insurance_txt, ");
		sql.append("trial_dt, sales_rep_nm, sales_rep_id, prep_video_dt, procedure_type_txt, ");
		sql.append("refer_phys_nm, other_refer_nm, create_dt, patient_proc_id) ");
		sql.append("values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ");
		log.debug("ANS Patient Proc SQL: " + sql);
		
		// Execute the insert or update command
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter("patientId"));
			ps.setString(2, req.getParameter("entryLead"));
			ps.setString(3, req.getParameter("finalLead"));
			ps.setString(4, req.getParameter("comments"));
			ps.setDate(5, Convert.formatSQLDate(req.getParameter("trialRemovalDate")));
			ps.setDate(6, Convert.formatSQLDate(req.getParameter("permDate")));
			ps.setString(7, req.getParameter("trialFacility"));
			ps.setString(8, req.getParameter("permFacility"));
			ps.setDate(9, Convert.formatSQLDate(req.getParameter("educationDate")));
			ps.setDate(10, Convert.formatSQLDate(req.getParameter("permConsultDate")));
			ps.setString(11, req.getParameter("insurance"));
			ps.setDate(12, Convert.formatSQLDate(req.getParameter("trialDate")));
			ps.setString(13, req.getParameter("repName"));
			ps.setString(14, req.getParameter("salesRepId"));
			ps.setDate(15, Convert.formatSQLDate(req.getParameter("prepVideoDate")));
			ps.setString(16, req.getParameter("procedureType"));
			ps.setString(17, req.getParameter("referringPhysician"));
			ps.setString(18, req.getParameter("otherPhysician"));
			ps.setTimestamp(19, Convert.getCurrentTimestamp());
			ps.setString(20, id);
			
			ps.executeUpdate();
		} catch (Exception sqle) {
			log.error("Unable to update stim tracker entry", sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
	}

}
