package com.depuysynthesinst;

import com.depuysynthesinst.emails.AbstractDSIEmailVO;
import com.depuysynthesinst.emails.AcctVerifiedVO;
import com.depuysynthesinst.emails.RegChiefEligibleVO;
import com.depuysynthesinst.emails.RegChiefIneligibleVO;
import com.depuysynthesinst.emails.RegDirectorVO;
import com.depuysynthesinst.emails.RegProfferVO;
import com.depuysynthesinst.emails.RegResidentEligibleVO;
import com.depuysynthesinst.emails.RegResidentIneligibleVO;
import com.depuysynthesinst.emails.TransferEligibleVO;
import com.depuysynthesinst.emails.TransferIneligibleVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.registration.ResponseLoader;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.MessageSender;

/****************************************************************************
 * <b>Title</b>: RegistrationPostProcessor.java<p/>
 * <b>Description: handles all the outgoing emails for NEW registration signups; there a 4 versions
 * of the confirmation email, and one special case that gets sent to the admin if a Proffer Letter is
 * necessary.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 22, 2015
 ****************************************************************************/
public class RegistrationPostProcessor extends SimpleActionAdapter {

	public RegistrationPostProcessor() {
	}

	/**
	 * @param arg0
	 */
	public RegistrationPostProcessor(ActionInitVO arg0) {
		super(arg0);
	}


	/**
	 * this is called from the admintool, to notify the user their account has been verified.
	 */
	public void update(ActionRequest req) throws ActionException {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		DSIUserDataVO dsiUser = new DSIUserDataVO();
		req.setValidateInput(false);
		dsiUser.setProfileId(req.getParameter("profileId"));
		dsiUser.setFirstName(req.getParameter("firstName"));
		dsiUser.setLastName(req.getParameter("lastName"));
		dsiUser.setEmailAddress(req.getParameter("email"));
		req.setValidateInput(true);
		log.debug("email DSIUser=" + StringUtil.getToString(dsiUser));

		//if eligible AND verified was just toggled to 'yes', send the email
		MessageSender ms = new MessageSender(getAttributes(), dbConn);
		sendAcctVerifiedEmail(dsiUser, site, ms);
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 * 
	 * RegistrationSubmittal invokes the build method on this class; that's the only 
	 * gateway into the other methods here-in.
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		Integer page = Convert.formatInteger(req.getParameter("page"),0);
		log.debug("starting post-processor, " + page);

		//if page = 3 and they're done then they did NOT register as a Future Leader.  Send the stock email.
		if (3 == page.intValue()) { 
			//SubmittalAction will do this for us; we're done!
			return;
		}

		//set a flag so the default email is not sent by SubmittalAction
		req.setParameter("skipEmail","true");

		//determine which type of user they are, and send the appropriate email
		MessageSender ms = new MessageSender(getAttributes(), dbConn);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		DSIUserDataVO dsiUser = new DSIUserDataVO((UserDataVO)req.getSession().getAttribute(Constants.USER_DATA));
		log.debug("email DSIUser=" + StringUtil.getToString(dsiUser));
		ResponseLoader loader = new ResponseLoader();
		loader.setDbConn(dbConn);
		loader.setSite(site);
		loader.loadRegistrationResponses(dsiUser, site.getSiteId());
		DSIRoleMgr dsiRoleMgr = new DSIRoleMgr();

		if (dsiRoleMgr.isDirector(dsiUser)) {
			sendDirectorEmail(dsiUser, site, ms);
		} else if (dsiUser.isTransfer() && dsiUser.isEligible()) {
			sendTransferEligibleEmail(dsiUser, site, ms);
		} else if (dsiUser.isTransfer()) {
			sendTransferIneligibleEmail(dsiUser, site, ms);
		} else if (dsiRoleMgr.isChiefResident(dsiUser) && dsiUser.isEligible()) {
			sendChiefEligibleEmail(dsiUser, site, ms);
		} else if (dsiRoleMgr.isChiefResident(dsiUser)) {
			sendChiefIneligibleEmail(dsiUser, site, ms);
		} else if ((dsiRoleMgr.isResident(dsiUser) || dsiRoleMgr.isFellow(dsiUser)) && dsiUser.isEligible()) {
			sendResidentEligibleEmail(dsiUser, site, ms);
		} else if (dsiRoleMgr.isResident(dsiUser) || dsiRoleMgr.isFellow(dsiUser)) {
			sendResidentIneligibleEmail(dsiUser, site, ms);
		}

		//if isProffer, send proffer email to the site admin
		String proffer = StringUtil.checkVal(dsiUser.getAttribute(DSIUserDataVO.RegField.DSI_MIL_HOSP.toString()));
		log.debug("proffer=" + proffer + " from " + dsiUser.getAttribute(DSIUserDataVO.RegField.DSI_MIL_HOSP.toString()));
		if (Convert.formatBoolean(proffer))
			sendProfferEmail(dsiUser, site, ms);

	}


