package com.ansmed.sb.physician;

// JDK 1.5.0
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// JDK 1.5.0
import com.smt.sitebuilder.action.SBActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

/*****************************************************************************
 <p><b>Title</b>: PhysicianStaffAction.java</p>
 <p>Description: <b/>Manages the information for staff personnel
 assigned to a physician</p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 1.0
 @since Sep 22, 2007
 Last Updated:
 ***************************************************************************/

public class PhysicianStaffAction extends SBActionAdapter {
	public static final String STAFF_DATA = "staffData";
	
	/**
	 * 
	 */
	public PhysicianStaffAction() {
	}

	/**
	 * @param actionInit
	 */
	public PhysicianStaffAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("Updating staff info: " + req.getParameter("surgeonId"));
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
		log.debug("****************** Deleting Staff");
		String message = "You have successfully deleted the staff information";
		String schema = (String)getAttribute("customDbSchema");
		StringBuffer sql = new StringBuffer();
		sql.append("delete from ").append(schema).append("ans_staff where staff_id = ?");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter("staffId"));
			int count = ps.executeUpdate();
			if (count == 0) message = "Unable to delete staff information";
		} catch(SQLException sqle) {
			log.error("Error deleting ans physician staff", sqle);
			message = "Unable to delete staff information";
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
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		String schema = (String)getAttribute("customDbSchema");
		StringBuffer sql = new StringBuffer();
		sql.append("select * from ").append(schema).append("ans_staff ");
		sql.append("where surgeon_id = ? ");
		log.debug("ANS Staff SQL: " + sql + "|" + req.getParameter("surgeonId"));
		
		PreparedStatement ps = null;
		List<StaffVO> data = new ArrayList<StaffVO>();
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter("surgeonId"));
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				data.add(new StaffVO(rs));
			}
		} catch(SQLException sqle) {
			log.error("Error retrieving ans physician staff", sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		// Add the data to the request object
		req.setAttribute(STAFF_DATA, data);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		String message = "You have successfully updated the staff information";
		// Build the SQL Statement
		String schema = (String)getAttribute("customDbSchema");
		StringBuffer sql = new StringBuffer();
		String staffId = StringUtil.checkVal(req.getParameter("staffId"));
		if(staffId.length() > 0) {
			sql.append("update ").append(schema).append("ans_staff set ");
			sql.append("staff_type_id = ?, surgeon_id = ?, member_nm = ?, ");
			sql.append("email_txt = ?, phone_no = ?, comments_txt = ?, ");
			sql.append("create_dt = ? where staff_id = ?");
		} else {
			staffId = new UUIDGenerator().getUUID();
			sql.append("insert into ").append(schema).append("ans_staff (");
			sql.append("staff_type_id, surgeon_id, member_nm, email_txt, ");
			sql.append("phone_no, comments_txt, create_dt, staff_id) ");
			sql.append("values (?,?,?,?,?,?,?,?)");
		}
		
		log.debug("Staff Update SQL: " + sql);
		
		// Update the DB
		PreparedStatement ps = null;
		try {
			PhoneVO ph = new PhoneVO(req.getParameter("phoneNumber"));
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter("staffTypeId"));
			ps.setString(2, req.getParameter("surgeonId"));
			ps.setString(3, req.getParameter("memberName"));
			ps.setString(4, req.getParameter("emailAddress"));
			ps.setString(5, ph.getPhoneNumber());
			ps.setString(6, req.getParameter("comments"));
			ps.setTimestamp(7, Convert.getCurrentTimestamp());
			ps.setString(8, staffId);
			
			int count = ps.executeUpdate();
			if (count == 0) message = "Unable to update staff information";
		} catch(SQLException sqle) {
			log.error("Error retrieving ans physician staff", sqle);
			message = "Unable to update staff information";
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		req.setAttribute(PhysicianDataFacadeAction.ANS_PHYSICIAN_MESSAGE, message);
	}

}
