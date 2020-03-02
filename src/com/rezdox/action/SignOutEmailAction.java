package com.rezdox.action;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.io.mail.EmailRecipientVO;
import com.siliconmtn.sb.email.util.EmailCampaignBuilderUtil;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: SignOutEmailAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Sends an email letting the user know that a rezdox admin 
 * is finished and signed out of their account.
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Feb 25, 2020
 * @updates:
 ****************************************************************************/
public class SignOutEmailAction extends SimpleActionAdapter {
	public static final String REZDOX_SIGNOUT = "REZDOX_SIGNOUT";

	public SignOutEmailAction() {
		super();
	}

	public SignOutEmailAction(ActionInitVO arg0) {
		super(arg0);
	}

	/**
	 * @param dbConn
	 * @param attributes
	 */
	public SignOutEmailAction(Connection dbConn, Map<String, Object> attributes) {
		this();
		this.setAttributes(attributes);
		this.setDBConnection((SMTDBConnection) dbConn);
	}
	
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		EmailCampaignBuilderUtil ecbu = new EmailCampaignBuilderUtil(dbConn, attributes);
		
		Map<String, Object> emailParams = new HashMap<>();
		emailParams.put("firstName", user.getFirstName());
		emailParams.put("lastName", user.getLastName());
		emailParams.put("emailAddressText", user.getEmailAddress());
		
		List<EmailRecipientVO> recipients = new ArrayList<>();
		recipients.add(new EmailRecipientVO(user.getProfileId(), user.getEmailAddress(), EmailRecipientVO.TO));
	
		ecbu.sendMessage(emailParams, recipients, REZDOX_SIGNOUT);
	}

}
