package com.ansmed.sb.action;

// JDK 1.6
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

// SB libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

// SMT base libs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

/*****************************************************************************
<p><b>Title</b>: PhysicianAltQualDataAction.java</p>
<p><b>Description<b/>: Retrieves or inserts the physician's alternate event 
qualification data.  This is up-to-date qualification data that a TM enters for
a surgeon in order to calculate event qualification based on the latest available
data.</p>
<p>Copyright: Copyright (c) 2000 - 2009 SMT, All Rights Reserved</p>
<p>Company: Silicon Mountain Technologies</p>
@author Dave Bargerhuff
@version 1.0
@since Oct 12, 2009
Last Updated:
***************************************************************************/
public class PhysicianAltQualDataAction extends SimpleActionAdapter {

	public static final String PHYS_ALT_QUAL_DATA_VO = "physQualDataVo";
	
	/**
	 * 
	 */
	public PhysicianAltQualDataAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public PhysicianAltQualDataAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
	public void build(ActionRequest req) throws ActionException {
		
		// delete the surgeon's existing alternate qualification data.
		this.delete(req);
		
		// insert the surgeon's new alternate qualification data.
		final String schema = (String) this.getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		StringBuffer sql = new StringBuffer();
		sql.append("insert into ").append(schema).append("ans_event_surgeon_altqualdata ");
		sql.append("(altqualdata_id, surgeon_id, sjm_trials_no, sjm_perms_no, ");
		sql.append("bsx_trials_no, bsx_perms_no, mdt_trials_no, mdt_perms_no, create_dt) ");
		sql.append("values (?,?,?,?,?,?,?,?,?) ");
		
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, req.getParameter("surgeonId"));
			ps.setInt(3, Convert.formatInteger(StringUtil.checkVal(req.getParameter("sjmTrials"))));
			ps.setInt(4, Convert.formatInteger(StringUtil.checkVal(req.getParameter("sjmPerms"))));
			ps.setInt(5, Convert.formatInteger(StringUtil.checkVal(req.getParameter("bsxTrials"))));
			ps.setInt(6, Convert.formatInteger(StringUtil.checkVal(req.getParameter("bsxPerms"))));
			ps.setInt(7, Convert.formatInteger(StringUtil.checkVal(req.getParameter("mdtTrials"))));
			ps.setInt(8, Convert.formatInteger(StringUtil.checkVal(req.getParameter("mdtPerms"))));
			ps.setTimestamp(9, Convert.getCurrentTimestamp());
			
			ps.execute();
			
			// Update the surgeon's specialty_id, scs_start_dt.
			updateSurgeonData(req);
			
		} catch (SQLException sqle) {
			log.debug("Error inserting physician's alternate qualification data.", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		
	}
	
    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#delete(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		
		final String schema = (String) this.getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String surgeonId = StringUtil.checkVal(req.getParameter("surgeonId"));
		
		// If no surgeon ID then no delete operation.
		if (surgeonId.length() == 0) return;
		
		StringBuffer sql = new StringBuffer();
		sql.append("delete from ").append(schema).append("ans_event_surgeon_altqualdata ");
		sql.append("where surgeon_id = ? ");
		
		log.debug("delete SQL: " + sql.toString() + " | " + surgeonId);
		
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1,surgeonId);
			ps.execute();
		} catch (SQLException sqle) {
			log.error("Error deleting physician's alternate qualification data.", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		
	}
	
	
    /* (non-Javadoc)
     * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
     */
	@Override
    public void retrieve(ActionRequest req) throws ActionException {
		
    	PhysQualDataVO vo = null;
    	
		final String schema = (String) this.getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String surgeonId = StringUtil.checkVal(req.getParameter("surgeonId"));
		StringBuffer sql = new StringBuffer();

		sql.append("select a.surgeon_id, a.profile_id, a.first_nm, a.last_nm, ");
		sql.append("a.title_nm, a.surgeon_type_id, a.scs_start_dt, ");
		sql.append("a.specialty_id, b.sjm_trials_no, b.sjm_perms_no, b.bsx_trials_no, ");
		sql.append("b.bsx_perms_no, b.mdt_trials_no, b.mdt_perms_no from ");
		sql.append(schema).append("ans_surgeon a left join ").append(schema);
		sql.append("ans_event_surgeon_altqualdata b on a.surgeon_id = b.surgeon_id ");
		sql.append("where a.surgeon_id = ? ");
				
		log.debug("Physician Event Alt Qual sql=" + sql);
		
	    PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, surgeonId);
			ResultSet rs = ps.executeQuery();
			vo = new PhysQualDataVO();
			if (rs.next()) {
				vo.setAlternateData(rs); 
			}
			log.debug("Implant count: " + vo.getImplantCnt());
		} catch (SQLException sqle) {
			log.error("Error retrieving physician's alternate qualification data.", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		
		log.debug("PhysQualDataVO = " + vo.toString());
		req.setAttribute(PHYS_ALT_QUAL_DATA_VO, vo);
    
	}
	
	/**
	 * Updates the physician's specialty ID and SCS start date in the 
	 * surgeon table if the values on the request are valid.
	 * @param req
	 * @throws SQLException
	 */
	private void updateSurgeonData(ActionRequest req) throws SQLException {
		
		// If specialty_id and/or scs_start_dt are not valid, return.
		String specialty = StringUtil.checkVal(req.getParameter("altSpecialtyId"));
		String scs_start_dt = StringUtil.checkVal(req.getParameter("altSCSStartDate"));
		
		Integer specId = Convert.formatInteger(specialty);
		Timestamp scsTimestamp = Convert.formatTimestamp("MM/dd/yyyy", scs_start_dt);
		
		if (specId == 0 || scsTimestamp == null) return;
		
		// Update surgeon specialty and scs start date values.
		final String schema = (String) this.getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String surgeonId = StringUtil.checkVal(req.getParameter("surgeonId"));
		
		StringBuffer sql = new StringBuffer();
		
		sql.append("update ").append(schema).append("ans_surgeon ");
		sql.append("set specialty_id = ?, scs_start_dt = ? where surgeon_id = ? ");
		
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sql.toString());
		
			ps.setInt(1, Convert.formatInteger(StringUtil.checkVal(req.getParameter("altSpecialtyId"))));
			ps.setTimestamp(2, Convert.formatTimestamp("MM/dd/yyyy", StringUtil.checkVal(req.getParameter("altSCSStartDate"))));
			ps.setString(3, surgeonId);

			ps.execute();
		} catch(SQLException sqle) {
			throw new SQLException("Error updating surgeon specialty and start date.",sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {
				log.error("Failed to close the prepared statement.", e);
			}
		}
	}
	
}
