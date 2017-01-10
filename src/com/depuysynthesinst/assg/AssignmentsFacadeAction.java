package com.depuysynthesinst.assg;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.depuysynthesinst.DSIRoleMgr;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: AssignmentsFacadeAction.java<p/>
 * <b>Description: facades all professor and student transactions for the My Assignments 
 * section of the DSI website.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jun 27, 2015
 ****************************************************************************/
public class AssignmentsFacadeAction extends SimpleActionAdapter {

	public static final String RES_DIR_ID = "dsiResidentDirectorId"; //used as a session variable
	
	public AssignmentsFacadeAction() {
	}

	/**
	 * @param arg0
	 */
	public AssignmentsFacadeAction(ActionInitVO arg0) {
		super(arg0);
	}

	public void retrieve(ActionRequest req) throws ActionException {
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		DSIRoleMgr dsiRoleMgr = new DSIRoleMgr();
		boolean isProfessor = dsiRoleMgr.isAssgAdmin(user);

		SMTActionInterface action;
		if (isProfessor && "residents".equals(req.getParameter("pg"))) {
			action = new MyResidentsAction();
		} else if ((isProfessor && req.hasParameter("view")) || dsiRoleMgr.isDirector(user)) {
			//admin view of assignments - or a single assignment (edit mode)
			action = new MyAssignmentsAdminAction(actionInit);
		} else {
			//load list of 'My Assignments'
			action = new MyAssignmentsAction();
		}

		action.setAttributes(attributes);
		action.setDBConnection(dbConn);
		action.retrieve(req);
	}


	/**
	 * calls to build are 'write' actions on an assignment; reflective of retrieve which is read from DB.
	 */
	public void build(ActionRequest req) throws ActionException {
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		DSIRoleMgr dsiRoleMgr = new DSIRoleMgr();
		boolean isProfessor = dsiRoleMgr.isAssgAdmin(user);
		
		//if we don't already know the residentDirectorId, go get it.  Add if necessary.
		if (req.getSession().getAttribute(RES_DIR_ID) == null)
			loadResidentDirector(req, user);

		SMTActionInterface action;
		if (isProfessor && "residents".equals(req.getParameter("pg"))) {
			action = new MyResidentsAction();
		} else if (isProfessor && req.hasParameter("view")) {
			//admin view of assignments - or a single assignment (edit mode)
			action = new MyAssignmentsAdminAction();
		} else {
			//load list of 'My Assignments'
			action = new MyAssignmentsAction();
		}

		action.setAttributes(attributes);
		action.setDBConnection(dbConn);
		action.build(req);
		
		// Setup the redirect if one of our child actions hasn't do so already. (MyResidentAction does)
		if (req.getAttribute(Constants.REDIRECT_URL) == null) {
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			StringBuilder url = new StringBuilder();
			url.append(page.getRequestURI()).append("?view=").append(req.getParameter("view")); //display admin menus
			if (req.hasParameter("pg")) url.append("&pg=").append(req.getParameter("pg")); //admin page (include)
			if (req.hasParameter("redirAssignmentId")) {
				url.append("&assignmentId=").append(req.getParameter("redirAssignmentId"));
				if (req.hasParameter("isNew")) url.append("&isNew=1");
			}
			if (req.hasParameter("redirMsg")) url.append("&msg=").append(req.getParameter("redirMsg"));
			
			req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
			req.setAttribute(Constants.REDIRECT_URL, url.toString());
		}
	}

	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	
	/**
	 * does a lookup for the resident director's ID, and automatically adds them if 
	 * they're not already in the system
	 * @param req
	 * @param user
	 * @throws ActionException
	 */
	private void loadResidentDirector(ActionRequest req, UserDataVO user) throws ActionException {
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(100);
		sql.append("select res_dir_id from ").append(customDb).append("DPY_SYN_INST_RES_DIR where profile_id=?");
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, user.getProfileId());
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				req.getSession().setAttribute(RES_DIR_ID, rs.getInt(1));
				return;
			}
		} catch (SQLException sqle) {
			log.error("could not load res_dir_id", sqle);
		}
		
		//don't have one; let's add them
		sql = new StringBuilder(100);
		sql.append("insert into ").append(customDb).append("DPY_SYN_INST_RES_DIR (PROFILE_ID, CREATE_DT) values (?,?)");
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
			ps.setString(1, user.getProfileId());
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.executeUpdate();
			ResultSet rs = ps.getGeneratedKeys();
			if (rs.next()) {
				req.getSession().setAttribute(RES_DIR_ID, rs.getInt(1));
			} else {
				throw new ActionException("new ResDir could not be added");
			}
		} catch (SQLException sqle) {
			log.error("could not load res_dir_id", sqle);
			throw new ActionException("new ResDir could not be added");
		}
	}
}