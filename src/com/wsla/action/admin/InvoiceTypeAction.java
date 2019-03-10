package com.wsla.action.admin;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

// WSLA Libs
import com.wsla.data.admin.InvoiceTypeVO;

/****************************************************************************
 * <b>Title</b>: InvoiceTypeAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the invoice Type data
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 24, 2019
 * @updates:
 ****************************************************************************/

public class InvoiceTypeAction extends SBActionAdapter {

	public static final String AJAX_KEY = "invoiceType";
	
	/**
	 * 
	 */
	public InvoiceTypeAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public InvoiceTypeAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		StringBuilder sql = new StringBuilder(64);
		sql.append("select * from ").append(getCustomSchema()).append("wsla_invoice_type ");
		sql.append("order by type_nm");
		log.debug(sql.length() + "|" + sql.toString());
		
		DBProcessor db = new DBProcessor(getDBConnection());
		setModuleData(db.executeSelect(sql.toString(), null, new InvoiceTypeVO()));
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		InvoiceTypeVO it = new InvoiceTypeVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		try {
			if (req.getBooleanParameter("isInsert")) {
				db.insert(it);
			} else {
				db.update(it);
			}
			
			putModuleData(it);
		} catch (Exception e) {
			log.error("Unable to save Invoice Type: " + it.toString(), e);
			putModuleData(it, 1, false, e.getLocalizedMessage(), true);
		}
	}
}

