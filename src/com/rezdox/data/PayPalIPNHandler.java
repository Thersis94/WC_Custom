package com.rezdox.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

// PayPal Core 1.7.2
import com.paypal.ipn.IPNMessage;
import com.rezdox.action.DataEntryBuyEmailAction;
// WC_Custom
import com.rezdox.action.MembershipAction;
import com.rezdox.vo.MembershipVO;
import com.rezdox.vo.SubscriptionVO;
import com.rezdox.vo.TransactionVO;
// SMT BaseLibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
// WebCrescendo 3
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.paymentnotification.PaymentNotificationAction;

/****************************************************************************
 * <p><b>Title</b>: PayPalIPNHandler.java</p>
 * <p><b>Description: </b>Handler for PayPal Instant Payment Notifications.
 * Marks the member's newly purchased memberships as paid.</p>
 * <p> 
 * <p>Copyright: (c) 2018 SMT, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author Tim Johnson
 * @version 1.0
 * @since Feb. 5, 2018
 ****************************************************************************/
public class PayPalIPNHandler extends SimpleActionAdapter {

	private static final String PAYPAL_PAYMENT_TYPE_ID = "9bab9efe5c0643aabd5ed0670d4c9b02";

	/**
	 * Keys we are using from the PayPal IPN API
	 */
	private static final String PAYMENT_STATUS = "payment_status";
	private static final String CUSTOM = "custom";
	private static final String ITEM_NUMBER = "item_number";
	private static final String QUANTITY = "quantity";
	private static final String TRANSACTION_ID = "txn_id";

	/**
	 * Possible values returned in the PayPal IPN API
	 */
	private static final String PAYMENT_STATUS_COMPLETED = "Completed";

	public PayPalIPNHandler() {
		super();
	}

	/**
	 * @param arg0
	 */
	public PayPalIPNHandler(ActionInitVO arg0) {
		super(arg0);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("Running RezDox PayPal IPN Handler");
		IPNMessage ipnMessage = (IPNMessage) req.getAttribute(PaymentNotificationAction.NOTIFICATION_DATA);
		String paymentStatus = ipnMessage.getIpnValue(PAYMENT_STATUS);

		if (PAYMENT_STATUS_COMPLETED.equals(paymentStatus)) {
			// Save the transaction record
			String paypalTransactionCode = ipnMessage.getIpnValue(TRANSACTION_ID);
			TransactionVO transaction = saveTransaction(paypalTransactionCode);

			// Get the paid memberships off the PayPal req, and save as member subscriptions
			String[] arr = ipnMessage.getIpnValue(CUSTOM).split("~");
			String memberId = arr[0];
			String businessId = arr.length > 1 ? StringUtil.checkVal(arr[1], null) : null;
			Map<String, Integer> purchaseQtys = getPurchases(ipnMessage);
			List<MembershipVO> paidMemberships = retrievePaidMemberships(purchaseQtys.keySet());
			saveSubscriptions(memberId, transaction, paidMemberships, purchaseQtys, businessId);
		}
	}


	/**
	 * Gets all the membershipId/qty pairs from the PayPal payment notification
	 * @param ipnMessage
	 * @return
	 */
	private Map<String, Integer> getPurchases(IPNMessage ipnMessage) {
		Map<String, Integer> purchases = new HashMap<>();

		// Scan the data returned by PayPal for the purchsed item/qty pairs
		for (Entry<String, String> entry : ipnMessage.getIpnMap().entrySet()) {
			if (entry.getKey().startsWith(ITEM_NUMBER)) {
				// Get the quantity for the item purchased
				String itemIndex = entry.getKey().substring(ITEM_NUMBER.length());
				int quantity = Convert.formatInteger(ipnMessage.getIpnValue(QUANTITY + itemIndex));

				// Add the item (membershipId) and quantity to the map
				purchases.put(entry.getValue(), quantity);
			}
		}

		return purchases;
	}


	/**
	 * Retrieves the base data for the memberships paid for by the member.
	 * @param membershipIds
	 * @return
	 */
	private List<MembershipVO> retrievePaidMemberships(Set<String> membershipIds) {
		ActionRequest membershipReq = new ActionRequest();
		membershipReq.setParameter(MembershipAction.MEMBERSHIP_ID, membershipIds.toArray(new String[0]), true);

		MembershipAction ma = new MembershipAction(getDBConnection(), getAttributes());
		return ma.retrieveMemberships(membershipReq);
	}


	/**
	 * Saves the member's transaction data
	 * 
	 * @param transactionCode
	 * @return
	 */
	private TransactionVO saveTransaction(String transactionCode) {
		TransactionVO transaction = new TransactionVO();
		transaction.getPaymentType().setPaymentTypeId(PAYPAL_PAYMENT_TYPE_ID);
		transaction.setTransactionCode(transactionCode);

		try {
			new DBProcessor(dbConn, getCustomSchema()).save(transaction);
		} catch (Exception e) {
			log.error("Unable to save RezDox paypal transaction data. ", e);
		}

		return transaction;
	}


	/**
	 * Saves the member's subscriptions for the membership(s) they paid for
	 * @param memberId
	 * @param transaction
	 * @param paidMemberships
	 * @param purchaseQtys
	 */
	private void saveSubscriptions(String memberId, TransactionVO transaction, 
			List<MembershipVO> paidMemberships, Map<String, Integer> purchaseQtys, String businessId) {
		List<SubscriptionVO> subscriptions = new ArrayList<>();

		for (MembershipVO membership : paidMemberships) {
			int paidQty = purchaseQtys.get(membership.getMembershipId());

			SubscriptionVO subscription = new SubscriptionVO();
			subscription.getMember().setMemberId(memberId);
			subscription.setMembership(membership);
			subscription.setTransaction(transaction);
			subscription.setBusinessId(businessId);

			// a single membership may contain more than one unit
			// multiply by the paid quantity to get the total cost & units purchased 
			subscription.setCostNo(membership.getCostNo() * paidQty);
			subscription.setQuantityNo(membership.getQuantityNo() * paidQty);

			// add to the list to be saved in a batch
			subscriptions.add(subscription);
		}

		try {
			new DBProcessor(dbConn, getCustomSchema()).executeBatch(subscriptions);
		} catch (Exception e) {
			log.error("Unable to save paid RezDox subscription data. ", e);
		}
		
		List<String> dataEntryCodes = new ArrayList<>();
		dataEntryCodes.add("DATA_ENTRY5");
		dataEntryCodes.add("DATA_ENTRY15");
		dataEntryCodes.add("DATA_ENTRY25");
		dataEntryCodes.add("DATA_ENTRY50");
		dataEntryCodes.add("DATA_ENTRY100");
		
		for (SubscriptionVO svo : subscriptions) {
			if(dataEntryCodes.contains(svo.getMembershipId())) {
				DataEntryBuyEmailAction dataEntryEmail = new DataEntryBuyEmailAction(getDBConnection(),getAttributes());
				dataEntryEmail.sendEmail(svo);
			}
		}
	}
}
