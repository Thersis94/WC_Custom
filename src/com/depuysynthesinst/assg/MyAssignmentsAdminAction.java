package com.depuysynthesinst.assg;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSession;

import com.depuysynthesinst.DSIRoleMgr;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
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
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA); //used to load Solr assets
		String assgId = req.getParameter("assignmentId");
		
		if (req.hasParameter("searchData")) {
			//query for a list of Assets via Solr and return those.  This is the "Add Resources" modal
			searchAssets(mod, req);
			return;
		}
		
		//load the list of assignments
		List<AssignmentVO> data = loadAssignmentList(user.getProfileId(), assgId);
		
		for (AssignmentVO assg : data) {
			//load the residents and their enrollment/completion stats
			loadAssgResidents(assg, site, (data.size() == 1), req.getParameter("residentId"));
			
			//load assets from Solr
			if (assg.getAssets().size() > 0)
				loadSolrAssets(assg, role);
		}
		
		//if we're displaying the default page we also need some extra data
		if (!req.hasParameter("pg")) {
			//Total Resident Count - gets displayed above the list of assignments in the admin view; we just need a number
			req.setAttribute("residentCount", loadResidentCount(user.getProfileId()));
			
			//count active/inactive assignments so we know when to print white lines on the list page
			int act=0, inact=0;
			for (AssignmentVO vo : data) {
				if (vo.getPublishDt() != null) ++act;
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
		Integer cnt;
		HttpSession ses = req.getSession();
		
		switch (reqType) {
			case "edit":
				this.saveAssg(assg, false);
				this.saveAssgAssets(assg, req); //this runs a loop around the request obj. (assets)
				this.saveAssgResidents(assg, req); //this runs a loop around the request obj (residents)
				break;
			case "publish":
				this.saveAssg(assg, !req.hasParameter("publishDt")); //pass the previous publish date so we know; this only gets saved once. ("First published date")
				this.saveAssgAssets(assg, req);
				this.saveAssgResidents(assg, req);
				//TODO email all the residents
				break;
			case "add":
				this.saveAssg(assg, false);
				req.setParameter("redirAssignmentId", assg.getAssgId());
				
				//increment the count displayed in the left menu for DIRECTORs only
				if (DSIRoleMgr.isDirector((UserDataVO)ses.getAttribute(Constants.USER_DATA))) {
					cnt = Convert.formatInteger("" + ses.getAttribute("myAssgCnt"), 0);
					++cnt;
					req.getSession().setAttribute("myAssgCnt", cnt);
				}
				break;
			case "delete":
				this.deleteAssg(assg);
				//TODO email all the residents that this course is gone. - need to get a list first, before we purge the relationship!
				
				//decrement the count displayed in the left menu for DIRECTORs only
				if (DSIRoleMgr.isDirector((UserDataVO)ses.getAttribute(Constants.USER_DATA))) {
						cnt = Convert.formatInteger("" + ses.getAttribute("myAssgCnt"), 0);
						if (cnt > 0) --cnt;
						ses.setAttribute("myAssgCnt", cnt);
				}
				break;
			case "addAssets":
				this.addAssgAssets(assg, req);
				break;
			case "deleteAsset":
				this.deleteAssgAsset(assg, req.getParameter("assgAssetId"));
				req.setParameter("redirAssignmentId", assg.getAssgId());
				break;
		}
	}
	
	
	/**
	 * searches Solr just like site search
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	private void searchAssets(ModuleVO mod, SMTServletRequest req) throws ActionException {
		//Solr necessary params
		req.setParameter("fieldSort", "score");
		req.setParameter("rpp", "3000");
		req.setParameter("page", "0");
		
		//call SolrAction 
		actionInit.setActionId((String)mod.getAttribute(SBModuleVO.ATTRIBUTE_1));
		SolrAction sa = new SolrAction(actionInit);
		sa.setAttributes(attributes);
		sa.setDBConnection(dbConn);
		sa.retrieve(req);
	}
	
	
	/**
	 * loads all of the residents attached to an Assignment; inclusive of their PGY
	 * and %complete for the assignment.
	 * To get %complete we need to load the resident's completion of the assets.
	 * We already know how many assets are in the assignment.
	 * @param assg
	 * @return
	 */
	private void loadAssgResidents(AssignmentVO assg, SiteVO site, boolean fullDetail, String residentId) {
		MyResidentsAction mra = new MyResidentsAction();
		mra.setDBConnection(dbConn);
		mra.setAttributes(getAttributes());
		mra.loadAssgResidents(assg, site, fullDetail, residentId);
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
	public List<AssignmentVO> loadAssignmentList(String profileId, String assignmentId) {
		Map<String, AssignmentVO> data = new HashMap<>();
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(200);
		sql.append("select a.assg_id, a.parent_id, a.assg_nm, a.desc_txt, a.due_dt, a.sequential_flg, a.publish_dt, ");
		sql.append("aa.solr_document_id, aa.assg_asset_id, aa.order_no, rd.profile_id as res_dir_profile_id, rd.res_dir_id, a.update_dt ");
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
	private void saveAssg(AssignmentVO assg, boolean publish) throws ActionException {
		log.debug("saving assignment");
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(350);
		if (assg.getAssgId() == null) {
			assg.setAssgId(new UUIDGenerator().getUUID());
			sql.append("insert into ").append(customDb).append("DPY_SYN_INST_ASSG ");
			sql.append("(RES_DIR_ID, ASSG_NM, DESC_TXT, DUE_DT, SEQUENTIAL_FLG, ");
			sql.append("CREATE_DT, ASSG_ID) values (?,?,?,?,?,?,?)");
		} else {
			sql.append("update ").append(customDb).append("DPY_SYN_INST_ASSG ");
			sql.append("set RES_DIR_ID=?, ASSG_NM=?, DESC_TXT=?, DUE_DT=?, ");
			sql.append("SEQUENTIAL_FLG=?, ");
			if (publish) sql.append("PUBLISH_DT=?, ");
			sql.append("UPDATE_DT=? where ASSG_ID=?");
		}
		log.debug(sql);
		
		int x=1;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(x++, assg.getResDirId());
			ps.setString(x++, assg.getAssgName());
			ps.setString(x++, assg.getAssgDesc());
			ps.setDate(x++, Convert.formatSQLDate(assg.getDueDt()));
			ps.setInt(x++, assg.isSequentialFlg() ? 1 : 0);
			if (publish) ps.setTimestamp(x++, Convert.getCurrentTimestamp());
			ps.setTimestamp(x++, Convert.getCurrentTimestamp());
			ps.setString(x++, assg.getAssgId());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("could not save assignment", sqle);
		}
	}
	
	
	/**
	 * updates the ordering/sequence of the assets in the assignment
	 * @param assgAsset
	 * @throws ActionException
	 */
	private void saveAssgAssets(AssignmentVO assg, SMTServletRequest req) throws ActionException {
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(150);
		sql.append("update ").append(customDb).append("DPY_SYN_INST_ASSG_ASSET ");
		sql.append("set order_no=?, update_dt=? where assg_asset_id=?");
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (String key : req.getParameterMap().keySet()) {
				if (!key.startsWith("assgAsset~")) continue;
				ps.setInt(1,  assg.isSequentialFlg() ? Convert.formatInteger(req.getParameter(key)) : 0);
				ps.setTimestamp(2, Convert.getCurrentTimestamp());
				ps.setString(3, key.split("~")[1]);
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException sqle) {
			log.error("could not add users to assignment", sqle);
		}
	}
	
	
	/**
	 * assigns an Assignment to a Resident - both must be pre-existing in the database.
	 * @param assg
	 * @param resident
	 * @throws ActionException
	 */
	private void saveAssgResidents(AssignmentVO assg, SMTServletRequest req) throws ActionException {
		String[] residentKeys= req.getParameterValues("residentId");
		Set<String> addIds = new HashSet<>();
		Set<String> persistIds = new HashSet<>();
		//split the key into two lists; one to protect from deletion, the other to add.  We don't need to do any SQL updates.
		for (String s : residentKeys) {
			String[] val = s.split("~");
			if (val.length == 2 && val[1].length() > 0) {
				//already on the roster, just protect from deletion
				persistIds.add(val[1]);
			} else {
				//not currently assigned, put in the list to insert
				addIds.add(val[0]);
			}
		}
		log.debug("persisting " + persistIds.size());
		log.debug("adding " + addIds.size());
		
		//delete all residents NOT in the selected set
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(150);
		sql.append("delete from ").append(customDb).append("DPY_SYN_INST_RES_ASSG ");
		sql.append("where res_assg_id not in ('~'");
		for (int x=persistIds.size(); x > 0; x--) sql.append(",?");
		sql.append(") and assg_id=?");
		log.debug(sql);
		
		int x=1;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (String resAssgId : persistIds)
				ps.setString(x++, resAssgId);
			ps.setString(x++, assg.getAssgId());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("could not delete users from assignment", sqle);
		}
		
		//add the residents not already on the roster
		sql = new StringBuilder(150);
		sql.append("insert into ").append(customDb).append("DPY_SYN_INST_RES_ASSG ");
		sql.append("(RES_ASSG_ID, RESIDENT_ID, ASSG_ID, CREATE_DT) values (?,?,?,?)");
		log.debug(sql);
		
		UUIDGenerator uuid = new UUIDGenerator();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (String residentId : addIds) {
				x = 1;
				ps.setString(x++, uuid.getUUID());
				ps.setString(x++, residentId);
				ps.setString(x++, assg.getAssgId());
				ps.setTimestamp(x++, Convert.getCurrentTimestamp());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException sqle) {
			log.error("could not add users to assignment", sqle);
		}
	}
	
	
	/**
	 * deletes an Assignment, all it's assets, and all Resident affiliations to it.
	 * @param assg
	 * @throws ActionException
	 */
	private void deleteAssg(AssignmentVO assg) throws ActionException {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(150);
		sql.append("delete from ").append(customDb).append("DPY_SYN_INST_ASSG ");
		sql.append("where res_dir_id=? and assg_id=?");
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, assg.getResDirId());
			ps.setString(2, assg.getAssgId());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("could not delete assignment", sqle);
		}
	}
	
	
	/**
	 * adds NEW assets to the assignment - submitted from the "add" modal window
	 * @param assg
	 * @throws ActionException
	 */
	private void addAssgAssets(AssignmentVO assg, SMTServletRequest req) throws ActionException {
		String[] assets = req.getParameterValues("solrDocumentId");
		if (assets == null || assets.length == 0) return;
		
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(150);
		sql.append("insert into ").append(customDb).append("DPY_SYN_INST_ASSG_ASSET ");
		sql.append("(assg_asset_id, assg_id, solr_document_id, create_dt) values (?,?,?,?)");
		log.debug(sql);
		
		UUIDGenerator uuid = new UUIDGenerator();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (String sorlDocumentId : assets) {
				ps.setString(1, uuid.getUUID());
				ps.setString(2, assg.getAssgId());
				ps.setString(3, sorlDocumentId);
				ps.setTimestamp(4, Convert.getCurrentTimestamp());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException sqle) {
			log.error("could not add assets to assignment", sqle);
		}
	}
	
	
	/**
	 * deletes a single asset from an assignment
	 * @param assg
	 * @param req
	 * @throws ActionException
	 */
	private void deleteAssgAsset(AssignmentVO assg, String assgAssetId) throws ActionException {
		StringBuilder sql = new StringBuilder(100);
		sql.append("delete from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_INST_ASSG_ASSET where assg_id=? and assg_asset_id=?");
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, assg.getAssgId());
			ps.setString(2, assgAssetId);
			ps.executeUpdate();
		} catch (SQLException sqle) {
			throw new ActionException("could not delete assg asset");
		}
	}
}