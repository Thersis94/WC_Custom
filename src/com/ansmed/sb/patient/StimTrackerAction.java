package com.ansmed.sb.patient;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.ansmed.sb.physician.SurgeonVO;
import com.ansmed.sb.security.ANSRoleFilter;
import com.ansmed.sb.security.AnsRoleModule;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;


/****************************************************************************
 * <b>Title</b>:StimTrackerAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Feb 10, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class StimTrackerAction extends SBActionAdapter {
	public static final String STIM_UPDATE_SUCCESS_MSG = "You have successfully saved the patient information";
	public static final String STIM_UPDATE_ERROR_MSG = "Unable to save the patient information";
	
	/**
	 * 
	 */
	public StimTrackerAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public StimTrackerAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}
	
	/**
	 * 
	 */
	public void retrieve(ActionRequest req) throws ActionException {
		log.info("Starting Stim Tracker Retrieval");
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		String schema = (String) this.getAttribute("customDbSchema");
		ANSRoleFilter filter = new ANSRoleFilter();
		StringBuffer sql = new StringBuffer();
		sql.append("select a.* from ").append(schema).append("ans_surgeon a ");
		sql.append("inner join ").append(schema).append("ans_sales_rep b ");
		sql.append("on a.sales_rep_id = b.sales_rep_id ");
		sql.append("inner join ").append(schema).append("ans_sales_region c ");
		sql.append("on b.region_id = c.region_id ");
		sql.append("where a.status_id < 10 ");
		
		// Add the role filter
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		sql.append(filter.getSearchFilter(role, "b"));
		sql.append(" order by last_nm, first_nm ");
		log.debug("Retrieve surgeons sql: " + sql);
		log.debug("Role: " + role);
		
		List<SurgeonVO> data = new ArrayList<SurgeonVO>();
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				SurgeonVO vo = new SurgeonVO();
				vo.setSurgeonId(rs.getString("surgeon_id"));
				vo.setFirstName(rs.getString("first_nm"));
				vo.setLastName(rs.getString("last_nm"));
				
				data.add(vo);
			}
		} catch(SQLException sqle) {
			log.error("Unable to retrieve surgeon list", sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		mod.setActionData(data);
		attributes.put(Constants.MODULE_DATA, mod);
	}
	
	/**
	 * Inserts or updates a record in the database
	 */
	public void build(ActionRequest req) throws ActionException {
		log.debug("Updating Stim Tracker");
		String msg = STIM_UPDATE_SUCCESS_MSG;
		
		Boolean isUpdate = false;
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		String regionId = StringUtil.checkVal(role.getAttribute(AnsRoleModule.ANS_REGION_ID));
		String patientId = StringUtil.checkVal(req.getParameter("patientId"));
		String physician = StringUtil.checkVal(req.getParameter("physician"));
		String schema = (String) this.getAttribute("customDbSchema");
		
		// Validate the input
		int patientFirstName = StringUtil.checkVal(req.getParameter("patientFirstName")).length();
		int patientLastName = StringUtil.checkVal(req.getParameter("patientLastName")).length();
		if (physician.length() == 0 || patientFirstName == 0 || patientLastName == 0) {
			
			// redirect back to the list of areas
			String url = this.buildPostUrl(req);
			url += "&msg=You must enter data for all required fields";
			req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
			req.setAttribute(Constants.REDIRECT_URL, url);
			return;
		}
		
		// Process the data elements
		if (regionId.length() == 0) regionId = getRegionId(physician, schema);
		if (patientId.length() > 0) isUpdate = true;
		
		// Build the sql statement
		StringBuffer sql = new StringBuffer();
		
		if (isUpdate) {
			sql.append("update ").append(schema).append("ans_stim_tracker ");
			sql.append("set surgeon_id = ?, patient_first_nm = ?, patient_last_nm = ?, ");
			sql.append("search_first_nm = ?, search_last_nm = ?, patient_phone_no = ?,");
			sql.append("region_id = ?, create_dt = ? ");
			sql.append("where stim_tracker_id = ?");
		} else {
			patientId = new UUIDGenerator().getUUID();
			sql.append("insert into ").append(schema).append("ans_stim_tracker ");
			sql.append("(surgeon_id, patient_first_nm, patient_last_nm, ");
			sql.append("search_first_nm, search_last_nm, patient_phone_no, ");
			sql.append("region_id, create_dt,  ");
			sql.append("stim_tracker_id) values (?,?,?,?,?,?,?,?,?) ");
		}
		
		// Execute the insert or update command
		PreparedStatement ps = null;
		StringEncrypter se = null;
		try {
			PhoneVO phone = new PhoneVO(req.getParameter("patientPhoneNumber"));
			se = new StringEncrypter((String)getAttribute(Constants.ENCRYPT_KEY));
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, physician);
			ps.setString(2, se.encrypt(StringUtil.checkVal(req.getParameter("patientFirstName"))));
			ps.setString(3, se.encrypt(StringUtil.checkVal(req.getParameter("patientLastName"))));
			ps.setString(4, se.encrypt(StringUtil.checkVal(req.getParameter("patientFirstName")).toUpperCase()));
			ps.setString(5, se.encrypt(StringUtil.checkVal(req.getParameter("patientLastName")).toUpperCase()));
			ps.setString(6, phone.getPhoneNumber());
			ps.setString(7, regionId);
			ps.setTimestamp(8, Convert.getCurrentTimestamp());
			ps.setString(9, patientId);
			ps.executeUpdate();
			
			// Update the procedure information
			req.setParameter("patientId", patientId);
			ActionInterface action = new PatientProcedureAction(this.actionInit);
			action.setDBConnection(dbConn);
			action.setAttributes(attributes);
			action.build(req);
		} catch (Exception sqle) {
			msg = STIM_UPDATE_ERROR_MSG;
			log.error("Unable to update stim tracker entry", sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		// Build the redir url
		String url = req.getRequestURI() + "?msg=" + msg;
		
		// redirect back to the list of areas
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url);
	}
	
	/**
	 * If an admin user updates a record, there is no region associated to the 
	 * request.  
	 * @param id
	 * @param schema
	 * @return
	 */
	public String getRegionId(String id, String schema) {
		StringBuffer sql = new StringBuffer();
		sql.append("select region_id from ").append(schema).append("ANS_SURGEON a ");
		sql.append("inner join ").append(schema).append("ANS_SALES_REP b ");
		sql.append("on a.SALES_REP_ID = b.SALES_REP_ID ");
		sql.append("where SURGEON_ID = ?  ");
		
		// Execute the insert or update command
		PreparedStatement ps = null;
		String regionId = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) regionId = rs.getString(1);
		} catch(Exception e) {
			log.error("Unable to retrieve region id", e);
		}
		
		return regionId;
	}
}
