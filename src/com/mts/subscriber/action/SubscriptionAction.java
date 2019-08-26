package com.mts.subscriber.action;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

//WC Custom
import com.mts.subscriber.data.MTSUserVO;
import com.mts.subscriber.data.SubscriptionUserVO;

/****************************************************************************
 * <b>Title</b>: SubscriptionAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Widget tp manage user subscriptions
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Apr 19, 2019
 * @updates:
 ****************************************************************************/
public class SubscriptionAction extends SBActionAdapter {

	/**
	 * Enum for the subscription types
	 */
	public enum SubscriptionType {
		USER("End User"),
		CORPORATE("Corporate Account"),
		IP("IP Address"),
		MULTIPLE("Multi-User Account");

		private String typeName;
		SubscriptionType(String typeName) {  
			this.typeName = typeName;
		}

		public String getTypeName() {	return typeName; }
	}

	/**
	 * 
	 */
	public SubscriptionAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public SubscriptionAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * 
	 * @param dbConn
	 * @param attributes
	 */
	public SubscriptionAction(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super();
		setDBConnection(dbConn);
		setAttributes(attributes);
	}

	/**
	 * Assigns the new set of subscriptions for the given user
	 * @param userId
	 * @param publications
	 * @throws DatabaseException
	 */
	public void assignSubscriptions(String userId, List<String> publications) 
			throws DatabaseException {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());

		// Create the new list of subscriptions
		List<SubscriptionUserVO> subs = new ArrayList<>();
		for (String publicationId : publications) {
			SubscriptionUserVO suvo = new SubscriptionUserVO();
			suvo.setUserId(userId);
			suvo.setPublicationId(publicationId);
			subs.add(suvo);			
		}

		// Delete the existing subscriptions and add the new set
		try {
			deleteSubscriptions(userId);
			db.executeBatch(subs);
		} catch (Exception e) {
			throw new DatabaseException("Unable to update subscriptions", e);
		}
	}

	/**
	 * Deletes the existing subscriptions for a user
	 * @param userId
	 * @throws SQLException
	 */
	private void deleteSubscriptions(String userId) throws SQLException {
		StringBuilder s = new StringBuilder(64);
		s.append("delete from ").append(getCustomSchema());
		s.append("mts_subscription_publication_xr ");
		s.append("where user_id = ?");

		try (PreparedStatement ps = dbConn.prepareStatement(s.toString())) {
			ps.setString(1, userId);
			ps.execute();
		}
	}


	/**
	 * Gets the extended user and subscriber info.  Called from the login modules.
	 * @param authUser
	 */
	public void loadSubscriptions(UserDataVO authUser) {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		List<Object> vals = Arrays.asList(authUser.getProfileId());

		// Build the sql
		StringBuilder sql = new StringBuilder(128);
		sql.append(DBUtil.SELECT_FROM_STAR).append(schema).append("mts_user a "); 
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema);
		sql.append("mts_subscription_publication_xr b on a.user_id=b.user_id ");
		sql.append("where profile_id=?");
		log.debug(sql.length() + "|" + sql + "|" + vals);

		// Get the user extended info and assign it to the user object 
		DBProcessor db = new DBProcessor(dbConn, schema);
		List<MTSUserVO> userPubs = db.executeSelect(sql.toString(), vals, new MTSUserVO());
		MTSUserVO user = (!userPubs.isEmpty()) ? userPubs.get(0) : new MTSUserVO();

		// If the user is an author or admion assign.  Otherwise only assign
		// if expiration date is in the future
		if (user.getActiveFlag() == 0) return;
		if (user.getExpirationDate() == null) user.setExpirationDate(Convert.formatDate("01/01/2000"));
		if ("100".equals(user.getRoleId()) || "AUTHOR".equals(user.getRoleId()) || new Date().before(user.getExpirationDate()))
			authUser.setUserExtendedInfo(user);
	}
}
