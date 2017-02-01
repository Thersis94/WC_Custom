package com.sjm.corp.locator.action;

import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;
import com.smt.sitebuilder.action.dealer.DealerLocatorAction;
import com.smt.sitebuilder.db.DatabaseException;

/****************************************************************************
 * <b>Title</b>: AdminClinicAction.java <p/>
 * <b>Project</b>: SB_ANS_Medical <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Mar 18, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class AdminClinicAction extends SBActionAdapter {

	/**
	 * 
	 */
	public AdminClinicAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public AdminClinicAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(ActionRequest req) throws ActionException {
		// Retrieve the clinic data
		String dealerLocationId = StringUtil.checkVal(req.getSession().getAttribute("dealerLocationId"));
		DealerLocatorAction dla = new DealerLocatorAction(this.actionInit);
		dla.setAttributes(attributes);
		dla.setDBConnection(dbConn);
		try {
			List<DealerLocationVO> dealer = dla.getDealerInfo(req, new String[] {dealerLocationId}, null);
			DealerLocationVO vo = new DealerLocationVO();
			if (dealer.size() > 0) vo = dealer.get(0);
			this.putModuleData(vo);
		} catch (DatabaseException e) {
			log.debug("Unable to retrieve the dealer info", e);
		}
	}
}
