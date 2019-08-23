package com.wsla.action.ticket.transaction;

import java.sql.PreparedStatement;
// JDK 1.8.x
import java.sql.SQLException;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.TicketCommentVO;
import com.wsla.data.ticket.TicketCommentVO.ActivityType;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: TicketCommentTransaction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Adds a comment / activity to a ticket
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 29, 2018
 * @updates:
 ****************************************************************************/

public class TicketCommentTransaction extends SBActionAdapter {
	/**
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "activity";
	
	/**
	 * 
	 */
	public TicketCommentTransaction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TicketCommentTransaction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		// Get the WSLA User
		UserVO user = (UserVO)getAdminUser(req).getUserExtendedInfo();
		TicketLedgerVO ledger = new TicketLedgerVO(req);
		ledger.setDispositionBy(user.getUserId());
		
		try {
			TicketCommentVO tcvo = new TicketCommentVO(req);
			addTicketComment(tcvo, ledger);
			
			if (req.getBooleanParameter("endUserReply") && ! StringUtil.isEmpty(tcvo.getParentId())) {
				updateEndUserReplyFlag(tcvo.getParentId());
			}
		} catch(Exception e) {
			log.error("Unable to perform action", e);
			putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * update the parent record when it is from a user comment
	 * @param ticketCommentId
	 * @throws SQLException
	 */
	public void updateEndUserReplyFlag(String ticketCommentId) throws SQLException {
		StringBuilder sql = new StringBuilder(64);
		sql.append("update ").append(getCustomSchema()).append("wsla_ticket_comment ");
		sql.append("set wsla_reply_flg = 1 where ticket_comment_id = ?");
		
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, ticketCommentId);
			ps.executeUpdate();
		}
	}
	
	/**
	 * saves a ticket comment
	 * @param comment
	 * @throws SQLException
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	public void addTicketComment(TicketCommentVO comment, TicketLedgerVO ledger) throws InvalidDataException, DatabaseException {
		
		// Get the DB Processor
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());

		// Add a ledger entry if it isnt a comment		
		if (ledger != null && (!ActivityType.isActivityType(comment.getActivityType()) || ! ActivityType.COMMENT.equals(ActivityType.valueOf(comment.getActivityType())))) {
			ledger.setSummary(LedgerSummary.ACTIVITY_ADDED.summary + ": " + comment.getComment());
			log.debug(ledger);
			db.save(ledger);
		}
		
		// Add the activity / comment to the DB
		if (StringUtil.isEmpty(comment.getParentId())) comment.setParentId(null);
		db.save(comment);
	}
}

