package com.wsla.action.ticket.transaction;

import java.sql.SQLException;
//java 1.8
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

//base libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.RandomAlphaNumeric;
import com.siliconmtn.util.StringUtil;
import com.wsla.action.admin.LogisticsAction;
//wc custom
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.action.ticket.TicketEditAction;
import com.wsla.common.WSLAConstants;
import com.wsla.data.ticket.CreditMemoVO;
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.PartVO;
import com.wsla.data.ticket.RefundReplacementVO;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketAssignmentVO;
import com.wsla.data.ticket.TicketAssignmentVO.TypeCode;
import com.wsla.data.ticket.TicketDataVO;
import com.wsla.data.ticket.TicketLedgerVO;
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
	public static final String TICKET_ATTRIBUTE_CODE = "attr_purchasePrice";

	public enum ApprovalTypes {
		REFUND_DENIED, REFUND_REQUEST, REPLACEMENT_REQUEST;
	}

	public enum DispositionCodes {
		RETURN_REPAIR, RETURN_HARVEST, HARVEST_UNIT, DISPOSE;
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
		log.debug("refund replacement transaction build called ");

		RefundReplacementVO rrvo = new RefundReplacementVO(req);
		UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();

		if (req.getBooleanParameter(CREATE_RAR)) {
			saveRefundRep(rrvo);
		}
		
		ApprovalTypes approvalType = EnumUtil.safeValueOf(ApprovalTypes.class, req.getParameter(APPROVAL_TYPE));
		switch (approvalType) {
			case REPLACEMENT_REQUEST:
				handleReplacementRequest(rrvo, user);
				break;
			case REFUND_REQUEST:
				handleRefundRequest(rrvo, user);
				break;
			case REFUND_DENIED:
				handleRefRepRejection(rrvo, user);
				break;
		}

		putModuleData(rrvo);

	}
	
	/**
	 * Manages the tasks associated to a replacement request.
	 * 
	 * @param rrvo
	 * @param user
	 */
	private void handleReplacementRequest(RefundReplacementVO rrvo, UserVO user) {
		try {
			createReplacementShipment(rrvo);
			processDispostion(rrvo, user);
		} catch (Exception e) {
			log.error("could not create replacement shipment or process dispostion", e);
			putModuleData(rrvo, 0, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * Manages the tasks associated to a refund request.
	 * 
	 * @param rrvo
	 * @param user
	 */
	private void handleRefundRequest(RefundReplacementVO rrvo, UserVO user) {
		try {
			createCreditMemo(rrvo);
			processDispostion(rrvo, user);
		} catch (Exception e) {
			log.error("could not create credit memo or process dispostion", e);
			putModuleData(rrvo, 0, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * Manages the tasks associated to a refund/replacement rejection.
	 * 
	 * @param rrvo
	 * @param user
	 */
	private void handleRefRepRejection(RefundReplacementVO rrvo, UserVO user) {
		try {
			processRejection(rrvo, user);
		} catch (DatabaseException e) {
			log.error("could not write ledger entry or save status", e);
			putModuleData(rrvo, 0, false, e.getLocalizedMessage(), true);
		}
	}

	/**
	 * Creates a shipment for a replacement unit. The shipment becomes pending
	 * if the return of old equipment is required first.
	 * 
	 * @param refRep
	 * @throws SQLException
	 */
	private void createReplacementShipment(RefundReplacementVO refRep) throws SQLException {
		PartVO product = new PartVO();
		product.setProductId(refRep.getReplacementProductId());
		product.setTicketId(refRep.getTicketId());
		product.setQuantity(1);
		product.setSubmitApprovalFlag(1);
		
		DispositionCodes disposition = EnumUtil.safeValueOf(DispositionCodes.class, refRep.getUnitDisposition());
		boolean isPending = disposition == DispositionCodes.RETURN_HARVEST || disposition == DispositionCodes.RETURN_REPAIR;

		LogisticsAction la = new LogisticsAction(getAttributes(), getDBConnection());
		la.saveUnitShipment(refRep.getTicketId(), product, isPending, true);
	}
	
	/**
	 * process control for each of the dispositions
	 * @param rrvo
	 * @param user 
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	private void processDispostion(RefundReplacementVO rrvo, UserVO user) throws Exception {
		TicketVO tvo  = getBaseTicket(rrvo.getTicketId());
		if(tvo == null) throw new InvalidDataException("Unable to find ticket by id ");
		
		if(!UnitLocation.WSLA.name().equalsIgnoreCase(tvo.getUnitLocation().name()) && 
				!UnitLocation.CAS.name().equalsIgnoreCase(tvo.getUnitLocation().name())) {
			changeStatus(rrvo.getTicketId(), user.getUserId(), StatusCode.CAS_ASSIGNED, null, null);
			return;
		}
		
		if(rrvo.getUnitDisposition().equalsIgnoreCase(DispositionCodes.DISPOSE.name())) {
			disposeUnit(rrvo, user, tvo);
		}else if (rrvo.getUnitDisposition().equalsIgnoreCase(DispositionCodes.HARVEST_UNIT.name())) {
			//set to harvest status
			changeStatus(rrvo.getTicketId(), user.getUserId(), StatusCode.HARVEST_APPROVED, null, null);
		}else {
			//is return
			returnUnit(rrvo, user, tvo);
		}
	}

	/**
	 * @param rrvo
	 * @param user
	 * @param tvo
	 * @throws DatabaseException 
	 */
	private void returnUnit(RefundReplacementVO rrvo, UserVO user, TicketVO tvo) throws DatabaseException {
		TicketLedgerVO ledger;
		
		if(UnitLocation.WSLA.name().equalsIgnoreCase(tvo.getUnitLocation().name())) {
			if(rrvo.getUnitDisposition().equalsIgnoreCase(DispositionCodes.RETURN_REPAIR.name())) {
				//set status to repair
				ledger = changeStatus(rrvo.getTicketId(), user.getUserId(), StatusCode.CLOSED, null, null);
				
				DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
				TicketEditAction tea = new TicketEditAction(dbConn, attributes);
				
				//clone a new ticket
				TicketCloneTransaction tct = new TicketCloneTransaction();
				tct.setActionInit(actionInit);
				tct.setAttributes(getAttributes());
				tct.setDBConnection(dbConn);
				
				// Load ticket core data
				TicketVO childTicket = getCloneTicket(db,tea,tvo, tct, user, rrvo);
			
				//set assign the cas as wlsa ware house
				assignWSLACas(childTicket, user, db);
				
				//set the ticket owner element to oem
				TicketDataTransaction tdt = new TicketDataTransaction();
				tdt.setActionInit(actionInit);
				tdt.setAttributes(getAttributes());
				tdt.setDBConnection(dbConn);
				//String ticketId, String attr, String value, boolean overwrite
				try {
					tdt.saveDataAttribute(childTicket.getTicketId(), "attr_ownsTv","OEM", true);
				} catch (SQLException e) {
					log.error("could not set owner to wsla and or the cas to wsla",e);
				}
				
				//set ticket to "repair in progress"
				changeStatus(childTicket.getTicketId(), user.getUserId(), StatusCode.CAS_IN_REPAIR, null, null);
				
			}else {
				//status change for harvest
				ledger = changeStatus(rrvo.getTicketId(), user.getUserId(), StatusCode.HARVEST_APPROVED, null, null);
			}
			buildNextStep(ledger.getStatusCode(), null, false);
		}else {
			//if anywhere other then wsla make a shipment
			TicketPartsTransaction tpt = new TicketPartsTransaction();
			tpt.setActionInit(actionInit);
			tpt.setDBConnection(dbConn);
			tpt.setAttributes(getAttributes());
			try {
				tpt.saveShipment(tvo.getTicketId(), true);
				ledger = changeStatus(tvo.getTicketId(), user.getUserId(), StatusCode.DEFECTIVE_PENDING, LedgerSummary.SHIPMENT_CREATED.summary, null);
				buildNextStep(ledger.getStatusCode(), null, false);
			} catch (Exception e) {
				log.error("could not build shipment ",e);
				putModuleData(tvo, 0, false, e.getLocalizedMessage(), true);
			}
		}
	}

	/**
	 * @param user 
	 * @param childTicket 
	 * @param db 
	 * 
	 */
	private void assignWSLACas(TicketVO childTicket, UserVO user, DBProcessor db) {
		TicketAssignmentTransaction tat = new TicketAssignmentTransaction();
		tat.setActionInit(actionInit);
		tat.setAttributes(getAttributes());
		tat.setDBConnection(dbConn);
		
		TicketAssignmentVO tAss = new TicketAssignmentVO();
		tAss.setTicketId(childTicket.getTicketId());
		tAss.setLocationId(WSLAConstants.DEFAULT_SHIPPING_SRC);
		tAss.setUserId(user.getUserId());
		tAss.setTypeCode(TypeCode.CAS);
		
		try {
			tat.getAssignmentData(tAss, db);
		} catch (Exception e1) {
			log.error("could not save ticket assigment",e1);
		}
		
	}

	/**
	 * @param tvo 
	 * @param tea 
	 * @param db 
	 * @param tct 
	 * @param rrvo 
	 * @param user 
	 * @return
	 */
	private TicketVO getCloneTicket(DBProcessor db, TicketEditAction tea, TicketVO tvo, TicketCloneTransaction tct, UserVO user, RefundReplacementVO rrvo) {
		
		TicketVO childTicket = new TicketVO();
		try {
			childTicket = tct.processTicket(db, tea, tvo.getTicketId());
			childTicket.setTicketData(tct.processTicketData(db, childTicket, tea));
			childTicket.setAssignments(tct.processAssignments(db, childTicket, tea));
			tct.addLedgerEntry(db, user, tvo.getTicketId());
			
		} catch (Exception e) {
			log.error("could not clone ticket",e);
			putModuleData(rrvo, 0, false, e.getLocalizedMessage(), true);
		}
		
		childTicket.setUnitLocation(UnitLocation.WSLA);
		childTicket.setCreateDate(new Date());
		try {
			db.save(childTicket);
		} catch (Exception e1) {
			log.error("could not save cloned ticket",e1);
		}
		
		log.debug("new id is " + childTicket.getTicketId());
		
		return childTicket;
	}

	/**
	 * processes the disposal of a unit
	 * @param rrvo
	 * @param user
	 * @param tvo
	 * @throws DatabaseException 
	 */
	private void disposeUnit(RefundReplacementVO rrvo, UserVO user, TicketVO tvo) throws DatabaseException {
		//dispose of unit status change
		changeStatus(rrvo.getTicketId(), user.getUserId(), StatusCode.DISPOSE_UNIT, null, null);
		
		//save new location
		tvo.setUnitLocation(UnitLocation.DECOMMISSIONED);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			db.save(tvo);
		} catch (InvalidDataException e) {
			log.error("could not save location change to ticket",e);
			putModuleData(rrvo, 0, false, e.getLocalizedMessage(), true);
		}
		
		//after dispose records close ticket.
		changeStatus(rrvo.getTicketId(), user.getUserId(), StatusCode.CLOSED, null, null);
		
	}

	/**
	 * crates and saves the credit memo
	 * @param rrvo
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	public void createCreditMemo(RefundReplacementVO rrvo) throws Exception {
		CreditMemoVO cmvo = new CreditMemoVO();
		cmvo.setCustomerMemoCode(RandomAlphaNumeric.generateRandom(WSLAConstants.TICKET_RANDOM_CHARS).toUpperCase());
		cmvo.setRefundReplacementId(rrvo.getRefundReplacementId());
		cmvo.setRefundAmount(getRefundAmount(rrvo.getTicketId()));
		cmvo.setCreateDate(new Date());
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.save(cmvo);
			
	}

	/**
	 * 
	 * @param ticketId
	 * @return
	 */
	private double getRefundAmount(String ticketId) {
		List<Object> params = new ArrayList<>();
		params.add(ticketId);
		params.add(TICKET_ATTRIBUTE_CODE);

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());

		StringBuilder sql = new StringBuilder(115);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema()).append("wsla_ticket_data td ");
		sql.append("where ticket_id = ? and attribute_cd = ? ");
		
		log.debug( sql );
		log.debug( params );
		
		List<TicketDataVO> data = db.executeSelect(sql.toString(), params, new TicketDataVO());
		
		Double price = 0.0;
		
		if (data != null && data.size() == 1) {
			price = Convert.formatDouble(data.get(0).getValue());
		} 

		return price;
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

	/**
	 * @param ticketId
	 */
	public void processDisposition(String ticketId, ActionRequest req) {
		
		RefundReplacementVO rrvo = getRefRepVoByTicketId(ticketId);
		UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
		try {
			processDispostion(rrvo, user);
		} catch (Exception e) {
			log.error("could not process disposition ",e);
		}
		
	}

	/**
	 * @param ticketId
	 * @return
	 */
	public RefundReplacementVO getRefRepVoByTicketId(String ticketId) {
		List<Object> params = new ArrayList<>();
		params.add(ticketId);

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());

		StringBuilder sql = new StringBuilder(93);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema()).append("wsla_ticket_ref_rep where ticket_id = ? ");

		List<RefundReplacementVO> data = db.executeSelect(sql.toString(), params, new RefundReplacementVO());
		
		if (data != null && !data.isEmpty()) {
			return data.get(0);
		}else {
			return new RefundReplacementVO();
		}
		
	}

	/**
	 * @param creditMemoId
	 * @return
	 */
	public CreditMemoVO getCompleteCreditMemo(String creditMemoId) {
		List<Object> params = new ArrayList<>();
		params.add(creditMemoId);

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());

		StringBuilder sql = new StringBuilder(93);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema()).append("wsla_credit_memo cm ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_ticket_ref_rep rr on cm.ticket_ref_rep_id = rr.ticket_ref_rep_id ");
		sql.append("where credit_memo_id = ? "); 
		
		List<CreditMemoVO> data = db.executeSelect(sql.toString(), params, new CreditMemoVO());
		
		if (data != null && !data.isEmpty()) {
			return data.get(0);
		}else {
			return new CreditMemoVO();
		}
	}

}
