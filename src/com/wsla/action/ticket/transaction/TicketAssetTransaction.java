package com.wsla.action.ticket.transaction;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.EnumUtil;
// WC Libs
import com.wsla.action.ticket.BaseTransactionAction;
import com.wsla.action.ticket.CASSelectionAction;
import com.wsla.action.ticket.TicketEditAction;
import com.wsla.data.ticket.ApprovalCode;
import com.wsla.data.ticket.CreditMemoVO;
// WSLA Libs
import com.wsla.data.ticket.LedgerSummary;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.TicketAssignmentVO;
import com.wsla.data.ticket.TicketAssignmentVO.ProductOwner;
import com.wsla.data.ticket.TicketAssignmentVO.TypeCode;
import com.wsla.data.ticket.TicketDataVO;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.data.ticket.TicketVO;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: TicketAssetTransaction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Micro changes to the asset feature
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 23, 2018
 * @updates:
 ****************************************************************************/

public class TicketAssetTransaction extends BaseTransactionAction {
	/**
	 * Key for the Ajax Controller to utilize when calling this class
	 */
	public static final String AJAX_KEY = "asset";
	
	/**
	 * Approvable asset attribute keys
	 */
	public static final String PROOF_PURCHASE = "attr_proofPurchase";
	public static final String SERIAL_NO = "attr_serialNumberImage";
	
