package com.rezdox.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.rezdox.vo.BusinessVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.tools.NotificationLogUtil;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.MessageParser;
import com.smt.sitebuilder.util.MessageParser.MessageType;

/****************************************************************************
 * <p><b>Title</b>: RezDoxNotifier.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2018, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Apr 24, 2018
 * <b>Changes:</b>
 ****************************************************************************/
public class RezDoxNotifier {

	private SiteVO site;
	private SMTDBConnection dbConn;
	private String customDbSchema;
	protected static Logger log = Logger.getLogger(RezDoxNotifier.class);

	/**
	 * All of the RezDox Notification Messages, in one nicely packaged bundle
	 */
	public enum Message {
		BLOG_NEW("New Home Improvement Information, Insights & Trends <a target='_blank' href=\"${blogUrl}\">Blog</a> posted"),
		CONNECTION_REQ("${senderName} wants to <a href=\"${url}\">connect</a> with you"),
		CONNECTION_APPRVD("${recipientName} accepted your connection request"),
		RESIDENCE_TRANS_PENDING("A Residence transfer is waiting in your email"),
		RESIDENCE_TRANS_COMPLETE("A Residence transfer you initiated is complete"),
		REWARD_NEW("A new gift card has been added to RezRewards"),
		REWARD_APPRVD("Your RezRewards gift card has been authorized"),
		REVIEW_RCVD("Your business received a <a href=\"${url}\">review</a> from a RezDox user"),
		REFFERAL("Your business has been referred by a RezDox user"),
		PROJ_SHARE("${companyName} wants to share a recently completed project with you"),
		PROJ_SHARE_TO_BIZ("${firstName} ${lastName} wants to share a project completed at ${residenceName}"),
		NEW_RES_PRIVACY("Adjust your privacy settings to your comfort zone."),
		NEW_RES_PIC("Include a picture of your greatest asset."),
		NEW_RES_EQUITY("The RezDox equity calculator captures the value of your home from the inside out in real time."),
		NEW_RES_INVENTORY("Catalog the improvements, renovations, maintenance, and service performed on your home."),
		NEW_RES_LOG("Record your personal possessions in your Home Inventory Log."),
		NEW_RES_CONNECT("Connecty with members & businesses to enjoy the benefits of RexDox' ecosystem."),
		NEW_RES_REWARDS("Watch your reward points grow when you perform activities."),
		NEW_BUS_LOGO("Set up your business profile & add your company's logo."),
		NEW_BUS_SERVICES("Tell RezDox members about the professional services you offer.  Help them Grow Equity"),
		NEW_BUS_STOREFRONT("Build your virtual storefront.  Advertise your business on RezDox."),
		NEW_BUS_INVITE("Invite your customers to join RezDox, giving them visibility to the deals you offer."),
		NEW_BUS_CONNECT("Connect with members.  Link the work you performed to their Home History Log."),
		NEW_BUS_ONLINE("Keep an online digital footprint of the work you performed for your customers."),
		NEW_BUS_REWARDS("Watch your reward points grow."),
		SHARED_RESIDENCE("${senderName} shared their residence \"${residenceName}\" with you"),
		SHARED_BUSINESS("${senderName} shared their business \"${businessName}\" with you"),
		NEW_BUS_PROMOTION("${companyName} has a new special offer. <a href=\"${storefrontUrl}\">Click here</a> for further details.");

		private String msg;
		private Message(String msg) {
			this.msg = msg;
		}
		public String getMessage() { return msg; }
	}


	/**
	 * Default constructor - requires args
	 */
	public RezDoxNotifier(SiteVO site, SMTDBConnection dbConn, String customDbSchema) {
		super();
		this.site = site;
		this.dbConn = dbConn;
		this.customDbSchema = customDbSchema;
	}


	/**
	 * Helper method for RezDox actions to create notification log entries
	 * @param msg
	 * @param params
	 * @param sndrProfileId
	 * @param url
	 * @param rcptProfileIds
	 */
	public void send(Message msg, Map<String, Object> params, String url, String... rcptProfileIds) {
		if (msg == null || rcptProfileIds == null || rcptProfileIds.length == 0) return;
		NotificationLogUtil util = new NotificationLogUtil(dbConn);

		String message = msg.getMessage();
		if (params != null) {
			try {
				message = MessageParser.parse(message, params, msg.name(), MessageType.TEXT);
			} catch (Exception e) {
				//leave the message as-is
			}
		}

		//create a notification for each of the recipients
		util.createNotification(RezDoxUtils.MEMBER_SITE_ID, message, url, null, rcptProfileIds);
	}


