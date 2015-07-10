package com.depuysynthes.gfp;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.depuysynthes.gfp.GFPFacadeAction.GFPLevel;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: GFPProgramAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Action for use in handling the display, management, and
 * assignment of GFP programs
 * <b>Copyright:</b> Copyright (c) 2015
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Eric Damschroder
 * @version 1.0
 * @since July 6, 2015
 ****************************************************************************/

public class GFPProgramAction extends SBActionAdapter {

	public void retrieve(SMTServletRequest req) throws ActionException {
		String programId = StringUtil.checkVal(req.getParameter("programId"));
		UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
		String profileId = StringUtil.checkVal(user.getProfileId());
		boolean isUser = req.hasParameter("dashboard");
		
		if (!isUser && programId.length() == 0) {
			// This is only called when we are not editing any particular program.
			super.putModuleData(getAllPrograms());
		} else {
			super.putModuleData(getProgram(isUser? profileId : programId, isUser));
		}
	}
	
	
	/**
	 * Get the program associated with the supplied id
	 * @param id the id of the program we are looking for
	 * @param isUser determines the starting point of the search
	 */
	private GFPProgramVO getProgram (String id, boolean isUser) {
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(500);
		sql.append("SELECT * FROM ");
		if (isUser) {
			// Since this is a user centric search the first table needs to be
			// the user in order to ensure that the information validating the
			// user as an authorized GFP user is returned.
			sql.append(customDb).append("DPY_SYN_GFP_USER u ");
			sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_GFP_PROGRAM p ");
			sql.append("ON u.PROGRAM_ID = p.PROGRAM_ID ");
		}else {
			sql.append(customDb).append("DPY_SYN_GFP_PROGRAM p ");
		}
		sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_GFP_PROGRAM_RESOURCE_XR prx");
		sql.append("ON prx.PROGRAM_ID = p.PROGRAM_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_GFP_RESOURCE pr ");
		sql.append("ON pr.RESOURCE_ID = prx.RESOURCE_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("DPT_SYN_GFP_CATEGORY c ");
		sql.append("ON c.CATEGORY_ID = pr.CATEGORY_ID ");
		if (isUser) {
			sql.append("WHERE u.PROFILE_ID = ?");
		} else {
			sql.append("WHERE p.PROGRAM_ID = ?");
		}
		
		GFPProgramVO program = null;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, id);
			
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				if (program == null) program = new GFPProgramVO(rs);
				program.addResource(new GFPResourceVO(rs));
			}
			program.setWorkshops(getWorkshops(program.getProgramId(), isUser, id));
		} catch (SQLException e) {
			log.error("Unable to get GFP program with " + (isUser? "user id of" : "program id of ") + id, e);
		}
		return program;
	}

	
	/**
	 * Get All workshops and associated resources for a program
	 * @param programId
	 * @return
	 */
	private List<GFPWorkshopVO> getWorkshops(String programId, boolean isUser, String userId) {
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(600);
		sql.append("SELECT w.*, wr.RESOURCE_ID, wr.CATEGORY_ID, wr.RESOURCE_NM, ");
		sql.append("wr.RESOURCE_DESC, wr.SHORT_DESC as RESOURCE_SHORT_DESC, wr.DPY_SYN_MEDIABIN_ID, ");
		sql.append("wr.ACTIVE_FLG as RESOURCE_ACTIVE_FLG, c.*");
		if (isUser) sql.append(", cw.*");
		sql.append(" FROM ").append(customDb).append("DPY_SYN_GFP_PROGRAM p ");
		sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_GFP_WORKSHOP w ");
		sql.append("ON w.PROGRAM_ID = p.PROGRAM_ID");
		sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_GFP_WORKSHOP_RESOURCE_XR wrx ");
		sql.append("ON wrx.WORKSHOP_ID = w.WORKSHOP_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_GFP_RESOURCE wr ");
		sql.append("ON wr.RESOURCE_ID = wrx.RESOURCE_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("DPT_SYN_GFP_CATEGORY c ");
		sql.append("ON c.CATEGORY_ID = wr.CATEGORY_ID ");
		if (isUser) {
			sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_GFP_COMPLETED_WORKSHOP cw");
			sql.append("ON cw.WORKSHOP_ID = w.WORKSHOP_ID and cw.USER_ID = ? ");
		}
		sql.append("WHERE p.PROGRAM_ID = ?");
		
		List<GFPWorkshopVO> workshops = new ArrayList<>();
		GFPWorkshopVO workshop = null;
		String workshopId = "";
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int i = 1;
			if (isUser) ps.setString(i++, userId);
			ps.setString(i, programId);
			
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				if (workshopId.equals(rs.getString("WORKSHOP_ID"))) {
					if (workshop != null) workshops.add(workshop);
					workshop = new GFPWorkshopVO(rs);
				}
			}
		} catch (SQLException e) {
			log.error("Unable to get workshops with program id of " + programId, e);
		}
		return workshops;
	}
	
	/**
	 * Gets all the programs in the database
	 * @return 
	 * @throws ActionException
	 */
	protected List<GFPProgramVO> getAllPrograms() throws ActionException {
		StringBuilder sql = new StringBuilder(60);
		sql.append("SELECT * FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("DPY_SYN_GFP_PROGRAM");
		
		List<GFPProgramVO> programs = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()) programs.add(new GFPProgramVO(rs));
		} catch (SQLException e) {
			log.error("Unable to get GFP programs from database ", e);
			throw new ActionException(e);
		}
		
		return programs;
	}
	

	public void delete(SMTServletRequest req) throws ActionException {
		StringBuilder sql = new StringBuilder(120);
		GFPLevel level = GFPLevel.valueOf(req.getParameter("level"));
		
		sql.append("DELETE ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_GFP_").append(level.toString()).append(" WHERE ");
		sql.append(level.toString()).append("_ID = ?");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, req.getParameter("id"));
			ps.executeUpdate();
			
		} catch (SQLException e) {
			log.error("Unable to delete " + level.toString() + " with id of " + req.getParameter("id"), e);
			throw new ActionException(e);
		}
	}

	
	public void update(SMTServletRequest req) throws ActionException {
		GFPLevel level = GFPLevel.valueOf(req.getParameter("level"));
		
		switch (level) {
			case PROGRAM:
				updateProgram(new GFPProgramVO(req));
				break;
			case WORKSHOP:
				updateWorkshop(new GFPWorkshopVO(req));
				break;
			case RESOURCE:
				updateResource(new GFPResourceVO(req), GFPLevel.valueOf(req.getParameter("parentLevel")));
				break;
		}
	}
	
	
	/**
	 * Build and run an update/insert query for a program
	 * @param req
	 */
	private void updateProgram (GFPProgramVO program) throws ActionException {
		StringBuilder sql = new StringBuilder(200);
		if (program.getProgramId() == null) {
			program.setProgramId(new UUIDGenerator().getUUID());
			sql.append("INSERT INTO ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
			sql.append("DPY_SYN_GFP_PROGRAM (PROGRAM_NM, CREATE_DT, PROGRAM_ID) ");
			sql.append("VALUES(?,?,?)");
		} else {
			sql.append("UPDATE ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
			sql.append("DPY_SYN_GFP_PROGRAM SET PROGRAM_NM = ?, UPDATE_DT = ? ");
			sql.append("WHERE PROGRAM_ID = ?");
		}
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int i = 1;
			ps.setString(i++, program.getProgramName());
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.setString(i++, program.getProgramId());
			
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Unable to update program with id " + program.getProgramId(), e);
			throw new ActionException(e);
		}
 	}
	
	
	/**
	 * Build and run an update/insert query for a workshop
	 * @param req
	 * @throws ActionException
	 */
	private void updateWorkshop (GFPWorkshopVO workshop) throws ActionException {
		StringBuilder sql = new StringBuilder(200);
		if (workshop.getWorkshopId() == null) {
			workshop.setWorkshopId(new UUIDGenerator().getUUID());
			sql.append("INSERT INTO ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
			sql.append("DPY_SYN_GFP_WORKSHOP (PROGRAM_ID, WORKSHOP_NM, WORKSHOP_DESC, SHORT_DESC, ");
			sql.append("ACTIVE_FLG, SEQUENCE_NO, CREATE_DT, WORKSHOP_ID)");
			sql.append("VALUES(?,?,?,?,?,?,?,?)");
		} else {
			sql.append("UPDATE ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
			sql.append("DPY_SYN_GFP_WORKSHOP SET PROGRAM_ID = ?, WORKSHOP_NM = ?, ");
			sql.append("WORKSHOP_DESC = ?, SHORT_DESC = ?, ACTIVE_FLG = ?, ");
			sql.append("SEQUENCE_NO = ?, UPDATE_DT = ? WHERE WORKSHOP_ID = ?");
		}
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int i = 1;
			ps.setString(i++, workshop.getParentId());
			ps.setString(i++, workshop.getName());
			ps.setString(i++, workshop.getDesc());
			ps.setString(i++, workshop.getShortDesc());
			ps.setInt(i++, workshop.getActiveFlg());
			ps.setInt(i++, workshop.getSequenceNo());
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.setString(i++, workshop.getWorkshopId());
			
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Unable to update program with id " + workshop.getWorkshopId(), e);
			throw new ActionException(e);
		}
	}
	

	/**
	 * Build and run an update/insert query for a resource as well as 
	 * create the xr table record if an insert is being run
	 * @param req
	 * @throws ActionException
	 */
	private void updateResource (GFPResourceVO resource, GFPLevel xrType) throws ActionException {
		StringBuilder sql = new StringBuilder(200);
		boolean insert = false;
		if (resource.getResourceId() == null) {
			resource.setResourceId(new UUIDGenerator().getUUID());
			insert = true;
			sql.append("INSERT INTO ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
			sql.append("DPY_SYN_GFP_RESOURCE (CATEGORY_ID, RESOURCE_NM, RESOURCE_DESC, ");
			sql.append("SHORT_DESC, DPY_SYN_MEDIABIN_ID, ACTIVE_FLG, CREATE_DT, RESOURCE_ID) ");
			sql.append("VALUES(?,?,?,?,?,?,?,?)");
		} else {
			sql.append("UPDATE ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
			sql.append("DPY_SYN_GFP_RESOURCE SET CATEGORY_ID = ?, RESOURCE_NM = ?, ");
			sql.append("RESOURCE_DESC = ?, SHORT_DESC = ?, DPY_SYN_MEDIABIN_ID = ?, ");
			sql.append("ACTIVE_FLG = ?, CREATE_DT = ? WHERE RESOURCE_ID = ?");
		}
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int i = 1;
			ps.setString(i++, resource.getCategoryId());
			ps.setString(i++, resource.getName());
			ps.setString(i++, resource.getDesc());
			ps.setString(i++, resource.getShortDesc());
			ps.setString(i++, resource.getMediabinId());
			ps.setInt(i++, resource.getActiveFlg());
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.setString(i++, resource.getResourceId());
			
			ps.executeUpdate();
			if (insert) addResourceXR(resource, xrType);
			
		} catch (SQLException e) {
			log.error("Unable to update program with id " + resource.getResourceId(), e);
			throw new ActionException(e);
		}
	}


	/**
	 * Create an xr record for the the supplied resource.
	 * @param resource
	 * @param xrType Determines whether this is a workshop or a program resource and 
	 * provides the sql query with the name of the table and the ids it is working with.
	 * @throws ActionException
	 */
	private void addResourceXR(GFPResourceVO resource, GFPLevel xrType) throws ActionException {
		StringBuilder sql = new StringBuilder(250);
		sql.append("INSERT INTO ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_GFP_").append(xrType.toString()).append("_XR ");
		sql.append("(").append(xrType.toString()).append("_XR_ID, ").append(xrType.toString());
		sql.append("_ID, RESOURCE_ID, ORDER_NO, CREATE_DT) ");
		sql.append("VALUES(?,?,?,?,?)");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int i = 1;
			ps.setString(i++, new UUIDGenerator().getUUID());
			ps.setString(i++, resource.getParentId());
			ps.setString(i++, resource.getResourceId());
			ps.setInt(i++, resource.getOrderNo());
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Unable to create " + xrType.toString().toLowerCase() + " xr record for resource " + resource.getResourceId(), e);
			throw new ActionException(e);
		}
	}
	
}
