package com.depuysynthesinst.assg;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>: MyAssignmentsAction.java<p/>
 * <b>Description: Returns a list of My Assignments.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 7, 2015
 ****************************************************************************/
public class MyAssignmentsAdminAction extends SBActionAdapter {

	/**
	 * 
	 */
	public MyAssignmentsAdminAction() {
	}

	/**
	 * @param actionInit
	 */
	public MyAssignmentsAdminAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	* loads a list of My Assignments for Admins (profession's view)
	 * if a single ID is passed we also load a list of Solr Assets (details) as well 
	 * as the list of enrolled/pending users.
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA); //used to load Solr assets
		String assgId = req.getParameter("assignmentId");
		
		//load the list of assignments
		List<AssignmentVO> data = loadAssignmentList(user.getProfileId(), assgId);
		
		//if we're displaying only one assignment we need to load all of it's Solr assets (for detailed view/display)
		if (assgId != null && data.size() == 1 && req.hasParameter("pg")) {
			AssignmentVO assg = data.get(0);
			if (assg.getAssets().size() > 0)
				loadSolrAssets(assg, role);
			
			//load the residents and their enrollment/completion stats
			loadAssgResidents(assg);
		} else {
			//gets displayed above the list of assignments in the admin view; we just need a number
			req.setAttribute("residentCount", loadResidentCount(user.getProfileId()));
			
			//count active/inactive assignments so we know when to print white lines on the list page
			int act=0, inact=0;
			for (AssignmentVO vo : data) {
				if (vo.isActiveFlg()) ++act;
				else ++inact;
			}
			req.setAttribute("activeCount", act);
			req.setAttribute("inactiveCount", inact);
		}
		
