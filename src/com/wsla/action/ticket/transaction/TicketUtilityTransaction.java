package com.wsla.action.ticket.transaction;

// JDK 1.8.x
import java.util.Arrays;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: TicketUtilityTransaction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Micro changes that are utility in nature and don't'require 
 * an independent action
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Nov 5, 2018
 * @updates:
 ****************************************************************************/
public class TicketUtilityTransaction extends SBActionAdapter {

	/**
	 * Transaction key for the facade
	 */
	public static final String AJAX_KEY = "utility";
	
	/**
	 * 
	 */
	public TicketUtilityTransaction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TicketUtilityTransaction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		String typeCode = req.getParameter("typeCode");
		try {
			if ("EMAIL".equals(typeCode)) {
				String userTicketLink = req.getParameter("userTicketLink");
				String recipient = StringUtil.checkVal(req.getParameter("recipient"));
				sendLinkEmails(recipient, userTicketLink);
			}
		} catch (Exception e) {
			log.error("Unable to save asset", e);
			putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * Sends the email link to the supplied users
	 * @param recipient
	 */
	public void sendLinkEmails(String recipient, String link) {
		List<String> recipients = Arrays.asList(recipient.split("\\,"));
		log.debug(recipients);
		log.debug(link);
		
		//TODO Connect to Email Campaigns?
	}
}
