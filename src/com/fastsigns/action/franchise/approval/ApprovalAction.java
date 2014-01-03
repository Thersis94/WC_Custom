package com.fastsigns.action.franchise.approval;

import java.sql.PreparedStatement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import com.fastsigns.action.franchise.FranchisePageAction;
import com.fastsigns.action.franchise.vo.CenterModuleOptionVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.security.UserRoleVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.SMTMail;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.content.ContentVO;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.admin.action.PageModuleRoleAction;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
/****************************************************************************
 * <b>Title</b>: ApprovalAction.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Billy Larsen
 * @version 1.0
 * @since Dec 30, 2011<p/>
 * <b>Changes: Migrated all Approval action code to new package and updated code 
 * to use new workflow</b>
 ****************************************************************************/
@Deprecated
public class ApprovalAction extends SBActionAdapter{
	public static final int FRANCHISE_MAIN_IMAGE_APPROVE = 12;
	public static final int FRANCHISE_MAIN_IMAGE_DENY = 16;
	public static final int PAGE_APPROVE = 4;
	public static final int PAGE_DENY = 5;
	public static final int PAGE_SUBMIT = 6;
	public static final int MODULE_APPROVE = 10;
	public static final int MODULE_DENY = 15;
	public static final int MODULE_SUBMIT = 11;
	public static final int DELETE_ALL_MODULES = 30;
	public static final int WHITEBOARD_APPROVE = 35;
	public static final int WHITEBOARD_DENY = 36;
	public static enum Types {ctrImage, ctrPgModule, sitePg, ctrWhiteBrd};
	public static String [] hFriendlyTypes = {"Center Image", "Center Page Module", "Site Page", "Center White Board"};
	
	public ApprovalAction(){
	}
	
	public ApprovalAction(ActionInitVO actionInit){
		super(actionInit);
	}
	
