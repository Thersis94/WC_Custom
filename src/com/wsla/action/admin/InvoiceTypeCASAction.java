package com.wsla.action.admin;

// JDK 1.8.x
import java.util.Arrays;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.admin.InvoiceTypeCasVO;

/****************************************************************************
 * <b>Title</b>: InvoiceTypeCASAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the invoice Type data and provider cross reference
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 24, 2019
 * @updates:
 ****************************************************************************/

public class InvoiceTypeCASAction extends SBActionAdapter {

	public static final String AJAX_KEY = "invoiceCASType";
	
	/**
	 * 
	 */
	public InvoiceTypeCASAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public InvoiceTypeCASAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {

		StringBuilder sql = new StringBuilder(256);
		sql.append("select a.*, invoice_cas_xr_id, provider_id, amount_no from ");
		sql.append(getCustomSchema()).append("wsla_invoice_type a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(getCustomSchema()).append("wsla_invoice_cas_xr b ");
		sql.append("on a.invoice_type_cd = b.invoice_type_cd and provider_id = ? ");
		sql.append("order by type_nm");
		log.debug(sql.length() + "|" + sql.toString());
		
		List<Object> vals = Arrays.asList(req.getParameter("providerId"));
		DBProcessor db = new DBProcessor(getDBConnection());
		setModuleData(db.executeSelect(sql.toString(), vals, new InvoiceTypeCasVO(), "invoice_type_cd"));
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		InvoiceTypeCasVO vo = new InvoiceTypeCasVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		try {
			db.save(vo);
			setModuleData(vo);
		} catch(Exception e) {
			log.error("Unable to save invoice amount for: " + vo.toString(), e);
			setModuleData(vo, 0, e.getLocalizedMessage());
		}
		
		log.info("Saving: " + vo);
	}
}

