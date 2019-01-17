package com.wsla.action.ticket.transaction;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.common.WSLAConstants;
import com.wsla.data.ticket.CreditMemoVO;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.RefundReplacementVO;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketVO;
import com.wsla.data.ticket.TicketVO.UnitLocation;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: RefundReplacementTransaction.java <b>Project</b>: WC_Custom
 * <b>Description: </b> this class handles the micro transactions related to refunds and replacements
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Ryan Riker
 * @version 3.0
 * @since Jan 4, 2019
 * @updates:
 ****************************************************************************/

public class RefundReplacementTransaction extends BaseTransactionAction {

	/**
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "rar";
	public static final String CREATE_RAR = "createRar";
	public static final String APPROVAL_TYPE = "approvalType";
	public static final String DISPOSITION_CODE = "unitDispostion";

	public enum ApprovalTypes {
		REFUND_DENIED, REFUND_REQUEST, REPLACEMENT_REQUEST;
	}

	public enum DispositionCodes {
		RETURN, HARVEST_UNIT, DISPOSE;
	}

	/**
	 * 
	 */
	public RefundReplacementTransaction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public RefundReplacementTransaction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * @param dbConn
	 * @param attributes
	 */
	public RefundReplacementTransaction(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super(dbConn, attributes);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.
	 * ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("#### refund replacement transaction build called ");

		RefundReplacementVO rrvo = new RefundReplacementVO(req);
		UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();

		if (req.getBooleanParameter(CREATE_RAR)) {
			saveRefundRep(rrvo);
		}

		if (ApprovalTypes.REFUND_REQUEST.name().equalsIgnoreCase(req.getParameter(APPROVAL_TYPE))) {
			try {
				createCreditMemo(rrvo);
				processDispostion(rrvo, user);
			} catch (Exception e) {
				log.error("!!! could not create credit memo or process dispostion ",e);
				putModuleData(rrvo, 0, false, e.getLocalizedMessage(), true);
			}
		}

		if (ApprovalTypes.REFUND_DENIED.name().equalsIgnoreCase(req.getParameter(APPROVAL_TYPE))) {
			try {
				processRejection(rrvo, user);
			} catch (DatabaseException e) {
				log.error("!!! could not write ledger entry or save status", e);
				putModuleData(rrvo, 0, false, e.getLocalizedMessage(), true);
			}
		}

		putModuleData(rrvo);

	}

	/**
	 * process control for each of the dispositions
	 * @param rrvo
	 * @param user 
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	private void processDispostion(RefundReplacementVO rrvo, UserVO user) throws DatabaseException, InvalidDataException {
		
		if(rrvo.getUnitDisposition().equalsIgnoreCase(DispositionCodes.DISPOSE.name())) {
			//dispose of unit status change
			changeStatus(rrvo.getTicketId(), user.getUserId(), StatusCode.DISPOSE_UNIT, null, null);
			
			//save new location
			TicketVO tvo  = getBaseTicket(rrvo.getTicketId());
			if(tvo == null) throw new InvalidDataException("Unable to find ticket by id ");
			tvo.setUnitLocation(UnitLocation.DECOMMISSIONED);
			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			log.debug("# save " + tvo);
			try {
				db.save(tvo);
			} catch (InvalidDataException e) {
				log.error("could not save location change to ticket",e);
				putModuleData(rrvo, 0, false, e.getLocalizedMessage(), true);
			}
			
			//after dispose records close ticket.
			changeStatus(rrvo.getTicketId(), user.getUserId(), StatusCode.CLOSED, null, null);
			
		}else if (rrvo.getUnitDisposition().equalsIgnoreCase(DispositionCodes.HARVEST_UNIT.name())) {
			changeStatus(rrvo.getTicketId(), user.getUserId(), StatusCode.HARVEST_REQ, null, null);

		}else {
			//is return
			//TODO return status and ledgers control here
		}

	}

	/**
	 * crates and saves the credit memo
	 * @param rrvo
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	public void createCreditMemo(RefundReplacementVO rrvo) throws Exception {
		log.debug("# start save credit memo ");
		CreditMemoVO cmvo = new CreditMemoVO();
		cmvo.setCustomerMemoCode(RandomAlphaNumeric.generateRandom(WSLAConstants.TICKET_RANDOM_CHARS));
		cmvo.setRefundReplacementId(rrvo.getRefundReplacementId());
		cmvo.setCreateDate(new Date());
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		log.debug("# save " + cmvo);
		db.save(cmvo);
		log.debug("####### post save " + cmvo);
		
	}

	/**
	 * process control for a rejected refund replacement
	 * 
	 * @param rrvo
	 * @param user
	 */
	private void processRejection(RefundReplacementVO rrvo, UserVO user) throws DatabaseException {

		TicketVO tvo = getBaseTicket(rrvo.getTicketId());

		String message = StringUtil.join(LedgerSummary.REFUND_REJECTED.summary, " : ", rrvo.getRejectComment());

		addLedger(rrvo.getTicketId(), user.getUserId(), null, message, null);

		if (tvo != null && tvo.getUnitLocation() != null && "CAS".equalsIgnoreCase(tvo.getUnitLocation().name())) {
			changeStatus(rrvo.getTicketId(), user.getUserId(), StatusCode.PENDING_UNIT_RETURN, null, null);
		} else {
			changeStatus(rrvo.getTicketId(), user.getUserId(), StatusCode.CLOSED, LedgerSummary.TICKET_CLOSED.summary,
					null);
		}

	}

	/**
	 * pulls only the simplest and base level of ticket information without any unneeded joins, 
	 *  might belong in ticket edit action or overview
	 * @param ticketId
	 * @return
	 */
	private TicketVO getBaseTicket(String ticketId) {
		List<Object> params = new ArrayList<>();
		params.add(ticketId);

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());

		StringBuilder sql = new StringBuilder(87);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema());
		sql.append("wsla_ticket ").append(DBUtil.WHERE_CLAUSE).append(" ticket_id = ? ");

		List<TicketVO> data = db.executeSelect(sql.toString(), params, new TicketVO());

		if (data != null && data.size() == 1) {
			return data.get(0);
		} else {
			return null;
		}
	}

	/**
	 * saves the refund replacement data
	 * @param ticketId
	 * @return
	 */
	public RefundReplacementVO saveRefundRep(RefundReplacementVO rrvo) {

		try {
			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			db.save(rrvo);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Unable to save: " + rrvo, e);
			putModuleData(rrvo, 0, false, e.getLocalizedMessage(), true);
		}

		log.info(rrvo);
		return rrvo;
	}

}