	/**
	 * Helper for sending a single notification to all currently-active members.
	 * Basically does a DB lookup of profileIds, then calls send();
	 * @param msg
	 * @param params
	 * @param url
	 */
	public void sendToAllMembers(Message msg, Map<String, Object> params, String url) {
		StringBuilder sql = new StringBuilder(200);
		sql.append("select distinct m.profile_id from ").append(customDbSchema).append("REZDOX_MEMBER m ");
		sql.append(DBUtil.INNER_JOIN).append("PROFILE_ROLE pr on m.profile_id=pr.profile_id and pr.site_id=? and pr.status_id=? ");
		log.debug(sql);

		List<String> profileIds = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, StringUtil.checkVal(site.getAliasPathParentId(), site.getSiteId())); //the user would have a role on our parent site, if we have one
			ps.setInt(2, SecurityController.STATUS_ACTIVE);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				profileIds.add(rs.getString(1));

		} catch (SQLException sqle) {
			log.error("could not load profileIds", sqle);
		}
		if (profileIds.isEmpty()) return;

		log.debug(String.format("sending notification to %d users", profileIds.size()));
		send(msg, params, url, profileIds.toArray(new String[profileIds.size()]));
	}


	/**
	 * Transposes memberId into profileId, then sends the notification
	 * @param msg
	 * @param params
	 * @param url
	 * @param memberId
	 */
	public void sendToMember(Message msg, Map<String, Object> params, String url, String memberId) {
		String profileId = getProfileId(memberId);
		send(msg, params, url, profileId);
	}


	/**
	 * turns a memberId into a profileId
	 * @param parameter
	 * @return
	 */
	private String getProfileId(String memberId) {
		String sql = StringUtil.join("select profile_id as key from ", customDbSchema, "REZDOX_MEMBER where member_id=? limit 1");
		DBProcessor dbp = new DBProcessor(dbConn, customDbSchema);
		List<GenericVO> data = dbp.executeSelect(sql, Arrays.asList(memberId), new GenericVO());
		return (data != null && !data.isEmpty()) ? StringUtil.checkVal(data.get(0).getKey()) : null; 
	}


	/**
	 * cross-lookup profileIds based on either a residenceId or businesId. 
	 * Used from various edge-cases where that's all we have as a starting point
	 * @param entityId
	 * @param isResidence
	 * @return
	 */
	protected String[] getProfileIds(String entityId, boolean isResidence) {
		Set<String> profileIds = new HashSet<>();
		StringBuilder sql = new StringBuilder(200);
		if (isResidence) {
			sql.append("select distinct b.profile_id from ").append(customDbSchema).append("REZDOX_RESIDENCE_MEMBER_XR a ");
			sql.append(DBUtil.INNER_JOIN).append(customDbSchema).append("REZDOX_MEMBER b on a.member_id=b.member_id ");
			sql.append("where a.residence_id=? and a.status_flg=1");
		} else {
			sql.append("select distinct b.profile_id from ").append(customDbSchema).append("REZDOX_BUSINESS_MEMBER_XR a ");
			sql.append(DBUtil.INNER_JOIN).append(customDbSchema).append("REZDOX_MEMBER b on a.member_id=b.member_id ");
			sql.append("where a.business_id=? and a.status_flg=1");
		}
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString()))  {
			ps.setString(1,  entityId);
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				profileIds.add(rs.getString(1));

		} catch (SQLException sqle) {
			log.error("could not load profileIds from businessId", sqle);
		}
		return profileIds.toArray(new String[profileIds.size()]);
	}


	/**
	 * Load profileIds for members connected to the given business, and message them.
	 * @param site
	 * @param business
	 */
	public void notifyConnectedMembers(BusinessVO business, Message msg, Map<String, Object> params, String url) {
		StringBuilder sql = new StringBuilder(300);
		sql.append("select distinct m.profile_id from ").append(customDbSchema).append("REZDOX_MEMBER m ");
		sql.append(DBUtil.INNER_JOIN).append("PROFILE_ROLE pr on m.profile_id=pr.profile_id and pr.site_id=? and pr.status_id=? ");
		sql.append(DBUtil.INNER_JOIN).append(customDbSchema).append("REZDOX_CONNECTION c on ");
		sql.append("(m.member_id=c.sndr_member_id or m.member_id=c.rcpt_member_id) ");
		sql.append("and (c.sndr_business_id=? or c.rcpt_business_id=?) and c.approved_flg=1");
		log.debug(sql);

		List<String> profileIds = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, StringUtil.checkVal(site.getAliasPathParentId(), site.getSiteId()));
			ps.setInt(2, SecurityController.STATUS_ACTIVE);
			ps.setString(3, business.getBusinessId());
			ps.setString(4, business.getBusinessId());
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				profileIds.add(rs.getString(1));

		} catch (SQLException sqle) {
			log.error("could not load profileIds", sqle);
		}
		if (profileIds.isEmpty()) return;

		log.debug(String.format("sending notification to %d users", profileIds.size()));
		send(msg, params, url, profileIds.toArray(new String[profileIds.size()]));
	}
}