	/**
	 * fires the DIRECTOR email
	 * @param dsiUser
	 * @param site
	 * @param ms
	 */
	private void sendDirectorEmail(DSIUserDataVO dsiUser, SiteVO site, MessageSender ms) {
		try {
			AbstractDSIEmailVO mail = new RegDirectorVO();
			mail.addRecipient(dsiUser.getEmailAddress());
			mail.setFrom(site.getMainEmail());
			mail.buildMessage(dsiUser, site);
			ms.sendMessage(mail);
		} catch (Exception e) {
			log.error("could not send director email", e);
		}
	}

	
	/**
	 * fires the RESIDENT/FELLOW elidgible email
	 * @param dsiUser
	 * @param site
	 * @param ms
	 */
	private void sendTransferEligibleEmail(DSIUserDataVO dsiUser, SiteVO site, MessageSender ms) {
		try {
			AbstractDSIEmailVO mail = new TransferEligibleVO();
			mail.addRecipient(dsiUser.getEmailAddress());
			mail.setFrom(site.getMainEmail());
			mail.buildMessage(dsiUser, site);
			ms.sendMessage(mail);
		} catch (Exception e) {
			log.error("could not send transfer eligible email", e);
		}
	}

	
	/**
	 * fires the transfer ineligible email
	 * @param dsiUser
	 * @param site
	 * @param ms
	 */
	private void sendTransferIneligibleEmail(DSIUserDataVO dsiUser, SiteVO site, MessageSender ms) {
		try {
			AbstractDSIEmailVO mail = new TransferIneligibleVO();
			mail.addRecipient(dsiUser.getEmailAddress());
			mail.setFrom(site.getMainEmail());
			mail.buildMessage(dsiUser, site);
			ms.sendMessage(mail);
		} catch (Exception e) {
			log.error("could not send transfer ineligible email", e);
		}
	}

	
	/**
	 * fires the CHEIF eligble email
	 * @param dsiUser
	 * @param site
	 * @param ms
	 */
	private void sendChiefEligibleEmail(DSIUserDataVO dsiUser, SiteVO site, MessageSender ms) {
		try {
			AbstractDSIEmailVO mail = new RegChiefEligibleVO();
			mail.addRecipient(dsiUser.getEmailAddress());
			mail.setFrom(site.getMainEmail());
			mail.buildMessage(dsiUser, site);
			ms.sendMessage(mail);
		} catch (Exception e) {
			log.error("could not send chief eligible email", e);
		}
	}

	
	/**
	 * fires the CHIEF ineligible email
	 * @param dsiUser
	 * @param site
	 * @param ms
	 */
	private void sendChiefIneligibleEmail(DSIUserDataVO dsiUser, SiteVO site, MessageSender ms) {
		try {
			AbstractDSIEmailVO mail = new RegChiefIneligibleVO();
			mail.addRecipient(dsiUser.getEmailAddress());
			mail.setFrom(site.getMainEmail());
			mail.buildMessage(dsiUser, site);
			ms.sendMessage(mail);
		} catch (Exception e) {
			log.error("could not send cheif ineligible email", e);
		}
	}


	/**
	 * fires the resident ineligible email
	 * @param dsiUser
	 * @param site
	 * @param ms
	 */
	private void sendResidentIneligibleEmail(DSIUserDataVO dsiUser, SiteVO site, MessageSender ms) {
		try {
			AbstractDSIEmailVO mail = new RegResidentIneligibleVO();
			mail.addRecipient(dsiUser.getEmailAddress());
			mail.setFrom(site.getMainEmail());
			mail.buildMessage(dsiUser, site);
			ms.sendMessage(mail);
		} catch (Exception e) {
			log.error("could not send resident ineligible email", e);
		}
	}
	

	/**
	 * fires the resident eligible email
	 * @param dsiUser
	 * @param site
	 * @param ms
	 */
	private void sendResidentEligibleEmail(DSIUserDataVO dsiUser, SiteVO site, MessageSender ms) {
		try {
			AbstractDSIEmailVO mail = new RegResidentEligibleVO();
			mail.addRecipient(dsiUser.getEmailAddress());
			mail.setFrom(site.getMainEmail());
			mail.buildMessage(dsiUser, site);
			ms.sendMessage(mail);
		} catch (Exception e) {
			log.error("could not send resident eligible email", e);
		}
	}
	

	/**
	 * fires the proffer notification to the admins
	 * @param dsiUser
	 * @param site
	 * @param ms
	 */
	private void sendProfferEmail(DSIUserDataVO dsiUser, SiteVO site, MessageSender ms) {
		try {
			AbstractDSIEmailVO mail = new RegProfferVO();
			mail.addRecipient("futureleaders@its.jnj.com");
			mail.setFrom(site.getMainEmail());
			mail.buildMessage(dsiUser, site);
			ms.sendMessage(mail);
		} catch (Exception e) {
			log.error("could not send proffer email", e);
		}
	}
	

	/**
	 * fires the acct verified email to the user
	 * @param dsiUser
	 * @param site
	 * @param ms
	 */
	private void sendAcctVerifiedEmail(DSIUserDataVO dsiUser, SiteVO site, MessageSender ms) {
		try {
			AbstractDSIEmailVO mail = new AcctVerifiedVO();
			mail.addRecipient(dsiUser.getEmailAddress());
			mail.setFrom(site.getMainEmail());
			mail.buildMessage(dsiUser, site);
			ms.sendMessage(mail);
		} catch (Exception e) {
			log.error("could not send transfer eligible email", e);
		}
	}
}
