package com.biomed.smarttrak.admin;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashMap;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.util.BiomedSupportEmailUtil;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.http.parser.DirectoryParser;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.content.DocumentVO;
import com.smt.sitebuilder.action.content.ProfileDocumentBinaryHandler;
import com.smt.sitebuilder.action.file.transfer.ProfileDocumentAction;
import com.smt.sitebuilder.action.support.SupportTicketAction;
import com.smt.sitebuilder.action.support.SupportTicketActivityAction;
import com.smt.sitebuilder.action.support.SupportTicketAttachmentAction;
import com.smt.sitebuilder.action.support.TicketActivityVO;
import com.smt.sitebuilder.action.support.TicketAttachmentVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>: SmarttrakSupportTicketActivityAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Custom Action overrides default Email Behavior
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Mar 12, 2017
 ****************************************************************************/
public class SmarttrakSupportTicketActivityAction extends SupportTicketActivityAction {

	public SmarttrakSupportTicketActivityAction() {
		super();
	}

	public SmarttrakSupportTicketActivityAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/** 
	 * Get the cc addresses off the action request and store it in
	 * the attributes before passing everything up to the superclass.
	 */
	@Override
	public void buildCallback(ActionRequest req, TicketActivityVO item) throws ActionException {
		attributes.put("ccAddresses", req.getParameter("ccAddresses"));
		
		if (req.hasParameter("fileName")) {
			req.setParameter("moduleTypeId", "BMG_TICKET");
			addAttachment(req);
			addToItem(item, req);
		}
		
		super.buildCallback(req, item);
	}
	
	
	/**
	 * Get the file data in byte array format for the attachment and add it to the ticket
	 * @param item
	 * @param req
	 * @throws ActionException
	 */
	private void addToItem(TicketActivityVO item, ActionRequest req) throws ActionException {
		ProfileDocumentAction pda = new ProfileDocumentAction();
		pda.setAttributes(attributes);
		pda.setDBConnection(dbConn);
		pda.setActionInit(actionInit);
		DocumentVO doc = pda.getDocumentByProfileDocumentId(req.getParameter(ProfileDocumentAction.PROFILE_DOC_ID));
		try {
			ProfileDocumentBinaryHandler handler = new ProfileDocumentBinaryHandler(doc.getFilePathUrl(), ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getOrganizationId(), "", dbConn, attributes, req);
			item.addAttachment(createTicketAttachment(handler.getFile(), doc));
		} catch (Exception e) {
			log.error("Failed to get file data", e);
		}
	}
	
	
	/**
	 * Create a TicketAttachmentVO from the supplied file and document vo
	 * @param f
	 * @param doc
	 * @return
	 * @throws IOException
	 */
	private TicketAttachmentVO createTicketAttachment(File f, DocumentVO doc) throws IOException {
		byte[] b = new byte[(int)f.length()];
		try (FileInputStream fis = new FileInputStream(f)) {
			fis.read(b); 
			fis.close();
			doc.setDocument(b);
			
			TicketAttachmentVO a = new TicketAttachmentVO();
			a.setFileData(b);
			a.setFileNm(doc.getFileName());
			return a;
		}
	}
	

	/**
	 * Create the attachment record from the request.
	 * @param req
	 * @throws ActionException
	 */
	private void addAttachment(ActionRequest req) throws ActionException {
		SupportTicketAttachmentAction a = new SupportTicketAttachmentAction(this.actionInit);
		a.setAttributes(getAttributes());
		a.setDBConnection(getDBConnection());
		a.build(req);
	}
	
	
	/**
	 * Helper method that sends an email built off the TicketActivityVO.
	 * @param message
	 * @param t
	 */
	@Override
	protected void sendEmailToCustomer(TicketActivityVO act) {
		try {
			new BiomedSupportEmailUtil(getDBConnection(), getAttributes()).sendEmail(act);
		} catch (ActionException e) {
			log.error("There was a problem Sending Support Activity Email", e);
		}
	}

	@Override
	protected LinkedHashMap<String, Object> getParams(ActionRequest req) throws ActionException {
		LinkedHashMap<String, Object> params = new LinkedHashMap<>();

		//Check for TicketId
		if(!StringUtil.isEmpty(req.getParameter(SupportTicketAction.TICKET_ID))) {
			params.put(SupportTicketAction.TICKET_ID, req.getParameter(SupportTicketAction.TICKET_ID));
		} else if(req.hasParameter(DirectoryParser.PARAMETER_PREFIX + "1")) {
			params.put(SupportTicketAction.TICKET_ID, req.getParameter(DirectoryParser.PARAMETER_PREFIX + "1"));
			req.setParameter(SupportTicketAction.TICKET_ID, req.getParameter(DirectoryParser.PARAMETER_PREFIX + "1"));
		} else {
			throw new ActionException("Missing Ticket Id on request.");
		}

		//Check for role Level.  If Registered, filter internal messages out.
		SBUserRole r = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		if(AdminControllerAction.DEFAULT_ROLE_LEVEL >= r.getRoleLevel()) {
			params.put("internalFlg", 0);
		}

		//Check for ActivityId
		if(!StringUtil.isEmpty(req.getParameter(ACTIVITY_ID))) {
			params.put(ACTIVITY_ID, req.getParameter(ACTIVITY_ID));
		}

		return params;
	}
}