	/**
	 * 
	 */
	public TicketAssetTransaction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TicketAssetTransaction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		try {
			if (req.hasParameter("isApproval")) {
				approveAsset(req);
			} else {
				saveAsset(req);
			}
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Unable to save asset", e);
			putModuleData("", 0, false, e.getLocalizedMessage(), true);
		}
	}
	
	/**
	 * Saves a file asset loaded into the system
	 * @param req
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 */
	public void saveAsset(ActionRequest req) throws InvalidDataException, DatabaseException {
		TicketDataVO td = new TicketDataVO(req);
		td.setApprovalCode(ApprovalCode.PENDING);
		
		// Get the DB Processor
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		UserVO user = null;
	
		// Get the WSLA User
		if(req.hasParameter("userId") && req.hasParameter("publicUserForm")) {
			//coming in from the public user portal the id is on the form.
			user = new UserVO();
			user.setUserId(req.getParameter("userId"));
		}else {
			//coming in from he secure wsla portal the user object is available
			user = (UserVO)getAdminUser(req).getUserExtendedInfo();
		}
		
		
		// Add a ledger entry, but only change status if the uploaded asset
		// triggers a need for approval.
		TicketLedgerVO ledger;
		StatusCode status;
		if (isReadyForApproval(td)) {
			ledger = changeStatus(td.getTicketId(), user.getUserId(), StatusCode.USER_DATA_APPROVAL_PENDING, LedgerSummary.ASSET_LOADED.summary, null);
			status = ledger.getStatusCode();
		} else {
			ledger = addLedger(td.getTicketId(), user.getUserId(), null, LedgerSummary.ASSET_LOADED.summary, null);
			TicketVO ticket = new TicketEditAction(getDBConnection(), getAttributes()).getBaseTicket(td.getTicketId());
			status = ticket.getStatusCode();
		}
		
		
		// Build the next step
		Map<String, Object> params = new HashMap<>();
		params.put("ticketId", ledger.getTicketId());
		buildNextStep(status, params, false);
		
		// Build the additional Ticket Data
		td.setLedgerEntryId(ledger.getLedgerEntryId());
		td.setMetaValue(req.getParameter("fileName"));
		
		db.save(td);

		if("attr_credit_memo".equalsIgnoreCase(req.getParameter("attributeCode"))) {
			log.debug(" save the attribute id in credit memo table");
			saveMemoAssetId(req.getParameter("creditMemoId"), td.getDataEntryId(), req.getDoubleParameter("refundAmount"), req.getParameter("approvedBy"));
			
		}
	}
	
	
	
	/**
	 * this method updates the credit memo id when a new credit memo asset is saved.
	 * @param ticketId
	 * @param dataEntryId
	 */
	private void saveMemoAssetId(String creditMemoId, String dataEntryId, Double refundAmount, String approvedBy) {
		CreditMemoVO cmvo = new CreditMemoVO();
		cmvo.setAssetId(dataEntryId);
		cmvo.setCreditMemoId(creditMemoId);
		cmvo.setRefundAmount(refundAmount);
		cmvo.setApprovedBy(approvedBy);
		cmvo.setApprovalDate(new Date());
		cmvo.setUpdateDate(new Date());
		
		List<String> fields = new ArrayList<>();
		fields.add("asset_id");
		fields.add("refund_amount_no");
		fields.add("approved_by_txt");
		fields.add("approval_dt");
		fields.add("credit_memo_id");
		
		StringBuilder sql = new StringBuilder(93);
		sql.append("update ").append(getCustomSchema()).append("wsla_credit_memo set asset_id = ?, refund_amount_no = ?, approved_by_txt = ?, approval_dt = ?  where credit_memo_id = ? ");
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			db.executeSqlUpdate(sql.toString(), cmvo, fields);
		} catch (DatabaseException e) {
			log.error("could not save asset id to credit memo",e);
			putModuleData(cmvo, 0, false, e.getLocalizedMessage(), true);
		}
	}

	/**
	 * Checks if a status change is warranted on the ticket based on the assets submitted by the user.
	 * The TicketDataVO passed in is assumed to not exist in the db yet.
	 * 
	 * @param td
	 * @return
	 */
	private boolean isReadyForApproval(TicketDataVO td) {
		TicketEditAction tea = new TicketEditAction(getDBConnection(), getAttributes());
		List<TicketDataVO> assets = tea.getExtendedData(td.getTicketId(), "ASSET_GROUP");
		assets.add(td);
		
		// Assume neither type has been submitted yet, unless the owner is a retailer.
		// Retailers aren't required to submit a POP.
		ProductOwner owner = getProductOwnerType(td.getTicketId());
		int popApproval = getDefaultPopApprovalLevel(owner);
		int snApproval = 2;
		
		// Determine the approval levels for each asset type requiring approval
		for (TicketDataVO asset : assets) {
			String attributeCode = asset.getAttributeCode();
			int approvalLevel = asset.getApprovalCode() == null ? ApprovalCode.REJECTED.getLevel() : asset.getApprovalCode().getLevel();
			
			if (PROOF_PURCHASE.equals(attributeCode))
				popApproval = getApprovalLevel(popApproval, approvalLevel);
			else if (SERIAL_NO.equals(attributeCode))
				snApproval = getApprovalLevel(snApproval, approvalLevel);
		}
		
		// Approval is not needed if both are approved or at least one hasn't been submitted
		int approvedLevel = ApprovalCode.APPROVED.getLevel();
		if ((popApproval == approvedLevel && snApproval == approvedLevel) || popApproval == 2 || snApproval == 2)
			return false;
		
		// Approval is needed when one or both are in the UNKNOWN state
		int unknownApprovalLevel = ApprovalCode.PENDING.getLevel();
		return popApproval == unknownApprovalLevel || snApproval == unknownApprovalLevel;
	}
	
	/**
	 * Determines the current owner type
	 * 
	 * @param ticketId
	 * @return
	 */
	private ProductOwner getProductOwnerType(String ticketId) {
		TicketEditAction tea = new TicketEditAction(getDBConnection(), getAttributes());

		TicketVO ticket = new TicketVO();
		ticket.setTicketData(tea.getExtendedData(ticketId, "DATA_GROUP"));
		String ownerAttr = ticket.getTicketDataMap().get("attr_ownsTv").getValue();

		return EnumUtil.safeValueOf(ProductOwner.class, ownerAttr);
	}
	
	/**
	 * Returns the default POP approval level, dependent on the owner
	 * 
	 * @param owner
	 * @return
	 */
	private int getDefaultPopApprovalLevel(ProductOwner owner) {
		return ProductOwner.RETAILER == owner ? ApprovalCode.APPROVED.getLevel() : 2;
	}
	
	/**
	 * Sets the approval level to the most critical level, in the event more than
	 * one asset exists for a given attribute code. (We care most about unknown and
	 * least about rejected.)
	 * 
	 * @param curApprovalLevel
	 * @param newApprovalLevel
	 * @return
	 */
	private int getApprovalLevel (int curApprovalLevel, int newApprovalLevel) {
		if (newApprovalLevel < curApprovalLevel)
			return newApprovalLevel;
		
		return curApprovalLevel;
	}
	
	/**
	 * Manages the flow surrounding approval of an asset
	 * 
	 * @param req
	 * @throws DatabaseException 
	 */
	public void approveAsset(ActionRequest req) throws DatabaseException {
		TicketDataVO td = new TicketDataVO(req);
		td.setUpdateDate(new Date());
		saveApproval(td);

		// Ticket status is only managed when approving/rejecting a POP or SN
		if (PROOF_PURCHASE.equals(td.getAttributeCode()) || SERIAL_NO.equals(td.getAttributeCode())) 
			manageTicketApproval(td.getTicketId(), req);
	}
	
	/**
	 * Determines the approval status of each asset, changing the ticket status if
	 * both types have been reviewed.
	 * 
	 * @param ticketId
	 * @param req
	 * @throws DatabaseException
	 */
	protected void manageTicketApproval(String ticketId, ActionRequest req) throws DatabaseException {
		TicketEditAction tea = new TicketEditAction(getDBConnection(), getAttributes());
		List<TicketDataVO> assets = tea.getExtendedData(ticketId, "ASSET_GROUP");
		Map<String, ApprovalCode> approvals = new HashMap<>();
		
		// Retailers aren't required to submit a POP
		if (ProductOwner.RETAILER == getProductOwnerType(ticketId))
			approvals.put(PROOF_PURCHASE, ApprovalCode.APPROVED);
		
		// Determine if we have approval for each required asset
		for (TicketDataVO asset : assets) {
			String attributeCode = asset.getAttributeCode();
			ApprovalCode thisApproval = asset.getApprovalCode() == null ? ApprovalCode.PENDING : asset.getApprovalCode();
			if (thisApproval == ApprovalCode.PENDING) continue;
			
			// Approved always overrides any previous that were rejected
			ApprovalCode prevApproval = approvals.get(attributeCode);
			if (prevApproval == null || prevApproval == ApprovalCode.REJECTED) 
				approvals.put(attributeCode, thisApproval);
		}
		
		// Manage the status based on approval
		if (approvals.get(PROOF_PURCHASE) != null && approvals.get(SERIAL_NO) != null) {
			boolean popHasApproval = !approvals.get(PROOF_PURCHASE).isRejected();
			boolean snHasApproval = !approvals.get(SERIAL_NO).isRejected();
			finalizeApproval(req, popHasApproval && snHasApproval);
		}
	}
	
	/**
	 * Updates approval/rejection status for an individual asset
	 * 
	 * @param td
	 * @throws DatabaseException
	 */
	protected void saveApproval(TicketDataVO td) throws DatabaseException {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		// Create the SQL for updating the record
		StringBuilder sql = new StringBuilder(150);
		sql.append("update ").append(getCustomSchema()).append("wsla_ticket_data " );
		sql.append("set approval_cd = ?, meta_value1_txt = ?, update_dt = ? ");
		sql.append("where data_entry_id = ? ");
		log.debug(sql);
		
		// Set the fields we are updating from
		List<String> fields = Arrays.asList("approval_cd", "meta_value1_txt", "update_dt", "data_entry_id");

		// Save the approval to the record
		try {
			db.executeSqlUpdate(sql.toString(), td, fields);
		} catch (DatabaseException e1) {
			log.error("Could not update the asset approval status",e1);
		}
	}
	
	/**
	 * Approves the assets, and moves the ticket to the next status
	 * 
	 * @param req
	 * @throws DatabaseException 
	 */
	public void finalizeApproval(ActionRequest req, boolean isApproved) throws DatabaseException {
		StatusCode status = isApproved ? StatusCode.USER_DATA_COMPLETE : StatusCode.USER_CALL_DATA_INCOMPLETE;
		String summary = isApproved ? LedgerSummary.ASSET_APPROVED.summary : LedgerSummary.ASSET_REJECTED.summary;
		
		// Update the status based on approval or rejection.
		TicketVO ticket = new TicketVO(req);
		UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
		TicketLedgerVO ledger = changeStatus(ticket.getTicketId(), user.getUserId(), status, summary, null);
		buildNextStep(ledger.getStatusCode(), null, false);
		if (!isApproved) return;

		// Assign the nearest CAS
		CASSelectionAction csa = new CASSelectionAction(getDBConnection(), getAttributes());
		List<GenericVO> locations = csa.getUserSelectionList(ticket.getTicketId(), user.getLocale());
		if (!locations.isEmpty()) {
			GenericVO casLocation = locations.get(0);
			
			TicketAssignmentVO tAss = new TicketAssignmentVO(req);
			tAss.setLocationId(casLocation.getKey().toString());
			tAss.setTypeCode(TypeCode.CAS);

			try {
				TicketAssignmentTransaction tat = new TicketAssignmentTransaction(getDBConnection(), getAttributes());
				tat.assign(tAss, user);
				setNextStep(tat.getNextStep());
			} catch (InvalidDataException | SQLException e) {
				throw new DatabaseException(e);
			}
		}
	}
}

