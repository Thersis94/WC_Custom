package com.biomed.smarttrak.admin;

import java.util.LinkedHashMap;
import java.util.Map;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.util.BiomedSupportEmailUtil;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.parser.DirectoryParser;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.file.transfer.ProfileDocumentAction;
import com.smt.sitebuilder.action.support.SupportTicketAction;
import com.smt.sitebuilder.action.support.SupportTicketActivityAction;
import com.smt.sitebuilder.action.support.SupportTicketAttachmentAction;
import com.smt.sitebuilder.action.support.TicketActivityVO;
import com.smt.sitebuilder.action.support.TicketAttachmentVO;
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
	 * Build Support_Activity Sql Query.
	 * @param ticketId
	 * @param schema
	 * @return
	 */
	@Override
	protected String formatRetrieveQuery(Map<String, Object> params) {
		
		StringBuilder sql = new StringBuilder(150);
		sql.append("select sa.*, p.first_nm, p.last_nm ");
		
		sql.append(", pd.profile_document_id, pd.file_nm ");
		
		sql.append("from support_activity sa ");
		sql.append("inner join profile p on sa.profile_id = p.profile_id ");
		
		// Get any files uploaded as part of this activity.
		// Uploads are associated by ticket id and must therefore be associated to
		// activities based on file name and creation time.
		sql.append("left outer join profile_document pd ");
		sql.append("on pd.feature_id = sa.ticket_id and 'Attachment ' + pd.file_nm + ' uploaded' = sa.desc_txt ");
		sql.append("and sa.create_dt - '1 minute'::interval < pd.create_dt and sa.create_dt + '1 minute'::interval > pd.create_dt ");
		
		sql.append("where sa.ticket_id = ? ");
		if(params.containsKey(ACTIVITY_ID)) {
			sql.append("and sa.activity_id = ? ");
		}

		if(params.containsKey("internalFlg")) {
			sql.append("and sa.internal_flg = ? ");
		}

		sql.append("order by sa.create_dt desc ");
		log.debug(sql);
		return sql.toString();
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
			TicketAttachmentVO attach = new TicketAttachmentVO();
			attach.setActionId(req.getParameter(ProfileDocumentAction.PROFILE_DOC_ID));
			item.addAttachment(attach);
		}
		
		super.buildCallback(req, item);
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