		mod.setActionData(data);
	}
	
	
	/**
	 * handles all the 'write' transactions related to managing Assignments & Residents.
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(SMTServletRequest req) throws ActionException {
		String reqType = StringUtil.checkVal(req.getParameter("reqType"), null);
		AssignmentVO assg = new AssignmentVO(req);
		
		switch (reqType) {
			case "edit":
				this.saveAssg(assg);
				this.saveAssgAssets(assg, req); //this runs a loop around the request obj. (assets)
				this.saveAssgResidents(assg, req); //this runs a loop around the request obj (residents)
				break;
			case "publish":
				this.saveAssg(assg);
				this.saveAssgAssets(assg, req);
				this.saveAssgResidents(assg, req);
				//TODO email all the residents
				break;
			case "add":
				this.saveAssg(assg);
				req.setParameter("redirAssignmentId", assg.getAssgId());
				break;
			case "delete":
				//TODO
				break;
		}
		
	}
	
	
	/**
	 * loads all of the residents attached to an Assignment; inclusive of their PGY
	 * and %complete for the assignment.
	 * To get %complete we need to load the resident's completion of the assets.
	 * We already know how many assets are in the assignment.
	 * @param assg
	 * @return
	 */
	private void loadAssgResidents(AssignmentVO assg) {
		MyResidentsAction mra = new MyResidentsAction();
		mra.setDBConnection(dbConn);
		mra.setAttributes(getAttributes());
		mra.loadAssgResidents(assg);
		mra = null;
	}
	
	
	/**
	 * loads a count of residents attached to the given Resident Director
	 * @param directorProfileId
	 * @return
	 */
	private int loadResidentCount(String directorProfileId) {
		MyResidentsAction mra = new MyResidentsAction();
		mra.setDBConnection(dbConn);
		mra.setAttributes(getAttributes());
		return mra.loadResidentCount(directorProfileId);
	}
	
	
	/**
	 * load solr assets.  This is the same functionality we use for MyAssigns, so 
	 * we'll just leverage that code.
	 * @param assg
	 * @param role
	 */
	private void loadSolrAssets(AssignmentVO assg, SBUserRole role) {
		MyAssignmentsAction maa = new MyAssignmentsAction();
		maa.setAttributes(getAttributes());
		maa.loadSolrAssets(assg, role);
	}
	
	
	/**
	 * load a list of assignments attached to the given user.
	 * @param profileId
	 * @param assignmentId
	 * @return
	 */
	private List<AssignmentVO> loadAssignmentList(String profileId, String assignmentId) {
		Map<String, AssignmentVO> data = new HashMap<>();
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(200);
		sql.append("select a.assg_id, a.parent_id, a.assg_nm, a.desc_txt, a.due_dt, a.sequential_flg, a.active_flg, ");
		sql.append("aa.solr_document_id, aa.order_no, rd.profile_id as res_dir_profile_id, a.update_dt ");
		sql.append("from ").append(customDb).append("DPY_SYN_INST_ASSG a ");
		sql.append("inner join ").append(customDb).append("DPY_SYN_INST_RES_DIR rd on a.res_dir_id=rd.res_dir_id ");
		sql.append("left outer join ").append(customDb).append("DPY_SYN_INST_ASSG_ASSET aa on a.assg_id=aa.assg_id ");
		sql.append("where rd.profile_id=? ");
		if (assignmentId != null) sql.append("and a.assg_id=? ");
		sql.append("order by a.due_dt, a.assg_nm, a.assg_id");
		log.debug(sql);
		
		AssignmentVO vo;
		String assgId;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, profileId);
			if (assignmentId != null) ps.setString(2, assignmentId);
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				assgId = rs.getString(1);
				if (data.containsKey(assgId)) {
					//get the existing Assignment and add to it - assets
					vo = data.get(assgId);
				} else {
					//create a new Assignment the first time by
					vo = new AssignmentVO(rs);
				}
				
				vo.addAsset(new AssignmentAssetVO(rs));
				data.put(assgId, vo);
			}
			
		} catch (SQLException sqle) {
			log.error("could not load list of user assignemtns", sqle);
		}
		
		return new ArrayList<AssignmentVO>(data.values());
	}
	
	
	/**
	 * writes an Assignment to the database
	 * @param assg
	 * @throws ActionException
	 */
	private void saveAssg(AssignmentVO assg) throws ActionException {
		log.debug("saving assignment");
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(350);
		if (assg.getAssgId() == null) {
			assg.setAssgId(new UUIDGenerator().getUUID());
			sql.append("insert into ").append(customDb).append("DPY_SYN_INST_ASSG ");
			sql.append("(PARENT_ID, RES_DIR_ID, ASSG_NM, DESC_TXT, DUE_DT, SEQUENTIAL_FLG, ");
			sql.append("ACTIVE_FLG, CREATE_DT, ASSG_ID) values (?,?,?,?,?,?,?,?,?)");
		} else {
			sql.append("update ").append(customDb).append("DPY_SYN_INST_ASSG ");
			sql.append("set PARENT_ID=?, RES_DIR_ID=?, ASSG_NM=?, DESC_TXT=?, DUE_DT=?, ");
			sql.append("SEQUENTIAL_FLG=?, ACTIVE_FLG=?, UPDATE_DT=? where ASSG_ID=?");
		}
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, assg.getParentId());
			ps.setInt(2, assg.getResDirId());
			ps.setString(3, assg.getAssgName());
			ps.setString(4, assg.getAssgDesc());
			ps.setDate(5, Convert.formatSQLDate(assg.getDueDt()));
			ps.setInt(6, assg.isSequentialFlg() ? 1 : 0);
			ps.setInt(7, assg.isActiveFlg() ? 1 : 0);
			ps.setTimestamp(8, Convert.getCurrentTimestamp());
			ps.setString(9, assg.getAssgId());
			ps.executeUpdate();
			
		} catch (SQLException sqle) {
			log.error("could not load list of user assignemtns", sqle);
		}
	}
	
	
	/**
	 * writes an Assignment's Asset to the database
	 * @param assgAsset
	 * @throws ActionException
	 */
	private void saveAssgAssets(AssignmentVO assg, SMTServletRequest req) throws ActionException {
		
	}
	
	
	/**
	 * assigns an Assignment to a Resident - both must be pre-existing in the database.
	 * @param assg
	 * @param resident
	 * @throws ActionException
	 */
	private void saveAssgResidents(AssignmentVO assg, SMTServletRequest req) throws ActionException {
		
	}
	
}