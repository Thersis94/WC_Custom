package com.wsla.util;

// JDK 1.8.x
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//SMT Base Libs
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.io.mail.EmailRecipientVO;
import com.siliconmtn.io.mail.EmailMessageVO.Header;
import com.siliconmtn.sb.email.util.EmailCampaignBuilderUtil;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.workflow.data.WorkflowModuleVO;
import com.siliconmtn.workflow.modules.AbstractWorkflowModule;
import com.smt.sitebuilder.common.constants.Constants;

// WSLA Libs
import com.wsla.action.BasePortalAction;
import com.wsla.action.admin.StatusCodeAction;
import com.wsla.action.ticket.TicketEditAction;
import com.wsla.common.WSLAConstants;
import com.wsla.common.WSLAConstants.WSLARole;
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
	public static final String TICKET_ID = "ticketId";
	
	public static final String WSLA_END_CUSTOMER = "WSLA_END_CUSTOMER";
	private List<String> wslaRoles = new ArrayList<>();
	
	/**
	 * @param config
	 * @param conn
	 * @throws Exception
	 */
	public NotificationWorkflowModule(WorkflowModuleVO mod, Connection conn, String schema) throws Exception {
		super(mod, conn, schema);
		wslaRoles.add(WSLARole.ADMIN.getRoleId());
		wslaRoles.add(WSLARole.WSLA_CALL_CENTER.getRoleId());
		wslaRoles.add(WSLARole.WSLA_CUSTOMER_SVC.getRoleId());
		wslaRoles.add(WSLARole.WSLA_EXECUTIVE.getRoleId());
		wslaRoles.add(WSLARole.WSLA_WAREHOUSE.getRoleId());
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
		BasePortalAction bpa = new BasePortalAction(getConnection(), attributes);
		List<UserVO> users = new ArrayList<>();
		
		// If the role is an end customer, send one email to the originator user on the ticket.
		if (notification.getRoles().contains(WSLA_END_CUSTOMER) && !StringUtil.isEmpty(ticket.getTicketIdText())) {
			UserVO user = bpa.getUser(ticket.getUserId());
			if (StringUtil.checkVal(user.getLocale(), "es_MX").equals(notification.getLocale())) {
				users.add(user);
			}
		}
		
		// Additionally, send an email to all users in the system with the given role and relation to the ticket.
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
		String schema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		
		List<String> roles = notification.getRoles();
		
		// Ensure we don't mess with the original list
		List<String> wRoles = new ArrayList<>();
		wRoles.addAll(notification.getRoles());
		wRoles.retainAll(wslaRoles);
		
		// Build the sql
		StringBuilder sql = new StringBuilder(768);
		sql.append(DBUtil.SELECT_CLAUSE).append("b.user_id, email_address_txt, b.profile_id, locale_txt ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_provider_user_xr a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_user b on a.user_id = b.user_id ");
		sql.append(DBUtil.INNER_JOIN).append("profile_role c on b.profile_id = c.profile_id ");
		sql.append(DBUtil.WHERE_CLAUSE).append("location_id in (?, ?) "); 
		sql.append("and role_id in (").append(DBUtil.preparedStatmentQuestion(roles.size())).append(") ");
		sql.append("and locale_txt = ? ");
		sql.append(DBUtil.UNION);
		sql.append(DBUtil.SELECT_CLAUSE).append("c.user_id, email_address_txt, c.profile_id, locale_txt ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_provider_location a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_provider_user_xr b on a.location_id = b.location_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_user c on b.user_id = c.user_id ");
		sql.append(DBUtil.INNER_JOIN).append("profile_role d on c.profile_id = d.profile_id ");
		sql.append(DBUtil.WHERE_CLAUSE).append("provider_id = ? and role_id in (");
		sql.append(DBUtil.preparedStatmentQuestion(roles.size())).append(") and locale_txt = ? ");
		if( ! wRoles.isEmpty()) {
			sql.append(DBUtil.UNION);
			sql.append(DBUtil.SELECT_CLAUSE).append("a.user_id, email_address_txt, a.profile_id, locale_txt ");
			sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_user a ");
			sql.append(DBUtil.INNER_JOIN).append("profile_role b on a.profile_id = b.profile_id and site_id = 'WSLA_1' ");
			sql.append(DBUtil.WHERE_CLAUSE).append("role_id in (");
			sql.append(DBUtil.preparedStatmentQuestion(wRoles.size())).append(") and locale_txt = ? ");
		}
		
		
		// Add the parameters
		List<Object> vals = new ArrayList<>();
		vals.add(ticket.getRetailerId());
		vals.add(ticket.getCas().getLocationId());
		vals.addAll(roles);
		vals.add(notification.getLocale());
		vals.add(ticket.getOemId());
		vals.addAll(roles);
		vals.add(notification.getLocale());
		if( ! wRoles.isEmpty()) {
			vals.addAll(wRoles);
			vals.add(notification.getLocale());
		}
		// Return the data
		DBProcessor db = new DBProcessor(getConnection());
		db.setGenerateExecutedSQL(true);
		List<UserVO> users = db.executeSelect(sql.toString(), vals, new UserVO());
		log.info(db.getExecutedSql());
		return users;
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
		
		// Add additional header param to the data map so the user can reply with comments
		String ticketId = StringUtil.checkVal(mData.get(TICKET_ID));
		String headerValue = StringUtil.join("<", ticketId, "|", user.getUserId(), WSLAConstants.TICKET_EMAIL_REFERENCE_SUFFIX, ">");
		mData.put(Header.IN_REPLY_TO.toString(), headerValue);
		mData.put(Header.REFERENCES.toString(), headerValue);
		
		mData.put("firstName", user.getFirstName());
		mData.put("lastName", user.getLastName());
		mData.put("emailAddress", user.getEmail());
		
		TicketEditAction tea = new TicketEditAction(getConnection(), attributes);
		try {
			TicketVO ticket = tea.getCompleteTicket(ticketId);
			if(ticket.getOem() != null) {
				mData.put("providerName", StringUtil.checkVal(ticket.getOem().getProviderName()));
			}else {
				mData.put("providerName", "");
			}
			
			if( ticket.getProductSerial() != null && ticket.getProductSerial().getProduct() != null) {
				mData.put("productName", StringUtil.checkVal(ticket.getProductSerial().getProduct().getProductName()));
			}else {
				mData.put("productName", "");
			}
			
			if(ticket.getStatus() != null) {
				mData.put("groupStatusCode", StringUtil.checkVal(ticket.getStatus().getGroupStatusCode()));
				mData.put("statusName", StringUtil.checkVal(ticket.getStatus().getStatusName()));
				mData.put("statusCd",  StringUtil.checkVal(ticket.getStatus()));
			}else {
				mData.put("groupStatusCode", "");
				mData.put("statusName", "");
				mData.put("statusCd",  "");
			}
			
		} catch (DatabaseException e) {
			log.error("could not get ticket ",e);
		}
		
		if(user.getProfile() != null && user.getProfile().getLocation() != null ) {
			mData.put("addressText", StringUtil.checkVal(user.getProfile().getLocation().getAddress()));
			mData.put("address2Text", StringUtil.checkVal(user.getProfile().getLocation().getAddress2()));
			mData.put("cityName", StringUtil.checkVal(user.getProfile().getLocation().getCity()));
			mData.put("stateCd", StringUtil.checkVal(user.getProfile().getLocation().getState()));
			mData.put("zipCode", StringUtil.checkVal(user.getProfile().getLocation().getZipCode()));
		}else {
			mData.put("addressText", "");
			mData.put("address2Text", "");
			mData.put("cityName", "");
			mData.put("stateCd", "");
			mData.put("zipCode", "");
		}
		
		util.sendMessage(mData, rcpts, notification.getCampaignInstanceId());
	}
}
