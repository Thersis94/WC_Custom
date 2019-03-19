package com.wsla.util;

// JDK 1.8.x
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.db.DBUtil;
// SMT Base Libs
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.sb.email.util.EmailCampaignBuilderUtil;
import com.siliconmtn.sb.email.vo.EmailRecipientVO;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.workflow.data.WorkflowModuleVO;
import com.siliconmtn.workflow.modules.AbstractWorkflowModule;

// WSLA Libs
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
	private List<String> wslaRoles = new ArrayList<>();
	
	/**
	 * @param config
	 * @param conn
	 * @throws Exception
	 */
	public NotificationWorkflowModule(WorkflowModuleVO mod, Connection conn, String schema) throws Exception {
		super(mod, conn, schema);
		wslaRoles.add("100");
		wslaRoles.add("CUSTOMER_SERVICE");
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
		
		// Get the ticket info for the status/notification
		String ticketIdText = StringUtil.checkVal(mod.getWorkflowConfig(TICKET_ID_TEXT));
		TicketEditAction tea = new TicketEditAction(getConnection(), attributes);
		TicketVO ticket = tea.getBaseTicket(ticketIdText);
		ticket.setAssignments(tea.getAssignments(ticket.getTicketId()));
		
		// Loop the notifications and send to marketing campaigns
		for (StatusNotificationVO notification : notifications) {
			sendMails(notification, ticket);
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
	protected void sendMails(StatusNotificationVO notification, TicketVO ticket) throws SQLException {
		
		// If the role is an end customer, send ONE email to the originator user on the ticket.
		// Otherwise, send an email to all users in the system with the given role.

		BasePortalAction bpa = new BasePortalAction(getConnection(), attributes);
		List<UserVO> users = new ArrayList<>();
		
		if (WSLA_END_CUSTOMER.equals(notification.getRoleId()) && !StringUtil.isEmpty(ticket.getTicketIdText())) {
			users.add(bpa.getUser(ticket.getUserId()));
		}
		
		// Get the other users assigned to the roles
		users.addAll(getUsersByRole(notification, ticket));
		
		// Send the emails to each user
		for (UserVO user : users) {
			sendLocaleEmail(user, notification);
		}
	}
	
	/**
	 * Grabs the users for the given ticket and role
	 * @param notification
	 * @param ticket
	 * @return
	 * @throws SQLException
	 */
	public List<UserVO> getUsersByRole(StatusNotificationVO notification, TicketVO ticket) 
	throws SQLException {
		List<String> roles = notification.getRoles();
		List<String> wRoles = notification.getRoles();
		wRoles.retainAll(wslaRoles);
		
		StringBuilder sql = new StringBuilder(768);
		sql.append("select email_address_txt, b.profile_id, locale_txt ");
		sql.append("from wsla_provider_user_xr a ");
		sql.append("inner join wsla_user b on a.user_id = b.user_id ");
		sql.append("inner join profile_role c on b.profile_id = c.profile_id ");
		sql.append("where location_id in (?, ?) "); 
		sql.append("and role_id in (").append(DBUtil.preparedStatmentQuestion(roles.size()));
		sql.append(" and locale_txt = ? ");
		sql.append("union ");
		sql.append("select email_address_txt, c.profile_id, locale_txt ");
		sql.append("from wsla_provider_location a ");
		sql.append("inner join wsla_provider_user_xr b on a.location_id = b.location_id ");
		sql.append("inner join wsla_user c on b.user_id = c.user_id ");
		sql.append("inner join profile_role d on c.profile_id = d.profile_id ");
		sql.append("where provider_id = ? and role_id in (");
		sql.append(DBUtil.preparedStatmentQuestion(roles.size())).append(") and locale_txt = ? ");
		sql.append("union ");
		sql.append("select email_address_txt, a.profile_id, locale_txt ");
		sql.append("from wsla_user a ");
		sql.append("inner join profile_role b on a.profile_id = b.profile_id ");
		sql.append("where role_id in (");
		sql.append(DBUtil.preparedStatmentQuestion(wRoles.size())).append(") and locale_txt = ? ");
		
		List<Object> vals = new ArrayList<>();
		vals.add(ticket.getRetailerId());
		vals.add(ticket.getAssignments().get(0).getLocationId()); // CAS ID
		vals.add(notification.getLocale());
		vals.addAll(roles);
		vals.add(ticket.getOemId());
		vals.addAll(roles);
		vals.add(notification.getLocale());
		vals.addAll(wRoles);
		vals.add(notification.getLocale());
		
		DBProcessor db = new DBProcessor(getConnection());
		return db.executeSelect(sql.toString(), vals, new UserVO());
	}
	
	/**
	 * Sends an appropriate locale-based email to the given user.
	 * 
	 * @param user
	 * @param roleNotifications
	 * @throws SQLException 
	 */
	private void sendLocaleEmail(UserVO user, StatusNotificationVO notification) throws SQLException {
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
}
