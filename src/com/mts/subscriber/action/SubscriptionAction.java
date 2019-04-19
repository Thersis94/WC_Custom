package com.mts.subscriber.action;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mts.subscriber.data.SubscriptionUserVO;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: SubscriptionAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> ***Change Me
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
}
