package com.fastsigns.action.approval;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.fastsigns.action.approval.vo.AbstractChangeLogVO;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.SMTMail;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ApprovalTemplateAction.java
 * <p/>
 * <b>Project</b>: SB_FastSigns
 * <p/>
 * <b>Description: </b> This class templates out some of the ApprovalAction
 * functionality.  The send email class is implemented to work in a default
 * manner for request, approve, and deny changes.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Sept 20, 2012
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public abstract class ApprovalTemplateAction extends ApprovalAction {

	public ApprovalTemplateAction(){
	}
	public ApprovalTemplateAction(ActionInitVO actionInit) {
		super(actionInit);
		this.actionInit = actionInit;
	}

	/**
	 * Send email to requesting and approving parties when approval request
	 * status changes.
	 * @param vo
	 */
	@Override
	public void sendEmail(AbstractChangeLogVO vo) {
		String franchiseTxt = (!vo.getOrgId().contains("AU")) ? "FASTSIGNS" : "SIGNWAVE";
		try {
			ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
			UserDataVO u = pm.getProfile(vo.getSubmitterId(), dbConn, ProfileManager.PROFILE_ID_LOOKUP, null);
			UserDataVO r = pm.getProfile(vo.getReviewerId(), dbConn, ProfileManager.PROFILE_ID_LOOKUP, null);
			 //Build Message for the center.
			if(u.isUserReachable() && r.isUserReachable()){
				SMTMail mail = new SMTMail(attributes.get(Constants.CFG_SMTP_SERVER).toString());
				String msg = "Your request to change the ";
				msg += vo.getHFriendlyType() + " ";
				if(vo.getTypeId().equals("ctrPgModule")){
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
				if(vo.getStatusNo() == AbstractChangeLogVO.Status.DENIED.ordinal() )
					msg += "\nThe reason for this is as follows:\n" + vo.getResolutionTxt();
				msg += "\nIf you need assistance please contact eteam@fastsigns.com"; 
				//Set Email Fields
				mail.setUser(attributes.get(Constants.CFG_SMTP_USER).toString());
				mail.setPassword(attributes.get(Constants.CFG_SMTP_PASSWORD).toString());
				mail.setPort(Integer.valueOf(attributes.get(Constants.CFG_SMTP_PORT).toString()));
				mail.setRecpt(u.getEmailAddress().split(","));
				mail.setSubject("Resolution of " + vo.getHFriendlyType() + " request.");
				log.debug(r.getEmailAddress());
				mail.setFrom(r.getEmailAddress());
				mail.setTextBody(msg);
				mail.postMail();
			} else {
				log.error("profiles not found for user: " + vo.getSubmitterId() + ", " + vo.getReviewerId());
			}
		} catch (MailException e) {
			log.error(e);
		} catch (IllegalArgumentException e) {
			log.error(e);
		} catch (DatabaseException e) {
			log.error(e);
		} catch (NullPointerException e) {
			log.error(e);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.fastsigns.action.approval.ApprovalAction#sendRequestNotificationEmail(com.fastsigns.action.approval.vo.AbstractChangeLogVO)
	 */
	@Override
	public void sendRequestNotificationEmail(AbstractChangeLogVO vo){
		final String corpName = (!vo.getOrgId().contains("AU")) ? "FASTSIGNS" : "SIGNWAVE";
		
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
		SMTMail mail = new SMTMail(attributes.get(Constants.CFG_SMTP_SERVER).toString());
		StringBuilder msg = new StringBuilder();
	}
}
