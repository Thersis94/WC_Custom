package com.fastsigns.action.approval;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.io.mail.MessageVO;
import com.siliconmtn.io.mail.MessageVO.InstanceName;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.approval.AbstractApprover;
import com.smt.sitebuilder.approval.ApprovalController.ModuleType;
import com.smt.sitebuilder.approval.ApprovalController.SyncStatus;
import com.smt.sitebuilder.approval.ApprovalException;
import com.smt.sitebuilder.approval.ApprovalVO;
import com.smt.sitebuilder.approval.PageApprover;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.CacheAdministrator;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 * <b>Title</b>: WebeditApprover.java<p/>
 * <b>Description: Handles all approvals for the webedit tool and passes them
 * off to the appropriate approver.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Apr 6, 2015
 ****************************************************************************/

public class WebeditApprover extends AbstractApprover {
	
	private final String ADMIN_EMAIL = "eteam@fastsigns.com";
	
	public enum WebeditType {
		CenterPage("Center Page"), 
		CenterImage("Center Image"),
		Whiteboard("Whiteboard"), 
		CenterModule("Center Page Module"),
		Career("Career");
		
		private String label;
		
		private WebeditType(String label){this.label = label;}
		public String getLabel(){return this.label;}
	}

	public WebeditApprover(SMTDBConnection conn, Map<String, Object> attributes) {
		super(conn, attributes);
	}

	@Override
	public void approve(ApprovalVO... approvables) throws ApprovalException {
		CacheAdministrator cache = new CacheAdministrator(getAttributes());
		
		AbstractApprover app = null;
		for (ApprovalVO vo : approvables) {
			List<String> sites = new ArrayList<>();
			sites.add(vo.getOrganizationId()+"_1");
			sites.add(vo.getOrganizationId()+"_2");
			
			
			switch(WebeditType.valueOf(vo.getItemDesc())) {
				case CenterPage:
					app = new PageApprover(dbConn, getAttributes());
					app.approve(vo);
					break;
				case CenterImage:
					app = new CenterImageApprover(dbConn, getAttributes());
					app.approve(vo);
					break;
				case Whiteboard:
					app = new WhiteboardApprover(dbConn, getAttributes());
					app.approve(vo);
					break;
				case CenterModule:
					app = new CenterPageModuleApprover(dbConn, getAttributes());
					app.approve(vo);
					if (vo.getParentId() != null)
						sites.add(vo.getParentId()+"_7");
					buildSiteList(sites, vo);
					break;
				case Career:
					// Any edits made to a job immediately remove it from the site and change the master record
					// and all deletes bypass approval.  Furthermore the wc_sync table handles the entirety of the
					// approval status and only needs to be updated to approved when it reaches this point.
					vo.setSyncCompleteDt(Convert.getCurrentTimestamp());
					vo.setSyncStatus(SyncStatus.Approved);
					cache.clearCacheByGroup(vo.getOrganizationId().substring(0, vo.getOrganizationId().lastIndexOf('_'))+"_7");
					break;
			}
			
			cache.clearCacheByGroup(sites.toArray(new String[sites.size()]));
		}
		
		prepareEmails(approvables);
	}
	
	/**
	 * Get all franchises that are using a particular module asset so that they can have their cache cleared.
	 * @param sites
	 * @param app
	 */
	private void buildSiteList(List<String> sites, ApprovalVO app) {
		boolean addGlobal = false;
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql =new StringBuilder(180);
		
		sql.append("SELECT lmx.FRANCHISE_ID, mo.FRANCHISE_ID FROM ").append(customDb).append("FTS_CP_MODULE_FRANCHISE_XR mfx ");
		sql.append("left join ").append(customDb).append("FTS_CP_LOCATION_MODULE_XR lmx ");
		sql.append("on lmx.CP_LOCATION_MODULE_XR_ID = mfx.CP_LOCATION_MODULE_XR_ID ");
		sql.append("left join ").append(customDb).append("FTS_CP_MODULE_OPTION mo ");
		sql.append("on mo.CP_MODULE_OPTION_ID = mfx.CP_MODULE_OPTION_ID ");
		sql.append("WHERE mfx.CP_MODULE_OPTION_ID in (?,?) ");
		log.debug(sql.toString()+"|"+app.getWcKeyId()+"|"+app.getOrigWcKeyId());
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, app.getWcKeyId());
			ps.setString(2, app.getOrigWcKeyId());
			
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				String desktop = "FTS_" + rs.getString(1) + "_1";
				String mobile = "FTS_" + rs.getString(1) + "_2";
				if (!sites.contains(desktop))
					sites.add(desktop);
				
