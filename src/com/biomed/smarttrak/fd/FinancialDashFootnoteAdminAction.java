package com.biomed.smarttrak.fd;

import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: FinancialDashFootnoteAdminAction.java<p/>
 * <b>Description: Manages edits of footnote data for the financial dashboard.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Mar 22, 2017
 ****************************************************************************/

public class FinancialDashFootnoteAdminAction extends FinancialDashFootnoteAction {

	public FinancialDashFootnoteAdminAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public FinancialDashFootnoteAdminAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		super.build(req);
		
		FinancialDashFootnoteVO fvo = new FinancialDashFootnoteVO(req);
		DBProcessor dbp = new DBProcessor(dbConn, (String) attributes.get(Constants.CUSTOM_DB_SCHEMA));
		
		try {
			if (req.hasParameter("isDelete")) {
				dbp.delete(fvo);
			} else {
				dbp.save(fvo);
				
				String newId = dbp.getGeneratedPKId();
				if (newId != null)
					fvo.setFootnoteId(newId);
			}
		} catch (Exception e) {
			throw new ActionException("Couldn't create/update financial dash footnote.", e);
		}
		
		Map<String, Object> response = new HashMap<>();
		response.put("footnote", fvo);
		putModuleData(response);
	}
}