	public void update(SMTServletRequest req) throws ActionException {
		String orgId = ((SiteVO)req.getAttribute("siteData")).getOrganizationId();
		log.debug("Beginning approval Process");
		Integer bType = null;
		bType = Convert.formatInteger(req.getParameter("bType"));
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		String redir = page.getFullPath() + "?";
		String siteId = orgId + "_" + req.getSession().getAttribute("FranchiseId") + "_1";
		Object msg = "msg.updateSuccess";
		if (req.getParameter("apprFranchiseId") != null)
			siteId = orgId + "_" + req.getParameter("apprFranchiseId") + "_1";
		//turn off string encoding since this is an administrative (& secure) method call
		req.setValidateInput(Boolean.FALSE);
		// Determine which data is being updated
		try {
			switch(bType) {
				case MODULE_SUBMIT:
					log.debug("Beginning module Submission");
					this.reqModOptApproval(req);
					redir += "pendApproval=true&";
					break;
				case FRANCHISE_MAIN_IMAGE_APPROVE:
					log.debug("Beginning image approval");
					this.approveCenterImage(req);
					break;
				case MODULE_APPROVE:
					Integer modOptId = Convert.formatInteger(req.getParameter("moduleOptionId"));
					log.debug("Beginning module approval");
					this.approveModuleOption(modOptId, req);
					break;
				case PAGE_SUBMIT:
					log.debug("Beginning page submit");
					this.reqPageApproval(req);
					redir += "pendApproval=true&";
					break;
				case PAGE_APPROVE:
					log.debug("Beginning page approval");
					this.approvePage(req);
					break;
				case FRANCHISE_MAIN_IMAGE_DENY:
					log.debug("Beginning Module Denial");
					this.denyCenterImage(req);
					break;
				case MODULE_DENY:
					log.debug("Beginning module denial");
					this.deleteModuleOption(req);
					break;
				case PAGE_DENY:
					log.debug("Beginning page denial (In progress");
					this.denyPage(req);
					break;
				case DELETE_ALL_MODULES:
					log.debug("Beginning module cleanup");
					this.removePendingModules(req);
					break;
				case WHITEBOARD_APPROVE:
					log.debug("Beginning Whiteboard Approval");
					this.approveWhiteboard(req);
					break;
				case WHITEBOARD_DENY:
					log.debug("Beginning Whiteboard Approval");
					this.denyWhiteboard(req);
					break;
			}
		} catch(Exception e){
			log.error("Error Updating Center Page", e);	
			msg = "msg.cannotUpdate";
		}
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, redir + "msg=" + msg);
		log.debug("clearing cache group for site: " + siteId);
		super.clearCacheByGroup(siteId);
	}
	/**
	 * Sets the proper request parameters for the LogChange method.
	 * @param req
	 * @return String [id, type]
	 */
	public static String [] getComponentId(SMTServletRequest req){
		String [] temp;
		if(req.getParameter("moduleOptionId") != null)
			temp = new String [] {req.getParameter("moduleOptionId"), Types.ctrPgModule.toString()};
		else if(req.getParameter("cmpId") != null)
			temp = new String [] {req.getParameter("cmpId"), Types.ctrPgModule.toString()};
		else if(req.getParameter("deleteMod") != null)
			temp = new String [] {req.getParameter("deleteMod"), Types.ctrPgModule.toString()};
		else if(req.getParameter("pageId") != null)
			temp = new String [] {req.getParameter("pageId"), Types.sitePg.toString()};
		else if(req.getParameter("isWhiteBrd") != null)
			temp = new String [] {req.getParameter("apprFranchiseId"), Types.ctrWhiteBrd.toString()};
		else
			temp = new String [] {req.getParameter("apprFranchiseId"), Types.ctrImage.toString()};
		req.setParameter("cmpType", temp[1]);
		return temp;
	}
	/**
	 * Send email to requesting center upon approval or denial of page, page module, or center page item.
	 * @param vo
	 */
	public void respondToCenter(ChangeLogVO vo, String orgId){
		String franchiseTxt = (!orgId.contains("AU")) ? "FASTSIGNS" : "SIGNWAVE";
		try {
			ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
			UserDataVO u = pm.getProfile(vo.getSubmitterId(), dbConn, "profile_id");
			UserDataVO r = pm.getProfile(vo.getReviewerId(), dbConn, "profile_id");
			 //Build Message for the center.
			if(u != null){
				SMTMail mail = new SMTMail(attributes.get(Constants.CFG_SMTP_SERVER).toString());
				String msg = "Your request to change the ";
				msg += hFriendlyTypes[Types.valueOf(vo.getTypeId()).ordinal()] + " ";
				if(Types.valueOf(vo.getTypeId()).ordinal() == Types.ctrPgModule.ordinal()){
					String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
					try{
						StringBuilder sb = new StringBuilder("select * from ").append(customDb);
						sb.append("FTS_CP_MODULE_OPTION where cp_module_option_id = ?");
						PreparedStatement ps = null;
						ps = dbConn.prepareStatement(sb.toString());
						ps.setString(1, vo.getComponentId());
						ResultSet rs = ps.executeQuery();
						if(rs.next())
							msg+= rs.getString("option_nm") + " ";
					} catch(SQLException sqle){
						log.debug(sqle);
					}
				}
				msg+= "for " + franchiseTxt + " Location " + vo.getFranchiseId() + " has been " + vo.getFriendlyStatus() + ".";
				if(vo.statusNo == ChangeLogVO.Status.DENIED.ordinal() )
					msg += "\nThe reason for this is as follows:\n" + vo.getResolutionTxt();
				msg += "\nIf you need assistance please contact eteam@fastsigns.com"; 
				//Set Email Fields
				mail.setUser(attributes.get(Constants.CFG_SMTP_USER).toString());
				mail.setPassword(attributes.get(Constants.CFG_SMTP_PASSWORD).toString());
				mail.setPort(Integer.valueOf(attributes.get(Constants.CFG_SMTP_PORT).toString()));
				mail.setRecpt(u.getEmailAddress().split(","));
				mail.setSubject("Resolution of " + hFriendlyTypes[Types.valueOf(vo.getTypeId()).ordinal()] + " request.");
				mail.setFrom(r.getEmailAddress());
				mail.setTextBody(msg);
				mail.postMail();
			}
		} catch (MailException e) {
			log.debug(e);
		} catch (IllegalArgumentException e) {
			log.debug(e);
		} catch (DatabaseException e) {
			log.debug(e);
		}
	}
	
	/**
	 * Returns either a complete list of all logs in the Changelog Table or a 
	 * truncated list of pending changelogs only.
	 * @param pendingOnly return all ChangeLogs or just those pending.
	 * @return
	 */
	public List<ChangeLogVO> getLogs(boolean pendingOnly, SMTServletRequest req){
		Map<String, Object> config = new HashMap<String, Object>();
		config.put("encKey", (String) attributes.get(Constants.ENCRYPT_KEY));
		ProfileManager pm = ProfileManagerFactory.getInstance(config);
		StringEncrypter se = null;
		try {
			se = new StringEncrypter((String) attributes.get(Constants.ENCRYPT_KEY));
		} catch (EncryptionException e1) {
			e1.printStackTrace();
		}
		StringBuilder sb = new StringBuilder();
		List<ChangeLogVO> vos = new ArrayList<ChangeLogVO>();
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		log.debug("Retrieving logs");
		sb.append("select a.type_id, a.desc_txt, a.submitted_dt, a.update_dt, ");
		sb.append("a.submitter_id, b.franchise_id as modfranchise_id, b.option_nm, ");
		sb.append("b.option_desc, c.new_center_image_url, c.FRANCHISE_ID as franchise_id, "); 
		sb.append("d.page_display_nm, d.site_id from ");
		sb.append(customDb).append("FTS_CHANGELOG a left outer join ");
		sb.append(customDb).append("FTS_CP_MODULE_OPTION b on a.COMPONENT_ID = Cast(b.cp_module_option_id as nvarchar(32)) ");
		sb.append("left outer join ").append(customDb).append("FTS_FRANCHISE c on c.FRANCHISE_ID = b.FRANCHISE_ID or ");
		sb.append("Cast(c.FRANCHISE_ID as nvarchar(32)) = a.COMPONENT_ID ");
		sb.append("left outer join PAGE d on a.COMPONENT_ID = d.PAGE_ID");
		if(pendingOnly)
			sb.append(" where status_no = " + ChangeLogVO.Status.PENDING.ordinal());
		sb.append(" order by update_dt desc, submitted_dt desc");
		PreparedStatement ps = null;
		log.debug("sql: " + sb);
		try{
			ps = dbConn.prepareStatement(sb.toString());
			ResultSet rs = ps.executeQuery();
			while(rs.next()){
				ChangeLogVO vo = new ChangeLogVO().setData(rs);
				try{
					UserDataVO p = pm.getProfile(vo.getSubmitterId(), dbConn, "profile_id");
					vo.setSubmitterName(StringUtil.checkVal(se.decrypt(p.getFirstName())) + " " + StringUtil.checkVal(se.decrypt(p.getLastName())));
				} catch (EncryptionException e) {
					log.debug(e);
				} catch (IllegalArgumentException e) {
					log.debug(e);
				} catch (DatabaseException e) {
					log.debug(e);
				}
				vos.add(vo);
			}
		} catch(SQLException sqle){
			log.debug(sqle);
		} finally {
			try { ps.close(); } catch(Exception e) {}
	    }
		return vos;
	}
	/**
	 * Checks to see if a changelog exists and then forwards to either the create
	 * or update methods below.
	 * @param req
	 * @return
	 */
	public void logChange(SMTServletRequest req){
		log.debug("Editing changelog...");
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String [] cmp = getComponentId(req);
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ").append(customDb).append("FTS_CHANGELOG where COMPONENT_ID = ? and STATUS_NO = 0");
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, cmp[0]);
			ResultSet rs = ps.executeQuery();
			if(rs.next()){
				switch(Types.valueOf(cmp[1])){
					case ctrImage:
						updateChangeLog(new CenterImageLogVO(rs), req);
						break;
					case ctrPgModule:
						updateChangeLog(new ModuleLogVO(rs), req);
						break;
					case sitePg:
						updateChangeLog(new PageLogVO(rs), req);
						break;
					case ctrWhiteBrd:
						updateChangeLog(new WhiteBoardLogVO(rs), req);
						break;
				}
			}
			else{
				switch(Types.valueOf(cmp[1])){
				case ctrImage:
					createChangeLog(new CenterImageLogVO(req));
					break;
				case ctrPgModule:
					createChangeLog(new ModuleLogVO(req));
					break;
				case sitePg:
					createChangeLog(new PageLogVO(req));
					break;
				case ctrWhiteBrd:
					createChangeLog(new WhiteBoardLogVO(req));
					break;
				}
			}
		} catch(SQLException sqle){
			log.debug(sqle);
		}
	}
	/**
	 * Creates a changelog entry for the provided ChangeLogVO
	 * @param vo
	 * @return
	 */
	public void createChangeLog(ChangeLogVO vo){
		log.debug("Beginning create ChangeLog...");
		StringBuilder sb = new StringBuilder();
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sb.append("insert into ").append(customDb).append("FTS_CHANGELOG (FTS_CHANGELOG_ID, COMPONENT_ID, TYPE_ID,");
		sb.append(" SUBMITTER_ID, STATUS_NO, DESC_TXT, SUBMITTED_DT) values (?,?,?,?,?,?,?)");
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, vo.getComponentId());
			ps.setString(3, vo.getTypeId());
			ps.setString(4, vo.getSubmitterId());
			ps.setInt(5, vo.getStatusNo());
			ps.setString(6, vo.getDescTxt());
			ps.setTimestamp(7, Convert.getCurrentTimestamp());		
			if (ps.executeUpdate() < 1){
	           log.debug("error creating changelog, could not update.");
	           return;
			}
	
		} catch (SQLException sqle) {
	           log.debug(sqle);
	           return;
		} finally {
			try { ps.close(); } catch(Exception e) {}
	    }
	}
	/**
	 * Updates the changelog when a franchise updates or an admin approves/denys
	 * a centerimage, centerpage or page module.
	 * @param vo
	 * @param req
	 * @return
	 */
	public void updateChangeLog(ChangeLogVO vo, SMTServletRequest req){
		log.debug("Beginning update ChangeLog...");
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		boolean reviewed = (ChangeLogVO.Status.valueOf(req.getParameter("subStatus")) != ChangeLogVO.Status.PENDING);
		HttpSession ses = req.getSession();
    	UserRoleVO role = (UserRoleVO) ses.getAttribute(Constants.ROLE_DATA);
    	vo.setReviewerId(role.getProfileId());
    	vo.setResolutionTxt(req.getParameter("revComments"));
    	if(vo.getResolutionTxt() == null)
    		vo.setResolutionTxt("Approved");
    	vo.setStatusNo(Convert.formatInteger(ChangeLogVO.Status.valueOf(req.getParameter("subStatus")).ordinal()));
    	vo.setFranchiseId(req.getParameter("apprFranchiseId"));
		StringBuilder sb = new StringBuilder();
		if(reviewed){
			sb.append("update ").append(customDb).append("FTS_CHANGELOG set REVIEWER_ID = ?, RESOLUTION_TXT = ?, ");
			sb.append("REVIEW_DT = ?, STATUS_NO = ?, UPDATE_DT = ? where FTS_CHANGELOG_ID = ?");
		}else{
			sb.append("update ").append(customDb).append("FTS_CHANGELOG set UPDATE_DT = ? where FTS_CHANGELOG_ID = ?");
		}
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sb.toString());
			if(reviewed){
			ps.setString(1, vo.getReviewerId());
			ps.setString(2, vo.getResolutionTxt());
			ps.setTimestamp(3, Convert.getCurrentTimestamp());
			ps.setTimestamp(5, Convert.getCurrentTimestamp());
			ps.setInt(4, Convert.formatInteger(ChangeLogVO.Status.valueOf(req.getParameter("subStatus")).ordinal()));
			ps.setString(6, vo.getFtsChangelogId());
			}
			else{
				ps.setTimestamp(1, Convert.getCurrentTimestamp());
				ps.setString(2, vo.getFtsChangelogId());
			}
			if (ps.executeUpdate() < 1){
	            log.debug("error updating changelog, could not update");
	            return;
			}
		} catch (SQLException sqle) {
	        log.error(sqle);
		} finally {
			try { ps.close();} catch(Exception e) {}
	    }
		if(vo.getStatusNo() != ChangeLogVO.Status.PENDING.ordinal())
			this.respondToCenter(vo, ((SiteVO)req.getAttribute("siteData")).getOrganizationId());
	}
	/**
	 * Removes all pending changes from the ChangeLog
	 */
	public void cleanPendingChangelog(){
		//cleanup log
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sb = new StringBuilder();
		sb.append("delete from ").append(customDb);
		sb.append("FTS_CHANGELOG where status_no = 0");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			int x = ps.executeUpdate();
			log.debug("purged " + x + " pending changelog records");
		} catch (SQLException e) {
			log.debug(e);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Removes all Changelog Entries for the given ID's
	 * @param ids String [] of ChangeLog Id's that need to be removed.
	 */
	public void deleteFromChangelog(List<String> ids){
		//cleanup log
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sb = new StringBuilder();
		sb.append("delete from ").append(customDb);
		sb.append("FTS_CHANGELOG where component_id =?");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			for (int x=0; x < ids.size(); x++) {
				ps.setString(1, ids.get(x));
				ps.addBatch();
			}
			ps.executeBatch();
			log.debug("purged " + ids.size() + " changelog records");
		} catch (SQLException e) {
			log.debug(e);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}
	
	/**
	 * Approves the Center Image and updates Changelog
	 * @param req
	 * @throws SQLException
	 */
	protected void approveCenterImage(SMTServletRequest req) throws SQLException {
		log.debug("Beginning Center Image Approval Process...");
		StringBuilder s = new StringBuilder();
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		s.append("update ").append(customDb).append("fts_franchise set ");
		s.append("center_image_url = new_center_image_url, new_center_image_url = ?, ");
		s.append("update_dt = ? where franchise_id = ? ");
		log.debug("apprFranchiseId = " + req.getParameter("apprFranchiseId"));
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setNull(1, java.sql.Types.VARCHAR);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, req.getParameter("apprFranchiseId"));
			ps.executeUpdate();
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		req.setParameter("subStatus", ChangeLogVO.Status.APPROVED.toString());
		logChange(req);
	}
	
	
	/**
	 * handles approval of new Module Options
	 * The live module gets updated with the data from the approved version,
	 * then the interim versions get purged from the system
	 * select approved record -> update into live record -> delete temp records/revisions -> update Changelog
	 * @param req
	 * @throws SQLException
	 */
	
	protected void approveModuleOption(int modOptId, SMTServletRequest req) throws SQLException {
		log.debug("Beginning Approve Module Option Process...");
		StringBuilder sb = null;
		PreparedStatement ps = null;
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);

		CenterModuleOptionVO vo = getModuleOption(modOptId, req);
		
		// update new attributes with old values (in case values have changes)
		Map<Integer, String> vals = getAttrVals(vo.getParentId(), req);
		
		if(vals.size() > 0){
			//remove old attributes (ensure clean transfer of new attributes)
			deleteAttributes(vo.getParentId(), req);
		
			// associate new Attributes (update cp_module_option_Id = parent cp_module_option_id) 
			updateAttributes(modOptId, req, vals, vo);
		}
		//update the existing module with the data from the new/approved revision
		sb = new StringBuilder();
		sb.append("update ").append(customDb);
		sb.append("FTS_CP_MODULE_OPTION set OPTION_NM=?, ");
		sb.append("OPTION_DESC=?, ARTICLE_TXT=?, RANK_NO=?, LINK_URL=?, FILE_PATH_URL=?, ");
		sb.append("THUMB_PATH_URL=?, VIDEO_STILLFRAME_URL=?, CONTENT_PATH_TXT=?, START_DT=?, ");
		sb.append("END_DT=?, CREATE_DT=?, APPROVAL_FLG=?, FRANCHISE_ID=?, PARENT_ID=? ");
		sb.append("where CP_MODULE_OPTION_ID=?");
		
		int i = 0;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(++i, vo.getOptionName());
			ps.setString(++i, vo.getOptionDesc());
			ps.setString(++i, vo.getArticleText());
			ps.setInt(++i, vo.getRankNo());
			ps.setString(++i, vo.getLinkUrl());
			ps.setString(++i, vo.getFilePath());
			ps.setString(++i, vo.getThumbPath());
			ps.setString(++i, vo.getStillFramePath());
			ps.setString(++i, vo.getContentPath());
			ps.setDate(++i, Convert.formatSQLDate(vo.getStartDate()));
			ps.setDate(++i, Convert.formatSQLDate(vo.getEndDate()));
			ps.setTimestamp(++i, Convert.getCurrentTimestamp());
			ps.setInt(++i, 1); //approval_flg=1 means approved
			if (vo.getFranchiseId() == null || vo.getFranchiseId() == 0) {
				ps.setNull(++i, java.sql.Types.INTEGER);
			} else {
				ps.setInt(++i, vo.getFranchiseId());
			}
			ps.setNull(++i, java.sql.Types.INTEGER);
			if (Convert.formatInteger(vo.getParentId()) > 0) {
				ps.setInt(++i, vo.getParentId());
			} else {
				ps.setInt(++i, vo.getModuleOptionId());
			}
			ps.executeUpdate();
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}		
		//delete all interim revision data related to this update
		if (Convert.formatInteger(vo.getParentId()) > 0) {
			sb = new StringBuilder();
			sb.append("delete from ").append(customDb);
			sb.append("FTS_CP_MODULE_OPTION where parent_id=?");
			try {
				ps = dbConn.prepareStatement(sb.toString());
				ps.setInt(1, vo.getParentId());
				int x = ps.executeUpdate();
				log.debug("purged " + x + " modules");

			} finally {
				try { ps.close(); } catch (Exception e) {}
			}
		}
		req.setParameter("cmpId", "" + vo.getParentId());
		req.setParameter("moduleOptionId", "" + vo.getParentId());
		req.setParameter("subStatus", ChangeLogVO.Status.APPROVED.toString());
		logChange(req);
	}
	/**
	 * simple wrapper to change the approval flag for a module_option.
	 * This tags the record to be visible on the Approval screen for 
	 * administrators to review and approve and updates Changelog
	 * @param req
	 * @throws SQLException
	 */
	protected void reqModOptApproval(SMTServletRequest req) throws SQLException {
		log.debug("Beginning Request Module Option Approval Process...");
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(customDb);
		sb.append("FTS_CP_MODULE_OPTION ");
		sb.append("set APPROVAL_FLG=? where CP_MODULE_OPTION_ID=?");
		String[] ids = req.getParameter("modOptsToSubmit").split(",");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			for (int x=0; x < ids.length; x++) {
				ps.setInt(1, 100);
				ps.setInt(2, Convert.formatInteger(ids[x]));
				ps.addBatch();
			}
			ps.executeBatch();
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		for(String s : ids){
			if(!s.equals("0")){
				sb = new StringBuilder();
				sb.append("select parent_id from ").append(customDb);
				sb.append("FTS_CP_MODULE_OPTION where CP_MODULE_OPTION_ID = ?");
				try{
					ps=dbConn.prepareStatement(sb.toString());
					ps.setString(1, s);
					ResultSet rs = ps.executeQuery();
					if(rs.next()){
						req.setParameter("cmpId", rs.getString(1));
						logChange(req);
					}
				} finally {
					try {ps.close(); } catch(Exception e){}
				}
			}
		}
	}
	
	/** 
	 * Called from ApprovalAction to approve a page and the 
	 * child CONTENT portlet(s) 
	 */
	protected void approvePage(SMTServletRequest req) throws ActionException {
		log.debug("Beginning Approve Page Process...");
		String groupId = "FAST_SIGNS";
		String countryCode = ((SiteVO)req.getAttribute("siteData")).getCountryCode();
		if(!countryCode.equals("US"))
			groupId +="_" + countryCode;
		String pageId = req.getParameter("pageId");
		//change page's startDate.  This only happens once, when the page is first created.
		//updated to the page only require refreshing the page_module table.
		if (Convert.formatBoolean(req.getParameter("updatePage"))) {
			String sql = "update page set live_start_dt=? where page_id=?";
			PreparedStatement ps = null;
			try {
				ps = dbConn.prepareStatement(sql);
				ps.setTimestamp(1, Convert.getCurrentTimestamp());
				ps.setString(2, pageId);
				ps.executeUpdate();
			} catch (SQLException sqle) {
				log.error(sqle);
				throw new ActionException(sqle);
			} finally {
				try { ps.close(); } catch (Exception e) {}
			}
		}
		//add roles to the page_module
		PageVO page = new PageVO();
		page.setPageId(pageId);
		FranchisePageAction fPA = new FranchisePageAction(this.actionInit);
		fPA.setDBConnection(dbConn);
		fPA.setAttributes(attributes);
		ContentVO c = fPA.getContent(page, null, groupId);
		
		//flush everything from the page except this one module
		String sql = "delete from page_module where page_id=? and action_id != ?";
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql);
			ps.setString(1, pageId);
			ps.setString(2, c.getActionId());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error(sqle);
			throw new ActionException(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		//create roles for all permission levels to our page_module
		SMTActionInterface aac = new PageModuleRoleAction(this.actionInit);
        aac.setDBConnection(dbConn);
		req.setAttribute("pageModuleId", c.getAttribute("pmid"));
        aac.setAttributes(attributes);
        aac.update(req);
        
        String siteId = req.getParameter("siteId");
        if (req.getParameter("apprFranchiseId") != null) 
        	siteId = "FTS_" + req.getParameter("apprFranchiseId") + "_1";
        
        log.debug("clearing cache for siteId=" + siteId);
        super.clearCacheByGroup(siteId);
        if(pageId == null){
        	sql = "select TOP 1 * from PAGE order by CREATE_DT desc";
        	try{
        		ps = dbConn.prepareStatement(sql);
        		ResultSet rs = ps.executeQuery();
        		if(rs.next()){
        			req.setParameter("pageId", rs.getString("PAGE_ID"));
        		}
        	} catch (SQLException e) {
				log.debug(e);
			} finally {
    			try { ps.close(); } catch (Exception e) {}
    		}
        }
		req.setParameter("subStatus", ChangeLogVO.Status.APPROVED.toString());
        logChange(req);
	}
	/**
	 * When a Page is submitted, this method sets the live_start_dt from 2100 to 
	 * 2200.
	 * @param req
	 * @throws ActionException
	 */
	protected void reqPageApproval(SMTServletRequest req) throws ActionException{
		log.debug("Beginning Submit Page Process...");
		String startDate = req.getParameter("startDate");
		String sql = "update page set live_start_dt=?, update_dt=? where page_id=?";
		String[] ids = null;
		if(req.getParameter("pageId") != null)
			ids = req.getParameter("pageId").split(",");
		else
			ids = req.getParameter("pagesToSubmit").split(",");
		PreparedStatement ps = null;
			try {
				ps = dbConn.prepareStatement(sql);
				for (int x=0; x < ids.length; x++) {
					ps.setTimestamp(1, Convert.formatTimestamp(Convert.DATE_SLASH_PATTERN, startDate));
					ps.setTimestamp(2, Convert.getCurrentTimestamp());
					ps.setString(3, ids[x]);
					ps.addBatch();
				}
					ps.executeBatch();
				log.debug("submitted " + ids.length + " pages for approval");
			} catch (SQLException e) {
				log.debug(e);
			} finally {
				try { ps.close(); } catch (Exception e) {}
			}
			for(String s : ids){
				if(!s.equals("0")){
				req.setParameter("pageId", s);
				logChange(req);
				}
			}
	}
	
	protected void approveWhiteboard(SMTServletRequest req) throws SQLException{
		log.debug("Beginning White Board Approval Process...");
		StringBuilder s = new StringBuilder();
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		s.append("update ").append(customDb).append("fts_franchise set ");
		s.append("white_board_text = new_white_board_text, new_white_board_text = ?, ");
		s.append("update_dt = ? where franchise_id = ? ");
		log.debug("apprFranchiseId = " + req.getParameter("apprFranchiseId"));
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setNull(1, java.sql.Types.VARCHAR);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, req.getParameter("apprFranchiseId"));
			ps.executeUpdate();
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		req.setParameter("subStatus", ChangeLogVO.Status.APPROVED.toString());
		logChange(req);
	}

	/**
	 * Removes all children modules from a given parent and updates changelog.
	 * @param req
	 * @throws SQLException
	 */
	private void deleteModuleOption(SMTServletRequest req) throws SQLException {
		Integer optionId = Convert.formatInteger(req.getParameter("deleteId"));
		StringBuilder sb = new StringBuilder();
		sb.append("select PARENT_ID from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sb.append("fts_cp_module_option where cp_module_option_id=? ");
		sb.append("or parent_id=?");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setInt(1, optionId);
			ps.setInt(2, optionId);
			ResultSet rs = ps.executeQuery();
			if(rs.next()){
				req.setParameter("deleteMod", rs.getString("PARENT_ID"));
				req.setParameter("subStatus", ChangeLogVO.Status.DENIED.toString());
				logChange(req);
			}
			
		} catch (SQLException e) {
			log.debug(e);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		sb = new StringBuilder();
		sb.append("delete from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sb.append("fts_cp_module_option where cp_module_option_id=? ");
		sb.append("or parent_id=?");
		
		ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setInt(1, optionId);
			ps.setInt(2, optionId);
			ps.executeUpdate();
			
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}
	/**
	 * Revmoves the new_center_image_url from a franchise in event of denial.
	 * @param req
	 * @throws SQLException
	 */
	private void denyCenterImage(SMTServletRequest req) throws SQLException {
		log.debug("Beginning Center Image Delete Process...");
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder s = new StringBuilder();
		s.append("update ").append(customDb).append("fts_franchise set ");
		s.append("new_center_image_url = ?, ");
		s.append("update_dt = ? where franchise_id = ? ");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setNull(1, java.sql.Types.VARCHAR);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, req.getParameter("apprFranchiseId"));
			ps.executeUpdate();
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		req.setParameter("subStatus", ChangeLogVO.Status.DENIED.toString());
		req.setParameter("pageId", null);

		logChange(req);
	}
	/**
	 * When a Franchise user wishes to take back pending submissions for approval
	 * this removes all pending modules for a given Franchise and removes changelogs
	 * that deal with them.
	 * @param req
	 * @throws SQLException
	 */
	private void removePendingModules(SMTServletRequest req) throws SQLException {
		log.debug("Beginning Module Cleanup Process...");
		List<String> ids = new ArrayList<String>();
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String franchiseId = StringUtil.checkVal(req.getSession().getAttribute("FranchiseId"));
		StringBuilder sb = new StringBuilder();
		sb.append("select PARENT_ID from ").append(customDb);
		sb.append("fts_cp_module_option where FRANCHISE_ID = ? ");
		sb.append("and PARENT_ID is not null and PARENT_ID <> 0 ");
		sb.append("and APPROVAL_FLG = 100");
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, franchiseId);
			ResultSet rs = ps.executeQuery();
			while(rs.next()){
				ids.add(rs.getString("PARENT_ID"));
			}
			} finally 
			{try {ps.close();} catch (Exception e) {}}
		if(ids.size() > 0){
			sb = new StringBuilder();
			sb.append("update ").append(customDb);
			sb.append("fts_cp_module_option set APPROVAL_FLG = 0 where APPROVAL_FLG = 100 ");
			sb.append("and PARENT_ID = ?");
			try{
				ps = dbConn.prepareStatement(sb.toString());
				for(String s : ids){
				ps.setString(1, s);
				ps.addBatch();
				}
				ps.executeBatch();
				log.debug("purged " + ids.size() + " pending modules");
				deleteFromChangelog(ids);
				} finally {
				try {
					ps.close();
				} catch (Exception e) {}
			}
		}
	}
	/**
	 * Changes the live_start_date of the page from 2200 to back to the Not 
	 * submitted status of 2100 and updates the changelog.
	 * @param req
	 */
	private void denyPage(SMTServletRequest req){
		log.debug("Beginning Page Cleanup Process...");
		String sql = "update page set live_start_dt=?, update_dt=? where page_id=?";
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sql);			
			ps.setTimestamp(1, Convert.formatTimestamp(Convert.DATE_SLASH_PATTERN, "01/01/2100"));
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, req.getParameter("pageId"));
			ps.executeUpdate();
			} catch (SQLException e) {
				log.debug(e);
			} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
			req.setParameter("subStatus", ChangeLogVO.Status.DENIED.toString());
			logChange(req);
	}
	
	private CenterModuleOptionVO getModuleOption(int modOptId, SMTServletRequest req){
		StringBuilder sb = new StringBuilder();
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sb.append("select cp_module_option_id as 'mod_opt_id', create_dt as 'option_create_dt', * from ").append(customDb);
		sb.append("FTS_CP_MODULE_OPTION where cp_module_option_id=? ");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setInt(1, modOptId);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				return new CenterModuleOptionVO(rs);
		} catch (SQLException sqle) {
			log.error(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		return null;
	}
	
	private Map<Integer, String> getAttrVals(int modOptId, SMTServletRequest req){
		StringBuilder sb = new StringBuilder();
		Map<Integer, String> vals = new HashMap<Integer, String>();
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sb = new StringBuilder();
		sb.append("select CP_OPTION_ATTR_ID, ATTRIB_VALUE_TXT from ").append(customDb).append("FTS_CP_OPTION_ATTR ");
		sb.append("where CP_MODULE_OPTION_ID = ?");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setInt(1, modOptId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()){
				vals.put(rs.getInt("CP_OPTION_ATTR_ID"),rs.getString("ATTRIB_VALUE_TXT"));
			}
		} catch (SQLException sqle) {
			log.error(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		return vals;
	}
	
	private void deleteAttributes(int modOptId, SMTServletRequest req){
		StringBuilder sb = new StringBuilder();
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sb = new StringBuilder();
		sb.append("delete from ").append(customDb).append("FTS_CP_OPTION_ATTR ");
		sb.append("where CP_MODULE_OPTION_ID = ?");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setInt(1, modOptId);
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}
	
	private void updateAttributes(int modOptId, SMTServletRequest req, Map<Integer, String> vals, CenterModuleOptionVO vo){
		StringBuilder sb = new StringBuilder();
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sb.append("update ").append(customDb).append("FTS_CP_OPTION_ATTR ");
		sb.append("set CP_MODULE_OPTION_ID = ?, ATTRIB_VALUE_TXT = ? ");
		sb.append("where CP_MODULE_OPTION_ID = ? and ATTR_PARENT_ID = ?");
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			for(int key : vals.keySet()){
				log.debug("sql = " + sb + " | " + vo.getParentId() + " | " + vals.get(key) + " | " + modOptId + " | " + key);
			if (vo.getParentId() > 0) {
				ps.setInt(1, vo.getParentId());
			} else {
				ps.setInt(1, vo.getModuleOptionId());
			}	
			ps.setString(2, vals.get(key));
			ps.setInt(3, modOptId);
			ps.setInt(4, key);
			ps.addBatch();
			}
			ps.executeUpdate();
		} catch (SQLException e) {
			log.debug(e);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		sb = new StringBuilder();
		sb.append("update ").append(customDb).append("FTS_CP_OPTION_ATTR ");
		sb.append("set CP_MODULE_OPTION_ID = ? ");
		sb.append("where CP_MODULE_OPTION_ID = ? ");
		try{
			ps = dbConn.prepareStatement(sb.toString());
			if (vo.getParentId() > 0) {
				ps.setInt(1, vo.getParentId());
			} else {
				ps.setInt(1, vo.getModuleOptionId());
			}	
			ps.setInt(2, modOptId);
			ps.executeUpdate();

		} catch (SQLException e) {
			log.debug(e);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
	}
	
	private void denyWhiteboard(SMTServletRequest req) throws SQLException{
		log.debug("Beginning White Board Delete Process...");
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder s = new StringBuilder();
		s.append("update ").append(customDb).append("fts_franchise set ");
		s.append("new_white_board_text = ?, ");
		s.append("update_dt = ? where franchise_id = ? ");
		
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setNull(1, java.sql.Types.VARCHAR);
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, req.getParameter("apprFranchiseId"));
			ps.executeUpdate();
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		req.setParameter("subStatus", ChangeLogVO.Status.DENIED.toString());
		req.setParameter("pageId", null);

		logChange(req);
	}
}