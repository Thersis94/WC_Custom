package com.restpeer.util;

import java.sql.Connection;

import com.restpeer.common.RPConstants;
import com.siliconmtn.commerce.catalog.InvoiceItemVO;
import com.siliconmtn.commerce.catalog.InvoiceVO;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.workflow.data.WorkflowModuleVO;
import com.siliconmtn.workflow.modules.AbstractWorkflowModule;
import com.smt.sitebuilder.action.commerce.ShoppingCartAction;
import com.smt.sitebuilder.action.commerce.product.InvoiceAction;
import com.smt.sitebuilder.action.commerce.product.InvoiceAction.SearchType;
import com.smt.sitebuilder.action.dealer.DealerInfoAction;

/****************************************************************************
 * <b>Title:</b> MembershipWorkflowModule.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Workflow module that manages memberships after a user
 * goes through checkout and makes a payment.
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Jun 10 2019
 * @updates:
 ****************************************************************************/

public class MembershipWorkflowModule extends AbstractWorkflowModule {

	public MembershipWorkflowModule(WorkflowModuleVO mod, Connection conn, String schema) throws Exception {
		super(mod, conn, schema);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.workflow.modules.AbstractWorkflowModule#run()
	 */
	@Override
	protected void run() throws Exception {
		String invoiceId = StringUtil.checkVal(mod.getWorkflowConfig(ShoppingCartAction.INVOICE_ID));
		log.debug("Starting membership workflow module for invoice_id: " + invoiceId);
		
		// Get the invoice
		InvoiceAction ia = new InvoiceAction(getConnection(), attributes);
		InvoiceVO invoice = ia.retrieveFullInvoice(invoiceId, SearchType.MERCHANT);
		
		// Check for a membership product that was purchased
		boolean hasMembership = false;
		for (InvoiceItemVO item : invoice.getItems()) {
			String categoryCode = item.getProductCategoryCode();
			if (categoryCode != null && categoryCode.contains(RPConstants.MEMBERSHIP_CAT)) {
				hasMembership = true;
				break;
			}
		}
		if (!hasMembership) return;
		
		// Set the purchaser's dealer & dealer location to active
		DealerInfoAction dia = new DealerInfoAction(getConnection(), attributes);
		dia.toggleDealerActive(invoice.getDealerId(), 1);
		dia.toggleDealerLocActive(invoice.getDealerLocationId(), 1);
	}

}
