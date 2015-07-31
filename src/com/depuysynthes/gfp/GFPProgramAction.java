package com.depuysynthes.gfp;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.depuysynthes.action.MediaBinAdminAction;
import com.depuysynthes.gfp.GFPFacadeAction.GFPLevel;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.FileManager;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
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
		String profileId = null;
		if (user != null) {
			profileId = StringUtil.checkVal(user.getProfileId());
		}
		
		boolean isUser = programId.length() == 0;
		
		if (req.hasParameter("dashboard") && programId.length() == 0) {
			// This is only called when we are not editing any particular program.
			super.putModuleData(getAllPrograms());
		} else {
			super.putModuleData(getProgram(isUser? profileId : programId, isUser, req.getParameter("workshopId")));
		}
		
		if (req.hasParameter("dashboard")) {
			req.setAttribute("categories", getAllCategories());
		}
	}
	
	
	/**
	 * Get a map of all possible resource categories
	 * @return
	 */
	private Map<String, String> getAllCategories() {
		StringBuilder sql = new StringBuilder(100);
		sql.append("SELECT * FROM ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("DPY_SYN_GFP_CATEGORY ");
		
		Map<String, String> categories = new HashMap<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()) {
				categories.put(rs.getString("CATEGORY_ID"), rs.getString("CATEGORY_NM"));
			}
		} catch (SQLException e) {
			log.error("Unable to get map of categories for DePuy GFP", e);
		}
		
		return categories;
	}
	
	
	/**
	 * Get the program associated with the supplied id
	 * @param id the id of the program we are looking for
	 * @param isUser determines the starting point of the search
	 */
	private GFPProgramVO getProgram (String id, boolean isUser, String workshopId) {
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(500);
		sql.append("SELECT * ");
		if (isUser) {
			// Since this is a user centric search the first table needs to be
			// the user in order to ensure that the information validating the
			// user as an authorized GFP user is returned.
			sql.append(", cr.CREATE_DT as COMPLETE_DT FROM ");
			sql.append(customDb).append("DPY_SYN_GFP_USER u ");
			sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_GFP_PROGRAM p ");
			sql.append("ON u.PROGRAM_ID = p.PROGRAM_ID ");
		}else {
			sql.append("FROM ").append(customDb).append("DPY_SYN_GFP_PROGRAM p ");
		}
		sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_GFP_PROGRAM_XR px ");
		sql.append("ON px.PROGRAM_ID = p.PROGRAM_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_GFP_RESOURCE pr ");
		sql.append("ON pr.RESOURCE_ID = px.RESOURCE_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_GFP_CATEGORY c ");
		sql.append("ON c.CATEGORY_ID = pr.CATEGORY_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_MEDIABIN m ");
		sql.append("ON m.DPY_SYN_MEDIABIN_ID = pr.DPY_SYN_MEDIABIN_ID ");
		if (isUser) {
			sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_GFP_COMPLETED_RESOURCE cr ");
			sql.append("ON cr.RESOURCE_ID = pr.RESOURCE_ID and cr.USER_ID = u.PROFILE_ID ");
			sql.append("WHERE u.PROFILE_ID = ? ");
		} else {
			sql.append("WHERE p.PROGRAM_ID = ? ");
		}
		sql.append("ORDER BY p.PROGRAM_ID, ORDER_NO ");
		
		GFPProgramVO program = null;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, id);
			
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				if (program == null) program = new GFPProgramVO(rs);
				if (rs.getString("RESOURCE_ID") != null)
					program.addResource(new GFPResourceVO(rs));
			}
			if (program != null) program.setWorkshops(getWorkshops(program.getProgramId(), isUser, id, workshopId));
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
	private List<GFPWorkshopVO> getWorkshops(String programId, boolean isUser, String userId, String currentWorkshop) {
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(600);
		sql.append("SELECT w.*, wr.RESOURCE_ID, wr.CATEGORY_ID, wr.RESOURCE_NM, ");
		sql.append("wr.RESOURCE_DESC, wr.SHORT_DESC as RESOURCE_SHORT_DESC, wr.DPY_SYN_MEDIABIN_ID, ");
		sql.append("wr.ACTIVE_FLG as RESOURCE_ACTIVE_FLG, c.*, m.*");
		if (isUser) sql.append(", cr.CREATE_DT as COMPLETE_DT");
		sql.append(" FROM ").append(customDb).append("DPY_SYN_GFP_PROGRAM p ");
		sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_GFP_WORKSHOP w ");
		sql.append("ON w.PROGRAM_ID = p.PROGRAM_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_GFP_WORKSHOP_XR wx ");
		sql.append("ON wx.WORKSHOP_ID = w.WORKSHOP_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_GFP_RESOURCE wr ");
		sql.append("ON wr.RESOURCE_ID = wx.RESOURCE_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_GFP_CATEGORY c ");
		sql.append("ON c.CATEGORY_ID = wr.CATEGORY_ID ");
		if (isUser) {
			sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_GFP_COMPLETED_RESOURCE cr ");
			sql.append("ON cr.RESOURCE_ID = wr.RESOURCE_ID and cr.USER_ID = ? ");
		}
		sql.append("LEFT JOIN ").append(customDb).append("DPY_SYN_MEDIABIN m ");
		sql.append("ON m.DPY_SYN_MEDIABIN_ID = wr.DPY_SYN_MEDIABIN_ID ");
		sql.append("WHERE p.PROGRAM_ID = ? ");
		if (currentWorkshop != null) sql.append(" and w.WORKSHOP_ID = ? ");
		sql.append("ORDER BY w.SEQUENCE_NO, wx.ORDER_NO ");
		log.debug(sql+"|"+programId+"|"+userId);
		
		List<GFPWorkshopVO> workshops = new ArrayList<>();
		GFPWorkshopVO workshop = null;
		String workshopId = "";
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int i = 1;
			if (isUser) ps.setString(i++, userId);
			ps.setString(i++, programId);
			if (currentWorkshop != null) ps.setString(i++, currentWorkshop);
			
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				if (!workshopId.equals(rs.getString("WORKSHOP_ID"))) {
					if (workshop != null) workshops.add(workshop);
					workshop = new GFPWorkshopVO(rs);
					workshopId = workshop.getWorkshopId();
				}
				if (rs.getString("RESOURCE_ID") != null)
					workshop.addResource(new GFPResourceVO(rs));
			}
			if (workshop != null && workshop.getWorkshopId() != null) workshops.add(workshop);
		} catch (SQLException e) {
			log.error("Unable to get workshops with program id of " + programId, e);
		}
		log.debug(workshops.size());
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
		log.debug(sql);
		
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
		
		FilePartDataBean file = req.getFile("newFile");
		if (file != null) {
			req.setParameter("filePath", writeNewFile(file));
		}
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
		if (program.getProgramId() == null || program.getProgramId().length() == 0) {
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
		if (workshop.getWorkshopId() == null || workshop.getWorkshopId().length() == 0) {
			workshop.setWorkshopId(new UUIDGenerator().getUUID());
			sql.append("INSERT INTO ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
			sql.append("DPY_SYN_GFP_WORKSHOP (PROGRAM_ID, WORKSHOP_NM, WORKSHOP_DESC, SHORT_DESC, ");
			sql.append("THUMBNAIL_PATH, ACTIVE_FLG, SEQUENCE_NO, CREATE_DT, WORKSHOP_ID) ");
			sql.append("VALUES(?,?,?,?,?,?,?,?,?)");
		} else {
			sql.append("UPDATE ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
			sql.append("DPY_SYN_GFP_WORKSHOP SET PROGRAM_ID = ?, WORKSHOP_NM = ?, ");
			sql.append("WORKSHOP_DESC = ?, SHORT_DESC = ?, THUMBNAIL_PATH = ?, ACTIVE_FLG = ?, ");
			sql.append("SEQUENCE_NO = ?, UPDATE_DT = ? WHERE WORKSHOP_ID = ?");
		}
		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int i = 1;
			ps.setString(i++, workshop.getParentId());
			ps.setString(i++, workshop.getName());
			ps.setString(i++, workshop.getDesc());
			ps.setString(i++, workshop.getShortDesc());
			ps.setString(i++, workshop.getFilePath());
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
	 * Write the new workshop image file
	 * @param req
	 * @throws ActionException 
	 */
	private String writeNewFile(FilePartDataBean file) throws ActionException {
		try {
			String path = (String)getAttribute(Constants.BINARY_PATH) + "/org/DEPUY/workshops/";
			FileManager fm = new FileManager();
			fm.setPath(path);
			log.debug(file);
			log.debug(file.getFileData());
			log.debug(file.getFileName());
			fm.writeFiles(file.getFileData(), path, file.getFileName(), true, false);
			log.debug("Wrote file to " + path + fm.getFileName());
			
			return fm.getFileName();
		} catch (Exception e) {
			log.error("Unable to upload file for new IFU document instance.", e);
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
		if (resource.getResourceId() == null || resource.getResourceId().length() == 0) {
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
		log.debug(sql);
		
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
		log.debug(sql);
		
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
	
	public void build(SMTServletRequest req) throws ActionException {
		
		if (req.hasParameter("completeState")) {
			completeResource(req);
			return;
		}
		
		req.setParameter("organizationId", ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getAliasPathOrgId());
		SMTActionInterface sai = new MediaBinAdminAction();
		sai.setDBConnection(dbConn);
		sai.setAttributes(attributes);
		sai.setActionInit(actionInit);
		sai.list(req);
		ModuleVO mod = (ModuleVO) sai.getAttribute(AdminConstants.ADMIN_MODULE_DATA);
		super.putModuleData(mod.getActionData(), mod.getDataSize(), false);
		
	}
	
	/**
	 * Creates a complete record for the supplied resource/user pair
	 */
	private void completeResource(SMTServletRequest req) throws ActionException {
		StringBuilder sql = new StringBuilder(250);
		sql.append("INSERT INTO ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_GFP_COMPLETED_WORKSHOP (COMPLETED_WORKSHOP_ID, ");
		sql.append("CREATE_DT, USER_ID, WORKSHOP_ID) ");
		sql.append("VALUES(?,?,?,?)");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			int i = 1;
			ps.setString(i++, new UUIDGenerator().getUUID());
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.setString(i++, req.getParameter("userId"));
			ps.setString(i++, req.getParameter("programId"));
		} catch (SQLException e) {
			log.error("Unable to update complete status of program " + req.getParameter("programId") + 
					" for user " + req.getParameter("userId"), e);
			throw new ActionException(e);
			
		}
	}
	
}
