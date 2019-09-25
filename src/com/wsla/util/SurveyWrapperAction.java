package com.wsla.util;

// JDK 1.8.x
import java.util.Arrays;
import java.util.List;

// SMT BAse Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;

// WSLA Libs
import com.wsla.action.ticket.transaction.TicketDataTransaction;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.TicketVO;

/****************************************************************************
 * <b>Title</b>: SurveyWrapperAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> This associates the survey response to a service order
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 19, 2019
 * @updates:
 ****************************************************************************/
public class SurveyWrapperAction extends SimpleActionAdapter {

	/**
	 * 
	 */
	public SurveyWrapperAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public SurveyWrapperAction(ActionInitVO arg0) {
		super(arg0);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		String ticketNumber = req.getParameter("formParam1");
		String surveyId = req.getParameter("fsi");
		if (StringUtil.isEmpty(ticketNumber) || StringUtil.isEmpty(surveyId)) return;
		
		StringBuilder sql = new StringBuilder(64);
		sql.append("select * from ").append(getCustomSchema()).append("wsla_ticket ");
		sql.append("where ticket_no = ?");
		
		DBProcessor db = new DBProcessor(getDBConnection());
		List<TicketVO> tickets = db.executeSelect(sql.toString(), Arrays.asList(ticketNumber), new TicketVO());
		if (tickets == null || tickets.isEmpty()) return;
		TicketVO ticket = tickets.get(0);
		
		try {
			// Add a ledger entry and survey link
			TicketDataTransaction tdt = new TicketDataTransaction(getDBConnection(), getAttributes());
			TicketLedgerVO ledger = tdt.addLedger(ticket.getTicketId(), ticket.getUserId(), null, LedgerSummary.SURVEY_SUBMITTED.summary, null);
			tdt.saveTicketData(null, ledger.getLedgerEntryId(), ticket.getTicketId(), "attr_surveyId", surveyId, null);
		} catch (Exception e) {
			log.error("Unable to link survey to ticket");
		}
	}
}
