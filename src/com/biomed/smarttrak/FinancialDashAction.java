package com.biomed.smarttrak;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: FinancialDashAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Tim Johnson
 * @version 1.0
 * @since Jan 04, 2017
 ****************************************************************************/

public class FinancialDashAction extends SBActionAdapter {

	public FinancialDashAction() {
		super();
	}

	public FinancialDashAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	public void delete(SMTServletRequest req) throws ActionException {
		super.delete(req);
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		
		String displayType = StringUtil.checkVal(req.getParameter("displayType"), "CURYR");
		Integer calendarYear = Convert.formatInteger(req.getParameter("calendarYear"), Convert.getCurrentYear());

		FinancialDashVO dash = new FinancialDashVO();
		dash.setColHeaders(displayType, calendarYear);
		dash.setTempData();

		this.putModuleData(dash);
	}
	
	public void build(SMTServletRequest req) throws ActionException {
		super.build(req);
		
		String priKey = StringUtil.checkVal(req.getParameter("pk"));
		String updateValue = StringUtil.checkVal(req.getParameter("value")); 
		String fieldName = StringUtil.checkVal(req.getParameter("name"));
		
		
	}
	
	public void list(SMTServletRequest req) throws ActionException {
		super.list(req);
	}

	public void update(SMTServletRequest req) throws ActionException {
		super.update(req);
	}
}
