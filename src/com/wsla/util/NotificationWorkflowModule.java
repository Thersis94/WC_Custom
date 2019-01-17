package com.wsla.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.siliconmtn.sb.email.util.EmailCampaignBuilderUtil;
import com.siliconmtn.sb.email.vo.EmailRecipientVO;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.workflow.data.WorkflowModuleVO;
import com.siliconmtn.workflow.modules.AbstractWorkflowModule;
import com.wsla.action.BasePortalAction;
import com.wsla.action.admin.StatusCodeAction;
import com.wsla.action.ticket.TicketEditAction;
import com.wsla.data.ticket.StatusNotificationVO;
import com.wsla.data.ticket.TicketVO;
import com.wsla.data.ticket.UserVO;



/********************************************************************
 * <b>Title:</b> NotificationWorkflowModule.java<br/>
 * <b>Description:</b> Workflow module to send notification emails.<br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies
 * @author Tim Johnson
 * @version 3.0
 * @since Nov 12, 2018
 *******************************************************************/
public class NotificationWorkflowModule extends AbstractWorkflowModule {

	// Constants for Workflow Config Param Mappings.
	public static final String TICKET_ID_TEXT = "ticketIdText";
	public static final String USER_ID = "userId";
	public static final String STATUS_CODE = "statusCode";
	public static final String EMAIL_DATA = "emailData";
	
	public static final String WSLA_END_CUSTOMER = "WSLA_END_CUSTOMER";

	/**
	 * @param config
	 * @param conn
	 * @throws Exception
	 */
	public NotificationWorkflowModule(WorkflowModuleVO mod, Connection conn, String schema) throws Exception {
		super(mod, conn, schema);
	}

	/* (non-Javadoc)
	 * @see com.ram.workflow.modules.WorkflowModuleIntfc#run()
	 */
	@Override
	protected final void run() throws Exception {
		String statusCode = mod.getWorkflowConfig(STATUS_CODE).toString();
		
		// Get all the notification emails to be sent
		StatusCodeAction sca = new StatusCodeAction(getConnection(), attributes);
		List<StatusNotificationVO> notifications = sca.getNotifications(statusCode);
		
		// Get a distinct list of roles we are sending to for this status
		Set<String> roleIds = new HashSet<>();
		for (StatusNotificationVO notification : notifications) {
			roleIds.add(notification.getRoleId());
		}
		
		// Send mails based on the role and each individual user's locale
		for (String roleId : roleIds) {
			sendMails(roleId, notifications);
		}
	}
	
	/**
	 * Sends appropriate role-based and locale-based emails to the appropriate
	 * users for the given status code passed to the workflow module.
	 * 
	 * @param roleId
	 * @param notifications
	 * @throws SQLException
	 */
	protected void sendMails(String roleId, List<StatusNotificationVO> notifications) throws SQLException {
		String ticketIdText = StringUtil.checkVal(mod.getWorkflowConfig(TICKET_ID_TEXT));
		Map<String, StatusNotificationVO> roleNotifications = getNotifications(roleId, notifications);
		
		// If the role is an end customer, send ONE email to the originator user on the ticket.
		// Otherwise, send an email to all users in the system with the given role.
		BasePortalAction bpa = new BasePortalAction(getConnection(), attributes);
		List<UserVO> users = new ArrayList<>();
		if (WSLA_END_CUSTOMER.equals(roleId) && !StringUtil.isEmpty(ticketIdText)) {
			TicketEditAction tea = new TicketEditAction(getConnection(), attributes);
			TicketVO ticket = tea.getBaseTicket(ticketIdText);
			users.add(bpa.getUser(ticket.getUserId()));
		} else if(!WSLA_END_CUSTOMER.equals(roleId)) {
			users = bpa.getUsersByRole(roleId);
		}
		// Send the emails to each user
		for (UserVO user : users) {
			sendLocaleEmail(user, roleNotifications);
		}
	}
	
	/**
	 * Sends an appropriate locale-based email to the given user.
	 * 
	 * @param user
	 * @param roleNotifications
	 * @throws SQLException 
	 */
	private void sendLocaleEmail(UserVO user, Map<String, StatusNotificationVO> roleNotifications) throws SQLException {
		// If a notification exists for the user's locale & role, then use it.
		// Otherwise we just send what has been setup.
		StatusNotificationVO notification = roleNotifications.get(user.getLocale());
		if (notification == null)
			notification = roleNotifications.entrySet().iterator().next().getValue();
		
		// Set the email recipient's data
		List<EmailRecipientVO> rcpts = new ArrayList<>();
		rcpts.add(new EmailRecipientVO(user.getProfileId(), user.getEmail(), EmailRecipientVO.TO));
		
		// Send the email to the user
		EmailCampaignBuilderUtil util = new EmailCampaignBuilderUtil(getConnection(), attributes);

		@SuppressWarnings("unchecked")
		Map<String, Object> mData = (Map<String, Object>) mod.getWorkflowConfig(NotificationWorkflowModule.EMAIL_DATA);
		
		if (mData == null || mData.size() == 0 ) {
			mData = new HashMap<>();
		}
		
		util.sendMessage(mData, rcpts, notification.getCampaignInstanceId());
	}
	
	/**
	 * Get's all notifications for a given role, and adds to a map using the locale
	 * 
	 * @param roleId
	 * @param notifications
	 * @return
	 */
	private Map<String, StatusNotificationVO> getNotifications(String roleId, List<StatusNotificationVO> notifications) {
		Map<String, StatusNotificationVO> roleNotifications = new HashMap<>();
		
		// Get all the notifications for this role, by locale
		for (StatusNotificationVO notification : notifications) {
			if (roleId.equals(notification.getRoleId())) {
				roleNotifications.put(notification.getLocale(), notification);
			}
		}
		
		return roleNotifications;
	}
}
