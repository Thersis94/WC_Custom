package com.depuysynthesinst.assg;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.search.SolrActionVO;
import com.smt.sitebuilder.action.search.SolrFieldVO;
import com.smt.sitebuilder.action.search.SolrQueryProcessor;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.action.search.SolrFieldVO.BooleanType;
import com.smt.sitebuilder.action.search.SolrFieldVO.FieldType;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>: MyAssignmentsAction.java<p/>
 * <b>Description: Returns a list of My Assignments; or one assignment with all of it's Solr assets loaded.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 7, 2015
 ****************************************************************************/
public class MyAssignmentsAction extends SBActionAdapter {

	/**
	 * 
	 */
	public MyAssignmentsAction() {
	}

	/**
	 * @param actionInit
	 */
	public MyAssignmentsAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	
	/**
	 * load a list of assignments assigned to "me", with a %complete for each one.  
	 * If ID is passed, only load that one Assignment - this includes a call to SOLR 
	 * for the asset's details & URL.
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(ActionRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA); //used to load Solr assets
		String assgId = req.getParameter("assignmentId");
		
		//load the list of assignments
		List<AssignmentVO> data = loadAssignmentList(user.getProfileId(), assgId);
		
		int cnt = 0;
		for (AssignmentVO assg : data) {			
			//load assets from Solr
			if (assg.getAssets().size() > 0)
				loadSolrAssets(assg, role);
		
			if (! assg.isComplete()) ++cnt;
		}
		//don't stomp a valid count if we're looking at a single course
		if (!req.hasParameter("assignmentId"))
				user.addAttribute("myAssgCnt", cnt);	
		
		//if we're displaying only one assignment we need to load all of it's Solr assets (for detailed view/display)
		if (assgId != null && data.size() == 1) {
			AssignmentVO assg = data.get(0);
			
			//also load the Profile for the Resident Director
			ProfileManager pm = ProfileManagerFactory.getInstance(getAttributes());
			try {
				assg.setDirectorProfile(pm.getProfile(assg.getDirectorProfileId(), dbConn, ProfileManager.PROFILE_ID_LOOKUP, role.getOrganizationId()));
			} catch (DatabaseException de) {
				log.error("could not load profile for RD", de);
			}
			pm = null;
		}
		
		mod.setActionData(data);
	}
	
	
	/**
	 * marks an assignment's Asset as complete.  AJAX call made from the browser
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(ActionRequest req) throws ActionException {
		String reqType = StringUtil.checkVal(req.getParameter("reqType"), "");
		switch (reqType) {
			case "complete":
				captureResAssgAsset(req);
				break;
			case "skipAhead":
				captureSkipAhead(req);
				break;
		}
	}
	
	
	/**
	 * captures the user's decision to skip ahead in their assignments, so we don't 
	 * prompt them repeatedly.
	 * @param req
	 * @throws ActionException
	 */
	private void captureSkipAhead(ActionRequest req) throws ActionException {
		String resAssgId = StringUtil.checkVal(req.getParameter("resAssgId"), null);
		String assgId = StringUtil.checkVal(req.getParameter("assgId"), null);
		
		//data validation - require a user and an ID to enact upon
		if (resAssgId == null || assgId == null) 
			throw new ActionException("missing data, cannot complete transaction");
		
		StringBuilder sql = new StringBuilder(100);
		sql.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_INST_RES_ASSG set skip_ahead_flg=? where assg_id=? and res_assg_id=?");
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, 1);
			ps.setString(2, assgId);
			ps.setString(3, resAssgId);
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("could not delete user assignment asset completion tag", sqle);
		}
	}
	
	
	/**
	 * controls transactions to the _RES_ASSG_ASSET table
	 * @param req
	 * @throws ActionException
	 */
	private void captureResAssgAsset(ActionRequest req) throws ActionException {
		String assgAssetId = StringUtil.checkVal(req.getParameter("assgAssetId"), null);
		String resAssgId = StringUtil.checkVal(req.getParameter("resAssgId"), null);
		
		//data validation - require a user and an ID to enact upon
		if (resAssgId == null || assgAssetId == null) 
			throw new ActionException("missing data, cannot complete transaction");
		
		if (req.hasParameter("delete")) {
			//for users who 'un-check' their completions
			deleteResAssgAsset(assgAssetId, resAssgId);
		} else {
			//record the transaction using the current timestamp
			insertResAssgAsset(assgAssetId, resAssgId);
		}
	}
	
	
	/**
	 * delete an entry from the RES_ASSG_ASSET table using the assgAssetId and the resAssgId
	 * @param assgAssetId
	 * @param resAssgId
	 */
	private void deleteResAssgAsset(String assgAssetId, String resAssgId) {
		StringBuilder sql = new StringBuilder(100);
		sql.append("delete from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_INST_RES_ASSG_ASSET where assg_asset_id=? and res_assg_id=?");
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, assgAssetId);
			ps.setString(2, resAssgId);
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("could not delete user assignment asset completion tag", sqle);
		}
	}
	
	
	/**
	 * inserts the 'is complete' record for "this user on this assignment-asset"
	 * @param assgAssetId
	 * @param resAssgId
	 */
	private void insertResAssgAsset(String assgAssetId, String resAssgId) {
		StringBuilder sql = new StringBuilder(100);
		sql.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_INST_RES_ASSG_ASSET (RES_ASSG_ASSET_ID, ");
		sql.append("ASSG_ASSET_ID, RES_ASSG_ID, CREATE_DT, COMPLETE_DT) values (?,?,?,?,?)");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, assgAssetId);
			ps.setString(3, resAssgId);
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
			ps.setTimestamp(5, Convert.getCurrentTimestamp());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("could not delete user assignment asset completion tag", sqle);
		}
	}
	
	
	/**
	 * call Solr and load each of the assets that are part of this assignment
	 * @param assg
	 */
	protected void loadSolrAssets(AssignmentVO assg, SBUserRole role) {
		SolrActionVO qData = new SolrActionVO();
		qData.setNumberResponses(assg.getAssets().size()); //all
		for (AssignmentAssetVO vo : assg.getAssets()) {
			log.debug("querying for asset: " + vo.getAssgAssetId() + " = " + vo.getSolrDocumentId());
			SolrFieldVO field = new SolrFieldVO();
			field.setBooleanType(BooleanType.OR);
			field.setFieldType(FieldType.SEARCH);
			field.setFieldCode(SearchDocumentHandler.DOCUMENT_ID);
			field.setValue(vo.getSolrDocumentId());
			qData.addSolrField(field);
		}
		qData.setOrganizationId(role.getOrganizationId());
		qData.setRoleLevel(role.getRoleLevel());
		SolrQueryProcessor sqp = new SolrQueryProcessor(getAttributes());
		SolrResponseVO resp = sqp.processQuery(qData);
		
		//merge the solr results into the AssignmentAssetVOs
		for (SolrDocument doc : resp.getResultDocuments()) {
			//find the matching assetVO
			for (AssignmentAssetVO vo : assg.getAssets()) {
				if (!doc.getFieldValue(SearchDocumentHandler.DOCUMENT_ID).equals(vo.getSolrDocumentId()))
						continue;
				
				vo.setSolrDocument(doc);
			}
		}
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
		StringBuilder sql = new StringBuilder(400);
		sql.append("select a.assg_id, a.assg_nm, a.due_dt, a.sequential_flg, a.desc_txt, a.publish_dt, a.update_dt, ");
		sql.append("ra.skip_ahead_flg, aa.solr_document_id, aa.order_no, aa.assg_asset_id, ");
		sql.append("raa.complete_dt, rd.profile_id as res_dir_profile_id, ra.res_assg_id ");
		sql.append("from ").append(customDb).append("DPY_SYN_INST_ASSG a ");
		sql.append("inner join ").append(customDb).append("DPY_SYN_INST_RES_DIR rd on a.res_dir_id=rd.res_dir_id ");
		sql.append("inner join ").append(customDb).append("DPY_SYN_INST_RES_ASSG ra on a.assg_id=ra.assg_id ");
		sql.append("inner join ").append(customDb).append("DPY_SYN_INST_RESIDENT r on ra.resident_id=r.resident_id and r.active_flg=1 ");
		sql.append("left outer join ").append(customDb).append("DPY_SYN_INST_ASSG_ASSET aa on a.assg_id=aa.assg_id ");
		sql.append("left outer join ").append(customDb).append("DPY_SYN_INST_RES_ASSG_ASSET raa on aa.assg_asset_id=raa.assg_asset_id and ra.res_assg_id=raa.res_assg_id ");
		sql.append("where r.profile_id=? ");
		if (assignmentId != null) sql.append("and a.assg_id=? ");
		sql.append("and a.publish_dt is not null and a.active_flg=1 ");
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
					log.debug(rs.getDate("update_dt"));
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
	 * adds all of the RD's attached to the given user, to their UserDataVO
	 * @param user
	 */
	public Map<String, UserDataVO> loadResidencyDirectors(String profileId, boolean pendingOnly) {
		String customDb = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(250);
		sql.append("select rd.profile_id, res.resident_id ");
		sql.append("from ").append(customDb).append("DPY_SYN_INST_RES_DIR rd ");
		sql.append("inner join ").append(customDb).append("DPY_SYN_INST_RESIDENT res on rd.res_dir_id=res.res_dir_id ");
		sql.append("where res.profile_id=? and res.active_flg=1 ");
		if (pendingOnly) sql.append("and res.invite_sent_dt is not null and res.consent_dt is null"); //invitation sent but not accepted
		log.debug(sql);
		
		Map<String, String> residents = new HashMap<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, profileId);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				residents.put(rs.getString(1), rs.getString(2));
			
		} catch (SQLException sqle) {
			log.error("could not load pending ResDirs", sqle);
		}
		
		if (residents.isEmpty()) return null;
		
		Map<String,UserDataVO> resDirs = new HashMap<>(residents.size());
		ProfileManager pm = ProfileManagerFactory.getInstance(getAttributes());
		try {
			Map<String, UserDataVO> users = pm.searchProfileMap(dbConn, new ArrayList<String>(residents.keySet()));
			//bind the two maps into one, keyed with residentId so we know which record to update when the user accepts.
			for (String key : users.keySet())
				resDirs.put(residents.get(key), users.get(key));

		} catch (Exception e) {
			log.error("could not load profiles for resDirs", e);
		}
		return resDirs;
	}
}