				if (!sites.contains(mobile))
					sites.add(mobile);
				if (rs.getInt(2) == -1)
					addGlobal = true;
			}
		} catch (SQLException e) {
			log.error("Unable to get franchises that use this center module.", e);
		}
		
		if (addGlobal)
			addGlobalSites(sites);
	}

	/**
	 * Get all franchises that use omnipresent global modules assets
	 * @param sites
	 */
	private void addGlobalSites(List<String> sites) {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(110);
		sql.append("SELECT FRANCHISE_ID FROM ").append(customDb).append("FTS_FRANCHISE ");
		sql.append("WHERE USE_GLOBAL_MODULES_FLG = 1");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String desktop = "FTS_" + rs.getString(1) + "_1";
				String mobile = "FTS_" + rs.getString(1) + "_2";
				if (!sites.contains(desktop))
					sites.add(desktop);
				
				if (!sites.contains(mobile))
					sites.add(mobile);
			}
		} catch(SQLException e) {
			log.error("Unable to get list of franchises that use omnipresent global assets.", e);
		}
	}

	/**
	 * Prepare the Success emails 
	 */
	private void prepareEmails(ApprovalVO[] approvables) {
		MessageSender ms = new MessageSender(getAttributes(), dbConn);
		for(ApprovalVO app : approvables) {
			if (app.getSyncStatus() == SyncStatus.Approved) {
				MessageVO msg = buildApproveEmail(app);
				if (msg != null) {
					ms.sendMessage(msg);
				}
			}
		}
	}

	private MessageVO buildApproveEmail(ApprovalVO app) {
		EmailMessageVO msg;
		try {
			msg = new EmailMessageVO();
			msg.addRecipient(app.getUserDataVO().getEmailAddress());
			msg.setFrom(ADMIN_EMAIL);
			msg.setInstance(InstanceName.FASTSIGNS);
			
			StringBuilder body = new StringBuilder(250);
			body.append("Your request to change the ").append(WebeditType.valueOf(app.getItemDesc()).getLabel());
			body.append(" for FASTSIGNS Location ").append(app.getOrganizationId().substring(app.getOrganizationId().lastIndexOf('_')+1));
			body.append(" has been approved.\nIf you need assistance please contact ");
			String htmlEnd = "<a href='mailto:eteam@fastsigns.com'>eteam@fastsigns.com</a>";
			String textEnd = "eteam@fastsigns.com";
			
			msg.setHtmlBody(body.toString() + htmlEnd);
			msg.setTextBody(body.toString() + textEnd);
			msg.setSubject("Resolution of " + WebeditType.valueOf(app.getItemDesc()).getLabel() + " Request");
		} catch (InvalidDataException e) {
			log.error("Unable to make webedit success email for approval " + app.getWcSyncId(), e);
			return null;
		}
		
		return msg;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.approval.Approver#reject(com.smt.sitebuilder.approval.ApprovalVO[])
	 */
	@Override
	public void reject(ApprovalVO... approvables) throws ApprovalException {
		
		AbstractApprover app = null;
		for (ApprovalVO vo : approvables) {
			
			switch(WebeditType.valueOf(vo.getItemDesc())) {
				case CenterPage:
					app = new PageApprover(dbConn, getAttributes());
					app.reject(vo);
					break;
				case CenterImage:
					app = new CenterImageApprover(dbConn, getAttributes());
					app.reject(vo);
					break;
				case Whiteboard:
					app = new WhiteboardApprover(dbConn, getAttributes());
					app.reject(vo);
					break;
				case CenterModule:
				case Career:
					super.reject(approvables);
					break;
			}
			
		}
	
	}
	
	/**
	 * Send emails to the eteam to let them know that 
	 * items have been submitted for approval
	 */
	public void submit(ApprovalVO... approvables) throws ApprovalException {
		MessageSender ms = new MessageSender(getAttributes(), dbConn);
		for(ApprovalVO app : approvables) {
			MessageVO msg = buildSubmitEmail(app);
			if (msg != null) {
				log.debug("Sending submitted message");
				ms.sendMessage(msg);
			}
		}
	}
	
	/**
	 * Build the submittal email
	 */
	private MessageVO buildSubmitEmail(ApprovalVO app) {
		EmailMessageVO msg;
		try {
			msg = new EmailMessageVO();
			msg.addRecipient(ADMIN_EMAIL);
			msg.setFrom(ADMIN_EMAIL);
			msg.setInstance(InstanceName.FASTSIGNS);
			
			String siteAlias = findSite(app.getOrganizationId().substring(0, app.getOrganizationId().lastIndexOf('_')));
			
			StringBuilder body = new StringBuilder(250);
			body.append("A request to change the  ").append(WebeditType.valueOf(app.getItemDesc()).getLabel());
			body.append(" for FASTSIGNS Location ").append(app.getOrganizationId().substring(app.getOrganizationId().lastIndexOf('_')+1));
			body.append(" has been submitted by ");
			body.append("");
			String htmlEnd = "<a href='mailTo:"+app.getUserDataVO().getEmailAddress()+"'>"+app.getUserDataVO().getEmailAddress()+
					"</a>.\nPlease log in to <a href='http://"+siteAlias+"/webedit'>webedit</a> to review and approve this change";
			String textEnd = app.getUserDataVO().getEmailAddress()+".\nPlease log in to webedit to review and approve this change";
			
			msg.setHtmlBody(body.toString() + htmlEnd);
			msg.setTextBody(body.toString() + textEnd);
			msg.setSubject("Submission of " + WebeditType.valueOf(app.getItemDesc()).getLabel() + " Request");
		} catch (Exception e) {
			log.error("Unable to make webedit success email for approval " + app.getWcSyncId(), e);
			return null;
		}
		
		return msg;
	}

	/**
	 * Get the site alias for the current organization.
	 * @param orgId
	 * @return
	 * @throws SQLException
	 */
	private String findSite(String orgId) throws SQLException {
		StringBuilder sql = new StringBuilder(150);
		
		sql.append("SELECT sa.SITE_ALIAS_URL FROM SITE s left join SITE_ALIAS ");
		sql.append("sa on s.SITE_ID = sa.SITE_ID where s.SITE_ID = ? and PRIMARY_FLG = 1");
		log.debug(sql+"|"+orgId);
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, orgId+"_7");
			
			ResultSet rs = ps.executeQuery();
			if(rs.next()) return rs.getString(1);
		}
		return "";
	}

	@Override
	public List<ApprovalVO> list(SyncStatus status) throws ApprovalException {
		List<ApprovalVO> list = new ArrayList<>();
		
		StringBuilder sql = new StringBuilder(170);
		
		sql.append("SELECT * FROM WC_SYNC ws ");
		sql.append("left join SITE s on s.ORGANIZATION_ID = ws.ORGANIZATION_ID and (s.SITE_ID = s.ORGANIZATION_ID + '_7' or ");
		sql.append("(s.SITE_ID = s.ORGANIZATION_ID + '_1' and s.ALLOW_ALIAS_PATH_FLG = 0)) ");
		sql.append("left join SITE_ALIAS sa on (sa.SITE_ID = s.SITE_ID or sa.SITE_ID = s.ALIAS_PATH_PARENT_ID)and sa.PRIMARY_FLG = 1 ");
		sql.append("left join PAGE p on p.PAGE_ID = ws.WC_KEY_ID ");
		sql.append("left join PROFILE pr on pr.PROFILE_ID = ws.ADMIN_PROFILE_ID ");
		sql.append("WHERE (MODULE_TYPE_ID = ? or (");
		sql.append("MODULE_TYPE_ID = ? and PORTLET_DESC = ?)) ");
		if (status != null) {
			sql.append("and WC_SYNC_STATUS_CD = ?");
		} else {
			sql.append("and WC_SYNC_STATUS_CD in (?,?,?)");
		}
		sql.append("ORDER BY ws.ORGANIZATION_ID");
		
		log.debug(sql);
		
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, ModuleType.Webedit.toString());
			ps.setString(2, ModuleType.Page.toString());
			ps.setString(3, WebeditType.CenterPage.toString());
			if (status != null) {
				ps.setString(4, status.name());
			} else {
				ps.setString(4, SyncStatus.PendingCreate.name());
				ps.setString(5, SyncStatus.PendingDelete.name());
				ps.setString(6, SyncStatus.PendingUpdate.name());
			}
			
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()) {
				ApprovalVO app = new ApprovalVO(rs);
				// Each type of approval has its variation on the preview url, those urls are built here
				switch(WebeditType.valueOf(app.getItemDesc())) {
					case Career:
						app.setPreviewUrl("//"+rs.getString("SITE_ALIAS_URL")+"/careers?jobPostingId="+app.getWcKeyId());
						break;
					case CenterPage:
						app.setPreviewUrl("//"+rs.getString("SITE_ALIAS_URL")+"/"+rs.getString("ALIAS_PATH_NM")+rs.getString("FULL_PATH_TXT"));
						break;
					case CenterImage:
					case Whiteboard:
					case CenterModule:
						if (rs.getString("ALIAS_PATH_NM") != null)
							app.setPreviewUrl("//"+rs.getString("SITE_ALIAS_URL")+"/"+rs.getString("ALIAS_PATH_NM"));
					default:;
				}
				app.setUserDataVo(new UserDataVO(rs));
				
				list.add(app);
				
			}
		} catch (SQLException e) {
			log.error("Unable to get sync records for webedit.", e);
		}
		return list;
	}

}
